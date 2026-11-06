import { test, expect } from "@playwright/test";
import fs from "node:fs";
const route = JSON.parse(
  fs.readFileSync(
    new URL("../../scripts/routes/chatelet.geojson", import.meta.url),
  ),
).geometry.coordinates;
test("consentement, GPS réel émulé, arrivée, code, fin", async ({
  browser,
  request,
  baseURL,
}) => {
  test.setTimeout(300000);
  const client = await browser.newContext(),
    provider = await browser.newContext({
      permissions: ["geolocation"],
      geolocation: {
        latitude: route[0][1],
        longitude: route[0][0],
        accuracy: 5,
      },
    });
  const cp = await client.newPage(),
    pp = await provider.newPage();
  const connectedPages = new Set();
  if (process.env.MAZLOVE_TEST_NGINX === "true") {
    const index = await request.get("/");
    expect(index.status()).toBe(200);
    expect(index.headers()["referrer-policy"]).toBe("no-referrer");
    const asset = (await index.text()).match(/\/assets\/[^"\s]+\.js/);
    expect(asset).not.toBeNull();
    const js = await request.get(asset[0]);
    expect(js.status()).toBe(200);
    expect(js.headers()["referrer-policy"]).toBe("no-referrer");
    expect(js.headers()["cache-control"]).toContain("immutable");
    expect((await request.get("/assets/absent-e2e.js")).status()).toBe(404);
    const shared = await request.get("/t/controle");
    expect(shared.status()).toBe(200);
    expect(shared.headers()["referrer-policy"]).toBe("no-referrer");
    for (const page of [cp, pp]) {
      page.on("websocket", (socket) => {
        const url = new URL(socket.url());
        if (url.host !== new URL(baseURL).host || !url.pathname.startsWith("/ws")) return;
        socket.on("framereceived", (frame) => {
          if (String(frame.payload).includes("CONNECTED\n")) connectedPages.add(page);
        });
      });
    }
  }
  const login = async (email, page) => {
    const mailhog = process.env.MAILHOG_URL || "http://127.0.0.1:8025";
    const inbox = async () => {
      const url = new URL("/api/v2/search", mailhog);
      url.searchParams.set("kind", "to");
      url.searchParams.set("query", email);
      const result = await request.get(url.toString());
      if (!result.ok()) throw new Error("MailHog indisponible");
      return (await result.json()).items || [];
    };
    const previous = new Set((await inbox()).map((message) => message.ID));
    await page.goto(`${baseURL}/#/connexion`);
    await page.getByLabel("Email", { exact: true }).fill(email);
    await page.getByLabel("Mot de passe", { exact: true }).fill("password123");
    await page
      .getByRole("button", { name: "Recevoir mon code de connexion" })
      .click();
    await expect(
      page.getByRole("heading", { name: "Votre code de connexion" }),
    ).toBeVisible();
    let code;
    await expect
      .poll(
        async () => {
          const message = (await inbox()).find(
            (item) => !previous.has(item.ID),
          );
          const subject = message?.Content?.Headers?.Subject?.[0] || "";
          code = subject.match(/\b\d{6}\b/)?.[0];
          return !!code;
        },
        { timeout: 15000 },
      )
      .toBe(true);
    const verifyRequest = page.waitForRequest(
      (req) =>
        req.url().includes("/api/auth/verify-otp") && req.method() === "POST",
    );
    await page.getByLabel("Code à 6 chiffres").fill(code);
    const submission = (await verifyRequest).postDataJSON();
    expect(typeof submission.challenge).toBe("string");
    await expect
      .poll(() => page.evaluate(() => !!localStorage.getItem("mazlove.token")))
      .toBe(true);
    await expect(page).toHaveURL(/\/#\/$/);
    const token = await page.evaluate(() =>
      localStorage.getItem("mazlove.token"),
    );
    expect(typeof token).toBe("string");
    return token;
  };
  const a = await login("alice@test.com", cp),
    p = await login("sophie@test.com", pp);
  const headers = { Authorization: `Bearer ${p}` };
  await request.post("/api/dev/reset-demo", { headers });
  const { id } = await (
    await request.get("/api/dev/demo-session", { headers })
  ).json();
  await cp.goto(`${baseURL}/#/seances/${id}`);
  await pp.goto(`${baseURL}/#/seances/${id}`);
  await expect(
    pp.getByRole("heading", { name: "Console prestataire" }),
  ).toBeVisible();
  const consent = pp.getByRole("button", {
    name: "J’accepte le partage de ma position",
  });
  const consentStatus = await request.get("/api/consents", { headers });
  expect(consentStatus.ok()).toBe(true);
  if (!(await consentStatus.json()).granted) {
    await expect(consent).toBeVisible();
    await consent.click();
  }
  await pp.getByRole("button", { name: "Je pars", exact: true }).click();
  if (process.env.MAZLOVE_TEST_NGINX === "true")
    await expect.poll(() => connectedPages.size, { timeout: 15000 }).toBe(2);
  await expect(cp.getByText("Statut : En route")).toBeVisible();
  const snapshot = async () =>
    (await request.get(`/api/tracking/${id}/snapshot`, { headers })).json();
  await expect
    .poll(async () => (await snapshot()).pos?.etaSeconds, { timeout: 15000 })
    .toBeGreaterThan(0);
  const initialEta = (await snapshot()).pos.etaSeconds;
  const aliceHeaders = { Authorization: `Bearer ${a}` };
  const share = async () => {
    const response = await request.post(`/api/sessions/${id}/share`, {
      headers: aliceHeaders,
    });
    expect(response.ok()).toBe(true);
    return new URL((await response.json()).url).pathname;
  };
  const publicContext = await browser.newContext();
  await publicContext.route("https://tiles.openfreemap.org/**", (r) =>
    r.fulfill({ json: { version: 8, sources: {}, layers: [] } }),
  );
  const publicPage = await publicContext.newPage();
  const revokedPath = await share();
  await publicPage.goto(`${baseURL}${revokedPath}`);
  await expect(publicPage.locator(".tracking-marker")).toBeVisible();
  expect(
    (await request.delete(`/api/sessions/${id}/share`, { headers: aliceHeaders })).ok(),
  ).toBe(true);
  await expect(publicPage.getByRole("alert")).toBeVisible({ timeout: 15000 });
  await expect(publicPage.locator(".tracking-marker")).toHaveCount(0);
  expect(
    (await request.get(`/api/public/trips/${revokedPath.split("/").pop()}`)).status(),
  ).toBe(404);
  const publicPath = await share();
  await publicPage.goto(`${baseURL}${publicPath}`);
  await expect(publicPage.locator(".tracking-marker")).toBeVisible();
  let throttled = false;
  for (let i = 0; i < 31; i++) {
    const response = await request.get(
      `/api/public/trips/${publicPath.split("/").pop()}`,
    );
    if (response.status() === 429) throttled = true;
  }
  expect(throttled).toBe(true);
  await expect(publicPage.getByRole("alert")).toContainText(
    "Mise à jour retardée",
    { timeout: 15000 },
  );
  await expect(publicPage.locator(".tracking-marker")).toBeVisible();
  await pp.evaluate(() => (location.hash = "/"));
  await expect(
    pp.getByRole("heading", { name: "Du départ au premier bonjour." }),
  ).toBeVisible();
  // Last 150m of the recorded route; reach it gradually to stay within the 70m/s filter.
  const selected = [];
  let rest = 0;
  for (let i = 1; i < route.length; i++) {
    const a = route[i - 1],
      b = route[i];
    const length = Math.hypot((b[0] - a[0]) * 73000, (b[1] - a[1]) * 111195);
    for (let d = rest; d < length; d += 100) {
      const f = d / length;
      selected.push([a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f]);
    }
    rest = (rest - length) % 100;
    if (rest < 0) rest += 100;
  }
  selected.push(route.at(-1));
  let step = 0;
  for (const [lng, lat] of selected) {
    await provider.setGeolocation({
      latitude: lat,
      longitude: lng,
      accuracy: 5,
    });
    await pp.waitForTimeout(3100);
    if (++step === Math.max(2, Math.floor(selected.length / 2))) {
      await expect
        .poll(async () => (await snapshot()).pos?.etaSeconds, {
          timeout: 15000,
        })
        .toBeLessThan(initialEta);
      await provider.setOffline(true);
      await pp.waitForTimeout(1200);
      await provider.setOffline(false);
    }
  }
  await provider.setGeolocation({
    latitude: 48.8584,
    longitude: 2.347,
    accuracy: 5,
  });
  await pp.waitForTimeout(11000);
  await provider.setGeolocation({
    latitude: 48.8584,
    longitude: 2.347,
    accuracy: 5,
  });
  await expect(cp.getByText("Statut : Arrivé", { exact: true })).toBeVisible({
    timeout: 20000,
  });
  await expect(publicPage.locator(".tracking-marker")).toHaveCount(0, {
    timeout: 80000,
  });
  await expect(publicPage.getByRole("alert")).toBeVisible();
  expect(
    (await request.get(`/api/public/trips/${publicPath.split("/").pop()}`)).status(),
  ).toBe(404);
  await pp.evaluate((id) => (location.hash = `/seances/${id}`), id);
  await pp.getByLabel("Code donné par le client").fill("123456");
  await pp.getByRole("button", { name: "Démarrer la séance" }).click();
  await expect(
    cp.getByText("Statut : En cours", { exact: true }),
  ).toBeVisible();
  await pp.getByRole("button", { name: "Terminer la séance" }).click();
  await expect(cp.getByText("Statut : Terminé", { exact: true })).toBeVisible();
  const snap = await (
    await request.get(`/api/tracking/${id}/snapshot`, { headers })
  ).json();
  expect(snap.pos).toBeNull();
  expect(snap.route).toBeNull();
  await client.close();
  await provider.close();
  await publicContext.close();
});
