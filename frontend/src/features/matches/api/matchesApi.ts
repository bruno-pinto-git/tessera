import { apiDelete, apiGet, apiPatch, apiPost } from "@/api/client";
import type { components } from "@/api/schema.gen";

/**
 * The match payload as returned by the API. The generated schema is not yet
 * regenerated to include `homeClubId`/`awayClubId`, but the backend returns
 * them (nullable), so we widen the type here.
 */
export type Match = components["schemas"]["Match"] & {
  homeClubId: number | null;
  awayClubId: number | null;
};
export type MatchStatus = components["schemas"]["MatchStatus"];
export type MatchCreateRequest = components["schemas"]["MatchCreateRequest"];
export type MatchUpdateRequest = components["schemas"]["MatchUpdateRequest"];

export interface PageEnvelope<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const MATCH_STATUSES: MatchStatus[] = [
  "SCHEDULED",
  "LIVE",
  "FINISHED",
  "POSTPONED",
  "ABANDONED",
  "CANCELLED",
];

export function matchStatusLabel(s: MatchStatus): string {
  switch (s) {
    case "SCHEDULED":
      return "Agendado";
    case "LIVE":
      return "A decorrer";
    case "FINISHED":
      return "Terminado";
    case "POSTPONED":
      return "Adiado";
    case "ABANDONED":
      return "Interrompido";
    case "CANCELLED":
      return "Cancelado";
  }
}

export interface ListMatchesParams {
  page?: number;
  size?: number;
  from?: string;
  to?: string;
  status?: MatchStatus;
  clubId?: number;
  sort?: string;
}

export function listMatches(params: ListMatchesParams = {}) {
  const q = new URLSearchParams();
  if (params.page != null) q.set("page", String(params.page));
  if (params.size != null) q.set("size", String(params.size));
  if (params.from) q.set("from", params.from);
  if (params.to) q.set("to", params.to);
  if (params.status) q.set("status", params.status);
  if (params.clubId != null) q.set("clubId", String(params.clubId));
  if (params.sort) q.set("sort", params.sort);
  const qs = q.toString();
  return apiGet<PageEnvelope<Match>>(`/matches${qs ? `?${qs}` : ""}`);
}

export function getMatch(id: number) {
  return apiGet<Match>(`/matches/${id}`);
}

export function createMatch(body: MatchCreateRequest) {
  return apiPost<MatchCreateRequest, Match>("/matches", body);
}

export function updateMatch(id: number, body: MatchUpdateRequest) {
  return apiPatch<MatchUpdateRequest, Match>(`/matches/${id}`, body);
}

export function deleteMatch(id: number) {
  return apiDelete(`/matches/${id}`);
}
