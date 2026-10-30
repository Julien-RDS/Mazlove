import React, { createContext, useContext, useEffect, useState } from "react";
import { trackingApi } from "../../lib/trackingApi";
import { startVisiblePolling } from "../../lib/visiblePolling";
import { SESSION_UPDATED_EVENT } from "../../lib/sessionTracking";
import { useLiveTracking } from "../../hooks/useLiveTracking";
import { useProviderBroadcaster } from "../../hooks/useProviderBroadcaster";
const JourneyContext = createContext(null);
export const useProviderJourney = () => useContext(JourneyContext);
function ActiveJourney({ session, token, providerId, children }) {
  const live = useLiveTracking(session.id, token);
  const state = live.status || session.status;
  const broadcaster = useProviderBroadcaster(
    session.id,
    token,
    state === "EN_ROUTE" && !live.authExpired,
    providerId,
  );
  const value = { ...broadcaster, id: session.id };
  return (
    <JourneyContext.Provider value={value}>
      {state === "EN_ROUTE" && (
        <aside
          className="journey-banner"
          aria-label="Partage de position actif"
        >
          <p>
            📍 Votre position est partagée avec {session.clientName}. Gardez
            l’application ouverte et l’écran allumé.
          </p>
          <a href={`#/seances/${session.id}`}>
            Ouvrir la console / arrêter le partage
          </a>
          {live.authExpired && <p role="alert">{live.error}</p>}
          {broadcaster.error && <p role="alert">{broadcaster.error}</p>}
          {broadcaster.error && (
            <button type="button" onClick={broadcaster.takeover}>
              Reprendre le suivi dans cet onglet
            </button>
          )}
        </aside>
      )}
      {children}
    </JourneyContext.Provider>
  );
}
export default function ProviderJourney({
  token,
  user,
  enabled = true,
  children,
}) {
  const [record, setRecord] = useState({
    token: null,
    session: null,
    error: "",
  });
  useEffect(() => {
    if (!enabled || !token || !user?.providerId) return;
    let alive = true,
      revision = 0;
    const poll = startVisiblePolling(
      async (signal) => {
        const version = revision;
        const session = await trackingApi(
          "/users/me/provider-trip",
          token,
          "GET",
          undefined,
          { signal },
        );
        if (!alive) return;
        if (version !== revision) {
          poll.refresh();
          return;
        }
        setRecord({ token, session, error: "" });
      },
      15000,
      {
        onError: (error) => {
          if (alive) setRecord({ token, session: null, error: error.message });
        },
      },
    );
    const refresh = (e) => {
      revision++;
      if (e.detail?.token === token && e.detail.session)
        setRecord({
          token,
          session:
            e.detail.session.status === "EN_ROUTE" ? e.detail.session : null,
          error: "",
        });
      poll.refresh();
    };
    window.addEventListener(SESSION_UPDATED_EVENT, refresh);
    return () => {
      alive = false;
      poll.stop();
      window.removeEventListener(SESSION_UPDATED_EVENT, refresh);
    };
  }, [token, user?.id, user?.providerId, enabled]);
  const session =
    record.token === token && enabled && user?.providerId
      ? record.session
      : null;
  if (session?.status === "EN_ROUTE")
    return (
      <ActiveJourney
        key={`${user.id}:${session.id}:${token}`}
        session={session}
        token={token}
        providerId={user.id}
      >
        {children}
      </ActiveJourney>
    );
  return (
    <JourneyContext.Provider value={null}>
      {record.token === token && enabled && record.error && (
        <p className="journey-banner" role="alert">
          Le suivi GPS est suspendu : {record.error}
        </p>
      )}
      {children}
    </JourneyContext.Provider>
  );
}
