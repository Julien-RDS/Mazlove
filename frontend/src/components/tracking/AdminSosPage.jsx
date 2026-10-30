import React, { useState, useEffect } from "react";
import { trackingApi as api } from "../../lib/trackingApi";
import { tokenStore } from "../../api";
export default function AdminSosPage() {
  const token = tokenStore.get(),
    [rows, R] = useState([]),
    [error, E] = useState("");
  const load = () =>
    api("/admin/sos", token)
      .then(R)
      .catch((e) => E(e.message));
  useEffect(() => {
    load();
    const t = setInterval(load, 10000);
    return () => clearInterval(t);
  }, []);
  return (
    <main className="tracking-panel">
      <h1>Alertes SOS</h1>
      <p role="alert">{error}</p>
      {rows.map((a) => (
        <article key={a.id}>
          <h2>
            {a.name} — séance {a.session_id}
          </h2>
          <p>{a.created_at}</p>
          {a.latitude != null && (
            <a
              href={`https://www.openstreetmap.org/?mlat=${a.latitude}&mlon=${a.longitude}#map=16/${a.latitude}/${a.longitude}`}
              target="_blank"
              rel="noreferrer"
            >
              Dernière position sur la carte
            </a>
          )}
          <button
            onClick={async () => {
              try {
                await api(`/admin/sos/${a.id}/resolve`, token, "PATCH");
                load();
              } catch (e) {
                E(e.message);
              }
            }}
          >
            Résolu
          </button>
        </article>
      ))}
    </main>
  );
}
