import { apiDelete, apiGet, apiPatch, apiPost } from "@/api/client";
import type { components } from "@/api/schema.gen";

export type Venue = components["schemas"]["Venue"];
export type VenueCreateRequest = components["schemas"]["VenueCreateRequest"];
export type VenueUpdateRequest = components["schemas"]["VenueUpdateRequest"];

export interface PageEnvelope<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ListVenuesParams {
  page?: number;
  size?: number;
  name?: string;
}

export function listVenues(params: ListVenuesParams = {}) {
  const q = new URLSearchParams();
  if (params.page != null) q.set("page", String(params.page));
  if (params.size != null) q.set("size", String(params.size));
  if (params.name) q.set("name", params.name);
  const qs = q.toString();
  return apiGet<PageEnvelope<Venue>>(`/venues${qs ? `?${qs}` : ""}`);
}

export function getVenue(id: number) {
  return apiGet<Venue>(`/venues/${id}`);
}

export function createVenue(body: VenueCreateRequest) {
  return apiPost<VenueCreateRequest, Venue>("/venues", body);
}

export function updateVenue(id: number, body: VenueUpdateRequest) {
  return apiPatch<VenueUpdateRequest, Venue>(`/venues/${id}`, body);
}

export function deleteVenue(id: number) {
  return apiDelete(`/venues/${id}`);
}
