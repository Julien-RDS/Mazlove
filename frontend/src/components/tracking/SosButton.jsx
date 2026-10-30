import React, { useState, useRef, useEffect } from "react";
import { trackingApi as api } from "../../lib/trackingApi";
import { startVisiblePolling } from "../../lib/visiblePolling";
const deliveryText = (r) =>
  `${r.message} Emails envoyés : ${r.emailsSent}, en attente : ${r.emailsQueued}, échecs : ${r.emailsFailed}, envoi incertain : ${r.emailsUncertain}. En cas d’échec ou d’incertitude, contactez directement vos proches.`;
export default function SosButton({ id, token }) {
  const [armed, setArmed] = useState(false),
    [message, setMessage] = useState(""),
    [busy, setBusy] = useState(false);
  const [alert, setAlert] = useState(null);
  const timer = useRef();
  useEffect(() => {
    if (!alert) return;
    let alive = true;
    const poll = startVisiblePolling(
      async (signal) => {
        const r = await api(
          `/sessions/${id}/sos/${alert}/delivery`,
          token,
          "GET",
          undefined,
          { signal },
        );
        if (alive) {
          setMessage(deliveryText(r));
          if (!r.emailsQueued) setAlert(null);
        }
      },
      5000,
      {
        onError: () => {
          if (alive)
            setMessage(
              "Alerte enregistrée. Le résultat des emails est indisponible : contactez directement vos proches.",
            );
        },
      },
    );
    return () => {
      alive = false;
      poll.stop();
    };
  }, [alert, id, token]);
  const stop = () => clearTimeout(timer.current);
  useEffect(() => stop, []);
  const send = async () => {
    if (busy) return;
    setBusy(true);
    try {
      const r = await api(`/sessions/${id}/sos`, token, "POST");
      setMessage(deliveryText(r));
      if (r.emailsQueued) setAlert(r.alertId);
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
