// npm ci --prefix frontend. TOKEN is a provider JWT, sent only in CONNECT.
import { createRequire } from "node:module";
import { readFile } from "node:fs/promises";
const require = createRequire(
  new URL("../frontend/package.json", import.meta.url),
);
const { Client } = require("@stomp/stompjs"),
  WebSocket = require("ws");
const args = Object.fromEntries(
  process.argv.slice(2).reduce((a, v, i, all) => {
    if (v.startsWith("--")) a.push([v.slice(2), all[i + 1]]);
    return a;
  }, []),
);
if (!process.env.TOKEN || !args.session || !args.route)
  throw new Error(
    "TOKEN requis ; --session ID --route fichier.geojson|gpx --speed 10",
  );
const raw = await readFile(args.route, "utf8");
const coords = args.route.endsWith(".gpx")
  ? [...raw.matchAll(/<trkpt\b([^>]+)>/g)].map((m) => {
      const lat = m[1].match(/lat=["']([^"']+)/),
        lng = m[1].match(/lon=["']([^"']+)/);
      return [Number(lng?.[1]), Number(lat?.[1])];
    })
  : JSON.parse(raw).geometry.coordinates;
if (
  coords.length < 2 ||
  coords.some((p) => p.length < 2 || !p.every(Number.isFinite))
)
  throw new Error("Itinéraire invalide.");
const distance = (a, b) => {
  const rad = Math.PI / 180,
    dlat = (b[1] - a[1]) * rad,
    dlng = (b[0] - a[0]) * rad;
  const x =
    Math.sin(dlat / 2) ** 2 +
    Math.cos(a[1] * rad) * Math.cos(b[1] * rad) * Math.sin(dlng / 2) ** 2;
  return 12742000 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
};
let seq = Date.now(),
  index = 1,
  offset = 0,
  timer,
  arrivals = 0;
const speed = Math.min(60, Math.max(1, Number(args.speed || 10)));
const advance = () => {
  offset += speed * 1.1;
  while (
    index < coords.length - 1 &&
    offset > distance(coords[index - 1], coords[index])
  ) {
    offset -= distance(coords[index - 1], coords[index]);
    index++;
  }
  const a = coords[index - 1],
    b = coords[index],
    len = distance(a, b),
    f = len ? Math.min(1, offset / len) : 1;
  if (index === coords.length - 1 && f === 1) arrivals++;
  return [a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f];
};
const client = new Client({
  webSocketFactory: () =>
    new WebSocket(process.env.WS_URL || "ws://localhost:3001/ws"),
  connectHeaders: { Authorization: `Bearer ${process.env.TOKEN}` },
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  onConnect: () => {
    timer = setInterval(() => {
      if (arrivals >= 2) {
        clearInterval(timer);
        client.deactivate();
        return;
      }
      const [lng, lat] = advance();
      client.publish({
        destination: `/app/tracking/${args.session}/position`,
        body: JSON.stringify({
          lat,
          lng,
          accuracy: 5,
          ts: Date.now(),
          seq: ++seq,
        }),
      });
    }, 1100);
  },
  onStompError: (frame) => {
    console.error("Connexion refusée :", frame.headers.message);
    clearInterval(timer);
    client.deactivate();
  },
});
client.activate();
process.on("SIGINT", () => {
  clearInterval(timer);
  client.deactivate();
});
