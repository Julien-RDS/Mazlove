import { useEffect, useState } from "react";
import { trackingApi } from "../lib/trackingApi";
import { wsClient } from "../lib/ws";
import { startVisiblePolling } from "../lib/visiblePolling";
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
    key: null,
    status: null,
    pos: null,
    route: null,
    connected: false,
  });
  const key = `${token}:${id}`;
  useEffect(() => {
    if (!id || !token) return;
    let alive = true,
      eventVersion = 0,
      terminal = false,
      timer,
      connectedOnce = false,
      client,
      polling;
    set({ key, status: null, pos: null, route: null, connected: false });
    const stopTerminal = () => {
      terminal = true;
      clearInterval(timer);
      polling?.stop();
      client?.deactivate();
    };
    const snapshot = async (signal) => {
      const version = eventVersion;
      const snap = await trackingApi(
        `/tracking/${id}/snapshot`,
        token,
        "GET",
        undefined,
        { signal },
      );
      if (!alive || signal.aborted) return;
      if (version !== eventVersion) {
        polling?.refresh();
        return;
      }
      set((previous) => ({
        ...previous,
        ...snap,
        key,
        error: null,
        pos:
          snap.pos && previous.pos?.ts > snap.pos.ts ? previous.pos : snap.pos,
      }));
      if (["DONE", "CANCELLED"].includes(snap.status)) stopTerminal();
    };
    client = wsClient(token, {
      onConnect: () => {
        if (!alive || terminal) {
          client.deactivate();
          return;
        }
        set((p) => ({ ...p, connected: true }));
        client.subscribe(`/topic/tracking.${id}`, (m) => {
          if (!alive) return;
          try {
            const u = JSON.parse(m.body);
            set((p) => applyUpdate(p, u));
          } catch {}
        });
        client.subscribe(`/topic/session.${id}.status`, (m) => {
          if (!alive) return;
          try {
            const u = JSON.parse(m.body);
            eventVersion++;
            set((p) => applyUpdate(p, u));
            if (["DONE", "CANCELLED"].includes(u.status)) stopTerminal();
            else polling.refresh();
          } catch {}
        });
        // Initial snapshot is already in flight. A reconnect must always fetch a fresh snapshot.
        if (connectedOnce) polling.refresh();
        connectedOnce = true;
      },
      onWebSocketClose: (event) => {
        if (!alive) return;
        if (event.code === 1008) {
          stopTerminal();
          set({
            key,
            status: null,
            pos: null,
            route: null,
            connected: false,
            authExpired: true,
            error: "Session expirée. Reconnectez-vous.",
          });
        } else set((p) => ({ ...p, connected: false }));
      },
      onStompError: () => {
        if (!alive) return;
        stopTerminal();
        set({
          key,
          status: null,
          pos: null,
          route: null,
          connected: false,
          authExpired: true,
          error: "Connexion de suivi refusée. Reconnectez-vous.",
        });
      },
    });
    polling = startVisiblePolling(snapshot, 30000, {
      onError: (e) => {
        if (!alive) return;
        if (e.status === 401 || e.status === 403) {
          stopTerminal();
          set({
            key,
            status: null,
            pos: null,
            route: null,
            connected: false,
            authExpired: true,
            error: e.message,
          });
        } else set((p) => ({ ...p, error: e.message }));
      },
    });
    client.activate();
    timer = setInterval(() => {
      if (!alive || document.hidden) return;
      set((p) => {
        const stale = !!p.pos && Date.now() - p.pos.ts > 15000;
        return stale === p.stale ? p : { ...p, stale };
      });
    }, 3000);
    return () => {
      alive = false;
      clearInterval(timer);
      polling.stop();
      client.deactivate();
    };
  }, [id, token, key]);
  return state.key === key
    ? state
    : { status: null, pos: null, route: null, connected: false };
}
