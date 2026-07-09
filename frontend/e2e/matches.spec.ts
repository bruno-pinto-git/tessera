import { test, expect } from "@playwright/test";
import { login, USERS } from "./helpers/auth";
import { apiToken, seedClub, seedTeam, seedVenue, seedMatch } from "./helpers/api";

const API = process.env.E2E_BASE_URL ?? "http://localhost:8000";

// "4. Matches" + "6. Box Office" of the QA plan. Prerequisites (clubs + teams +
// venue) are seeded via the API so the journeys are deterministic on a fresh
// stack; the match and the box office are driven through the real UI.
test.describe("Match scheduling & box office", () => {
  test("MATCH-01: admin schedules a match via the UI form", async ({ page, request }) => {
    const stamp = Date.now();
    const token = await apiToken(request, USERS.admin.username, USERS.admin.password);
    const home = await seedClub(request, token, `E2E Casa ${stamp}`);
    const away = await seedClub(request, token, `E2E Fora ${stamp}`);
    const homeTeam = await seedTeam(request, token, home.id);
    const awayTeam = await seedTeam(request, token, away.id);
    const venue = await seedVenue(request, token, `E2E Estádio ${stamp}`, 5000);

    await login(page, USERS.admin.username, USERS.admin.password);
    await page.goto("/admin/matches");
    await page.getByRole("button", { name: "Novo jogo" }).click();

    const dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("heading", { name: "Novo jogo" })).toBeVisible();

    await dialog.locator("#m-home-club").selectOption(String(home.id));
    await expect(dialog.locator(`#m-home-team option[value="${homeTeam.id}"]`)).toHaveCount(1);
    await dialog.locator("#m-home-team").selectOption(String(homeTeam.id));

    await dialog.locator("#m-away-club").selectOption(String(away.id));
    await expect(dialog.locator(`#m-away-team option[value="${awayTeam.id}"]`)).toHaveCount(1);
    await dialog.locator("#m-away-team").selectOption(String(awayTeam.id));

    await dialog.locator("#m-venue").selectOption(String(venue.id));
    await dialog.locator("#m-kickoff").fill("2030-06-01T18:00");
    await dialog.getByRole("button", { name: "Criar" }).click();

    await expect(page.getByRole("dialog")).toHaveCount(0);
    await expect(page.getByRole("cell", { name: `E2E Casa ${stamp}` })).toBeVisible();
  });

  test("BOX-01: admin opens a box office and it becomes a public event", async ({ page, request }) => {
    const stamp = Date.now();
    const token = await apiToken(request, USERS.admin.username, USERS.admin.password);
    const home = await seedClub(request, token, `E2E Bilh Casa ${stamp}`);
    const away = await seedClub(request, token, `E2E Bilh Fora ${stamp}`);
    const homeTeam = await seedTeam(request, token, home.id);
    const awayTeam = await seedTeam(request, token, away.id);
    const venue = await seedVenue(request, token, `E2E Bilh Estádio ${stamp}`, 3000);
    const match = await seedMatch(request, token, homeTeam.id, awayTeam.id, venue.id, "2030-07-01T18:00:00Z");

    await login(page, USERS.admin.username, USERS.admin.password);
    await page.goto("/admin/matches");

    // Far-future match → top of the desc-sorted list; wait for its row (club
    // names resolve async), then hit that row's "Abrir bilheteira".
    const row = page.getByRole("row", { name: new RegExp(`E2E Bilh Casa ${stamp}`) });
    await expect(row).toBeVisible();
    await row.getByRole("button", { name: "Abrir bilheteira" }).click();

    const dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("heading", { name: "Abrir bilheteira" })).toBeVisible();
    await dialog.getByRole("button", { name: "Abrir bilheteira" }).click(); // defaults 8.00 / 4.00
    await expect(page.getByRole("dialog")).toHaveCount(0);

    // The box office is a PUBLISHED ticket-service event pointing at our match.
    const res = await request.get(`${API}/api/v1/events?size=100`);
    expect(res.status()).toBe(200);
    const events = (await res.json()).content as Array<{ matchId: number | null; status: string }>;
    expect(events.some((e) => e.matchId === match.id && e.status === "PUBLISHED")).toBeTruthy();
  });
});
