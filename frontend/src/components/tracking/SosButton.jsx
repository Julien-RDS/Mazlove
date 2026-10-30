import React, { useState, useRef, useEffect } from "react";
import { trackingApi as api } from "../../lib/trackingApi";
export default function SosButton({ id, token }) {
  const [armed, setArmed] = useState(false),
    [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false);
  const timer = useRef();
  const stop = () => clearTimeout(timer.current);
  useEffect(() => stop, []);
  const send = async () => {
    if (busy) return;
    setBusy(true);
    try {
      const r = await api(`/sessions/${id}/sos`, token, "POST");
      setMessage(
        `${r.message} Emails envoyés : ${r.emailsSent}, échecs : ${r.emailsFailed}.`,
      );
    } catch (e) {
      setMessage(e.message);
    } finally {
      setBusy(false);
      setArmed(false);
    }
  };
  const hold = () => {
    stop();
    timer.current = setTimeout(() => setArmed(true), 1500);
  };
  return (
    <section>
      <p>
        En cas de danger immédiat, appelez le <a href="tel:17">17</a> ou le{" "}
        <a href="tel:112">112</a>. Ce bouton prévient vos proches et l’équipe
        Mazlove mais ne remplace pas les secours.
      </p>
      <button
        className="tracking-sos"
        disabled={busy}
        onPointerDown={hold}
        onPointerUp={stop}
        onPointerLeave={stop}
        onPointerCancel={stop}
        onKeyDown={(e) => {
          if ((e.key === " " || e.key === "Enter") && !e.repeat) hold();
        }}
        onKeyUp={stop}
        onBlur={stop}
      >
        SOS — maintenir 1,5 seconde
      </button>
      {armed && (
        <div role="alert">
          <button disabled={busy} onClick={send}>
            Confirmer l’alerte SOS
          </button>
          <button onClick={() => setArmed(false)}>Annuler</button>
        </div>
      )}
      <p role="status">{message}</p>
    </section>
  );
}
