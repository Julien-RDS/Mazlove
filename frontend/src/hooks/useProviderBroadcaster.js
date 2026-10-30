import { useEffect, useState } from "react";
import { wsClient } from "../lib/ws";
import { meters } from "../lib/geo";
export function useProviderBroadcaster(id, token, enabled) {
  const [error, setError] = useState("");
  useEffect(() => {
    if (!enabled) return;
    let watch,
      lock,
      last = null,
      sent = 0,
      seq = Date.now(),
      latest = null,
      alive = true;
    const wake = async () => {
      if (!alive || document.hidden) return;
      try {
        const next = await navigator.wakeLock?.request("screen");
        if (alive) lock = next;
        else next?.release();
      } catch {}
    };
    const send = () => {
      if (!latest || !client.connected) return;
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
    const client = wsClient(token, { onConnect: send });
    client.activate();
    if (!navigator.geolocation)
      setError(
        "La géolocalisation nécessite HTTPS et un navigateur compatible.",
      );
    else
      watch = navigator.geolocation.watchPosition(
        (p) => {
          if (p.coords.accuracy > 100) {
            setError("GPS faible : recherchez un endroit dégagé.");
            return;
          }
          setError("");
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
        (e) =>
          setError(
            e.code === 1
              ? "Autorisez la localisation dans les réglages du navigateur."
              : "Position indisponible. Gardez l’application ouverte.",
          ),
        { enableHighAccuracy: true, maximumAge: 1000, timeout: 10000 },
      );
    const timer = setInterval(send, 1000);
    const visible = () => {
      if (document.hidden)
        setError("Gardez l’application ouverte au premier plan.");
      else wake();
    };
    document.addEventListener("visibilitychange", visible);
    wake();
    return () => {
      alive = false;
      clearInterval(timer);
      if (watch !== undefined) navigator.geolocation.clearWatch(watch);
      lock?.release();
      document.removeEventListener("visibilitychange", visible);
      client.deactivate();
    };
  }, [id, token, enabled]);
  return error;
}
