import { test, expect, type APIRequestContext } from "@playwright/test";
import { apiToken, seedClub, seedTeam } from "./helpers/api";

// RBAC / club-scoping negatives exercised end-to-end through the stack (real
// Keycloak tokens, nginx→bff→service). Maps to RBAC-05/06, CAT-05, MATCH-02.
// `gestor` (club-manager) is not assigned to any club group, so every
// club-scoped write must be refused; `staff` may not manage the catalogue.
const API = process.env.E2E_BASE_URL ?? "http://localhost:8000";

function postAs(request: APIRequestContext, token: string, path: string, data: unknown) {
  return request.post(`${API}${path}`, {
    headers: { Authorization: `Bearer ${token}`, Origin: API },
    data,
  });
}

test.describe("Authorization negatives (RBAC scoping)", () => {
  test("RBAC-05/CAT-05: a club-manager cannot add a team to a club they don't manage", async ({ request }) => {
    const admin = await apiToken(request, "admin", "admin");
    const club = await seedClub(request, admin, `E2E Authz ${Date.now()}`);
    const gestor = await apiToken(request, "gestor", "gestor");
    const res = await postAs(request, gestor, `/api/v1/clubs/${club.id}/teams`, { category: "SENIOR_M" });
    expect(res.status()).toBe(403);
  });

  test("club-manager cannot create a club (admin-only)", async ({ request }) => {
    const gestor = await apiToken(request, "gestor", "gestor");
    const res = await postAs(request, gestor, "/api/v1/clubs", { name: `E2E NoAuthz ${Date.now()}` });
    expect(res.status()).toBe(403);
  });

  test("MATCH-02: a club-manager cannot schedule a match for a club they don't manage", async ({ request }) => {
    const admin = await apiToken(request, "admin", "admin");
    const stamp = Date.now();
    const home = await seedClub(request, admin, `E2E AzCasa ${stamp}`);
    const away = await seedClub(request, admin, `E2E AzFora ${stamp}`);
    const homeTeam = await seedTeam(request, admin, home.id);
    const awayTeam = await seedTeam(request, admin, away.id);
    const gestor = await apiToken(request, "gestor", "gestor");
    const res = await postAs(request, gestor, "/api/v1/matches", {
      homeTeamId: homeTeam.id,
      awayTeamId: awayTeam.id,
      venueId: null,
      kickoffAt: "2030-09-01T18:00:00Z",
      refereeName: null,
    });
    expect(res.status()).toBe(403);
  });

  test("RBAC-06: staff cannot create catalogue entities", async ({ request }) => {
    const admin = await apiToken(request, "admin", "admin");
    const club = await seedClub(request, admin, `E2E StaffAz ${Date.now()}`);
    const staff = await apiToken(request, "staff", "staff");
    const clubRes = await postAs(request, staff, "/api/v1/clubs", { name: `E2E StaffClub ${Date.now()}` });
    expect(clubRes.status()).toBe(403);
    const teamRes = await postAs(request, staff, `/api/v1/clubs/${club.id}/teams`, { category: "SENIOR_M" });
    expect(teamRes.status()).toBe(403);
  });
});
