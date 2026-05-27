import { apiGet, apiPost } from "@/api/client";
import type { components } from "@/api/schema.gen";

export type Event = components["schemas"]["Event"];
export type EventCreateRequest = components["schemas"]["EventCreateRequest"];

export interface PageEnvelope<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export function listEvents(params: { page?: number; size?: number } = {}) {
  const q = new URLSearchParams();
  if (params.page != null) q.set("page", String(params.page));
  if (params.size != null) q.set("size", String(params.size));
  const qs = q.toString();
  return apiGet<PageEnvelope<Event>>(`/events${qs ? `?${qs}` : ""}`);
}

export function getEvent(id: number) {
  return apiGet<Event>(`/events/${id}`);
}

export function createEvent(body: EventCreateRequest) {
  return apiPost<EventCreateRequest, Event>("/events", body);
}
