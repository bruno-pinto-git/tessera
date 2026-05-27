import { Link } from "react-router-dom";
import { Calendar, MapPin } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Crest } from "@/components/Crest";
import type { CatalogEntry } from "../lib/catalog";

interface MatchCardProps {
  entry: CatalogEntry;
}

export function MatchCard({ entry }: MatchCardProps) {
  const { day, date, time } = formatKickoff(entry.kickoffAt);

  return (
    <Card className="flex flex-col overflow-hidden">
      {entry.homeCategory && (
        <div className="px-5 pt-5 pb-3">
          <div className="text-[11px] uppercase tracking-wider text-muted-foreground truncate">
            {entry.homeCategory}
            {entry.awayCategory && entry.awayCategory !== entry.homeCategory
              ? ` · ${entry.awayCategory}`
              : ""}
          </div>
        </div>
      )}

      <div className="px-5 pb-5 pt-5">
        <div className="flex items-center justify-between gap-3">
          <div className="flex flex-col items-center gap-2 flex-1 text-center">
            <Crest initials={entry.homeInitials} tone={entry.homeTone} size={56} />
            <span className="text-sm font-medium truncate max-w-full">{entry.homeShort}</span>
            <span className="text-[11px] uppercase text-muted-foreground tracking-wider">Casa</span>
          </div>
          <div className="flex flex-col items-center text-muted-foreground">
            <span className="text-xs font-medium">vs</span>
            <span className="mt-2 font-mono text-[11px] text-muted-foreground">{date}</span>
          </div>
          <div className="flex flex-col items-center gap-2 flex-1 text-center">
            <Crest initials={entry.awayInitials} tone={entry.awayTone} size={56} />
            <span className="text-sm font-medium truncate max-w-full">{entry.awayShort}</span>
            <span className="text-[11px] uppercase text-muted-foreground tracking-wider">Fora</span>
          </div>
        </div>
      </div>

      <div className="border-t bg-secondary/40 px-5 py-3 text-xs space-y-1.5">
        <span className="inline-flex items-center gap-1.5 text-muted-foreground">
          <Calendar className="size-3.5" />
          {day}, {date} · {time}
        </span>
        {entry.venueName && (
          <div className="inline-flex items-center gap-1.5 text-muted-foreground">
            <MapPin className="size-3.5" />
            <span className="truncate">{entry.venueName}</span>
          </div>
        )}
      </div>

      <div className="border-t px-5 py-3 flex items-center justify-between">
        <div className="text-sm">
          <span className="text-muted-foreground text-xs">a partir de</span>
          <div className="font-semibold">{entry.priceFrom.toFixed(2)} €</div>
        </div>
        <Button size="sm" asChild>
          <Link to={`/events/${entry.eventId}`}>Comprar bilhete →</Link>
        </Button>
      </div>
    </Card>
  );
}

function formatKickoff(iso: string | null): { day: string; date: string; time: string } {
  if (!iso) return { day: "—", date: "Data por confirmar", time: "" };
  const d = new Date(iso);
  if (isNaN(d.getTime())) return { day: "—", date: iso, time: "" };
  const day = d.toLocaleDateString("pt-PT", { weekday: "short" }).replace(".", "");
  const date = d.toLocaleDateString("pt-PT", { day: "2-digit", month: "short" }).replace(".", "");
  const time = d.toLocaleTimeString("pt-PT", { hour: "2-digit", minute: "2-digit" });
  return {
    day: day.charAt(0).toUpperCase() + day.slice(1),
    date: date.charAt(0).toUpperCase() + date.slice(1),
    time,
  };
}