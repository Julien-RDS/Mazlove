import { it, expect } from "vitest";
import { applyUpdate } from "./useLiveTracking";
it("efface immédiatement la position à l’arrivée", () => {
  const s = applyUpdate(
    { pos: { lat: 1 }, route: {}, signalLost: true },
    { status: "ARRIVED" },
  );
  expect(s.pos).toBeNull();
  expect(s.route).toBeNull();
  expect(s.signalLost).toBe(false);
});
it("ignore les positions reçues dans le désordre", () => {
  const s = { pos: { ts: 20 } };
  expect(applyUpdate(s, { lat: 1, ts: 10, status: "EN_ROUTE" })).toBe(s);
});
it("affiche perte puis récupération du signal", () => {
  let s = applyUpdate({}, { event: "SIGNAL_LOST" });
  expect(s.signalLost).toBe(true);
  expect(applyUpdate(s, { event: "SIGNAL_RECOVERED" }).signalLost).toBe(false);
});

it("ne réaffiche pas un point retardé après l’arrivée", () => {
  const s = { status: "ARRIVED", pos: null };
  expect(applyUpdate(s, { lat: 48, ts: 30, status: "EN_ROUTE" })).toBe(s);
});
