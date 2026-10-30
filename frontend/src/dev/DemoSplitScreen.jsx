import React, { useState, useEffect } from "react";
import { trackingApi as api } from "../lib/trackingApi";
import TripPanel from "../components/tracking/TripPanel";
const scenarios = {
  HAPPY_PATH: "Trajet normal",
  SLOW_TRAFFIC: "Circulation lente",
  SIGNAL_LOSS: "Perte de signal",
  DEVIATION: "Détour",
  TELEPORT: "Point aberrant",
  CANCEL_EN_ROUTE: "Annulation en route",
};
export default function DemoSplitScreen() {
  const [state, S] = useState(null),
    [scenario, C] = useState("HAPPY_PATH"),
    [speed, V] = useState(10),
    [error, E] = useState(""),
    [busy, B] = useState(false),
    [run, R] = useState(0),
    [real, G] = useState(false);
  const load = async () => {
    const [a, p] = await Promise.all(
      ["alice@test.com", "sophie@test.com"].map((email) =>
        api("/dev/login-as", null, "POST", { email }),
      ),
    );
    const { id } = await api("/dev/demo-session", p.token);
    const [client, provider] = await Promise.all([
      api(`/sessions/${id}`, a.token),
      api(`/sessions/${id}`, p.token),
    ]);
    S({ a: a.token, p: p.token, id, client, provider });
  };
  useEffect(() => {
    load().catch((e) => E(e.message));
  }, [run]);
  const command = async (action) => {
    B(true);
    E("");
    try {
      if (action === "reset") {
        await api("/dev/reset-demo", state.p, "POST");
        R((x) => x + 1);
      } else if (action === "stop")
        await api(`/dev/tracking/${state.id}/stop`, state.p, "POST");
      else {
        let s = await api(`/sessions/${state.id}`, state.p);
        if (s.status === "ACCEPTED") {
          await api("/consents", state.p, "POST");
          await api(`/sessions/${state.id}/depart`, state.p, "POST", {
            travelMode: "DRIVING",
            lat: s.meetingPoint.lat + 0.015,
            lng: s.meetingPoint.lng - 0.025,
          });
        }
        await api(
          `/dev/tracking/${state.id}/simulate?scenario=${scenario}&speedFactor=${speed}`,
          state.p,
          "POST",
        );
      }
    } catch (e) {
      E(e.message);
    } finally {
      B(false);
    }
  };
  return (
    <main>
      <header className="demo-bar tracking-panel">
        <h1>Démonstration du suivi Mazlove</h1>
        <p>
          « Lancer » utilise les comptes fictifs Alice/Sophie et accepte le
          partage pour cette démonstration. Pour tester le consentement
          manuellement, utilisez la console Sophie.
        </p>
        <select
          aria-label="Scénario"
          value={scenario}
          onChange={(e) => C(e.target.value)}
        >
          {Object.entries(scenarios).map(([k, v]) => (
            <option key={k} value={k}>
              {v}
            </option>
          ))}
        </select>
        <select
          aria-label="Vitesse"
          value={speed}
          onChange={(e) => V(+e.target.value)}
        >
          {[1, 5, 10, 20].map((x) => (
            <option key={x} value={x}>
              ×{x}
            </option>
          ))}
        </select>
        <label>
          <input
            type="checkbox"
            checked={real}
            onChange={(e) => G(e.target.checked)}
          />
          Vrai GPS
        </label>
        <button
          disabled={!state || busy || real}
          onClick={() => command("start")}
        >
          Lancer
        </button>
        <button disabled={!state || busy} onClick={() => command("stop")}>
          Stop
        </button>
        <button disabled={!state || busy} onClick={() => command("reset")}>
          Reset
        </button>
        <p role="alert">{error}</p>
      </header>
      {state && (
        <div className="demo-grid" key={run}>
          <section>
            <h2>Alice — cliente</h2>
            <TripPanel session={state.client} token={state.a} />
          </section>
          <section>
            <h2>Sophie — prestataire</h2>
            <TripPanel
              session={state.provider}
              token={state.p}
              simulated={!real}
            />
          </section>
        </div>
      )}
    </main>
  );
}
