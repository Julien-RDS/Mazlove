import { it, expect, vi, afterEach } from "vitest";
import { trackingApi } from "./trackingApi";
afterEach(() => vi.unstubAllGlobals());
it("transmet le signal d’annulation et le jeton dans les en-têtes", async () => {
  const fetch = vi
    .fn()
    .mockResolvedValue(new Response('{"status":"EN_ROUTE"}'));
  vi.stubGlobal("fetch", fetch);
  const controller = new AbortController();
  expect(
    await trackingApi("/tracking/1/snapshot", "token", "GET", undefined, {
      signal: controller.signal,
    }),
  ).toEqual({ status: "EN_ROUTE" });
  expect(fetch.mock.calls[0][1].signal).toBe(controller.signal);
  expect(fetch.mock.calls[0][1].headers.Authorization).toBe("Bearer token");
  expect(fetch.mock.calls[0][0]).not.toContain("token");
});
it("conserve les annulations et traduit les pannes réseau", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockRejectedValue(new DOMException("Annulation", "AbortError")),
  );
  await expect(
    trackingApi("/providers/online", "token"),
  ).rejects.toHaveProperty("name", "AbortError");
  vi.stubGlobal(
    "fetch",
    vi.fn().mockRejectedValue(new TypeError("Failed to fetch")),
  );
  await expect(trackingApi("/providers/online", "token")).rejects.toThrow(
    "Le serveur ne répond pas",
  );
});
