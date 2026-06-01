import { useEffect, useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Pencil, Plus, Ticket, Trash2 } from "lucide-react";
import { useMatches } from "../hooks/useMatches";
import { useMatchLookups } from "../hooks/useMatchLookups";
import { MatchStatusBadge } from "./MatchStatusBadge";
import { MatchFormDialog } from "./MatchFormDialog";
import { DeleteMatchDialog } from "./DeleteMatchDialog";
import { OpenBoxOfficeDialog } from "./OpenBoxOfficeDialog";
import type { Match } from "../api/matchesApi";
import { teamCategoryLabel } from "@/features/teams/api/teamsApi";

interface MatchesSectionProps {
  clubId: number;
  /** Whether the current user can create/edit/delete matches and open the
   *  box office for this club. The backend enforces it too. */
  canManage: boolean;
}

/**
 * Per-club "Jogos & Bilheteira" panel embedded in the club detail page.
 * Lists the matches where this club is the HOME side and (for managers /
 * admins) lets them schedule new home matches, edit/delete them, and open
 * the box office.
 */
export function MatchesSection({ clubId, canManage }: MatchesSectionProps) {
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Match | null>(null);
  const [deleting, setDeleting] = useState<Match | null>(null);
  const [openingBoxOffice, setOpeningBoxOffice] = useState<Match | null>(null);

  const lookups = useMatchLookups();
  const { data, loading, error, refetch } = useMatches({
    clubId,
    sort: "kickoffAt,desc",
    size: 50,
  });

  // Only the matches this club hosts.
  const homeMatches = useMemo(
    () => (data?.content ?? []).filter((m) => m.homeClubId === clubId),
    [data, clubId],
  );

  // Resolve all team ids referenced by the listed matches so we can render
  // their club + category labels.
  useEffect(() => {
    const need = new Set<number>();
    for (const m of homeMatches) {
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
  }, [homeMatches]);

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

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between gap-3">
          <div>
            <CardTitle className="text-base">Jogos &amp; Bilheteira</CardTitle>
            <CardDescription>
              Jogos em que este clube joga em casa. Abre a bilheteira para colocar à venda.
            </CardDescription>
          </div>
          {canManage && (
            <Button size="sm" onClick={() => setCreateOpen(true)} disabled={lookups.loading}>
              <Plus className="size-4" />
              Novo jogo
            </Button>
          )}
        </div>
      </CardHeader>
      <CardContent>
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
                <TableHead className="w-28">Estado</TableHead>
                <TableHead className="w-32 text-right">{canManage ? "Ações" : ""}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading && (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground py-8">
                    A carregar…
                  </TableCell>
                </TableRow>
              )}
              {!loading && homeMatches.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground py-8">
                    Sem jogos em casa registados.
                  </TableCell>
                </TableRow>
              )}
              {!loading &&
                homeMatches.map((m) => {
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
                      <TableCell>
                        <MatchStatusBadge status={m.status} />
                      </TableCell>
                      <TableCell>
                        {canManage && (
                          <div className="flex items-center justify-end">
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
                          </div>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })}
            </TableBody>
          </Table>
        </div>
      </CardContent>

      {canManage && (
        <>
          <MatchFormDialog
            open={createOpen}
            onOpenChange={setCreateOpen}
            clubs={lookups.clubs}
            venues={lookups.venues}
            knownTeams={lookups.teamCache}
            lockHomeClubId={clubId}
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
        </>
      )}
    </Card>
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
