import { test, expect } from "@playwright/test";
async function fixtures(
  page,
  { signed = false, provider = false, error = false } = {},
) {
  const requests = [];
  page.on("request", (r) => requests.push(r.url()));
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
          {
            id: "background",
            type: "background",
            paint: { "background-color": "#e2e8d8" },
          },
        ],
      },
    }),
  );
  await page.route("**/api/**", (route) => {
    const path = new URL(route.request().url()).pathname;
    let data = [];
    if (path === "/api/users/me")
      data = {
        id: 1,
        name: provider ? "Sophie" : "Alice",
        role: provider ? "PROVIDER" : "CLIENT",
        providerId: provider ? 2 : null,
      };
    if (path === "/api/users/me/active-session") {
      if (error)
        return route.fulfill({
          status: 503,
          json: { message: "Le suivi est temporairement indisponible." },
        });
      data = [
        {
          id: 42,
          status: "EN_ROUTE",
          datetime: "2026-10-30T17:00:00",
          duration: 1,
          providerName: "Sophie",
          clientName: "Alice",
          viewerRole: provider ? "PROVIDER" : "CLIENT",
        },
      ];
    }
    if (path === "/api/tracking/42/snapshot")
      data = {
        status: "EN_ROUTE",
        meetingPoint: { lat: 48.8584, lng: 2.347, label: "Châtelet" },
        pos: {
          lat: 48.863,
          lng: 2.34,
          ts: Date.now(),
          etaSeconds: 360,
          distanceMeters: 1800,
          estimated: true,
        },
        route: {
          coordinates: [
            [2.34, 48.863],
            [2.347, 48.8584],
          ],
        },
        providerName: "Sophie",
        signalLost: false,
      };
    if (path === "/api/users/me/active-session") data = data[0] || null;
    if (path === "/api/users/me/provider-trip") data = null;
    return route.fulfill({ json: data });
  });
  return requests;
}
test("visiteur : présentation et connexion, sans charger la carte ni les données privées", async ({
  page,
}) => {
  const requests = await fixtures(page);
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "Du départ au premier bonjour." }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Me connecter", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Essayer la démonstration" }),
  ).toHaveAttribute("href", "/demo");
  expect(
    requests.some(
      (u) =>
        u.includes("/api/users/me/active-session") ||
        /TrackingMap-.*\.js/.test(u),
    ),
  ).toBe(false);
});
test("client : ETA, bonne séance et carte chargée uniquement à l’ouverture", async ({
  page,
}) => {
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  const requests = await fixtures(page, { signed: true });
  await page.goto("/");
  await expect(
    page.getByRole("link", { name: "Suivre son arrivée" }),
  ).toHaveAttribute("href", "#/seances/42");
  await expect(page.getByText(/Sophie est à 1,8 km/)).toBeVisible();
  expect(
    requests.some(
      (u) =>
        /TrackingMap-.*\.js/.test(u) || u.includes("/api/providers/online"),
    ),
  ).toBe(false);
  await page.getByRole("button", { name: "Afficher le trajet ici" }).click();
  await expect(page.locator(".maplibregl-canvas")).toBeVisible();
  expect(requests.some((u) => /TrackingMap-.*\.js/.test(u))).toBe(true);
  await page.locator(".maplibregl-ctrl-zoom-in").click();
  await expect(page.getByRole("button", { name: "Recentrer" })).toBeVisible();
  await page.getByRole("button", { name: "Recentrer" }).click();
  await expect(page.getByRole("button", { name: "Recentrer" })).toHaveCount(0);
  expect(errors).toEqual([]);
});
test("disponibilités : centre cohérent et résultat vide explicite", async ({
  page,
}) => {
  const requests = await fixtures(page, { signed: true });
  await page.goto("/");
  await page.getByRole("button", { name: /Disponibles autour/ }).click();
  await expect(
    page.getByText(
      "Aucun prestataire en ligne dans cette zone pour le moment.",
    ),
  ).toBeVisible();
  expect(requests.some((u) => u.includes("lat=48.87&lng=2.35"))).toBe(true);
});
test("prestataire mobile : console et disponibilité, sans débordement", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await fixtures(page, { signed: true, provider: true });
  await page.goto("/");
  await expect(
    page.getByRole("link", { name: "Ouvrir ma console" }),
  ).toHaveAttribute("href", "#/seances/42");
  await expect(
    page.getByRole("checkbox", { name: "Me rendre disponible" }),
  ).not.toBeChecked();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});
test("erreur réseau : message et accès de secours aux séances", async ({
  page,
}) => {
  await fixtures(page, { signed: true, error: true });
  await page.goto("/");
  await expect(
    page.getByText("Le suivi est temporairement indisponible."),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Retrouver mes séances" }),
  ).toHaveAttribute("href", "#/seances");
});
