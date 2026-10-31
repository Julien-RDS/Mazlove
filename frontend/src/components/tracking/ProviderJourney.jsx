import React, { createContext, useContext, useEffect, useState } from "react";
import { trackingApi } from "../../lib/trackingApi";
import { startVisiblePolling } from "../../lib/visiblePolling";
import { SESSION_UPDATED_EVENT } from "../../lib/sessionTracking";
import { useLiveTracking } from "../../hooks/useLiveTracking";
import { useProviderBroadcaster } from "../../hooks/useProviderBroadcaster";
const JourneyContext = createContext(null);
export const useProviderJourney = () => useContext(JourneyContext);
const labels = {
  sharing: "GPS actif",
  starting: "Préparation du GPS",
  locating: "Localisation en cours",
  weak: "Signal GPS faible",
  reconnecting: "Reconnexion en cours",
  denied: "Localisation non autorisée",
  unsupported: "GPS indisponible",
  expired: "Session expirée",
  blocked: "GPS actif dans un autre onglet",
  transferred: "Suivi transféré vers un autre onglet",
  background: "Application en arrière-plan",
  idle: "Suivi en attente",
  error: "Trajet indisponible",
};
export function ProviderGpsStatus({ journey }) {
  if (!journey) return <p role="status">Préparation du suivi…</p>;
  const retry = ["blocked", "transferred", "denied", "error"].includes(
    journey.phase,
  );
  return (
    <div className="journey-state" role="status">
      <strong>{labels[journey.phase] || "GPS indisponible"}</strong>
      {journey.error && <p>{journey.error}</p>}
      {retry && (
        <button type="button" onClick={journey.takeover}>
          {["blocked", "transferred"].includes(journey.phase)
            ? "Reprendre le suivi dans cet onglet"
            : journey.phase === "error"
              ? "Réessayer le chargement du trajet"
              : "Réessayer la localisation"}
        </button>
      )}
    </div>
  );
}
export function JourneyNotice({ currentSessionId }) {
  const journey = useProviderJourney();
  if (
    !journey ||
    (journey.id != null && String(journey.id) === String(currentSessionId))
  )
    return null;
  return (
    <aside className="journey-banner" aria-label="État du suivi GPS">
      <ProviderGpsStatus journey={journey} />
      {journey.id && (
        <a href={`#/seances/${journey.id}`}>
          Trajet vers {journey.clientName || "votre rendez-vous"} →
        </a>
      )}
    </aside>
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
  const available = !!(enabled && token && user?.providerId);
  const session = available && record.token === token ? record.session : null;
  const live = useLiveTracking(session?.id, available ? token : null);
  const travelling =
    session?.status === "EN_ROUTE" &&
    (!live.status || live.status === "EN_ROUTE");
  const broadcaster = useProviderBroadcaster(
    session?.id,
    token,
    travelling && !live.authExpired,
    user?.id,
  );
  useEffect(() => {
    if (!available) return;
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
      30000,
      {
        onError: (error) => {
          if (!alive) return;
          // A temporary REST outage must not tear down a healthy GPS watch or the current screen.
          setRecord((previous) => ({
            token,
            session:
              error.status === 401 || error.status === 403
                ? null
                : previous.token === token
                  ? previous.session
                  : null,
            error: error.message,
          }));
        },
      },
    );
    const refresh = (e) => {
      revision++;
      const changed = e.detail?.token === token ? e.detail.session : null;
      if (changed)
        setRecord((previous) => {
          if (
            changed.viewerRole === "PROVIDER" &&
            changed.status === "EN_ROUTE"
          )
            return { token, session: changed, error: "" };
          if (
            previous.session?.id === changed.id &&
            changed.status !== "EN_ROUTE"
          )
            return { token, session: null, error: "" };
          return previous;
        });
      poll.refresh();
    };
    window.addEventListener(SESSION_UPDATED_EVENT, refresh);
    return () => {
      alive = false;
      poll.stop();
      window.removeEventListener(SESSION_UPDATED_EVENT, refresh);
    };
  }, [available, token, user?.id]);
  const journey = travelling
    ? {
        ...broadcaster,
        id: session.id,
        clientName: session.clientName,
        ...(live.authExpired ? { phase: "expired", error: live.error } : {}),
      }
    : null;
  // Keep the same component tree through departure, arrival, polling and retries.
  return (
    <JourneyContext.Provider
      value={
        journey ||
        (available && record.token === token && record.error
          ? {
              id: null,
              phase: "error",
              error: record.error,
              takeover: () =>
                window.dispatchEvent(new Event(SESSION_UPDATED_EVENT)),
            }
          : null)
      }
    >
      {children}
    </JourneyContext.Provider>
  );
}
