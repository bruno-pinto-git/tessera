// Re-export the API-layer types for use in feature code. Keeping a single
// source of truth avoids drift between the wire shape and what components
// render.
export type { Ticket, TicketStatus, PageEnvelope } from "@/api/ticketApi";
