import { Link } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useMatchLookups } from "@/features/matches/hooks/useMatchLookups";
import { useMatchHistory } from "../hooks/useStats";
import type { MatchSummary } from "../api/statsApi";

/**
 * Recent closed-match results. Shown to fans on the homepage and reused inside
 * the manager dashboard scoped to a single club. Club names are resolved from
 * the (small) clubs catalogue.
 */
export function RecentResults({ clubId, title = "Resultados recentes" }: { clubId?: number; title?: string }) {
  const { data, loading, error } = useMatchHistory({
    size: 5,
    sort: "kickoffAt,desc",
    ...(clubId != null ? { clubId } : {}),
  });
  const { clubs } = useMatchLookups();

  const clubName = (id: number) => clubs.get(id)?.name ?? `Clube #${id}`;

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle className="text-base">{title}</CardTitle>
        <Button variant="ghost" size="sm" asChild>
          <Link to="/events">Ver jogos →</Link>
        </Button>
      </CardHeader>
      <CardContent>
        {error ? (
          <p className="text-sm text-muted-foreground">Não foi possível carregar os resultados.</p>
        ) : loading ? (
          <p className="text-sm text-muted-foreground">A carregar…</p>
        ) : !data || data.length === 0 ? (
          <p className="text-sm text-muted-foreground">Ainda não há resultados publicados.</p>
        ) : (
          <ul className="divide-y">
            {data.map((m) => (
              <ResultRow key={m.matchId} m={m} home={clubName(m.homeClubId)} away={clubName(m.awayClubId)} />
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

function ResultRow({ m, home, away }: { m: MatchSummary; home: string; away: string }) {
  const hasScore = m.homeScore != null && m.awayScore != null;
  const date = new Date(m.kickoffAt).toLocaleDateString("pt-PT", {
    day: "2-digit",
    month: "short",
  });
  return (
    <li className="flex items-center gap-3 py-2.5 text-sm">
      <span className="w-12 shrink-0 text-xs text-muted-foreground">{date}</span>
      <span className="flex-1 truncate text-right font-medium">{home}</span>
      <span className="shrink-0 rounded-md bg-muted px-2 py-0.5 font-display font-bold tabular-nums">
        {hasScore ? `${m.homeScore} - ${m.awayScore}` : "vs"}
      </span>
      <span className="flex-1 truncate font-medium">{away}</span>
    </li>
  );
}
