import { useState } from "react";
import { ApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Trash2 } from "lucide-react";
import type { Player } from "@/features/players/api/playersApi";
import { addLineupEntry, removeLineupEntry, type LineupEntry, type LineupRole } from "../api/sheetApi";
import { lineupRoleLabel } from "../lib/labels";

function errMsg(e: unknown): string {
  if (e instanceof ApiError) {
    const body = e.body as { detail?: string } | null;
    return body?.detail ?? e.message;
  }
  return e instanceof Error ? e.message : "Erro inesperado";
}

/** Lineup editor for a single team's section of the sheet. */
export function LineupEditor({
  matchId,
  teamName,
  roster,
  entries,
  locked,
  onChanged,
}: {
  matchId: number;
  teamName: string;
  roster: Player[];
  entries: LineupEntry[];
  locked: boolean;
  onChanged: () => void;
}) {
  const [playerId, setPlayerId] = useState<string>("");
  const [role, setRole] = useState<LineupRole>("STARTER");
  const [shirt, setShirt] = useState<string>("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const inLineup = new Set(entries.map((e) => e.playerId));
  const available = roster.filter((p) => !inLineup.has(p.id));

  async function add() {
    if (!playerId) return;
    setBusy(true);
    setError(null);
    try {
      await addLineupEntry(matchId, {
        playerId: Number(playerId),
        role,
        shirtNumber: shirt ? Number(shirt) : null,
      });
      setPlayerId("");
      setShirt("");
      onChanged();
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  }

  async function remove(pid: number) {
    setBusy(true);
    setError(null);
    try {
      await removeLineupEntry(matchId, pid);
      onChanged();
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(false);
    }
  }

  const sorted = [...entries].sort((a, b) => {
    if (a.role !== b.role) return a.role === "STARTER" ? -1 : 1;
    return (a.shirtNumber ?? 99) - (b.shirtNumber ?? 99);
  });

  function name(pid: number, fallbackShirt?: number | null) {
    const p = roster.find((r) => r.id === pid);
    const num = fallbackShirt ?? p?.shirtNumber;
    const label = p ? `${p.firstName} ${p.lastName}` : `Jogador #${pid}`;
    return num != null ? `${num}. ${label}` : label;
  }

  return (
    <div className="space-y-3">
      <h3 className="font-display text-lg font-semibold tracking-tight">{teamName}</h3>

      {!locked && (
        <div className="flex flex-wrap items-center gap-2">
          <select
            className="h-9 rounded-md border bg-background px-2 text-sm min-w-44"
            value={playerId}
            onChange={(e) => setPlayerId(e.target.value)}
            disabled={busy || available.length === 0}
          >
            <option value="">{available.length ? "Escolher jogador…" : "Sem jogadores"}</option>
            {available.map((p) => (
              <option key={p.id} value={p.id}>
                {p.shirtNumber != null ? `${p.shirtNumber}. ` : ""}
                {p.firstName} {p.lastName}
              </option>
            ))}
          </select>
          <select
            className="h-9 rounded-md border bg-background px-2 text-sm"
            value={role}
            onChange={(e) => setRole(e.target.value as LineupRole)}
            disabled={busy}
          >
            <option value="STARTER">Titular</option>
            <option value="SUBSTITUTE">Suplente</option>
          </select>
          <Input
            type="number"
            min={1}
            max={99}
            placeholder="Nº"
            className="w-20"
            value={shirt}
            onChange={(e) => setShirt(e.target.value)}
            disabled={busy}
          />
          <Button size="sm" onClick={add} disabled={busy || !playerId}>
            Adicionar
          </Button>
        </div>
      )}

      {error && <p className="text-sm text-destructive">{error}</p>}

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Jogador</TableHead>
              <TableHead className="w-28">Função</TableHead>
              <TableHead className="w-12 text-right" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {sorted.length === 0 && (
              <TableRow>
                <TableCell colSpan={3} className="text-center text-muted-foreground py-6">
                  Sem convocados.
                </TableCell>
              </TableRow>
            )}
            {sorted.map((e) => (
              <TableRow key={e.playerId}>
                <TableCell className="font-medium">{name(e.playerId, e.shirtNumber)}</TableCell>
                <TableCell className="text-muted-foreground">{lineupRoleLabel(e.role)}</TableCell>
                <TableCell className="text-right">
                  {!locked && (
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => remove(e.playerId)}
                      disabled={busy}
                      aria-label="Remover"
                    >
                      <Trash2 className="size-4 text-destructive" />
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
