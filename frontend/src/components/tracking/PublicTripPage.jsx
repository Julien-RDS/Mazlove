import React, { useState, useEffect } from "react";
import { trackingApi as api } from "../../lib/trackingApi";
import TrackingMap from "./LazyTrackingMap";
import TripStatusStepper from "./TripStatusStepper";
export default function PublicTripPage({ token }) {
  const [data, set] = useState(null),
    [error, E] = useState("");
  useEffect(() => {
    let active = true;
    set(null);
    E("");
    let timer,
      controller,
      running = false,
      failures = 0,
      nextAt = 0;
    const schedule = () => {
      clearTimeout(timer);
      if (active && !document.hidden)
        timer = setTimeout(load, Math.max(0, nextAt - Date.now()));
    };
    const load = async () => {
      if (!active || document.hidden || running) return;
      if (Date.now() < nextAt) {
        schedule();
        return;
      }
      running = true;
      controller = new AbortController();
      const signal = controller.signal;
      try {
        const result = await api(
          `/public/trips/${encodeURIComponent(token)}`,
          null,
          "GET",
          undefined,
          { signal },
        );
        if (active && !signal.aborted) {
          set(result);
          E("");
          failures = 0;
          nextAt = Date.now() + 5000;
        }
      } catch (e) {
        if (!active || e.name === "AbortError") return;
        if (e.status === 404 || e.status === 410) {
          set(null);
          E(e.message);
          active = false;
          return;
        }
        if (e.status === 429 || !e.status || e.status >= 500) {
          failures++;
          const retry = e.retryAfter;
          const parsed =
            retry &&
            (/^\d+$/.test(retry)
              ? Number(retry) * 1000
              : Date.parse(retry) - Date.now());
          nextAt =
            Date.now() +
            (Number.isFinite(parsed) && parsed > 0
              ? parsed
              : Math.min(60000, 5000 * 2 ** Math.min(failures, 4)));
          E("Mise à jour retardée. Nouvelle tentative en cours.");
        } else {
          set(null);
          E(e.message);
          nextAt = Date.now() + 30000;
        }
      } finally {
        running = false;
        schedule();
      }
    };
    const visible = () => {
      if (!document.hidden) schedule();
      else clearTimeout(timer);
    };
    document.addEventListener("visibilitychange", visible);
    schedule();
    return () => {
      active = false;
      clearTimeout(timer);
      controller?.abort();
      document.removeEventListener("visibilitychange", visible);
    };
  }, [token]);
  return (
    <main className="tracking-panel">
      <h1>Suivi du trajet partagé</h1>
      {error && <p role="alert">{error}</p>}
      {data && (
        <>
          <h2>{data.providerName}</h2>
          <TripStatusStepper status={data.status} />
          <TrackingMap pos={data.pos} />
          {data.etaSeconds != null && (
            <p>
              Arrivée estimée dans {Math.ceil(data.etaSeconds / 60)} min —{" "}
              {(data.distanceMeters / 1000).toFixed(1)} km.
            </p>
          )}
          <p>Position approximative. Le partage s’arrête à l’arrivée.</p>
        </>
      )}
    </main>
  );
}
