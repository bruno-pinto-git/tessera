import { apiGet } from "@/api/client";
import type { components } from "@/api/schema.gen";

export type SalesSummary = components["schemas"]["salesSummary"];
export type SalesByMatch = components["schemas"]["salesByMatch"];
export type SalesByClub = components["schemas"]["salesByClub"];
export type MatchSummary = components["schemas"]["matchSummary"];

export interface PageEnvelope<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Global sales summary — platform-admin only. */
export function getSalesSummary() {
  return apiGet<SalesSummary>("/stats/sales/summary");
}

/** Sales over a date range — platform-admin only. `from`/`to` are ISO instants. */
export function getSalesRange(from: string, to: string) {
  const q = new URLSearchParams({ from, to });
  return apiGet<SalesSummary>(`/stats/sales/range?${q.toString()}`);
}

/** Club-scoped sales — platform-admin or a manager of the club. */
export function getSalesByClub(clubId: number) {
  return apiGet<SalesByClub>(`/stats/sales/by-club/${clubId}`);
}

export interface MatchHistoryParams {
  page?: number;
  size?: number;
  clubId?: number;
  season?: string;
  status?: MatchSummary["matchStatus"];
  sort?: string;
}

/** Historical match-sheet snapshots — public. Newest first when sorted desc. */
export function listMatchHistory(params: MatchHistoryParams = {}) {
  const q = new URLSearchParams();
  if (params.page != null) q.set("page", String(params.page));
  if (params.size != null) q.set("size", String(params.size));
  if (params.clubId != null) q.set("clubId", String(params.clubId));
  if (params.season) q.set("season", params.season);
  if (params.status) q.set("status", params.status);
  if (params.sort) q.set("sort", params.sort);
  const qs = q.toString();
  return apiGet<PageEnvelope<MatchSummary>>(`/stats/match-sheets${qs ? `?${qs}` : ""}`);
}
