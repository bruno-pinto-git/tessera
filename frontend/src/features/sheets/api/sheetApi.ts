import { apiDelete, apiGet, apiPatch, apiPost, apiPostNoBody } from "@/api/client";
import type { components } from "@/api/schema.gen";

export type MatchSheet = components["schemas"]["matchSheet"];
export type LineupEntry = components["schemas"]["lineupEntry"];
export type LineupRole = components["schemas"]["lineupRole"];
export type LineupCreateRequest = components["schemas"]["lineupCreateRequest"];
export type LineupUpdateRequest = components["schemas"]["lineupUpdateRequest"];
export type Occurrence = components["schemas"]["occurrence"];
export type OccurrenceType = components["schemas"]["occurrenceType"];
export type OccurrenceCreateRequest = components["schemas"]["occurrenceCreateRequest"];
export type MatchSheetHistory = components["schemas"]["matchSheetHistory"];

// ---- Editor (live sheet in match-service, scoped by @clubAuthz.canEditSheet) ----

export function getMatchSheet(matchId: number) {
  return apiGet<MatchSheet>(`/matches/${matchId}/sheet`);
}

export function addLineupEntry(matchId: number, body: LineupCreateRequest) {
  return apiPost<LineupCreateRequest, LineupEntry>(`/matches/${matchId}/sheet/lineup`, body);
}

export function updateLineupEntry(matchId: number, playerId: number, body: LineupUpdateRequest) {
  return apiPatch<LineupUpdateRequest, LineupEntry>(
    `/matches/${matchId}/sheet/lineup/${playerId}`,
    body,
  );
}

export function removeLineupEntry(matchId: number, playerId: number) {
  return apiDelete(`/matches/${matchId}/sheet/lineup/${playerId}`);
}

export function addOccurrence(matchId: number, body: OccurrenceCreateRequest) {
  return apiPost<OccurrenceCreateRequest, Occurrence>(
    `/matches/${matchId}/sheet/occurrences`,
    body,
  );
}

export function removeOccurrence(matchId: number, occurrenceId: number) {
  return apiDelete(`/matches/${matchId}/sheet/occurrences/${occurrenceId}`);
}

export function lockMatchSheet(matchId: number) {
  return apiPostNoBody<MatchSheet>(`/matches/${matchId}/sheet/lock`);
}

export function unlockMatchSheet(matchId: number) {
  return apiPostNoBody<MatchSheet>(`/matches/${matchId}/sheet/unlock`);
}

// ---- Public read-side (statistics snapshot of a closed sheet) ----

export function getMatchSheetHistory(matchId: number) {
  return apiGet<MatchSheetHistory>(`/stats/match-sheets/${matchId}`);
}
