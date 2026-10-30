import { it, expect, vi } from "vitest";
import { providerLease } from "./providerLease";
it("sans Web Locks : aucun GPS et message explicite", () => {
  const start = vi.fn(),
    blocked = vi.fn(),
    close = vi.fn();
  providerLease("gps", {
    start,
    blocked,
    locks: null,
    channelFactory: () => ({ close }),
  });
  expect(start).not.toHaveBeenCalled();
  expect(blocked).toHaveBeenCalled();
  expect(close).toHaveBeenCalled();
});
it("un composant démonté pendant la demande ne démarre pas le GPS", async () => {
  let acquire;
  const start = vi.fn(),
    close = vi.fn();
  const locks = {
    request: vi.fn((name, options, callback) => {
      acquire = callback;
      return Promise.resolve();
    }),
  };
  const lease = providerLease("gps", {
    start,
    locks,
    channelFactory: () => ({ addEventListener: () => {}, close }),
  });
  lease.stop();
  await acquire({});
  expect(start).not.toHaveBeenCalled();
  expect(close).toHaveBeenCalled();
});
it("libère le watcher avant de rendre le verrou", async () => {
  let callback, release;
  const events = [];
  const locks = {
    request: (name, options, cb) => {
      callback = cb;
      return Promise.resolve();
    },
  };
  const lease = providerLease("gps", {
    start: () => () => events.push("watcher arrêté"),
    locks,
    channelFactory: () => ({ addEventListener: () => {}, close: () => {} }),
  });
  const held = callback({}).then(() => events.push("verrou rendu"));
  lease.stop();
  await held;
  expect(events).toEqual(["watcher arrêté", "verrou rendu"]);
});
