import { useMemo, useState } from "react";
import { ApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Trash2 } from "lucide-react";
import type { Player } from "@/features/players/api/playersApi";
import {
  addOccurrence,
  removeOccurrence,
  type LineupEntry,
  type Occurrence,
  type OccurrenceType,
} from "../api/sheetApi";
import { OCCURRENCE_TYPES, occurrenceIcon, occurrenceTypeLabel } from "../lib/labels";

function errMsg(e: unknown): string {
  if (e instanceof ApiError) {
    const body = e.body as { detail?: string } | null;
    return body?.detail ?? e.message;
  }
  return e instanceof Error ? e.message : "Erro inesperado";
}

/** Records in-game occurrences (goals, cards, subs, fouls). */
export function OccurrenceEditor({
  matchId,
  lineup,
  players,
  homeTeamId,
  awayTeamId,
  homeName,
  awayName,
  occurrences,
  locked,
  onChanged,
}: {
  matchId: number;
  lineup: LineupEntry[];
  players: Map<number, Player>;
  homeTeamId: number;
  awayTeamId: number;
  homeName: string;
  awayName: string;
  occurrences: Occurrence[];
  locked: boolean;
  onChanged: () => void;
}) {
  const [minute, setMinute] = useState("");
  const [type, setType] = useState<OccurrenceType>("GOAL");
  const [teamId, setTeamId] = useState<number>(homeTeamId);
  const [playerId, setPlayerId] = useState("");
  const [replacedId, setReplacedId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const teamPlayers = useMemo(
    () => lineup.filter((e) => e.teamId === teamId),
    [lineup, teamId],
  );

  function name(pid: number): string {
    const p = players.get(pid);
    return p ? `${p.firstName} ${p.lastName}` : `Jogador #${pid}`;
  }

  async function add() {
    if (!playerId || minute === "") return;
    setBusy(true);
    setError(null);
    try {
      await addOccurrence(matchId, {
        minute: Number(minute),
        type,
        playerId: Number(playerId),
        replacedPlayerId: type === "SUBSTITUTION" && replacedId ? Number(replacedId) : null,
      });
      setMinute("");
      setPlayerId("");
      setReplacedId("");
      onChanged();
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  }

  async function remove(id: number) {
    setBusy(true);
    setError(null);
    try {
      await removeOccurrence(matchId, id);
      onChanged();
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  }

  const sorted = [...occurrences].sort((a, b) => a.minute - b.minute);
  const selectClass = "h-9 rounded-md border bg-background px-2 text-sm";

  return (
    <div className="space-y-3">
      <h3 className="font-display text-lg font-semibold tracking-tight">Lances</h3>

      {!locked && (
        <div className="flex flex-wrap items-end gap-2">
          <Input
            type="number"
            min={0}
            max={200}
            placeholder="Min."
            className="w-20"
            value={minute}
            onChange={(e) => setMinute(e.target.value)}
            disabled={busy}
          />
          <select
            className={selectClass}
            value={type}
            onChange={(e) => setType(e.target.value as OccurrenceType)}
            disabled={busy}
          >
            {OCCURRENCE_TYPES.map((t) => (
              <option key={t} value={t}>
                {occurrenceTypeLabel(t)}
              </option>
            ))}
          </select>
          <select
            className={selectClass}
            value={teamId}
            onChange={(e) => {
              setTeamId(Number(e.target.value));
              setPlayerId("");
              setReplacedId("");
            }}
            disabled={busy}
          >
            <option value={homeTeamId}>{homeName}</option>
            <option value={awayTeamId}>{awayName}</option>
          </select>
          <select
            className={`${selectClass} min-w-40`}
            value={playerId}
            onChange={(e) => setPlayerId(e.target.value)}
            disabled={busy || teamPlayers.length === 0}
          >
            <option value="">
              {teamPlayers.length ? "Jogador…" : "Convoca jogadores primeiro"}
            </option>
            {teamPlayers.map((e) => (
              <option key={e.playerId} value={e.playerId}>
                {name(e.playerId)}
              </option>
            ))}
          </select>
          {type === "SUBSTITUTION" && (
            <select
              className={`${selectClass} min-w-40`}
              value={replacedId}
              onChange={(e) => setReplacedId(e.target.value)}
              disabled={busy}
            >
              <option value="">Sai (substituído)…</option>
              {teamPlayers
                .filter((e) => String(e.playerId) !== playerId)
                .map((e) => (
                  <option key={e.playerId} value={e.playerId}>
                    {name(e.playerId)}
                  </option>
                ))}
            </select>
          )}
          <Button size="sm" onClick={add} disabled={busy || !playerId || minute === ""}>
            Registar
          </Button>
        </div>
      )}

      {error && <p className="text-sm text-destructive">{error}</p>}

      {sorted.length === 0 ? (
        <p className="text-sm text-muted-foreground">Sem lances registados.</p>
      ) : (
        <ul className="space-y-1.5">
          {sorted.map((o) => {
            const team = o.teamId === homeTeamId ? homeName : awayName;
            return (
              <li key={o.id} className="flex items-center gap-3 text-sm">
                <span className="w-9 shrink-0 tabular-nums text-muted-foreground">{o.minute}'</span>
                <span className="shrink-0">{occurrenceIcon(o.type)}</span>
                <span className="flex-1">
                  <span className="font-medium">{occurrenceTypeLabel(o.type)}</span> —{" "}
                  {name(o.playerId)}
                  {o.replacedPlayerId != null && (
                    <span className="text-muted-foreground"> (entra por {name(o.replacedPlayerId)})</span>
                  )}
                  <span className="text-muted-foreground"> · {team}</span>
                </span>
                {!locked && (
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => remove(o.id)}
                    disabled={busy}
                    aria-label="Remover lance"
                  >
                    <Trash2 className="size-4 text-destructive" />
                  </Button>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
