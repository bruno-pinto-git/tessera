import { type Page, expect } from "@playwright/test";

// Test users imported from the realm (infra/keycloak/realm-export.json).
export const USERS = {
  admin: { username: "admin", password: "admin" },
  gestor: { username: "gestor", password: "gestor" },
  staff: { username: "staff", password: "staff" },
  adepto: { username: "adepto", password: "adepto" },
} as const;

// Drives the real Keycloak login: open the app, hit "Entrar", fill the
// Keycloak-hosted form, submit, and wait until the SPA is authenticated
// (the "Entrar" button is gone). check-sso gives each fresh context an
// unauthenticated start, so this runs once per test.
export async function login(page: Page, username: string, password: string) {
  await page.goto("/");
  // The header holds exactly one "Entrar" button (the UserMenu); scoping avoids
  // the extra "Entrar" CTA the HomePage renders in its body.
  await page.locator("header").getByRole("button", { name: "Entrar" }).click();
  await page.locator("#username").waitFor({ state: "visible" });
  await page.locator("#username").fill(username);
  await page.locator("#password").fill(password);
  await page.locator("#kc-login").click();
  await expect(
    page.locator("header").getByRole("button", { name: "Entrar" }),
  ).toHaveCount(0, { timeout: 20_000 });
}

export async function logout(page: Page) {
  // Open the user menu (avatar button, top-right) and click "Sair".
  await page.getByRole("button", { name: "Entrar" }).waitFor({ state: "detached" }).catch(() => {});
  const avatar = page.locator("header button").last();
  await avatar.click();
  await page.getByRole("menuitem", { name: "Sair" }).click();
  await expect(page.getByRole("button", { name: "Entrar" }).first()).toBeVisible({
    timeout: 20_000,
  });
}
