import { test, expect } from "@playwright/test";

// Mode jour / nuit. API, STOMP et tuiles sont simulés (comme frontpage.spec.js) : ces scénarios valident
// l'interface, pas le fond de carte réel d'OpenFreeMap.
async function fixtures(page, { signed = false } = {}) {
  const styleRequests = [];
  page.on("request", (r) => {
    const url = new URL(r.url());
    if (url.origin === "https://tiles.openfreemap.org" && url.pathname.startsWith("/styles/"))
      styleRequests.push(url.pathname);
  });
  if (signed)
    await page.addInitScript(() =>
      localStorage.setItem("mazlove.token", "frontend-test-token"),
    );
  await page.routeWebSocket("**/ws", (socket) =>
    socket.onMessage((message) => {
      if (message.toString().startsWith("CONNECT"))
        socket.send("CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\u0000");
    }),
  );
  await page.route("https://tiles.openfreemap.org/**", (route) =>
    route.fulfill({
      json: {
        version: 8,
        sources: {},
        layers: [
          { id: "background", type: "background", paint: { "background-color": "#e2e8d8" } },
        ],
      },
    }),
  );
  await page.route("**/api/**", (route) => {
    const path = new URL(route.request().url()).pathname;
    let data = [];
    if (path === "/api/users/me")
      data = { id: 1, name: "Alice", role: "CLIENT", providerId: null };
    if (path === "/api/users/me/active-session")
      data = {
        id: 42,
        status: "EN_ROUTE",
        datetime: "2026-10-30T17:00:00",
        duration: 1,
        providerName: "Sophie",
        clientName: "Alice",
        viewerRole: "CLIENT",
      };
    if (path === "/api/tracking/42/snapshot")
      data = {
        status: "EN_ROUTE",
        meetingPoint: { lat: 48.8584, lng: 2.347, label: "Châtelet" },
        pos: { lat: 48.863, lng: 2.34, ts: Date.now(), etaSeconds: 360, distanceMeters: 1800, estimated: true },
        route: { coordinates: [[2.34, 48.863], [2.347, 48.8584]] },
        providerName: "Sophie",
        signalLost: false,
      };
    if (path === "/api/users/me/provider-trip") data = null;
    return route.fulfill({ json: data });
  });
  return styleRequests;
}

const html = (page) => page.locator("html");

test("le thème suit le réglage du système, en direct, et colore la barre du navigateur", async ({ page }) => {
  await page.emulateMedia({ colorScheme: "dark" });
  await fixtures(page);
  await page.goto("/");
  await expect(html(page)).toHaveAttribute("data-theme", "dark");
  await expect(page.locator('meta[name="theme-color"]')).toHaveAttribute("content", "#161219");
  await page.emulateMedia({ colorScheme: "light" });
  await expect(html(page)).toHaveAttribute("data-theme", "light");
  await expect(page.locator('meta[name="theme-color"]')).toHaveAttribute("content", "#F8F7F3");
});

test("bascule au clavier, choix conservé au rechargement, retour au réglage du système", async ({ page }) => {
  await page.emulateMedia({ colorScheme: "light" });
  await fixtures(page);
  await page.goto("/");
  const toggle = page.getByRole("button", { name: "Mode nuit" });
  await expect(toggle).toHaveAttribute("aria-pressed", "false");
  await toggle.focus();
  await page.keyboard.press("Enter");
  await expect(html(page)).toHaveAttribute("data-theme", "dark");
  await expect(toggle).toHaveAttribute("aria-pressed", "true");
  expect(await page.evaluate(() => localStorage.getItem("mazlove.theme"))).toBe("dark");
  await page.reload();
  await expect(html(page)).toHaveAttribute("data-theme", "dark");
  await page.getByRole("button", { name: "Mode nuit" }).click();
  await expect(html(page)).toHaveAttribute("data-theme", "light");
  // Revenu au réglage du système : plus de choix enregistré, l'application le suit de nouveau.
  expect(await page.evaluate(() => localStorage.getItem("mazlove.theme"))).toBeNull();
  await page.emulateMedia({ colorScheme: "dark" });
  await expect(html(page)).toHaveAttribute("data-theme", "dark");
});

test("un choix explicite l'emporte sur le réglage du système", async ({ page }) => {
  await page.emulateMedia({ colorScheme: "dark" });
  await page.addInitScript(() => localStorage.setItem("mazlove.theme", "light"));
  await fixtures(page);
  await page.goto("/");
  await expect(html(page)).toHaveAttribute("data-theme", "light");
  await page.emulateMedia({ colorScheme: "light" });
  await page.emulateMedia({ colorScheme: "dark" });
  await expect(html(page)).toHaveAttribute("data-theme", "light");
});

test("le fond de carte clair est demandé le jour, sans charger le fond sombre", async ({ page }) => {
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.emulateMedia({ colorScheme: "light" });
  const styles = await fixtures(page, { signed: true });
  await page.goto("/");
  await page.getByRole("button", { name: "Afficher le trajet ici" }).click();
  await expect(page.locator(".maplibregl-canvas")).toBeVisible();
  await expect.poll(() => styles).toContain("/styles/liberty");
  expect(styles).not.toContain("/styles/dark");
  expect(errors).toEqual([]);
});

test("la nuit, la carte s'ouvre directement sur le fond sombre", async ({ page }) => {
  await page.emulateMedia({ colorScheme: "dark" });
  const styles = await fixtures(page, { signed: true });
  await page.goto("/");
  await page.getByRole("button", { name: "Afficher le trajet ici" }).click();
  await expect(page.locator(".maplibregl-canvas")).toBeVisible();
  await expect.poll(() => styles).toContain("/styles/dark");
  expect(styles).not.toContain("/styles/liberty");
});

test("changer de thème carte ouverte la recrée avec le bon fond, sans erreur ni doublon", async ({ page }) => {
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.emulateMedia({ colorScheme: "light" });
  const styles = await fixtures(page, { signed: true });
  await page.goto("/");
  await page.getByRole("button", { name: "Afficher le trajet ici" }).click();
  await expect(page.locator(".maplibregl-canvas")).toBeVisible();
  await expect.poll(() => styles).toContain("/styles/liberty");
  await page.getByRole("button", { name: "Mode nuit" }).click();
  await expect.poll(() => styles).toContain("/styles/dark");
  await expect(page.locator(".maplibregl-canvas")).toHaveCount(1);
  await expect(page.locator(".maplibregl-canvas")).toBeVisible();
  await expect(page.locator(".tracking-marker")).toHaveCount(1);
  expect(errors).toEqual([]);
});
