import { apiDelete, apiGet, apiPatch, apiPost } from "@/api/client";
import type { components } from "@/api/schema.gen";

export type Player = components["schemas"]["Player"];
export type PlayerCreateRequest = components["schemas"]["PlayerCreateRequest"];
export type PlayerUpdateRequest = components["schemas"]["PlayerUpdateRequest"];
export type PlayerPosition = components["schemas"]["PlayerPosition"];
export type DominantFoot = components["schemas"]["DominantFoot"];
export type PlayerStatus = components["schemas"]["PlayerStatus"];

export interface PageEnvelope<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const POSITIONS: PlayerPosition[] = ["GK", "DF", "MF", "FW"];
export const DOMINANT_FEET: DominantFoot[] = ["LEFT", "RIGHT", "BOTH"];
export const STATUSES: PlayerStatus[] = ["ACTIVE", "INJURED", "SUSPENDED"];

export function positionLabel(p: PlayerPosition): string {
  switch (p) {
    case "GK":
      return "Guarda-redes";
    case "DF":
      return "Defesa";
    case "MF":
      return "Médio";
    case "FW":
      return "Avançado";
  }
}

export function dominantFootLabel(f: DominantFoot): string {
  switch (f) {
    case "LEFT":
      return "Esquerdo";
    case "RIGHT":
      return "Direito";
    case "BOTH":
      return "Ambidestro";
  }
}

export function statusLabel(s: PlayerStatus): string {
  switch (s) {
    case "ACTIVE":
      return "Activo";
    case "INJURED":
      return "Lesionado";
    case "SUSPENDED":
      return "Suspenso";
  }
}

export function listPlayersByTeam(teamId: number, params: { page?: number; size?: number } = {}) {
  const q = new URLSearchParams();
  if (params.page != null) q.set("page", String(params.page));
  if (params.size != null) q.set("size", String(params.size));
  const qs = q.toString();
  return apiGet<PageEnvelope<Player>>(`/teams/${teamId}/players${qs ? `?${qs}` : ""}`);
}

export function getPlayer(id: number) {
  return apiGet<Player>(`/players/${id}`);
}

export function createPlayer(teamId: number, body: PlayerCreateRequest) {
  return apiPost<PlayerCreateRequest, Player>(`/teams/${teamId}/players`, body);
}

export function updatePlayer(id: number, body: PlayerUpdateRequest) {
  return apiPatch<PlayerUpdateRequest, Player>(`/players/${id}`, body);
}

export function deletePlayer(id: number) {
  return apiDelete(`/players/${id}`);
}
