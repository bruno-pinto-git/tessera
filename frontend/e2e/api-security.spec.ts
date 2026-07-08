import { test, expect, type APIRequestContext } from "@playwright/test";

// These hit the real running stack end-to-end (nginx → bff → service, real
// Keycloak), unlike the backend @WebMvcTest suite which mocks the edges.
// Maps to AUTH-08, STATS-04, RBAC-06 and the CORS regression from the deploy.
const API = process.env.E2E_BASE_URL ?? "http://localhost:8000";
const KC = process.env.E2E_KEYCLOAK_URL ?? "http://localhost:8180";

async function token(request: APIRequestContext, username: string, password: string): Promise<string> {
  const res = await request.post(`${KC}/realms/tessera/protocol/openid-connect/token`, {
    form: {
      grant_type: "password",
      client_id: "tessera-web",
      username,
      password,
      scope: "openid",
    },
  });
  expect(res.ok(), `token request for ${username}`).toBeTruthy();
  return (await res.json()).access_token as string;
}

test.describe("API security & CORS (through the running stack)", () => {
  test("SMOKE: public catalogue read returns 200", async ({ request }) => {
    const res = await request.get(`${API}/api/v1/clubs`);
    expect(res.status()).toBe(200);
  });

  test("CORS: preflight for a browser write is allowed for the site origin", async ({ request }) => {
    const res = await request.fetch(`${API}/api/v1/clubs`, {
      method: "OPTIONS",
      headers: {
        Origin: API,
        "Access-Control-Request-Method": "POST",
        "Access-Control-Request-Headers": "authorization,content-type",
      },
    });
    expect(res.status()).toBe(200);
    expect(res.headers()["access-control-allow-origin"]).toBe(API);
  });

  test("AUTH-08: a protected endpoint without a token is 401", async ({ request }) => {
    const res = await request.get(`${API}/api/v1/stats/sales/summary`);
    expect(res.status()).toBe(401);
  });

  test("STATS-04: non-admin is 403 on the global stats summary; admin is 200", async ({ request }) => {
    const gestor = await token(request, "gestor", "gestor");
    const forbidden = await request.get(`${API}/api/v1/stats/sales/summary`, {
      headers: { Authorization: `Bearer ${gestor}` },
    });
    expect(forbidden.status()).toBe(403);

    const admin = await token(request, "admin", "admin");
    const ok = await request.get(`${API}/api/v1/stats/sales/summary`, {
      headers: { Authorization: `Bearer ${admin}` },
    });
    expect(ok.status()).toBe(200);
  });

  test("RBAC-06: a fan cannot create a club (403)", async ({ request }) => {
    const fan = await token(request, "adepto", "adepto");
    const res = await request.post(`${API}/api/v1/clubs`, {
      headers: { Authorization: `Bearer ${fan}`, Origin: API },
      data: { name: "E2E — should be rejected" },
    });
    expect(res.status()).toBe(403);
  });
});
