import { useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Pencil, Plus, Trash2 } from "lucide-react";
import {
  positionLabel,
  statusLabel,
  type Player,
  type PlayerStatus,
} from "../api/playersApi";
import { usePlayersByTeam } from "../hooks/usePlayersByTeam";
import { PlayerFormDialog } from "./PlayerFormDialog";
import { DeletePlayerDialog } from "./DeletePlayerDialog";

interface PlayersSectionProps {
  teamId: number;
  canManage: boolean;
}

export function PlayersSection({ teamId, canManage }: PlayersSectionProps) {
  const { players, loading, error, refetch } = usePlayersByTeam(teamId);
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Player | null>(null);
  const [deleting, setDeleting] = useState<Player | null>(null);

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between gap-3">
          <div>
            <CardTitle className="text-base">Jogadores</CardTitle>
            <CardDescription>
              Plantel desta equipa. Cada jogador pertence a uma única equipa.
            </CardDescription>
          </div>
          {canManage && (
            <Button size="sm" onClick={() => setCreateOpen(true)}>
              <Plus className="size-4" />
              Novo jogador
            </Button>
          )}
        </div>
      </CardHeader>
      <CardContent>
        {error && <p className="text-sm text-destructive">Falha a carregar jogadores: {error}</p>}

        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-12 text-center">#</TableHead>
                <TableHead>Nome</TableHead>
                <TableHead className="w-28">Posição</TableHead>
                <TableHead className="w-28">Estado</TableHead>
                <TableHead className="w-28 text-right">{canManage ? "Ações" : ""}</TableHead>
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
              {!loading && players.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground py-8">
                    Sem jogadores no plantel.
                  </TableCell>
                </TableRow>
              )}
              {!loading &&
                players.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell className="text-center text-muted-foreground">
                      {p.shirtNumber ?? "—"}
                    </TableCell>
                    <TableCell className="font-medium">
                      {p.firstName} {p.lastName}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {positionLabel(p.position)}
                    </TableCell>
                    <TableCell>
                      <StatusBadge status={p.status} />
                    </TableCell>
                    <TableCell className="text-right">
                      {canManage && (
                        <>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => setEditing(p)}
                            aria-label={`Editar ${p.firstName} ${p.lastName}`}
                          >
                            <Pencil className="size-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => setDeleting(p)}
                            aria-label={`Eliminar ${p.firstName} ${p.lastName}`}
                          >
                            <Trash2 className="size-4 text-destructive" />
                          </Button>
                        </>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
            </TableBody>
          </Table>
        </div>
      </CardContent>

      <PlayerFormDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        teamId={teamId}
        onSaved={refetch}
      />
      <PlayerFormDialog
        open={!!editing}
        onOpenChange={(o) => !o && setEditing(null)}
        teamId={teamId}
        player={editing}
        onSaved={refetch}
      />
      <DeletePlayerDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        player={deleting}
        onDeleted={refetch}
      />
    </Card>
  );
}

function StatusBadge({ status }: { status: PlayerStatus }) {
  const variant =
    status === "ACTIVE" ? "default" : status === "INJURED" ? "destructive" : "secondary";
  return <Badge variant={variant}>{statusLabel(status)}</Badge>;
}