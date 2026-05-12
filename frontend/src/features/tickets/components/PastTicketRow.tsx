import { StatusBadge } from "@/components/ui/status-badge";
import { Crest } from "@/components/Crest";
import { clubBy } from "@/features/clubs/clubs";
import type { PastTicketView } from "../mockMyTickets";
import { cn } from "@/lib/utils";

export function PastTicketRow({ ticket }: { ticket: PastTicketView }) {
  const home = clubBy(ticket.homeClubId);
  const away = clubBy(ticket.awayClubId);
  const faded = !ticket.cancelled;
  return (
    <li className={cn("flex items-center gap-4 px-6 py-3 text-sm", faded && "opacity-60")}>
      <Crest initials={home.initials} tone={home.tone} size={28} />
      <Crest initials={away.initials} tone={away.tone} size={28} />
      <div className="flex-1 min-w-0">
        <div className="font-medium truncate">
          {home.short} vs {away.short}
        </div>
        <div className="text-xs text-muted-foreground truncate">
          {ticket.day} · {ticket.tier}
        </div>
      </div>
      {ticket.cancelled ? null : (
        <span className="text-xs text-muted-foreground hidden md:inline">
          {ticket.validatedAt}
        </span>
      )}
      <StatusBadge status={ticket.cancelled ? "CANCELLED" : "VALIDATED"} />
    </li>
  );
}
