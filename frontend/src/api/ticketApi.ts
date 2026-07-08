import { apiGet, apiPost } from "./client";

export type TicketStatus = "PENDING" | "PAID" | "VALIDATED";

export interface Ticket {
  id: number;
  code: string;
  eventId: number;
  matchId: number | null;
  ownerSub: string | null;
  price: number | string;
  status: TicketStatus;
  paymentMethod: "MBWAY" | "CARD" | null;
  createdAt: string;
  paymentDate: string | null;
  validationDate: string | null;
  validatorSub: string | null;
  checkoutUrl: string | null;
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
  paymentMethod: "MBWAY" | "CARD";
  mbwayReference?: string;
  phoneNumber?: string;
}

export interface ValidateTicketRequest {
  code: string;
}

export interface WalletPassRequest {
  eventTitle: string;
  venue?: string | null;
  kickoffAt?: string | null;
  tierLabel?: string;
}

export interface WalletPassResponse {
  saveUrl: string;
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

export function getWalletSaveUrl(id: number, body: WalletPassRequest) {
  return apiPost<WalletPassRequest, WalletPassResponse>(`/tickets/${id}/wallet-pass`, body);
}
