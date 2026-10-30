import { it, expect, vi } from "vitest";
import { animateMarker } from "./animateMarker";
it("respecte la réduction des animations", () => {
  const marker = { setLngLat: vi.fn() };
  animateMarker(marker, [0, 0], [1, 1], 3000, true);
  expect(marker.setLngLat).toHaveBeenCalledWith([1, 1]);
});
it("interpole selon le temps observé et peut être annulé", () => {
  let cb;
  vi.stubGlobal("requestAnimationFrame", (fn) => {
    cb = fn;
    return 9;
  });
  vi.stubGlobal("cancelAnimationFrame", vi.fn());
  const marker = { setLngLat: vi.fn() };
  const stop = animateMarker(marker, [0, 0], [2, 2], 2000, false);
  cb(0);
  cb(1000);
  expect(marker.setLngLat).toHaveBeenLastCalledWith([1, 1]);
  stop();
  expect(cancelAnimationFrame).toHaveBeenCalledWith(9);
  vi.unstubAllGlobals();
});
