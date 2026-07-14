import { test, expect, type APIRequestContext } from "@playwright/test";
import { login, USERS } from "./helpers/auth";
import {
  apiToken,
  seedClub,
  seedTeam,
  seedVenue,
  seedMatch,
  seedPlayer,
  seedLineupEntry,
  seedOccurrence,
  lockSheet,
  seedEvent,
} from "./helpers/api";

// "5. Match Sheet / Ficha Técnica" of the QA plan. Squad + lineup + a goal are
// seeded via the API (the two-column no-id lineup/occurrence forms are awkward
// to target reliably); the publish (lock) action and the public viewer — the
// parts that matter — are driven through the real UI.
async function seedFilledSheet(request: APIRequestContext, token: string, stamp: number) {
  const home = await seedClub(request, token, `E2E FichaCasa ${stamp}`);
  const away = await seedClub(request, token, `E2E FichaFora ${stamp}`);
  const homeTeam = await seedTeam(request, token, home.id);
  const awayTeam = await seedTeam(request, token, away.id);
  const venue = await seedVenue(request, token, `E2E FichaEst ${stamp}`, 2000);
  const match = await seedMatch(request, token, homeTeam.id, awayTeam.id, venue.id, "2030-08-01T18:00:00Z");
  const player = await seedPlayer(request, token, homeTeam.id, "Marcador", `E2E ${stamp}`, 9);
  await seedLineupEntry(request, token, match.id, player.id);
  await seedOccurrence(request, token, match.id, player.id, 23);
  return match;
}

test.describe("Match sheet (ficha técnica)", () => {
  test("SHEET-03: admin locks (publishes) a filled sheet via the UI", async ({ page, request }) => {
    const stamp = Date.now();
    const token = await apiToken(request, USERS.admin.username, USERS.admin.password);
    const match = await seedFilledSheet(request, token, stamp);

    await login(page, USERS.admin.username, USERS.admin.password);
    await page.goto(`/matches/${match.id}/sheet`);

    await page.getByRole("button", { name: "Fechar ficha" }).click();

    await expect(page.getByRole("button", { name: "Reabrir ficha" })).toBeVisible();
    await expect(page.getByText("Ficha fechada")).toBeVisible();
  });

  test("SHEET-05: a published sheet renders on the public event page", async ({ page, request }) => {
    const stamp = Date.now();
    const token = await apiToken(request, USERS.admin.username, USERS.admin.password);
    const match = await seedFilledSheet(request, token, stamp);

    // Create event BEFORE locking the sheet
    const event = await seedEvent(request, token, match.id, `E2E Ficha Evento ${stamp}`);

    // Lock sheet AFTER event is created
    await lockSheet(request, token, match.id);

    // The public viewer reads statistics-service's match-sheet snapshot, built
    // asynchronously after the lock (match.sheet.closed over RabbitMQ). Reload
    // until it lands.
    await expect(async () => {
      await page.goto(`/events/${event.id}`);
      await expect(page.getByText("Onze inicial")).toBeVisible({ timeout: 3000 });
    }).toPass({ timeout: 45_000 });
  });
});
