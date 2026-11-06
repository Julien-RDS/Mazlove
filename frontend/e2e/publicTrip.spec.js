import { test, expect } from "@playwright/test";

test("panne réseau : la dernière position reste affichée pendant des délais croissants", async ({
  page,
}) => {
  await page.clock.install();
  await page.route("https://tiles.openfreemap.org/**", (r) =>
    r.fulfill({ json: { version: 8, sources: {}, layers: [] } }),
  );
  let calls = 0;
  await page.route("**/api/public/trips/**", (r) => {
    calls++;
    if (calls === 2 || calls === 3) return r.abort("failed");
    return r.fulfill({
      json: {
        providerName: "Sophie",
        status: "EN_ROUTE",
        pos: { lat: 48.86, lng: 2.34, ts: Date.now() },
      },
    });
  });
  await page.goto("/t/test-token");
  await expect(page.getByRole("heading", { name: "Sophie" })).toBeVisible();
  await page.clock.runFor(100);
  await expect(page.locator(".tracking-marker")).toBeVisible();
  await page.clock.runFor(5000);
  await expect(page.getByRole("alert")).toContainText("Mise à jour retardée");
  await expect(page.locator(".tracking-marker")).toBeVisible();
  await page.clock.runFor(9000);
  expect(calls).toBe(2);
  await page.clock.runFor(1000);
  await expect.poll(() => calls).toBe(3);
  await page.clock.runFor(19000);
  expect(calls).toBe(3);
  await page.clock.runFor(1000);
  await expect.poll(() => calls).toBe(4);
  await expect(page.getByRole("alert")).toHaveCount(0);
});

test("quota : conserve la position et respecte Retry-After avant reprise", async ({
  page,
}) => {
  await page.clock.install();
  await page.route("https://tiles.openfreemap.org/**", (r) =>
    r.fulfill({ json: { version: 8, sources: {}, layers: [] } }),
  );
  let calls = 0;
  await page.route("**/api/public/trips/**", (r) => {
    calls++;
    if (calls === 2)
      return r.fulfill({
        status: 429,
        headers: { "Retry-After": "20" },
        json: { message: "Quota atteint" },
      });
    return r.fulfill({
      json: {
        providerName: calls === 1 ? "Sophie" : "Sophie actualisée",
        status: "EN_ROUTE",
        pos: { lat: 48.86, lng: 2.34, ts: Date.now() },
      },
    });
  });
  await page.goto("/t/test-token");
  await expect(
    page.getByRole("heading", { name: "Sophie", exact: true }),
  ).toBeVisible();
  await page.clock.runFor(100);
  await expect(page.locator(".tracking-marker")).toBeVisible();
  await page.clock.runFor(5000);
  await expect(page.getByRole("alert")).toContainText("Mise à jour retardée");
  await expect(
    page.getByRole("heading", { name: "Sophie", exact: true }),
  ).toBeVisible();
  await expect(page.locator(".tracking-marker")).toBeVisible();
  await page.clock.runFor(19000);
  expect(calls).toBe(2);
  await page.clock.runFor(1000);
  await expect(
    page.getByRole("heading", { name: "Sophie actualisée" }),
  ).toBeVisible();
  await expect(page.getByRole("alert")).toHaveCount(0);
});

test("lien partagé : aucune requête simultanée et effacement après révocation", async ({
  page,
}) => {
  await page.clock.install();
  await page.route("https://tiles.openfreemap.org/**", (route) =>
    route.fulfill({ json: { version: 8, sources: {}, layers: [] } }),
  );
  let requests = 0;
  let release;
  const waiting = new Promise((resolve) => {
    release = resolve;
  });
  await page.route("**/api/public/trips/**", async (route) => {
    requests++;
    if (requests === 1) {
      await waiting;
      return route.fulfill({
        json: {
          providerName: "Sophie",
          status: "EN_ROUTE",
          pos: { lat: 48.86, lng: 2.34, ts: Date.now() },
        },
      });
    }
    return route.fulfill({
      status: 404,
      json: { message: "Lien expiré ou révoqué." },
    });
  });
  await page.goto("/t/test-token");
  await expect.poll(() => requests).toBe(1);
  await page.clock.runFor(11000);
  expect(requests).toBe(1);
  release();
  await expect(page.getByRole("heading", { name: "Sophie" })).toBeVisible();
  await page.clock.runFor(100);
  await expect(page.locator(".tracking-marker")).toBeVisible();
  await page.clock.runFor(5000);
  await expect(page.getByRole("alert")).toHaveText("Lien expiré ou révoqué.");
  await expect(page.getByRole("heading", { name: "Sophie" })).toHaveCount(0);
  await expect(page.locator(".tracking-marker")).toHaveCount(0);
});
