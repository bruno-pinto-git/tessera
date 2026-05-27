import { apiDelete, apiGet, apiPatch, apiPost } from "@/api/client";
import type { components } from "@/api/schema.gen";

export type Club = components["schemas"]["Club"];
export type ClubCreateRequest = components["schemas"]["ClubCreateRequest"];
export type ClubUpdateRequest = components["schemas"]["ClubUpdateRequest"];

export interface PageEnvelope<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ListClubsParams {
  page?: number;
  size?: number;
  name?: string;
}

export function listClubs(params: ListClubsParams = {}) {
  const q = new URLSearchParams();
  if (params.page != null) q.set("page", String(params.page));
  if (params.size != null) q.set("size", String(params.size));
  if (params.name) q.set("name", params.name);
  const qs = q.toString();
  return apiGet<PageEnvelope<Club>>(`/clubs${qs ? `?${qs}` : ""}`);
}

export function getClub(id: number) {
  return apiGet<Club>(`/clubs/${id}`);
}

export function createClub(body: ClubCreateRequest) {
  return apiPost<ClubCreateRequest, Club>("/clubs", body);
}

export function updateClub(id: number, body: ClubUpdateRequest) {
  return apiPatch<ClubUpdateRequest, Club>(`/clubs/${id}`, body);
}

export function deleteClub(id: number) {
  return apiDelete(`/clubs/${id}`);
}
