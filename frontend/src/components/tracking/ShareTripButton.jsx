import React, { useState } from "react";
import { trackingApi as api } from "../../lib/trackingApi";
export default function ShareTripButton({ id, token }) {
  const [message, set] = useState("");
  return (
    <div>
      <button
        onClick={async () => {
          try {
            const s = await api(`/sessions/${id}/share`, token, "POST");
            if (navigator.share)
              await navigator.share({
                title: "Mon trajet Mazlove",
                url: s.url,
              });
            else {
              await navigator.clipboard.writeText(s.url);
              set("Lien copié.");
            }
          } catch (e) {
            set(e.message);
          }
        }}
      >
        Partager le trajet
      </button>
      <button
        onClick={async () => {
          try {
            await api(`/sessions/${id}/share`, token, "DELETE");
            set("Liens révoqués.");
          } catch (e) {
            set(e.message);
          }
        }}
      >
        Révoquer les liens
      </button>
      <p role="status">{message}</p>
    </div>
  );
}
