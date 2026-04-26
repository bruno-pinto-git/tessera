import type { Ticket } from "../types";

interface TicketCardProps {
  ticket: Ticket;
}

export function TicketCard({ ticket }: TicketCardProps) {
  return (
    <div style={{ border: "1px solid #ccc", borderRadius: 8, padding: 16, marginTop: 16 }}>
      <h3>Ticket #{ticket.id}</h3>
      <p>
        <strong>Code:</strong> {ticket.code}
      </p>
      <p>
        <strong>Price:</strong> {ticket.price} EUR
      </p>
      <p>
        <strong>Status:</strong> {ticket.status}
      </p>
      <p>
        <strong>Created:</strong> {new Date(ticket.createdAt).toLocaleString()}
      </p>
    </div>
  );
}
