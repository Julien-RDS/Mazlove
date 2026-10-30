import { it, expect, vi, afterEach } from "vitest";
import { startVisiblePolling } from "./visiblePolling";
function visible() {
  const target = new EventTarget();
  target.hidden = false;
  return target;
}
afterEach(() => vi.useRealTimers());
it("évite les requêtes simultanées et regroupe les rafraîchissements", async () => {
  vi.useFakeTimers();
  let finish;
  const load = vi.fn(
    () =>
      new Promise((resolve) => {
        finish = resolve;
      }),
  );
  const p = startVisiblePolling(load, 1000, { visibility: visible() });
  p.refresh();
  p.refresh();
  await vi.advanceTimersByTimeAsync(5000);
  expect(load).toHaveBeenCalledTimes(1);
  finish();
  await vi.advanceTimersByTimeAsync(0);
  expect(load).toHaveBeenCalledTimes(2);
  p.stop();
  finish();
});
it("suspend les appels dans un onglet masqué et reprend au retour", async () => {
  vi.useFakeTimers();
  const visibility = visible(),
    load = vi.fn(async () => {});
  const p = startVisiblePolling(load, 1000, { visibility });
  await Promise.resolve();
  visibility.hidden = true;
  visibility.dispatchEvent(new Event("visibilitychange"));
  await vi.advanceTimersByTimeAsync(5000);
  expect(load).toHaveBeenCalledTimes(1);
  visibility.hidden = false;
  visibility.dispatchEvent(new Event("visibilitychange"));
  await Promise.resolve();
  expect(load).toHaveBeenCalledTimes(2);
  p.stop();
});
it("annule la requête en quittant l’écran", () => {
  vi.useFakeTimers();
  let signal;
  const p = startVisiblePolling(
    (s) => {
      signal = s;
      return new Promise(() => {});
    },
    1000,
    { visibility: visible() },
  );
  p.stop();
  expect(signal.aborted).toBe(true);
});
it("remonte une erreur puis réessaie au prochain intervalle", async () => {
  vi.useFakeTimers();
  const onError = vi.fn(),
    load = vi
      .fn()
      .mockRejectedValueOnce(new Error("Connexion perdue"))
      .mockResolvedValue(null);
  const p = startVisiblePolling(load, 1000, { visibility: visible(), onError });
  await vi.advanceTimersByTimeAsync(1000);
  expect(onError).toHaveBeenCalledTimes(1);
  expect(load).toHaveBeenCalledTimes(2);
  p.stop();
});
