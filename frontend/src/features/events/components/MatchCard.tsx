import { Link } from "react-router-dom";
import { Calendar, MapPin, Zap } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Crest } from "@/components/Crest";
import { clubBy } from "@/features/clubs/clubs";
import type { MatchListItem } from "../mockMatches";

interface MatchCardProps {
  match: MatchListItem;
}

export function MatchCard({ match }: MatchCardProps) {
  const home = clubBy(match.homeClubId);
  const away = clubBy(match.awayClubId);

  return (
    <Card className="flex flex-col overflow-hidden">
      <div className="px-5 pt-5 pb-3">
        <div className="flex items-center justify-between text-[11px] uppercase tracking-wider text-muted-foreground">
          <span className="truncate">{match.competition}</span>
          {match.hot && match.remaining && (
            <span className="inline-flex items-center gap-1 text-status-pending normal-case tracking-normal">
              <Zap className="size-3" />
              <span className="font-medium">{match.remaining}</span>
            </span>
          )}
        </div>
      </div>

      <div className="px-5 pb-5">
        <div className="flex items-center justify-between gap-3">
          <div className="flex flex-col items-center gap-2 flex-1 text-center">
            <Crest initials={home.initials} tone={home.tone} size={56} />
            <span className="text-sm font-medium">{home.short}</span>
            <span className="text-[11px] uppercase text-muted-foreground tracking-wider">
              Casa
            </span>
          </div>
          <div className="flex flex-col items-center text-muted-foreground">
            <span className="text-xs font-medium">vs</span>
            <span className="mt-2 font-mono text-[11px] text-muted-foreground">{match.date}</span>
          </div>
          <div className="flex flex-col items-center gap-2 flex-1 text-center">
            <Crest initials={away.initials} tone={away.tone} size={56} />
            <span className="text-sm font-medium">{away.short}</span>
            <span className="text-[11px] uppercase text-muted-foreground tracking-wider">
              Fora
            </span>
          </div>
        </div>
      </div>

      <div className="border-t bg-secondary/40 px-5 py-3 text-xs space-y-1.5">
        <span className="inline-flex items-center gap-1.5 text-muted-foreground">
          <Calendar className="size-3.5" />
          {match.day}, {match.date} · {match.time}
        </span>
        <div className="inline-flex items-center gap-1.5 text-muted-foreground">
          <MapPin className="size-3.5" />
          <span className="truncate">{match.venue}</span>
        </div>
      </div>

      <div className="border-t px-5 py-3 flex items-center justify-between">
        <div className="text-sm">
          <span className="text-muted-foreground text-xs">a partir de</span>
          <div className="font-semibold">{match.priceFrom},00 €</div>
        </div>
        <Button size="sm" asChild>
          <Link to={`/events/${match.id}`}>Comprar bilhete →</Link>
        </Button>
      </div>
    </Card>
  );
}
