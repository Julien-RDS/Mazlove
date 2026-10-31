import { beforeEach, afterEach, it, expect, vi } from "vitest";
vi.mock("./ws", () => ({ wsClient: vi.fn() }));
vi.mock("./trackingApi", () => ({ trackingApi: vi.fn() }));
import { wsClient } from "./ws";
import { trackingApi } from "./trackingApi";
import { subscribeLiveTracking } from "./liveTrackingSession";
let options, client, topics, stops;
beforeEach(() => {
  vi.useFakeTimers();
  stops = [];
  topics = {};
  vi.stubGlobal(
    "document",
    Object.assign(new EventTarget(), { hidden: false }),
  );
  vi.stubGlobal("window", new EventTarget());
  client = {
    activate: vi.fn(),
    deactivate: vi.fn(),
    subscribe: vi.fn((topic, cb) => (topics[topic] = cb)),
  };
  wsClient.mockImplementation((token, opts) => {
    options = opts;
    return client;
  });
  trackingApi.mockResolvedValue({ status: "EN_ROUTE", pos: null, route: null });
});
afterEach(() => {
  stops.forEach((stop) => stop());
  vi.clearAllMocks();
  vi.unstubAllGlobals();
  vi.useRealTimers();
});
const subscribe = (cb, token = "p") => {
  const stop = subscribeLiveTracking(42, token, cb);
  stops.push(stop);
  return stop;
};
it("partage une connexion et requête entre accueil et console", async () => {
  const a = vi.fn(),
    b = vi.fn();
  const stop = subscribe(a);
  subscribe(b);
  await vi.advanceTimersByTimeAsync(0);
  options.onConnect();
  expect(wsClient).toHaveBeenCalledTimes(1);
  expect(trackingApi).toHaveBeenCalledTimes(1);
  expect(client.subscribe).toHaveBeenCalledTimes(2);
  stop();
  expect(client.deactivate).not.toHaveBeenCalled();
  topics["/topic/tracking.42"]({
    body: JSON.stringify({
      lat: 1,
      lng: 2,
      ts: Date.now(),
      status: "EN_ROUTE",
    }),
  });
  expect(b.mock.lastCall[0].pos.lat).toBe(1);
});
it("sépare deux comptes observant la même séance", () => {
  subscribe(vi.fn(), "a");
  subscribe(vi.fn(), "b");
  expect(wsClient).toHaveBeenCalledTimes(2);
});
it("un snapshot retardé conserve une position reçue en direct", async () => {
  let resolve;
  trackingApi.mockReturnValue(new Promise((r) => (resolve = r)));
  const received = vi.fn();
  subscribe(received);
  options.onConnect();
  topics["/topic/tracking.42"]({
    body: JSON.stringify({
      lat: 1,
      lng: 2,
      ts: Date.now(),
      status: "EN_ROUTE",
    }),
  });
  resolve({ status: "EN_ROUTE", pos: null, route: null });
  await vi.advanceTimersByTimeAsync(0);
  expect(received.mock.lastCall[0].pos.lat).toBe(1);
});
it("nettoie connexion et timers au départ du dernier écran", async () => {
  const stop = subscribe(vi.fn());
  await vi.advanceTimersByTimeAsync(0);
  stop();
  stops = [];
  expect(client.deactivate).toHaveBeenCalledTimes(1);
  expect(vi.getTimerCount()).toBe(0);
});
