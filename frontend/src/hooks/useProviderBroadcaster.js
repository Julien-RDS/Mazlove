import { useEffect, useState } from "react";
import { providerLease } from "../lib/providerLease";
import { startProviderGps } from "../lib/providerGps";

export function useProviderBroadcaster(id, token, enabled, providerId) {
  const [state, setState] = useState({ phase: "idle", error: "" });
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    if (!enabled || !providerId) return;
    let mounted = true;
    const report = (next) => {
      if (mounted) setState(next);
    };
    report({ phase: "starting", error: "" });
    const lease = providerLease(`mazlove-gps:${providerId}`, {
      wait: attempt > 0,
      blocked: (error, phase = "blocked") => report({ phase, error }),
      transferred: () =>
        report({
          phase: "transferred",
          error: "Le suivi GPS a été transféré vers un autre onglet.",
        }),
      start: () => startProviderGps({ id, token, onState: report }),
    });
    return () => {
      mounted = false;
      lease.stop();
    };
  }, [id, token, enabled, providerId, attempt]);
  return {
    ...(enabled ? state : { phase: "idle", error: "" }),
    takeover: () => setAttempt((n) => n + 1),
  };
}
