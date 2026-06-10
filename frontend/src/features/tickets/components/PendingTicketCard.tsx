import { Link } from "react-router-dom";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import { Crest } from "@/components/Crest";
import type { TicketView } from "../hooks/useMyTickets";

export function PendingTicketCard({ ticket }: { ticket: TicketView }) {
  const { home, away } = ticket;
  return (
    <Card className="overflow-hidden">
      <div className="p-6 space-y-4">
        <div className="flex items-center justify-between">
          <StatusBadge status="PENDING" />
          <span className="text-[11px] text-status-pending font-medium">Pagamento pendente</span>
        </div>
        <div className="flex items-center gap-3">
          <Crest initials={home.initials} tone={home.tone} size={32} />
          <div className="text-xs text-muted-foreground">vs</div>
          <Crest initials={away.initials} tone={away.tone} size={32} />
          <div className="ml-1">
            <div className="font-semibold leading-tight">{ticket.title}</div>
            <div className="text-xs text-muted-foreground">
              {ticket.day} · {ticket.tier} · {ticket.price}
            </div>
          </div>
        </div>
        <div className="rounded-md border bg-status-pending/5 px-3 py-2 text-xs text-muted-foreground">
          Conclui o pagamento por MBWAY ou cartão para garantir o teu bilhete.
        </div>
        <div className="flex items-center gap-2">
          <Button size="sm" className="flex-1" asChild>
            <Link to={`/events/${ticket.eventId}`}>Concluir pagamento</Link>
          </Button>
        </div>
      </div>
    </Card>
  );
}
