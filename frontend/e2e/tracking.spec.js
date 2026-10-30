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
  const login = async (email) =>
    (
      await (
        await request.post("/api/dev/login-as", { data: { email } })
      ).json()
    ).token;
  const a = await login("alice@test.com"),
    p = await login("sophie@test.com");
  const headers = { Authorization: `Bearer ${p}` };
  await request.post("/api/dev/reset-demo", { headers });
  const { id } = await (
    await request.get("/api/dev/demo-session", { headers })
  ).json();
  const client = await browser.newContext(),
    provider = await browser.newContext({
      permissions: ["geolocation"],
      geolocation: {
        latitude: route[0][1],
        longitude: route[0][0],
        accuracy: 5,
      },
    });
  for (const [context, token] of [
    [client, a],
    [provider, p],
  ])
    await context.addInitScript(
      (token) => localStorage.setItem("mazlove.token", token),
      token,
    );
  const cp = await client.newPage(),
    pp = await provider.newPage();
  await cp.goto(`${baseURL}/#/seances/${id}`);
  await pp.goto(`${baseURL}/#/seances/${id}`);
  const consent = pp.getByRole("button", {
    name: "J’accepte le partage de ma position",
  });
  if (await consent.isVisible()) await consent.click();
  await pp.getByRole("button", { name: "Je pars", exact: true }).click();
  await expect(cp.getByText("Statut : En route")).toBeVisible();
  const snapshot = async () =>
    (await request.get(`/api/tracking/${id}/snapshot`, { headers })).json();
  await expect
    .poll(async () => (await snapshot()).pos?.etaSeconds, { timeout: 15000 })
    .toBeGreaterThan(0);
  const initialEta = (await snapshot()).pos.etaSeconds;
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
});
