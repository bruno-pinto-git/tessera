import { apiGet, apiPost } from "./client";

/**
 * Ticket-service API client. After running `npm run codegen:api`, prefer
 * importing types from `@/api/schema.gen` instead of redeclaring them here
 * — e.g.:
 *
 *   import type { components } from "@/api/schema.gen";
 *   type Ticket = components["schemas"]["Ticket"];
 *
 * For now we keep slim hand-written types so the project compiles before
 * the first codegen run.
 */

export type TicketStatus = "PENDING" | "PAID" | "VALIDATED";

export interface Ticket {
  id: number;
  code: string;
  eventId: number;
  matchId: number | null;
  ownerSub: string | null;
  price: number | string; // Jackson emits number; spec says string. Accept both.
  status: TicketStatus;
  paymentMethod: "MBWAY" | "CARD" | "CASH" | null;
  createdAt: string;
  paymentDate: string | null;
  validationDate: string | null;
  validatorSub: string | null;
}

export interface PageEnvelope<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateTicketRequest {
  eventId: number;
  supporter: boolean;
}

export interface PayTicketRequest {
  paymentMethod: "MBWAY" | "CARD" | "CASH";
  mbwayReference?: string;
  /** Required when paymentMethod === "MBWAY". Format: "351912345678". */
  phoneNumber?: string;
}

export interface ValidateTicketRequest {
  code: string;
}

export function createTicket(body: CreateTicketRequest) {
  return apiPost<CreateTicketRequest, Ticket>("/tickets", body);
}

export function payTicket(id: number, body: PayTicketRequest) {
  return apiPost<PayTicketRequest, Ticket>(`/tickets/${id}/pay`, body);
}

export function validateTicket(body: ValidateTicketRequest) {
  return apiPost<ValidateTicketRequest, Ticket>("/tickets/validate", body);
}

export function listMyTickets(params: { page?: number; size?: number } = {}) {
  const q = new URLSearchParams();
  if (params.page != null) q.set("page", String(params.page));
  if (params.size != null) q.set("size", String(params.size));
  const qs = q.toString();
  return apiGet<PageEnvelope<Ticket>>(`/tickets/mine${qs ? `?${qs}` : ""}`);
}

export function listTicketsByEvent(eventId: number, params: { page?: number; size?: number } = {}) {
  const q = new URLSearchParams({ eventId: String(eventId) });
  if (params.page != null) q.set("page", String(params.page));
  if (params.size != null) q.set("size", String(params.size));
  return apiGet<PageEnvelope<Ticket>>(`/tickets?${q.toString()}`);
}

export function getTicket(id: number) {
  return apiGet<Ticket>(`/tickets/${id}`);
}
