import React, { useState, useEffect } from "react";
import { trackingApi as api } from "../../lib/trackingApi";
import TrackingMap from "./LazyTrackingMap";
import TripStatusStepper from "./TripStatusStepper";
import { startVisiblePolling } from "../../lib/visiblePolling";
export default function PublicTripPage({ token }) {
  const [data, set] = useState(null),
    [error, E] = useState("");
  useEffect(() => {
    let active = true;
    set(null);
    E("");
    const poll = startVisiblePolling(
      async (signal) => {
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
        }
      },
      5000,
      {
        onError: (e) => {
          if (active) {
            set(null);
            E(e.message);
          }
        },
      },
    );
    return () => {
      active = false;
      poll.stop();
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
