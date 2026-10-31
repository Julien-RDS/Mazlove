import { trackingApi } from "./trackingApi";
import { wsClient } from "./ws";
import { startVisiblePolling } from "./visiblePolling";
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
const observers = new Map();
export function subscribeLiveTracking(id, token, listener) {
  const key = `${token}:${id}`;
  let entry = observers.get(key);
  if (!entry) {
    entry = createObserver(id, token);
    observers.set(key, entry);
  }
  entry.listeners.add(listener);
  listener(entry.getState());
  if (!entry.started) {
    entry.started = true;
    entry.start();
  }
  let subscribed = true;
  return () => {
    if (!subscribed) return;
    subscribed = false;
    entry.listeners.delete(listener);
    if (!entry.listeners.size) {
      entry.stop();
      observers.delete(key);
    }
  };
}
function createObserver(id, token) {
  let state = { status: null, pos: null, route: null, connected: false };
  let cleanup;
  const listeners = new Set();
  const set = (update) => {
    const next = typeof update === "function" ? update(state) : update;
    if (next === state) return;
    state = next;
    listeners.forEach((notify) => notify(state));
  };
  const start = () => {
    const key = `${token}:${id}`;
    let alive = true,
      eventVersion = 0,
      positionVersion = 0,
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
      const pointVersion = positionVersion;
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
          snap.status === "EN_ROUTE" &&
          previous.pos &&
          ((snap.pos && previous.pos.ts > snap.pos.ts) ||
            (!snap.pos && pointVersion !== positionVersion))
            ? previous.pos
            : snap.pos,
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
            if (u.lat !== undefined) positionVersion++;
            set((p) => applyUpdate(p, u));
            if (u.event === "DEVIATION") {
              eventVersion++;
              polling.refresh();
            }
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
    const localUpdate = (e) => {
      if (!alive || e.detail?.token !== token || e.detail.session?.id !== id)
        return;
      eventVersion++;
      set((p) => applyUpdate(p, { status: e.detail.session.status }));
      if (["DONE", "CANCELLED"].includes(e.detail.session.status))
        stopTerminal();
      else polling.refresh();
    };
    window.addEventListener("mazlove:session-updated", localUpdate);
    client.activate();
    timer = setInterval(() => {
      if (!alive || document.hidden) return;
      set((p) => {
        const stale = !!p.pos && Date.now() - p.pos.ts > 15000;
        return stale === p.stale ? p : { ...p, stale };
      });
    }, 3000);
    const dispose = () => {
      alive = false;
      window.removeEventListener("mazlove:session-updated", localUpdate);
      clearInterval(timer);
      polling.stop();
      client.deactivate();
    };
    cleanup = dispose;
  };
  return { listeners, start, stop: () => cleanup?.(), getState: () => state };
}
