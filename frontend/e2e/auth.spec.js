import { test, expect } from "@playwright/test";

test("les refus et expirations SSO affichent toujours un message", async ({
  page,
}) => {
  await fixture(page, { oidc: true });
  for (const code of [
    "access_denied",
    "authorization_request_not_found",
    "auth_unavailable",
    "unknown_error",
    "__proto__",
    "constructor",
    "toString",
    "provider_unavailable",
  ]) {
    await page.goto("/#/connexion?oidcError=" + code);
    await page.reload();
    await expect(page.getByRole("alert")).toBeVisible();
    if (
      ["__proto__", "constructor", "toString", "unknown_error"].includes(code)
    )
      await expect(page.getByRole("alert")).toContainText(
        "La connexion SSO a échoué.",
      );
    expect(
      await page.evaluate(() => localStorage.getItem("mazlove.token")),
    ).toBeNull();
  }
});

test("un jeton dans l’URL ne saute pas l’OTP lorsque le serveur impose la 2FA", async ({
  page,
}) => {
  await fixture(page, { oidc: true, mfa: true });
  await page.goto("/#/connexion?oidcToken=untrusted-token");
  await expect(
    page.getByText("Retour SSO invalide. Recommencez la connexion."),
  ).toBeVisible();
  expect(
    await page.evaluate(() => localStorage.getItem("mazlove.token")),
  ).toBeNull();
  expect(page.url()).not.toContain("untrusted-token");
});

async function fixture(page, { oidc = false, mfa = true } = {}) {
  await page.route("**/api/**", (route) => {
    const path = new URL(route.request().url()).pathname;
    let json = [];
    if (path === "/api/auth/config")
      json = {
        oidcEnabled: oidc,
        oidcProvider: "Google",
        twoFactorEnabled: mfa,
      };
    if (path === "/api/auth/login")
      json = mfa
        ? { mfaRequired: true, token: null, challenge: "a".repeat(64) }
        : { mfaRequired: false, token: "dev-login-token" };
    if (path === "/api/users/me")
      json = { id: 1, name: "Alice", role: "CLIENT" };
    if (path === "/api/users/me/active-session") json = null;
    return route.fulfill({ json });
  });
}

for (const viaSso of [false, true]) {
  test(`le défi serveur accompagne la vérification OTP (${viaSso ? "SSO" : "mot de passe"})`, async ({
    page,
  }) => {
    await fixture(page, { oidc: true });
    let received;
    const challenge = (viaSso ? "b" : "a").repeat(64);
    await page.route("**/api/auth/verify-otp", (route) => {
      received = route.request().postDataJSON();
      return route.fulfill({ json: { token: "verified-token" } });
    });
    await page.goto(
      viaSso
        ? "/#/connexion?oidcEmail=alice%40example.test&challenge=" + challenge
        : "/#/connexion",
    );
    if (!viaSso) {
      await page
        .getByLabel("Email", { exact: true })
        .fill("alice@example.test");
      await page
        .getByLabel("Mot de passe", { exact: true })
        .fill("password123");
      await page
        .getByRole("button", { name: "Recevoir mon code de connexion" })
        .click();
    }
    await page.getByLabel("Code à 6 chiffres").fill("123456");
    await expect(page).toHaveURL(/\/#\/$/);
    expect(received).toEqual({
      email: "alice@example.test",
      code: "123456",
      challenge,
    });
  });
}

test("la configuration serveur masque le SSO désactivé et permet le login dev sans OTP", async ({
  page,
}) => {
  await fixture(page, { mfa: false });
  await page.goto("/#/connexion");
  await expect(
    page.getByRole("heading", { name: "Se connecter", exact: true }),
  ).toBeVisible();
  await expect(page.getByRole("link", { name: /Continuer avec/ })).toHaveCount(
    0,
  );
  await page.getByLabel("Email", { exact: true }).fill("alice@example.test");
  await page.getByLabel("Mot de passe", { exact: true }).fill("password123");
  await page
    .getByRole("button", { name: "Recevoir mon code de connexion" })
    .click();
  await expect(page).toHaveURL(/\/#\/$/);
  expect(await page.evaluate(() => localStorage.getItem("mazlove.token"))).toBe(
    "dev-login-token",
  );
});

test("le SSO actif est proposé et la réponse serveur avec 2FA exige le code", async ({
  page,
}) => {
  await fixture(page, { oidc: true });
  await page.goto("/#/connexion");
  await expect(
    page.getByRole("link", { name: "Continuer avec Google" }),
  ).toHaveAttribute("href", "/oauth2/authorization/oidc");
  await page.getByLabel("Email", { exact: true }).fill("alice@example.test");
  await page.getByLabel("Mot de passe", { exact: true }).fill("password123");
  await page
    .getByRole("button", { name: "Recevoir mon code de connexion" })
    .click();
  await expect(
    page.getByRole("heading", { name: "Votre code de connexion" }),
  ).toBeVisible();
  expect(
    await page.evaluate(() => localStorage.getItem("mazlove.token")),
  ).toBeNull();
});

test("le retour SSO avec OTP ne connecte pas avant la saisie du code et affiche les refus", async ({
  page,
}) => {
  await fixture(page, { oidc: true });
  await page.goto("/#/connexion?oidcEmail=alice%40example.test");
  await expect(page.getByLabel("Code à 6 chiffres")).toBeVisible();
  expect(
    await page.evaluate(() => localStorage.getItem("mazlove.token")),
  ).toBeNull();
  await page.goto("/#/connexion?oidcError=email_not_verified");
  await page.reload();
  await expect(
    page.getByText("Le fournisseur n'a pas vérifié votre adresse email."),
  ).toBeVisible();
});
