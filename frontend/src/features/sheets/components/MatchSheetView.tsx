import { Card } from "@/components/ui/card";
import type { Player } from "@/features/players/api/playersApi";
import { useMatchSheetHistory, useTeamRosters } from "../hooks/useSheet";
import type { LineupEntry, Occurrence } from "../api/sheetApi";
import { occurrenceIcon, occurrenceTypeLabel } from "../lib/labels";
import { scoreMismatch, tallyGoals } from "../lib/score";

/**
 * Read-only public view of a CLOSED match sheet (lineups + occurrences),
 * built from the statistics snapshot. Player names are resolved from the
 * current team rosters (the snapshot stores only ids); a removed player
 * falls back to its shirt/id.
 */
export function MatchSheetView({
  matchId,
  homeName,
  awayName,
}: {
  matchId: number;
  homeName: string;
  awayName: string;
}) {
  const { history, loading, error, notAvailable } = useMatchSheetHistory(matchId);
  const home = history?.summary.homeTeamId;
  const away = history?.summary.awayTeamId;
  const { players } = useTeamRosters(home, away);

  return (
    <Card>
      <div className="px-6 py-5 border-b">
        <h2 className="font-semibold">Ficha técnica</h2>
        <p className="text-sm text-muted-foreground">Convocatórias e lances do jogo.</p>
      </div>
      <div className="px-6 py-5">
        {loading ? (
          <p className="text-sm text-muted-foreground">A carregar ficha…</p>
        ) : error ? (
          <p className="text-sm text-destructive">Falha a carregar a ficha: {error}</p>
        ) : notAvailable || !history ? (
          <p className="text-sm text-muted-foreground">
            A ficha técnica ainda não foi publicada para este jogo.
          </p>
        ) : (
          <div className="space-y-8">
            {scoreMismatch(
              history.summary.homeScore,
              history.summary.awayScore,
              tallyGoals(history.occurrences, history.summary.homeTeamId, history.summary.awayTeamId),
            ) && (
              <p className="rounded-md border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-xs text-muted-foreground">
                Nota: os golos listados não somam o resultado oficial{" "}
                <span className="font-medium text-foreground">
                  {history.summary.homeScore}–{history.summary.awayScore}
                </span>{" "}
                — a ficha pode estar incompleta.
              </p>
            )}
            <div className="grid gap-8 md:grid-cols-2">
              <LineupColumn
                title={homeName}
                entries={history.lineup.filter((e) => e.teamId === home)}
                players={players}
              />
              <LineupColumn
                title={awayName}
                entries={history.lineup.filter((e) => e.teamId === away)}
                players={players}
              />
            </div>
            <OccurrenceTimeline
              occurrences={history.occurrences}
              players={players}
              homeTeamId={home}
              homeName={homeName}
              awayName={awayName}
            />
          </div>
        )}
      </div>
    </Card>
  );
}

function playerLabel(playerId: number, players: Map<number, Player>, shirt?: number | null): string {
  const p = players.get(playerId);
  const num = shirt ?? p?.shirtNumber;
  const name = p ? `${p.firstName} ${p.lastName}` : `Jogador #${playerId}`;
  return num != null ? `${num}. ${name}` : name;
}

function LineupColumn({
  title,
  entries,
  players,
}: {
  title: string;
  entries: LineupEntry[];
  players: Map<number, Player>;
}) {
  const sorted = [...entries].sort((a, b) => {
    if (a.role !== b.role) return a.role === "STARTER" ? -1 : 1;
    return (a.shirtNumber ?? 99) - (b.shirtNumber ?? 99);
  });
  const starters = sorted.filter((e) => e.role === "STARTER");
  const subs = sorted.filter((e) => e.role === "SUBSTITUTE");

  return (
    <div>
      <h3 className="font-display text-lg font-semibold tracking-tight mb-3">{title}</h3>
      {sorted.length === 0 ? (
        <p className="text-sm text-muted-foreground">Sem convocados.</p>
      ) : (
        <div className="space-y-4">
          <Group label="Onze inicial" entries={starters} players={players} />
          {subs.length > 0 && <Group label="Suplentes" entries={subs} players={players} />}
        </div>
      )}
    </div>
  );
}

function Group({
  label,
  entries,
  players,
}: {
  label: string;
  entries: LineupEntry[];
  players: Map<number, Player>;
}) {
  if (entries.length === 0) return null;
  return (
    <div>
      <div className="text-[11px] uppercase tracking-wider text-muted-foreground mb-1">{label}</div>
      <ul className="text-sm space-y-1">
        {entries.map((e) => (
          <li key={e.playerId}>{playerLabel(e.playerId, players, e.shirtNumber)}</li>
        ))}
      </ul>
    </div>
  );
}

function OccurrenceTimeline({
  occurrences,
  players,
  homeTeamId,
  homeName,
  awayName,
}: {
  occurrences: Occurrence[];
  players: Map<number, Player>;
  homeTeamId?: number;
  homeName: string;
  awayName: string;
}) {
  if (occurrences.length === 0) {
    return <p className="text-sm text-muted-foreground">Sem lances registados.</p>;
  }
  const sorted = [...occurrences].sort((a, b) => a.minute - b.minute);
  return (
    <div>
      <div className="text-[11px] uppercase tracking-wider text-muted-foreground mb-2">Lances</div>
      <ul className="space-y-2">
        {sorted.map((o) => {
          const team = o.teamId === homeTeamId ? homeName : awayName;
          return (
            <li key={o.id} className="flex items-baseline gap-3 text-sm">
              <span className="w-9 shrink-0 tabular-nums text-muted-foreground">{o.minute}'</span>
              <span className="shrink-0">{occurrenceIcon(o.type)}</span>
              <span className="flex-1">
                <span className="font-medium">{occurrenceTypeLabel(o.type)}</span>
                {" — "}
                {playerLabel(o.playerId, players)}
                {o.replacedPlayerId != null && (
                  <span className="text-muted-foreground">
                    {" "}
                    (entra por {playerLabel(o.replacedPlayerId, players)})
                  </span>
                )}
                <span className="text-muted-foreground"> · {team}</span>
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
