import { apiDelete, apiGet, apiPost, apiPut } from "./client";

export interface UserSummary {
  id: string;
  username: string | null;
  email: string | null;
  firstName: string | null;
  lastName: string | null;
  enabled: boolean | null;
  roles: string[];
}

export interface CreateUserRequest {
  username: string;
  email?: string;
  firstName: string;
  lastName: string;
  password: string;
  role: "club-manager" | "staff";
}

export interface UpdateUserRequest {
  email?: string;
  firstName?: string;
  lastName?: string;
  enabled?: boolean;
  role?: "club-manager" | "staff";
  forcePasswordReset?: boolean;
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

export function updateUser(id: string, body: UpdateUserRequest) {
  return apiPut<UpdateUserRequest, UserSummary>(`/users/${encodeURIComponent(id)}`, body);
}

export function deleteUser(id: string) {
  return apiDelete(`/users/${encodeURIComponent(id)}`);
}
