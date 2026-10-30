import React, { useState, useEffect } from "react";
import { trackingApi as api } from "../../lib/trackingApi";
import { useProviderJourney } from "./ProviderJourney";
import { SESSION_UPDATED_EVENT } from "../../lib/sessionTracking";
import ConsentModal from "./ConsentModal";
export default function ProviderConsole({
  id,
  token,
  status,
  clientName = "votre client",
  simulated = false,
  onChange,
  meetingPoint,
}) {
  const [mode, setMode] = useState("DRIVING"),
    [consent, setConsent] = useState(false),
    [code, setCode] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const journey = useProviderJourney();
  const gpsError = simulated ? "" : journey?.error;
  useEffect(() => {
    api("/consents", token)
      .then((x) => setConsent(x.granted))
      .catch((e) => setError(e.message));
  }, [token]);
  const action = async (a, body) => {
    setBusy(true);
    setError("");
    try {
      const s = await api(`/sessions/${id}/${a}`, token, "POST", body);
      window.dispatchEvent(
        new CustomEvent(SESSION_UPDATED_EVENT, {
          detail: { token, session: s },
        }),
      );
      onChange?.(s);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };
  const depart = async () => {
    setBusy(true);
    try {
      let point;
      if (simulated)
        point = {
          lat: meetingPoint.lat + 0.015,
          lng: meetingPoint.lng - 0.025,
        };
      else
        point = await new Promise((resolve, reject) =>
          navigator.geolocation
            ? navigator.geolocation.getCurrentPosition(
                (p) =>
                  resolve({ lat: p.coords.latitude, lng: p.coords.longitude }),
                () =>
                  reject(
                    new Error("Autorisez la localisation avant de partir."),
                  ),
                { enableHighAccuracy: true, timeout: 10000 },
              )
            : reject(new Error("Géolocalisation indisponible.")),
        );
      await action("depart", { travelMode: mode, ...point });
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };
  return (
    <section className="tracking-panel">
      <h3>Console prestataire</h3>
      <p className="tracking-warning">
        Gardez l’application au premier plan et l’écran allumé. Le navigateur ne
        garantit pas le GPS en arrière-plan.
      </p>
      {status === "PENDING" && (
        <div>
          <button disabled={busy} onClick={() => action("accept")}>
            Accepter
          </button>
          <button disabled={busy} onClick={() => action("decline")}>
            Refuser
          </button>
        </div>
      )}
      {status === "ACCEPTED" && (
        <>
          {!consent && (
            <ConsentModal
              busy={busy}
              onAccept={async () => {
                try {
                  await api("/consents", token, "POST");
                  setConsent(true);
                } catch (e) {
                  setError(e.message);
                }
              }}
            />
          )}
          <label>
            Mode de déplacement
            <select value={mode} onChange={(e) => setMode(e.target.value)}>
              <option value="WALKING">À pied</option>
              <option value="CYCLING">À vélo</option>
              <option value="DRIVING">En voiture</option>
            </select>
          </label>
          <button disabled={!consent || busy} onClick={depart}>
            Je pars
          </button>
        </>
      )}
      {status === "EN_ROUTE" && (
        <>
          <p>
            📍 Votre position est partagée avec {clientName} jusqu’à votre
            arrivée.
          </p>
          <button disabled={busy} onClick={() => action("arrived")}>
            Je suis arrivé
          </button>
          <button disabled={busy} onClick={() => action("cancel")}>
            Arrêter le partage et annuler
          </button>
        </>
      )}
      {status === "ARRIVED" && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            action("validate-arrival", { code });
          }}
        >
          <label>
            Code donné par le client
            <input
              value={code}
              onChange={(e) => setCode(e.target.value)}
              maxLength={6}
              required
              autoComplete="off"
            />
          </label>
          <button disabled={busy || code.length !== 6}>
            Démarrer la séance
          </button>
        </form>
      )}
      {status === "IN_PROGRESS" && (
        <button disabled={busy} onClick={() => action("confirm-end")}>
          Terminer la séance
        </button>
      )}
      {(error || gpsError) && (
        <p role="alert" className="tracking-error">
          {error || gpsError}
        </p>
      )}
    </section>
  );
}
