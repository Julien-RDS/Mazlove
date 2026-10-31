import { test, expect } from "@playwright/test";
async function fixture(context, { initial = "EN_ROUTE" } = {}) {
  let status = initial;
  const sockets = [],
    positions = [],
    requests = [];
  await context.grantPermissions(["geolocation"]);
  await context.setGeolocation({
    latitude: 48.87,
    longitude: 2.32,
    accuracy: 5,
  });
  await context.addInitScript(() => {
    if (!localStorage.getItem("mazlove.token"))
      localStorage.setItem("mazlove.token", "provider-test");
    const geo = navigator.geolocation,
      watch = geo.watchPosition.bind(geo),
      clear = geo.clearWatch.bind(geo),
      active = new Set();
    window.gpsMetrics = {
      started: 0,
      stopped: 0,
      wake: 0,
      active: () => active.size,
    };
    geo.watchPosition = (...args) => {
      const id = watch(...args);
      active.add(id);
      window.gpsMetrics.started++;
      return id;
    };
    geo.clearWatch = (id) => {
      if (active.delete(id)) window.gpsMetrics.stopped++;
      clear(id);
    };
    Object.defineProperty(navigator, "wakeLock", {
      value: {
        request: async () => {
          window.gpsMetrics.wake++;
          const lock = {
            released: false,
            release: async () => {
              if (!lock.released) {
                lock.released = true;
                window.gpsMetrics.wake--;
              }
            },
          };
          return lock;
        },
      },
    });
  });
  const session = (role) => ({
    id: 42,
    status,
    datetime: "2026-10-30T17:00:00",
    duration: 1,
    clientName: "Alice",
    providerName: "Sophie",
    viewerRole: role || "PROVIDER",
    meetingPoint: { lat: 48.8584, lng: 2.347, label: "Châtelet" },
  });
  const emit = (destination, body) => {
    for (const { socket, subscriptions, closed } of sockets)
      if (!closed)
        for (const [id, d] of subscriptions)
          if (d === destination)
            socket.send(
              `MESSAGE\nsubscription:${id}\nmessage-id:${Date.now()}\ndestination:${d}\ncontent-type:application/json\n\n${JSON.stringify(body)}\u0000`,
            );
  };
  const change = (next) => {
    status = next;
    emit("/topic/session.42.status", { status });
  };
  await context.routeWebSocket("**/ws", (socket) => {
    const record = { socket, subscriptions: new Map(), closed: false };
    sockets.push(record);
    socket.onClose(() => (record.closed = true));
    socket.onMessage((raw) => {
      const text = raw.toString(),
        header = (name) => text.match(new RegExp(`^${name}:(.*)$`, "m"))?.[1];
      if (text.startsWith("CONNECT"))
        socket.send("CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\u0000");
      if (text.startsWith("SUBSCRIBE"))
        record.subscriptions.set(header("id"), header("destination"));
      if (text.startsWith("UNSUBSCRIBE"))
        record.subscriptions.delete(header("id"));
      if (text.startsWith("SEND"))
        positions.push(
          JSON.parse(text.split("\n\n")[1].replace(/\u0000$/, "")),
        );
      if (text.startsWith("DISCONNECT")) {
        if (header("receipt"))
          socket.send(`RECEIPT\nreceipt-id:${header("receipt")}\n\n\u0000`);
      }
    });
  });
  await context.route("https://tiles.openfreemap.org/**", (route) =>
    route.fulfill({ json: { version: 8, sources: {}, layers: [] } }),
  );
  await context.route("**/api/**", async (route) => {
    const req = route.request(),
      path = new URL(req.url()).pathname,
      token = req.headers().authorization;
    requests.push({ path, token });
    let data = [];
    if (path === "/api/users/me")
      data = { id: 2, name: "Sophie", role: "PROVIDER", providerId: 2 };
    if (path === "/api/users/me/active-session") data = session();
    if (path === "/api/users/me/provider-trip")
      data = status === "EN_ROUTE" ? session() : null;
    if (path === "/api/sessions/42")
      data = session(token?.includes("demo-alice") ? "CLIENT" : "PROVIDER");
    if (path === "/api/consents") data = { granted: true };
    if (path === "/api/tracking/42/snapshot")
      data = {
        status,
        meetingPoint: session().meetingPoint,
        pos: null,
        route: null,
      };
    if (path === "/api/sessions/42/depart") {
      change("EN_ROUTE");
      data = session();
    }
    if (path === "/api/sessions/42/arrived") {
      change("ARRIVED");
      data = session();
    }
    if (path === "/api/sessions/42/cancel") {
      change("CANCELLED");
      data = session();
    }
    if (path === "/api/dev/login-as")
      data = {
        token: req.postDataJSON().email.startsWith("alice")
          ? "demo-alice"
          : "demo-sophie",
      };
    if (path === "/api/dev/demo-session") data = { id: 42 };
    return route.fulfill({ json: data });
  });
  return { positions, sockets, requests, change };
}
const active = (page) => page.evaluate(() => window.gpsMetrics.active());
test("GPS : départ, navigation, rechargement et libération à l’arrivée", async ({
  context,
  page,
}) => {
  const f = await fixture(context, { initial: "ACCEPTED" });
  await page.goto("/#/seances/42");
  await page.getByRole("button", { name: "Je pars", exact: true }).click();
  await expect.poll(() => active(page)).toBe(1);
  await expect.poll(() => f.positions.length).toBeGreaterThan(0);
  await page.evaluate(() => (location.hash = "/"));
  await expect(
    page.getByRole("heading", { name: "Du départ au premier bonjour." }),
  ).toBeVisible();
  const before = f.positions.length;
  await context.setGeolocation({
    latitude: 48.8705,
    longitude: 2.32,
    accuracy: 5,
  });
  await expect
    .poll(() => f.positions.length, { timeout: 8000 })
    .toBeGreaterThan(before);
  await page.evaluate(() => (location.hash = "/seances/42"));
  await expect(
    page.getByRole("button", { name: "Je suis arrivé" }),
  ).toBeVisible();
  expect(await page.evaluate(() => window.gpsMetrics.started)).toBe(1);
  await page.reload();
  await expect.poll(() => active(page)).toBe(1);
  expect(await page.evaluate(() => window.gpsMetrics.started)).toBe(1);
  await page.getByRole("button", { name: "Je suis arrivé" }).click();
  await expect.poll(() => active(page)).toBe(0);
  await expect.poll(() => page.evaluate(() => window.gpsMetrics.wake)).toBe(0);
  const stopped = f.positions.length;
  await page.waitForTimeout(3200);
  expect(f.positions.length).toBe(stopped);
});
test("deux onglets : un seul GPS, prise de relais explicite et déconnexion propagée", async ({
  context,
  page,
}) => {
  await fixture(context);
  await page.goto("/");
  await expect.poll(() => active(page)).toBe(1);
  const second = await context.newPage();
  await second.goto("/");
  await expect(
    second.getByText(/Le GPS est actif dans un autre onglet/),
  ).toBeVisible();
  expect(await active(second)).toBe(0);
  await second
    .getByRole("button", { name: "Reprendre le suivi dans cet onglet" })
    .click();
  await expect.poll(() => active(second)).toBe(1);
  await expect.poll(() => active(page)).toBe(0);
  const locks = await second.evaluate(
    async () =>
      (await navigator.locks.query()).held.filter((x) =>
        x.name.startsWith("mazlove-gps:"),
      ).length,
  );
  expect(locks).toBe(1);
  await second.getByRole("button", { name: "Se déconnecter" }).click();
  await expect.poll(() => active(second)).toBe(0);
  await expect.poll(() => active(page)).toBe(0);
  await expect(
    page.getByRole("button", { name: "Se déconnecter" }),
  ).toHaveCount(0);
});
test("annulation et expiration STOMP arrêtent le GPS", async ({
  context,
  page,
}) => {
  const f = await fixture(context);
  await page.goto("/");
  await expect.poll(() => active(page)).toBe(1);
  f.change("CANCELLED");
  await expect.poll(() => active(page)).toBe(0);
  f.change("EN_ROUTE");
  await page.reload();
  await expect.poll(() => active(page)).toBe(1);
  for (const record of f.sockets)
    if (!record.closed)
      record.socket.close({ code: 1008, reason: "Session expirée" });
  await expect.poll(() => active(page)).toBe(0);
});
test("déconnexion pendant le chargement : aucune séance privée ne revient", async ({
  context,
  page,
}) => {
  await fixture(context, { initial: "ACCEPTED" });
  let release;
  const pending = new Promise((resolve) => (release = resolve));
  await page.route("**/api/users/me/active-session", async (route) => {
    await pending;
    await route
      .fulfill({
        json: { id: 99, status: "EN_ROUTE", providerName: "Ancien compte" },
      })
      .catch(() => {});
  });
  await page.goto("/");
  await page.getByRole("button", { name: "Se déconnecter" }).click();
  release();
  await expect(
    page.getByRole("link", { name: "Me connecter", exact: true }),
  ).toBeVisible();
  await expect(page.getByText("Ancien compte")).toHaveCount(0);
  expect(await active(page)).toBe(0);
});
test("démonstration simulée : jetons séparés, aucun vrai GPS", async ({
  context,
  page,
}) => {
  const f = await fixture(context);
  await page.goto("/demo");
  await expect(
    page.getByRole("heading", { name: "Sophie — prestataire" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Lancer", exact: true }).click();
  expect(await page.evaluate(() => window.gpsMetrics.started)).toBe(0);
  expect(await page.evaluate(() => localStorage.getItem("mazlove.token"))).toBe(
    "provider-test",
  );
  await expect
    .poll(
      () =>
        f.requests.filter((r) => r.path === "/api/tracking/42/snapshot").length,
    )
    .toBeGreaterThanOrEqual(2);
  expect(
    new Set(
      f.requests
        .filter((r) => r.path === "/api/tracking/42/snapshot")
        .map((r) => r.token),
    ),
  ).toEqual(new Set(["Bearer demo-alice", "Bearer demo-sophie"]));
});

test("changement de compte dans un autre onglet : libère le GPS et retire le trajet précédent", async ({
  context,
  page,
}) => {
  await fixture(context);
  await page.goto("/");
  await expect.poll(() => active(page)).toBe(1);
  await page.route("**/api/users/me", (route) =>
    route.fulfill({
      json: { id: 3, name: "Nouveau compte", role: "CLIENT", providerId: null },
    }),
  );
  await page.route("**/api/users/me/active-session", (route) =>
    route.fulfill({ json: null }),
  );
  await page.evaluate(() => {
    localStorage.setItem("mazlove.token", "other-account");
    window.dispatchEvent(
      new StorageEvent("storage", {
        key: "mazlove.token",
        newValue: "other-account",
        oldValue: "provider-test",
      }),
    );
  });
  await expect(page.getByLabel("Compte de Nouveau compte")).toBeVisible();
  await expect.poll(() => active(page)).toBe(0);
  await expect(
    page.getByRole("complementary", { name: "État du suivi GPS" }),
  ).toHaveCount(0);
  await expect(
    page.getByRole("link", { name: "Ouvrir ma console" }),
  ).toHaveCount(0);
});

test("une réponse de trajet retardée après l’arrivée ne redémarre pas le GPS", async ({
  context,
  page,
}) => {
  await fixture(context);
  await page.goto("/#/seances/42");
  await expect.poll(() => active(page)).toBe(1);
  let release, entered;
  const waiting = new Promise((resolve) => (release = resolve)),
    requested = new Promise((resolve) => (entered = resolve));
  await page.route(
    "**/api/users/me/provider-trip",
    async (route) => {
      entered();
      await waiting;
      await route
        .fulfill({ json: { id: 42, status: "EN_ROUTE", clientName: "Alice" } })
        .catch(() => {});
    },
    { times: 1 },
  );
  await page.evaluate(() =>
    window.dispatchEvent(new Event("mazlove:session-updated")),
  );
  await requested;
  await page.getByRole("button", { name: "Je suis arrivé" }).click();
  await expect.poll(() => active(page)).toBe(0);
  release();
  await page.waitForTimeout(500);
  expect(await page.evaluate(() => window.gpsMetrics.started)).toBe(1);
  expect(await active(page)).toBe(0);
});

test("un trajet indisponible au chargement affiche une reprise accessible depuis l’accueil", async ({
  context,
  page,
}) => {
  await fixture(context);
  await page.route(
    "**/api/users/me/provider-trip",
    (route) =>
      route.fulfill({ status: 503, json: { message: "Indisponible" } }),
    { times: 1 },
  );
  await page.goto("/");
  await expect(
    page.getByText("Trajet indisponible", { exact: true }),
  ).toBeVisible();
  expect(await active(page)).toBe(0);
  await page
    .getByRole("button", { name: "Réessayer le chargement du trajet" })
    .click();
  await expect.poll(() => active(page)).toBe(1);
  await expect(page.getByText("GPS actif", { exact: true })).toBeVisible();
});

test("départ sans recréer la carte, un seul abonnement partagé et continuité lors d’une panne REST", async ({
  context,
  page,
}) => {
  const f = await fixture(context, { initial: "ACCEPTED" });
  await page.goto("/#/seances/42");
  await expect(page.locator(".maplibregl-canvas")).toBeVisible({
    timeout: 15000,
  });
  await page
    .locator(".maplibregl-canvas")
    .evaluate((el) => (el.dataset.preserved = "yes"));
  await page.getByRole("button", { name: "Je pars", exact: true }).click();
  await expect.poll(() => active(page)).toBe(1);
  await expect(page.locator(".maplibregl-canvas")).toHaveAttribute(
    "data-preserved",
    "yes",
  );
  const subscriptions = () =>
    f.sockets
      .filter((s) => !s.closed)
      .flatMap((s) => [...s.subscriptions.values()])
      .filter((d) => d === "/topic/tracking.42").length;
  await expect.poll(subscriptions).toBe(1);
  await page.evaluate(() => (location.hash = "/"));
  await expect(
    page.getByRole("link", { name: "Ouvrir ma console" }),
  ).toBeVisible();
  await expect.poll(subscriptions).toBe(1);
  await page.route("**/api/users/me/provider-trip", (r) =>
    r.fulfill({ status: 503, json: { message: "Indisponible" } }),
  );
  await page.evaluate(() =>
    window.dispatchEvent(new Event("mazlove:session-updated")),
  );
  await page.waitForTimeout(300);
  expect(await active(page)).toBe(1);
  expect(await page.evaluate(() => window.gpsMetrics.started)).toBe(1);
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});

test("une connexion GPS en préparation n’affirme pas que la position est partagée", async ({
  context,
  page,
}) => {
  await fixture(context);
  await page.addInitScript(() => {
    navigator.geolocation.watchPosition = () => 77;
    navigator.geolocation.getCurrentPosition = () => {};
  });
  await page.goto("/");
  await expect(
    page.getByText("Localisation en cours", { exact: true }),
  ).toBeVisible();
  await expect(page.getByText("GPS actif", { exact: true })).toHaveCount(0);
  await expect(
    page.getByRole("button", { name: "Reprendre le suivi dans cet onglet" }),
  ).toHaveCount(0);
});
