import { test, expect } from "@playwright/test";
import fs from "node:fs";

test("un échec SMTP réel reste visible uniquement pour le destinataire concerné", async ({
  browser,
  request,
  baseURL,
}) => {
  test.skip(process.env.MAZLOVE_MAIL_STOP_IT !== "true");
  const ready = process.env.MAZLOVE_MAIL_BROWSER_READY;
  const done = process.env.MAZLOVE_MAIL_BROWSER_DONE;
  if (!ready || !done) throw new Error("Fichiers de synchronisation requis");
  const mailhog = process.env.MAILHOG_URL || "http://127.0.0.1:8025";
  const login = async (email) => {
    const inboxUrl = new URL("/api/v2/search", mailhog);
    inboxUrl.searchParams.set("kind", "to");
    inboxUrl.searchParams.set("query", email);
    const inbox = async () => (await (await request.get(inboxUrl.toString())).json()).items || [];
    const previous = new Set((await inbox()).map((item) => item.ID));
    const response = await request.post("/api/auth/login", {
      data: { email, password: "password123" },
    });
    expect(response.ok()).toBe(true);
    const { challenge } = await response.json();
    expect(typeof challenge).toBe("string");
    let code;
    await expect.poll(async () => {
      const message = (await inbox()).find((item) => !previous.has(item.ID));
      code = message?.Content?.Headers?.Subject?.[0]?.match(/\b\d{6}\b/)?.[0];
      return !!code;
    }).toBe(true);
    const verified = await request.post("/api/auth/verify-otp", {
      data: { email, code, challenge },
    });
    expect(verified.ok()).toBe(true);
    return (await verified.json()).token;
  };
  const alice = await login("alice@test.com");
  const sophie = await login("sophie@test.com");
  const headers = (token) => ({ Authorization: `Bearer ${token}` });
  const fixture = await request.get("/api/dev/demo-session", { headers: headers(sophie) });
  expect(fixture.ok()).toBe(true);
  const { id } = await fixture.json();
  fs.writeFileSync(ready, "ready");
  await expect.poll(async () => {
    const response = await request.get(`/api/sessions/${id}`, { headers: headers(sophie) });
    return response.ok() && (await response.json()).emailUnconfirmed;
  }, { timeout: 120000 }).toBe(true);
  const contexts = [];
  for (const [token, affected] of [[alice, false], [sophie, true]]) {
    const context = await browser.newContext();
    contexts.push(context);
    await context.addInitScript((value) => localStorage.setItem("mazlove.token", value), token);
    const page = await context.newPage();
    await page.goto(`${baseURL}/#/seances`);
    await page.getByRole("button", { name: /Historique/ }).click();
    await expect(page.locator(`a[href="#/seances/${id}"]`)).toBeVisible();
    const notice = page.getByText("Envoi de l’email de fin non confirmé.");
    if (affected) await expect(notice).toBeVisible();
    else await expect(notice).toHaveCount(0);
  }
  for (const context of contexts) await context.close();
  fs.writeFileSync(done, "done");
});
