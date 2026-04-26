import { apiFetch } from "./http";

export type CreateTicketRequest = {
  eventId: number;
  supporter: boolean;
};

export type TicketResponse = {
  id: number;
  code: string;
  price: string;
  status: string;
  createdAt: string;
};

export async function createTicket(request: CreateTicketRequest): Promise<TicketResponse> {
  const response = await apiFetch("/tickets", {
    method: "POST",
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error(`Failed to create ticket: ${response.status}`);
  }

  return response.json();
}
