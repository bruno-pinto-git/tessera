import { Card } from "@/components/ui/card";
import { useMatchSheetHistory, useTeamRosters } from "../hooks/useSheet";
import { KEY_EVENT_TYPES, occurrenceIcon, occurrenceTypeLabel } from "../lib/labels";
import type { Occurrence } from "../api/sheetApi";

export interface ResultCardMatch {
  matchId: number;
  homeTeamId: number;
  awayTeamId: number;
  homeScore?: number | null;
  awayScore?: number | null;
  kickoffAt: string;
}

/**
 * Compact recent-result card: the scoreline plus a chronological strip of the
 * headline events (goals + cards) with the player's name, drawn from the
 * match-sheet snapshot. Home events sit on the left, away on the right.
 */
export function MatchResultCard({
  match,
  homeName,
  awayName,
}: {
  match: ResultCardMatch;
  homeName: string;
  awayName: string;
}) {
  const { history } = useMatchSheetHistory(match.matchId);
  const { players } = useTeamRosters(match.homeTeamId, match.awayTeamId);

  const date = new Date(match.kickoffAt).toLocaleDateString("pt-PT", {
    day: "2-digit",
    month: "short",
  });

  const events = (history?.occurrences ?? [])
    .filter((o) => KEY_EVENT_TYPES.includes(o.type))
    .sort((a, b) => a.minute - b.minute);

  const playerName = (id: number) => {
    const p = players.get(id);
    return p ? `${p.firstName} ${p.lastName}` : `Jogador #${id}`;
  };

  return (
    <Card className="p-4">
      <div className="flex items-center gap-3">
        <span className="flex-1 truncate text-right font-medium">{homeName}</span>
        <span className="shrink-0 rounded-md bg-muted px-2.5 py-1 font-display text-lg font-bold tabular-nums">
          {match.homeScore ?? "–"} - {match.awayScore ?? "–"}
        </span>
        <span className="flex-1 truncate font-medium">{awayName}</span>
      </div>
      <div className="mt-1 text-center text-[11px] uppercase tracking-wider text-muted-foreground">
        {date}
      </div>

      {events.length > 0 ? (
        <ul className="mt-3 space-y-1 border-t pt-3">
          {events.map((o) => (
            <EventRow
              key={o.id}
              o={o}
              isHome={o.teamId === match.homeTeamId}
              player={playerName(o.playerId)}
            />
          ))}
        </ul>
      ) : (
        <p className="mt-3 border-t pt-3 text-center text-xs text-muted-foreground">
          Sem golos nem cartões registados.
        </p>
      )}
    </Card>
  );
}

function EventRow({ o, isHome, player }: { o: Occurrence; isHome: boolean; player: string }) {
  const cell = (
    <span className="flex items-center gap-1.5">
      <span className="w-7 shrink-0 tabular-nums text-muted-foreground">{o.minute}'</span>
      <span title={occurrenceTypeLabel(o.type)}>{occurrenceIcon(o.type)}</span>
      <span className="truncate">{player}</span>
    </span>
  );
  // Home events on the left half, away events mirrored on the right.
  return (
    <li className="grid grid-cols-2 gap-2 text-sm">
      {isHome ? <div className="text-left">{cell}</div> : <div />}
      {!isHome ? <div className="flex justify-end text-right">{cell}</div> : <div />}
    </li>
  );
}
