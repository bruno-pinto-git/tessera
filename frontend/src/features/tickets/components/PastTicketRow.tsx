import { StatusBadge } from "@/components/ui/status-badge";
import { Crest } from "@/components/Crest";
import type { TicketView } from "../hooks/useMyTickets";

export function PastTicketRow({ ticket }: { ticket: TicketView }) {
  const { home, away } = ticket;
  return (
    <li className="flex items-center gap-4 px-6 py-3 text-sm opacity-60">
      <Crest initials={home.initials} tone={home.tone} size={28} />
      <Crest initials={away.initials} tone={away.tone} size={28} />
      <div className="flex-1 min-w-0">
        <div className="font-medium truncate">{ticket.title}</div>
        <div className="text-xs text-muted-foreground truncate">
          {ticket.day} · {ticket.tier}
        </div>
      </div>
      {ticket.validatedAt && (
        <span className="text-xs text-muted-foreground hidden md:inline">
          Validado · {ticket.validatedAt}
        </span>
      )}
      <StatusBadge status="VALIDATED" />
    </li>
  );
}
