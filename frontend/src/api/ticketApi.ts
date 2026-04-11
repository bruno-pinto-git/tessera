const API_BASE = "http://localhost:8080/api";

export type CreateTicketRequest = {
  eventId: number;
  supporter: boolean;
}

export type TicketResponse = {
  id: number;
  code: string;
  price: string;
  status: string;
  createdAt: string;
}

export async function createTicket(
  request: CreateTicketRequest
): Promise<TicketResponse> {
  const response = await fetch(`${API_BASE}/tickets`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error(`Failed to create ticket: ${response.status}`);
  }

  return response.json();
}
