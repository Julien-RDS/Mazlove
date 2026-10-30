import React, { useState, useEffect } from "react";
import { trackingApi as api } from "../../lib/trackingApi";
export default function TrustedContacts({ token }) {
  const [items, set] = useState([]),
    [name, N] = useState(""),
    [email, E] = useState(""),
    [error, X] = useState("");
  const load = () =>
    api("/users/me/trusted-contacts", token)
      .then(set)
      .catch((e) => X(e.message));
  useEffect(() => {
    load();
  }, [token]);
  return (
    <details>
      <summary>Mes contacts de confiance</summary>
      <ul>
        {items.map((c) => (
          <li key={c.id}>
            {c.name} — {c.email || c.phone}
            <button
              onClick={async () => {
                try {
                  await api(
                    `/users/me/trusted-contacts/${c.id}`,
                    token,
                    "DELETE",
                  );
                  load();
                } catch (e) {
                  X(e.message);
                }
              }}
            >
              Supprimer
            </button>
          </li>
        ))}
      </ul>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          try {
            await api("/users/me/trusted-contacts", token, "POST", {
              name,
              email,
            });
            N("");
            E("");
            load();
          } catch (e) {
            X(e.message);
          }
        }}
      >
        <label>
          Prénom
          <input
            required
            maxLength={100}
            value={name}
            onChange={(e) => N(e.target.value)}
          />
        </label>
        <label>
          Email
          <input
            required
            type="email"
            maxLength={255}
            value={email}
            onChange={(e) => E(e.target.value)}
          />
        </label>
        <button>Ajouter un contact</button>
      </form>
      <p role="alert">{error}</p>
    </details>
  );
}
