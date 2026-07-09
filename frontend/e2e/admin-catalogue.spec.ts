import { test, expect } from "@playwright/test";
import { login, USERS } from "./helpers/auth";
import { apiToken, seedClub, seedTeam } from "./helpers/api";

// "3. Clubs, Teams, Players & Venues" + a route-guard from "2. RBAC" of the QA
// plan. These drive the real browser write paths (POST through nginx→bff→match)
// — the class of request the CORS bug used to reject with 403.
test.describe("Admin catalogue journeys", () => {
  test("CAT-01: admin creates a club and it shows up in the list", async ({ page }) => {
    const clubName = `E2E Clube ${Date.now()}`;
    await login(page, USERS.admin.username, USERS.admin.password);

    await page.goto("/admin/clubs");
    await page.getByRole("button", { name: "Novo clube" }).click();

    const dialog = page.getByRole("dialog");
    await expect(dialog.getByLabel("Nome")).toBeVisible();
    await dialog.getByLabel("Nome").fill(clubName);
    await dialog.getByRole("button", { name: "Criar" }).click();

    await expect(page.getByRole("dialog")).toHaveCount(0);
    await page.getByPlaceholder(/Procurar por nome/).fill(clubName);
    await expect(page.getByRole("cell", { name: clubName, exact: true })).toBeVisible();
  });

  test("CAT-02: admin creates a venue", async ({ page }) => {
    const venueName = `E2E Estádio ${Date.now()}`;
    await login(page, USERS.admin.username, USERS.admin.password);

    await page.goto("/admin/venues");
    await page.getByRole("button", { name: "Novo estádio" }).click();

    const dialog = page.getByRole("dialog");
    await dialog.locator("#v-name").fill(venueName);
    await dialog.locator("#v-capacity").fill("4200");
    await dialog.getByRole("button", { name: "Criar" }).click();

    await expect(page.getByRole("dialog")).toHaveCount(0);
    await page.getByPlaceholder(/Procurar por nome/).fill(venueName);
    await expect(page.getByRole("cell", { name: venueName, exact: true })).toBeVisible();
  });

  test("CAT-03: admin adds a team to a club", async ({ page, request }) => {
    const token = await apiToken(request, USERS.admin.username, USERS.admin.password);
    const club = await seedClub(request, token, `E2E Equipas ${Date.now()}`);
    await login(page, USERS.admin.username, USERS.admin.password);

    await page.goto("/admin/teams");
    await page.locator("#club").selectOption(String(club.id));
    await page.getByRole("button", { name: "Nova equipa" }).click();

    const dialog = page.getByRole("dialog");
    // Category defaults to SENIOR_M → "Sénior Masculina".
    await dialog.getByRole("button", { name: "Criar" }).click();

    await expect(page.getByRole("dialog")).toHaveCount(0);
    await expect(page.getByRole("cell", { name: "Sénior Masculina", exact: true })).toBeVisible();
  });

  test("CAT-04: admin adds a player to a team", async ({ page, request }) => {
    const stamp = Date.now();
    const token = await apiToken(request, USERS.admin.username, USERS.admin.password);
    const club = await seedClub(request, token, `E2E Plantel ${stamp}`);
    const team = await seedTeam(request, token, club.id);
    const first = "Jogador";
    const last = `E2E ${stamp}`;
    await login(page, USERS.admin.username, USERS.admin.password);

    await page.goto("/admin/players");
    await page.locator("#club").selectOption(String(club.id));
    // Team options load async after the club is picked.
    await expect(page.locator(`#team option[value="${team.id}"]`)).toHaveCount(1);
    await page.locator("#team").selectOption(String(team.id));
    await page.getByRole("button", { name: "Novo jogador" }).click();

    const dialog = page.getByRole("dialog");
    await dialog.locator("#firstName").fill(first);
    await dialog.locator("#lastName").fill(last);
    await dialog.getByRole("button", { name: "Criar" }).click();

    await expect(page.getByRole("dialog")).toHaveCount(0);
    await expect(page.getByRole("cell", { name: `${first} ${last}`, exact: true })).toBeVisible();
  });

  test("RBAC route guard: a fan hitting /admin/clubs lands on /unauthorized", async ({ page }) => {
    await login(page, USERS.adepto.username, USERS.adepto.password);
    await page.goto("/admin/clubs");
    await expect(page).toHaveURL(/\/unauthorized/);
    await expect(page.getByRole("button", { name: "Novo clube" })).toHaveCount(0);
  });
});
