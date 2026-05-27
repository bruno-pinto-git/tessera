import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ChevronLeft, ChevronRight, Pencil, Plus, Ticket, Trash2 } from "lucide-react";
import { useMatches } from "../hooks/useMatches";
import { useMatchLookups } from "../hooks/useMatchLookups";
import { MatchStatusBadge } from "../components/MatchStatusBadge";
import { MatchFormDialog } from "../components/MatchFormDialog";
import { DeleteMatchDialog } from "../components/DeleteMatchDialog";
import { OpenBoxOfficeDialog } from "../components/OpenBoxOfficeDialog";
import {
  MATCH_STATUSES,
  matchStatusLabel,
  type Match,
  type MatchStatus,
} from "../api/matchesApi";
import { teamCategoryLabel } from "@/features/teams/api/teamsApi";

const PAGE_SIZE = 20;

export function MatchesAdminPage() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<MatchStatus | "">("");
  const [clubId, setClubId] = useState<string>("");
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Match | null>(null);
  const [deleting, setDeleting] = useState<Match | null>(null);
  const [openingBoxOffice, setOpeningBoxOffice] = useState<Match | null>(null);

  const lookups = useMatchLookups();
  const { data, loading, error, refetch } = useMatches({
    page,
    size: PAGE_SIZE,
    status: status || undefined,
    clubId: clubId ? Number(clubId) : undefined,
    sort: "kickoffAt,desc",
  });

  // Resolve all team ids referenced by the current page, populating the
  // shared team cache so renders below have club + category info.
  useEffect(() => {
    if (!data) return;
    const need = new Set<number>();
    for (const m of data.content) {
      need.add(m.homeTeamId);
      need.add(m.awayTeamId);
    }
    for (const id of need) {
      if (!lookups.getCachedTeam(id)) {
        void lookups.resolveTeam(id).catch(() => {
          /* swallow; the row falls back to the id */
        });
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  function teamLabel(id: number): string {
    const t = lookups.getCachedTeam(id);
    if (!t) return `#${id}`;
    const club = lookups.clubs.get(t.clubId);
    const clubName = club?.name ?? `Clube #${t.clubId}`;
    return `${clubName} (${teamCategoryLabel(t.category)})`;
  }

  function matchLabel(m: Match): string {
    return `${teamLabel(m.homeTeamId)} vs ${teamLabel(m.awayTeamId)}`;
  }

  const sortedClubs = useMemo(
    () => Array.from(lookups.clubs.values()).sort((a, b) => a.name.localeCompare(b.name)),
    [lookups.clubs],
  );

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between gap-4">
        <div>
          <Link
            to="/admin"
            className="text-xs text-muted-foreground hover:text-foreground transition-colors"
          >
            ← Admin
          </Link>
          <h1 className="text-2xl font-bold tracking-tight">Jogos</h1>
          <p className="text-sm text-muted-foreground">
            Calendário, resultados e estado dos jogos.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)} disabled={lookups.loading}>
          <Plus className="size-4" />
          Novo jogo
        </Button>
      </header>

      {/* Filters */}
      <div className="flex flex-wrap items-end gap-3">
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">Estado</label>
          <select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as MatchStatus | "");
              setPage(0);
            }}
            className="flex h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
          >
            <option value="">Todos</option>
            {MATCH_STATUSES.map((s) => (
              <option key={s} value={s}>
                {matchStatusLabel(s)}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">Clube</label>
          <select
            value={clubId}
            onChange={(e) => {
              setClubId(e.target.value);
              setPage(0);
            }}
            className="flex h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
          >
            <option value="">Todos</option>
            {sortedClubs.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && <p className="text-sm text-destructive">Falha a carregar jogos: {error}</p>}
      {lookups.error && (
        <p className="text-sm text-destructive">
          Falha a carregar referências (clubes/estádios): {lookups.error}
        </p>
      )}

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-44">Data</TableHead>
              <TableHead>Encontro</TableHead>
              <TableHead className="w-32">Estádio</TableHead>
              <TableHead className="w-24 text-center">Resultado</TableHead>
              <TableHead className="w-28">Estado</TableHead>
              <TableHead className="w-24 text-right">Ações</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                  A carregar…
                </TableCell>
              </TableRow>
            )}
            {!loading && data && data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                  Sem jogos a corresponder aos filtros.
                </TableCell>
              </TableRow>
            )}
            {!loading &&
              data?.content.map((m) => {
                const venue = m.venueId != null ? lookups.venues.get(m.venueId) : null;
                return (
                  <TableRow key={m.id}>
                    <TableCell className="text-muted-foreground">
                      {formatKickoff(m.kickoffAt)}
                    </TableCell>
                    <TableCell className="font-medium">{matchLabel(m)}</TableCell>
                    <TableCell className="text-muted-foreground truncate max-w-32">
                      {venue?.name ?? "—"}
                    </TableCell>
                    <TableCell className="text-center font-mono">
                      {m.homeScore != null && m.awayScore != null
                        ? `${m.homeScore} – ${m.awayScore}`
                        : "—"}
                    </TableCell>
                    <TableCell>
                      <MatchStatusBadge status={m.status} />
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => setOpeningBoxOffice(m)}
                        aria-label="Abrir bilheteira"
                        title="Abrir bilheteira"
                      >
                        <Ticket className="size-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => setEditing(m)}
                        aria-label="Editar jogo"
                      >
                        <Pencil className="size-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => setDeleting(m)}
                        aria-label="Eliminar jogo"
                      >
                        <Trash2 className="size-4 text-destructive" />
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
          </TableBody>
        </Table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm">
          <p className="text-muted-foreground">
            {data.totalElements} jogo{data.totalElements === 1 ? "" : "s"} · página{" "}
            {data.page + 1} de {data.totalPages}
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              <ChevronLeft className="size-4" />
              Anterior
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={!data || page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Seguinte
              <ChevronRight className="size-4" />
            </Button>
          </div>
        </div>
      )}

      <MatchFormDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        clubs={lookups.clubs}
        venues={lookups.venues}
        knownTeams={lookups.teamCache}
        onSaved={refetch}
      />
      <MatchFormDialog
        open={!!editing}
        onOpenChange={(o) => !o && setEditing(null)}
        match={editing}
        clubs={lookups.clubs}
        venues={lookups.venues}
        knownTeams={lookups.teamCache}
        onSaved={refetch}
      />
      <DeleteMatchDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        match={deleting}
        label={deleting ? matchLabel(deleting) : ""}
        onDeleted={refetch}
      />
      <OpenBoxOfficeDialog
        open={!!openingBoxOffice}
        onOpenChange={(o) => !o && setOpeningBoxOffice(null)}
        matchId={openingBoxOffice?.id ?? null}
        defaultLabel={openingBoxOffice ? matchLabel(openingBoxOffice) : ""}
        onCreated={refetch}
      />
    </div>
  );
}

function formatKickoff(iso: string): string {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  return d.toLocaleString("pt-PT", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}