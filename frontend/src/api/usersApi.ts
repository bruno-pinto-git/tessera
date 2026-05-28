import { apiDelete, apiGet, apiPost } from "./client";

export interface UserSummary {
  id: string;
  username: string | null;
  email: string | null;
  firstName: string | null;
  lastName: string | null;
  enabled: boolean | null;
}

export interface CreateUserRequest {
  username: string;
  email?: string;
  firstName: string;
  lastName: string;
  password: string;
  /** Realm role assigned to the new user. */
  role: "club-manager" | "staff";
}

export function searchUsers(params: { search?: string; first?: number; max?: number } = {}) {
  const q = new URLSearchParams();
  if (params.search) q.set("search", params.search);
  if (params.first != null) q.set("first", String(params.first));
  if (params.max != null) q.set("max", String(params.max));
  const qs = q.toString();
  return apiGet<UserSummary[]>(`/users${qs ? `?${qs}` : ""}`);
}

export function createUser(body: CreateUserRequest) {
  return apiPost<CreateUserRequest, UserSummary>("/users", body);
}

export function deleteUser(id: string) {
  return apiDelete(`/users/${encodeURIComponent(id)}`);
}
