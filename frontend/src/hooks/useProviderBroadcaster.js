import { useEffect, useState } from "react";
import { wsClient } from "../lib/ws";
import { meters } from "../lib/geo";
import { providerLease } from "../lib/providerLease";
export function useProviderBroadcaster(id, token, enabled, providerId) {
  const [error, setError] = useState(""),
    [attempt, setAttempt] = useState(0);
  useEffect(() => {
    if (!enabled || !providerId) return;
    let mounted = true;
    const report = (message) => {
      if (mounted) setError(message);
    };
    setError("");
    const lease = providerLease(`mazlove-gps:${providerId}`, {
      wait: attempt > 0,
      blocked: report,
      transferred: () =>
        report("Le suivi GPS a été transféré vers un autre onglet."),
      start: () => {
        let watch,
          lock,
          waking = false,
          last = null,
          sent = 0,
          seq = Date.now(),
          latest = null,
          alive = true;
        const wake = async () => {
          if (!alive || document.hidden || waking || (lock && !lock.released))
            return;
          waking = true;
          try {
            const next = await navigator.wakeLock?.request("screen");
            if (alive) lock = next;
            else next?.release();
          } catch {
          } finally {
            waking = false;
          }
        };
        const send = () => {
          if (!alive || !latest || !client.connected) return;
          const now = Date.now();
          if (now - latest.ts > 30000) {
            latest = null;
            return;
          }
          if (now - sent < 3000) return;
          if (
            last &&
            meters([last.lng, last.lat], [latest.lng, latest.lat]) <= 5 &&
            now - sent < 10000
          )
            return;
          client.publish({
            destination: `/app/tracking/${id}/position`,
            body: JSON.stringify({ ...latest, seq: ++seq }),
          });
          last = latest;
          sent = now;
          latest = null;
        };
        const client = wsClient(token, {
          onConnect: () => {
            if (alive) send();
          },
          onStompError: () =>
            report(
              "Connexion GPS refusée. Reconnectez-vous si votre session a expiré.",
            ),
        });
        client.activate();
        if (!navigator.geolocation)
          report(
            "La géolocalisation nécessite HTTPS et un navigateur compatible.",
          );
        else
          watch = navigator.geolocation.watchPosition(
            (p) => {
              if (!alive) return;
              if (p.coords.accuracy > 100) {
                report("GPS faible : recherchez un endroit dégagé.");
                return;
              }
              report("");
              latest = {
                lat: p.coords.latitude,
                lng: p.coords.longitude,
                accuracy: p.coords.accuracy,
                heading: p.coords.heading,
                speed: p.coords.speed,
                ts: p.timestamp,
              };
              send();
            },
            (e) => {
              if (alive)
                report(
                  e.code === 1
                    ? "Autorisez la localisation dans les réglages du navigateur."
                    : "Position indisponible. Gardez l’application ouverte.",
                );
            },
            { enableHighAccuracy: true, maximumAge: 1000, timeout: 10000 },
          );
        const timer = setInterval(send, 1000),
          visible = () => {
            if (document.hidden)
              report(
                "Gardez l’application au premier plan pour maintenir le GPS.",
              );
            else wake();
          };
        document.addEventListener("visibilitychange", visible);
        wake();
        return () => {
          alive = false;
          latest = null;
          clearInterval(timer);
          if (watch !== undefined) navigator.geolocation.clearWatch(watch);
          lock?.release();
          document.removeEventListener("visibilitychange", visible);
          client.deactivate();
        };
      },
    });
    return () => {
      mounted = false;
      lease.stop();
    };
  }, [id, token, enabled, providerId, attempt]);
  return {
    error: enabled ? error : "",
    takeover: () => setAttempt((n) => n + 1),
  };
}
