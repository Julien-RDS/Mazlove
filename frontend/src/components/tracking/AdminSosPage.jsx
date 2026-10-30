import React, { useState, useEffect } from "react";
import { trackingApi as api } from "../../lib/trackingApi";
import { startVisiblePolling } from "../../lib/visiblePolling";
import { useRef } from "react";
import { tokenStore } from "../../api";
export default function AdminSosPage() {
  const token = tokenStore.get(),
    [rows, R] = useState([]),
    [error, E] = useState("");
  const poll = useRef();
  const load = () => poll.current?.refresh();
  useEffect(() => {
    let alive = true;
    poll.current = startVisiblePolling(
      async (signal) => {
        const rows = await api("/admin/sos", token, "GET", undefined, {
          signal,
        });
        if (alive) {
          R(rows);
          E("");
        }
      },
      10000,
      {
        onError: (e) => {
          if (alive) E(e.message);
        },
      },
    );
    return () => {
      alive = false;
      poll.current.stop();
    };
  }, [token]);
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
          {a.email_problems > 0 && (
            <p role="alert">
              {a.email_problems} email(s) en échec ou avec un envoi incertain.
              Contactez directement la personne.
            </p>
          )}
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
