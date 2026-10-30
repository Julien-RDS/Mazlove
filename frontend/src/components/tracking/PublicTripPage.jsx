import React, { useState, useEffect } from "react";
import { trackingApi as api } from "../../lib/trackingApi";
import TrackingMap from "./TrackingMap";
import TripStatusStepper from "./TripStatusStepper";
export default function PublicTripPage({ token }) {
  const [data, set] = useState(null),
    [error, E] = useState("");
  useEffect(() => {
    let active = true;
    const load = () =>
      api(`/public/trips/${encodeURIComponent(token)}`)
        .then((r) => {
          if (active) {
            set(r);
            E("");
          }
        })
        .catch((e) => {
          if (active) {
            set(null);
            E(e.message);
          }
        });
    load();
    const t = setInterval(load, 5000);
    return () => {
      active = false;
      clearInterval(t);
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
