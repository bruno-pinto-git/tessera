import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { useMatchLookups } from "@/features/matches/hooks/useMatchLookups";
import { MatchResultCard } from "@/features/sheets/components/MatchResultCard";
import { useMatchHistory } from "../hooks/useStats";

export function RecentResults({ clubId, title = "Resultados recentes" }: { clubId?: number; title?: string }) {
  const { data, loading, error } = useMatchHistory({
    size: 8,
    sort: "kickoffAt,desc",
    ...(clubId != null ? { clubId } : {}),
  });
  const { clubs } = useMatchLookups();

  const clubName = (id: number) => clubs.get(id)?.name ?? `Clube #${id}`;

  // Only surface matches that actually have a recorded result — not sheets that
  // were closed before a score was ever set.
  const results = (data ?? [])
    .filter((m) => m.homeScore != null && m.awayScore != null)
    .slice(0, 4);

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="font-display text-lg font-semibold tracking-tight">{title}</h3>
        <Button variant="ghost" size="sm" asChild>
          <Link to="/events">Ver jogos →</Link>
        </Button>
      </div>

      {error ? (
        <p className="text-sm text-muted-foreground">Não foi possível carregar os resultados.</p>
      ) : loading ? (
        <p className="text-sm text-muted-foreground">A carregar…</p>
      ) : results.length === 0 ? (
        <p className="text-sm text-muted-foreground">Ainda não há resultados publicados.</p>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {results.map((m) => (
            <MatchResultCard
              key={m.matchId}
              match={m}
              homeName={clubName(m.homeClubId)}
              awayName={clubName(m.awayClubId)}
            />
          ))}
        </div>
      )}
    </section>
  );
}
