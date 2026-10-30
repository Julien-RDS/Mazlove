import { Client } from "@stomp/stompjs";
export function wsUrl(path = "/ws") {
  const u = new URL(
    import.meta.env.VITE_API_URL || location.origin,
    location.origin,
  );
  u.protocol = u.protocol === "https:" ? "wss:" : "ws:";
  u.pathname = path;
  u.search = "";
  u.hash = "";
  return u.toString();
}
export function wsClient(token, options = {}) {
  const client = new Client({
    brokerURL: wsUrl(),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    ...options,
    onWebSocketClose: (event) => {
      if (event.code === 1008) client.deactivate();
      options.onWebSocketClose?.(event);
    },
  });
  return client;
}
