import { useCreateTicket } from "../hooks/useCreateTicket";
import { TicketCard } from "../components/TicketCard";

const SEED_EVENT_ID = 1;

export function TicketsPage() {
  const { ticket, loading, error, generate } = useCreateTicket();

  return (
    <div style={{ maxWidth: 480, margin: "40px auto", padding: 16 }}>
      <h1>Tessera</h1>
      <button
        onClick={() => generate(SEED_EVENT_ID, false)}
        disabled={loading}
      >
        {loading ? "A gerar..." : "Gerar Ticket"}
      </button>

      {error && <p style={{ color: "red" }}>{error}</p>}
      {ticket && <TicketCard ticket={ticket} />}
    </div>
  );
}
