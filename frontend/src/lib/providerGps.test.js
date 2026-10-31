import { it, expect, vi, beforeEach, afterEach } from "vitest";
import { startProviderGps } from "./providerGps";
let geo, client, options, watch, onState, visibility, stop;
const fix = (overrides = {}) => ({
  timestamp: Date.now(),
  coords: {
    latitude: 48.87,
    longitude: 2.32,
    accuracy: 5,
    heading: null,
    speed: null,
  },
  ...overrides,
});
beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(1000000);
  visibility = Object.assign(new EventTarget(), { hidden: false });
  onState = vi.fn();
  geo = {
    watchPosition: vi.fn((cb) => {
      watch = cb;
      return 7;
    }),
    clearWatch: vi.fn(),
    getCurrentPosition: vi.fn((cb) => cb(fix())),
  };
  client = {
    connected: false,
    activate: vi.fn(),
    deactivate: vi.fn(),
    publish: vi.fn(),
  };
});
afterEach(() => {
  stop?.();
  vi.useRealTimers();
});
function start() {
  stop = startProviderGps({
    id: 42,
    token: "p",
    geolocation: geo,
    wakeLock: null,
    visibility,
    onState,
    makeClient: (t, o) => {
      options = o;
      return client;
    },
  });
  client.connected = true;
  options.onConnect();
}
it("redemande une position fraîche à l’arrêt pour confirmer l’arrivée", async () => {
  start();
  expect(client.publish).toHaveBeenCalledTimes(1);
  await vi.advanceTimersByTimeAsync(10000);
  expect(client.publish).toHaveBeenCalledTimes(2);
  const [first, second] = client.publish.mock.calls.map(([p]) =>
    JSON.parse(p.body),
  );
  expect(second.ts).toBeGreaterThan(first.ts);
  expect(second.seq).toBeGreaterThan(first.seq);
});
it("limite les déplacements à un envoi par 3 secondes et garde le dernier point", async () => {
  start();
  await vi.advanceTimersByTimeAsync(1000);
  watch(fix({ coords: { latitude: 48.871, longitude: 2.32, accuracy: 5 } }));
  await vi.advanceTimersByTimeAsync(1000);
  watch(fix({ coords: { latitude: 48.872, longitude: 2.32, accuracy: 5 } }));
  expect(client.publish).toHaveBeenCalledTimes(1);
  await vi.advanceTimersByTimeAsync(1000);
  expect(client.publish).toHaveBeenCalledTimes(2);
  expect(JSON.parse(client.publish.mock.lastCall[0].body).lat).toBe(48.872);
});
it("ne rejoue pas un historique après une coupure", async () => {
  start();
  client.connected = false;
  options.onWebSocketClose({ code: 1006 });
  await vi.advanceTimersByTimeAsync(40000);
  watch(fix());
  expect(client.publish).toHaveBeenCalledTimes(1);
  client.connected = true;
  options.onConnect();
  expect(geo.getCurrentPosition).toHaveBeenCalledTimes(2);
  expect(JSON.parse(client.publish.mock.lastCall[0].body).ts).toBe(Date.now());
});
it("libère watcher et timers après refus de permission", async () => {
  start();
  geo.getCurrentPosition.mockImplementation((ok, no) => no({ code: 1 }));
  await vi.advanceTimersByTimeAsync(10000);
  expect(geo.clearWatch).toHaveBeenCalledWith(7);
  expect(client.deactivate).toHaveBeenCalled();
  expect(onState.mock.lastCall[0].phase).toBe("denied");
  expect(vi.getTimerCount()).toBe(0);
  watch(fix());
  expect(client.publish).toHaveBeenCalledTimes(1);
});
it("ignore précision insuffisante et horodatage ancien", async () => {
  start();
  geo.getCurrentPosition.mockImplementation((cb) =>
    cb(fix({ timestamp: 1000000 })),
  );
  await vi.advanceTimersByTimeAsync(20000);
  expect(client.publish).toHaveBeenCalledTimes(1);
  watch(fix({ coords: { latitude: 48.87, longitude: 2.32, accuracy: 200 } }));
  expect(client.publish).toHaveBeenCalledTimes(1);
  expect(onState.mock.lastCall[0].phase).toBe("weak");
});

it("envoie le premier point frais même si le GPS répond avant STOMP", () => {
  stop = startProviderGps({
    id: 42,
    token: "p",
    geolocation: geo,
    wakeLock: null,
    visibility,
    onState,
    makeClient: (t, o) => {
      options = o;
      return client;
    },
  });
  watch(fix());
  expect(client.publish).not.toHaveBeenCalled();
  geo.getCurrentPosition.mockImplementation(() => {});
  client.connected = true;
  options.onConnect();
  expect(client.publish).toHaveBeenCalledTimes(1);
  expect(onState.mock.lastCall[0].phase).toBe("sharing");
});
