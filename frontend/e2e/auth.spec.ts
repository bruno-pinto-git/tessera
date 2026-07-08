import { test, expect, type Page } from "@playwright/test";
import { login, USERS } from "./helpers/auth";

// Header-scoped, exact-name nav locators so content-area links/CTAs that share
// text (e.g. "Jogos"/"Ver jogos") don't cause strict-mode ambiguity.
const navLink = (page: Page, name: string) =>
  page.locator("header").getByRole("link", { name, exact: true });

// Maps to the "1. Authentication & Session" and "2. Role-based Access &
// Navigation" sections of the QA plan (docs/QA/Full-QA-Test-Plan.xlsx).
test.describe("Auth & role-based navigation", () => {
  test("AUTH-01 / RBAC-01: admin logs in and sees the Admin nav", async ({ page }) => {
    await login(page, USERS.admin.username, USERS.admin.password);
    await expect(navLink(page, "Admin")).toBeVisible();
    await expect(navLink(page, "Os meus bilhetes")).toBeVisible();
  });

  test("AUTH-02: invalid credentials are rejected at Keycloak", async ({ page }) => {
    await page.goto("/");
    await page.locator("header").getByRole("button", { name: "Entrar" }).click();
    await page.locator("#username").fill(USERS.admin.username);
    await page.locator("#password").fill("definitely-wrong");
    await page.locator("#kc-login").click();
    // Still on the Keycloak login form (not admitted): the username field remains.
    await expect(page.locator("#username")).toBeVisible();
    await expect(page).toHaveURL(/realms\/tessera/);
  });

  test("RBAC-02/03: club manager sees 'O meu clube' but not Admin", async ({ page }) => {
    await login(page, USERS.gestor.username, USERS.gestor.password);
    await expect(navLink(page, "O meu clube")).toBeVisible();
    await expect(navLink(page, "Admin")).toHaveCount(0);
  });

  test("RBAC-04: fan sees tickets but neither club nor admin", async ({ page }) => {
    await login(page, USERS.adepto.username, USERS.adepto.password);
    await expect(navLink(page, "Os meus bilhetes")).toBeVisible();
    await expect(navLink(page, "O meu clube")).toHaveCount(0);
    await expect(navLink(page, "Admin")).toHaveCount(0);
  });

  test("RBAC/anon: anonymous visitor sees Jogos + Entrar, no ticket/club/admin nav", async ({
    page,
  }) => {
    await page.goto("/");
    await expect(navLink(page, "Jogos")).toBeVisible();
    await expect(page.locator("header").getByRole("button", { name: "Entrar" })).toBeVisible();
    await expect(navLink(page, "Os meus bilhetes")).toHaveCount(0);
    await expect(navLink(page, "Admin")).toHaveCount(0);
  });
});
