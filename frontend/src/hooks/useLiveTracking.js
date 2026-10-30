import { useEffect, useState } from "react";
import { trackingApi } from "../lib/trackingApi";
import { wsClient } from "../lib/ws";
export function applyUpdate(s, u) {
  if (u.event)
    return {
      ...s,
      signalLost:
        u.event === "SIGNAL_LOST"
          ? true
          : u.event === "SIGNAL_RECOVERED"
            ? false
            : s.signalLost,
      message: u.message,
    };
  if (u.status && u.status !== "EN_ROUTE")
    return {
      ...s,
      ...u,
      pos: null,
      route: null,
      stale: false,
      signalLost: false,
    };
  if (
    u.lat !== undefined &&
    ["ARRIVED", "IN_PROGRESS", "DONE", "CANCELLED"].includes(s.status)
  )
    return s;
  if (u.lat !== undefined)
    return s.pos && u.ts < s.pos.ts
      ? s
      : { ...s, status: u.status, pos: u, stale: false, signalLost: false };
  return { ...s, ...u };
}
export function useLiveTracking(id, token) {
  const [state, set] = useState({
    status: null,
    pos: null,
    route: null,
    connected: false,
  });
  useEffect(() => {
    if (!id || !token) return;
    let alive = true,
      version = 0;
    const snapshot = async () => {
      const current = ++version;
      try {
        const s = await trackingApi(`/tracking/${id}/snapshot`, token);
        if (alive && current === version)
          set((p) => ({ ...p, ...s, error: null }));
      } catch (e) {
        if (alive) set((p) => ({ ...p, error: e.message }));
      }
    };
    const client = wsClient(token, {
      onConnect: () => {
        if (!alive) return;
        set((p) => ({ ...p, connected: true }));
        client.subscribe(`/topic/tracking.${id}`, (m) => {
          try {
            const u = JSON.parse(m.body);
            set((p) => applyUpdate(p, u));
          } catch {}
        });
        client.subscribe(`/topic/session.${id}.status`, (m) => {
          version++;
          set((p) => applyUpdate(p, JSON.parse(m.body)));
          snapshot();
        });
        snapshot();
      },
      onWebSocketClose: () => alive && set((p) => ({ ...p, connected: false })),
      onStompError: () =>
        alive && set((p) => ({ ...p, error: "Connexion de suivi refusée." })),
    });
    snapshot();
    client.activate();
    const timer = setInterval(() => {
      if (alive)
        set((p) => ({ ...p, stale: !!p.pos && Date.now() - p.pos.ts > 15000 }));
    }, 3000);
    const refresh = setInterval(snapshot, 30000);
    return () => {
      alive = false;
      clearInterval(timer);
      clearInterval(refresh);
      client.deactivate();
    };
  }, [id, token]);
  return state;
}
