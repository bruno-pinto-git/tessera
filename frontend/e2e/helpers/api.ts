import { type APIRequestContext, expect } from "@playwright/test";

// Seed prerequisite data through the real API so UI journeys that need existing
// entities (matches, box offices, sheets) are deterministic on a fresh stack.
const API = process.env.E2E_BASE_URL ?? "http://localhost:8000";
const KC = process.env.E2E_KEYCLOAK_URL ?? "http://localhost:8180";

export async function apiToken(
  request: APIRequestContext,
  username: string,
  password: string,
): Promise<string> {
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

async function post<T = { id: number }>(
  request: APIRequestContext,
  token: string,
  path: string,
  data: unknown,
): Promise<T> {
  const res = await request.post(`${API}${path}`, {
    headers: { Authorization: `Bearer ${token}`, Origin: API },
    data,
  });
  expect(res.ok(), `POST ${path} → ${res.status()}`).toBeTruthy();
  return (await res.json()) as T;
}

export function seedClub(request: APIRequestContext, token: string, name: string) {
  return post(request, token, "/api/v1/clubs", { name });
}

export function seedVenue(request: APIRequestContext, token: string, name: string, capacity = 1000) {
  return post(request, token, "/api/v1/venues", { name, capacity });
}

export function seedTeam(request: APIRequestContext, token: string, clubId: number, category = "SENIOR_M") {
  return post(request, token, `/api/v1/clubs/${clubId}/teams`, { category });
}

export function seedPlayer(
  request: APIRequestContext,
  token: string,
  teamId: number,
  firstName: string,
  lastName: string,
  shirtNumber: number,
  position = "MF",
) {
  return post(request, token, `/api/v1/teams/${teamId}/players`, {
    firstName,
    lastName,
    position,
    shirtNumber,
  });
}

export function seedMatch(
  request: APIRequestContext,
  token: string,
  homeTeamId: number,
  awayTeamId: number,
  venueId: number,
  kickoffAt: string,
) {
  return post(request, token, "/api/v1/matches", {
    homeTeamId,
    awayTeamId,
    venueId,
    kickoffAt,
    refereeName: null,
  });
}

// Opens a box office (PUBLISHED ticket-service Event) for a match; returns the
// event, whose id is the /events/:id public-detail route param.
export function seedEvent(request: APIRequestContext, token: string, matchId: number, name: string) {
  return post(request, token, "/api/v1/events", {
    name,
    matchLabel: name,
    matchId,
    priceNormal: "8.00",
    priceSupporter: "4.00",
    status: "PUBLISHED",
  });
}

export function seedLineupEntry(
  request: APIRequestContext,
  token: string,
  matchId: number,
  playerId: number,
  role = "STARTER",
) {
  return post(request, token, `/api/v1/matches/${matchId}/sheet/lineup`, {
    playerId,
    role,
    shirtNumber: null,
  });
}

export function seedOccurrence(
  request: APIRequestContext,
  token: string,
  matchId: number,
  playerId: number,
  minute: number,
  type = "GOAL",
) {
  return post(request, token, `/api/v1/matches/${matchId}/sheet/occurrences`, {
    minute,
    type,
    playerId,
    replacedPlayerId: null,
  });
}

export async function lockSheet(request: APIRequestContext, token: string, matchId: number) {
  const res = await request.post(`${API}/api/v1/matches/${matchId}/sheet/lock`, {
    headers: { Authorization: `Bearer ${token}`, Origin: API },
  });
  expect(res.ok(), `lock sheet ${matchId} → ${res.status()}`).toBeTruthy();
}
