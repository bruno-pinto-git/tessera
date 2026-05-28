import { apiDelete, apiGet, apiPatch, apiPost } from "@/api/client";
import type { components } from "@/api/schema.gen";

export type Team = components["schemas"]["Team"];
export type TeamCategory = components["schemas"]["TeamCategory"];
export type TeamCreateRequest = components["schemas"]["TeamCreateRequest"];
export type TeamUpdateRequest = components["schemas"]["TeamUpdateRequest"];

export const TEAM_CATEGORIES: TeamCategory[] = [
  "SENIOR_M",
  "SENIOR_F",
  "SUB_23",
  "SUB_19",
  "SUB_17",
  "SUB_15",
  "SUB_13",
  "SUB_11",
  "SUB_9",
  "SUB_7",
  "VETERANS",
  "OTHER",
];

export function teamCategoryLabel(c: TeamCategory): string {
  switch (c) {
    case "SENIOR_M":
      return "Sénior Masculina";
    case "SENIOR_F":
      return "Sénior Feminina";
    case "SUB_23":
      return "Sub-23";
    case "SUB_19":
      return "Sub-19";
    case "SUB_17":
      return "Sub-17";
    case "SUB_15":
      return "Sub-15";
    case "SUB_13":
      return "Sub-13";
    case "SUB_11":
      return "Sub-11";
    case "SUB_9":
      return "Sub-9";
    case "SUB_7":
      return "Sub-7";
    case "VETERANS":
      return "Veteranos";
    case "OTHER":
      return "Outra";
  }
}

export function listTeamsByClub(clubId: number) {
  return apiGet<Team[]>(`/clubs/${clubId}/teams`);
}

export function getTeam(id: number) {
  return apiGet<Team>(`/teams/${id}`);
}

export function createTeam(clubId: number, body: TeamCreateRequest) {
  return apiPost<TeamCreateRequest, Team>(`/clubs/${clubId}/teams`, body);
}

export function updateTeam(id: number, body: TeamUpdateRequest) {
  return apiPatch<TeamUpdateRequest, Team>(`/teams/${id}`, body);
}

export function deleteTeam(id: number) {
  return apiDelete(`/teams/${id}`);
}
