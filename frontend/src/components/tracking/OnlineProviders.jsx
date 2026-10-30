import React, { useState, useEffect } from "react";
import { useOnlineProviders } from "../../hooks/useOnlineProviders";
import { trackingApi as api } from "../../lib/trackingApi";
import TrackingMap from "./TrackingMap";
export default function OnlineProviders({ token }) {
  const providers = useOnlineProviders(token);
  const [online, set] = useState(false),
    [provider, P] = useState(false),
    [error, E] = useState("");
  useEffect(() => {
    if (token)
      api("/users/me", token)
        .then((u) => P(!!u.providerId))
        .catch(() => {});
  }, [token]);
  useEffect(() => {
    if (!online) return;
    let active = true;
    const send = () =>
      navigator.geolocation?.getCurrentPosition(
        (p) => {
          if (active)
            api("/providers/me/online", token, "PUT", {
              online: true,
              lat: p.coords.latitude,
              lng: p.coords.longitude,
            }).catch((e) => E(e.message));
        },
        () => E("Autorisez la localisation pour apparaître en ligne."),
      );
    send();
    const t = setInterval(send, 30000);
    return () => {
      active = false;
      clearInterval(t);
      api("/providers/me/online", token, "PUT", { online: false }).catch(
        () => {},
      );
    };
  }, [online, token]);
  if (!token) return null;
  return (
    <details className="tracking-panel">
      <summary>Prestataires disponibles autour de Paris</summary>
      {provider && (
        <label>
          <input
            type="checkbox"
            checked={online}
            onChange={(e) => set(e.target.checked)}
          />
          En ligne — montrer ma position approximative
        </label>
      )}
      <TrackingMap providers={providers} />
      <p>Positions approximatives, actualisées toutes les 15 secondes.</p>
      <p role="status">{error}</p>
    </details>
  );
}
