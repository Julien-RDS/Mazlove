import { test, expect } from "@playwright/test";

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
        json: { providerName: "Sophie", status: "EN_ROUTE", pos: null },
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
  await page.clock.runFor(5000);
  await expect(page.getByRole("alert")).toHaveText("Lien expiré ou révoqué.");
  await expect(page.getByRole("heading", { name: "Sophie" })).toHaveCount(0);
});
