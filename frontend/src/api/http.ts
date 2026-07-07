import { keycloak } from "@/auth/keycloak";

const API_BASE = "/api";

export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);

  if (keycloak.authenticated && keycloak.token) {
    try {
      await keycloak.updateToken(30);
    } catch {
    }
    headers.set("Authorization", `Bearer ${keycloak.token}`);
  }

  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  return fetch(`${API_BASE}${path}`, { ...init, headers });
}
