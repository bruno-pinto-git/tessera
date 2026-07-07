import { useState } from "react";
import { Link } from "react-router-dom";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ExternalLink, Pencil, Plus, Trash2 } from "lucide-react";
import { useTeamsByClub } from "../hooks/useTeamsByClub";
import { TeamFormDialog } from "./TeamFormDialog";
import { DeleteTeamDialog } from "./DeleteTeamDialog";
import { teamCategoryLabel, type Team } from "../api/teamsApi";

interface TeamsSectionProps {
  clubId: number;
  canManage: boolean;
}

export function TeamsSection({ clubId, canManage }: TeamsSectionProps) {
  const { teams, loading, error, refetch } = useTeamsByClub(clubId);
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Team | null>(null);
  const [deleting, setDeleting] = useState<Team | null>(null);

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between gap-3">
          <div>
            <CardTitle className="text-base">Equipas</CardTitle>
            <CardDescription>
              Cada equipa representa uma categoria deste clube (sénior, sub-19…).
            </CardDescription>
          </div>
          {canManage && (
            <Button size="sm" onClick={() => setCreateOpen(true)}>
              <Plus className="size-4" />
              Nova equipa
            </Button>
          )}
        </div>
      </CardHeader>
      <CardContent>
        {error && <p className="text-sm text-destructive">Falha a carregar equipas: {error}</p>}

        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Categoria</TableHead>
                <TableHead className="w-32 text-right">{canManage ? "Ações" : ""}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading && (
                <TableRow>
                  <TableCell colSpan={2} className="text-center text-muted-foreground py-8">
                    A carregar…
                  </TableCell>
                </TableRow>
              )}
              {!loading && teams.length === 0 && (
                <TableRow>
                  <TableCell colSpan={2} className="text-center text-muted-foreground py-8">
                    Ainda não há equipas neste clube.
                  </TableCell>
                </TableRow>
              )}
              {!loading &&
                teams.map((t) => (
                  <TableRow key={t.id}>
                    <TableCell className="font-medium">{teamCategoryLabel(t.category)}</TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="icon"
                        asChild
                        aria-label={`Abrir ${teamCategoryLabel(t.category)}`}
                      >
                        <Link to={`/team/${t.id}`}>
                          <ExternalLink className="size-4" />
                        </Link>
                      </Button>
                      {canManage && (
                        <>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => setEditing(t)}
                            aria-label={`Editar ${teamCategoryLabel(t.category)}`}
                          >
                            <Pencil className="size-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => setDeleting(t)}
                            aria-label={`Eliminar ${teamCategoryLabel(t.category)}`}
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

      <TeamFormDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        clubId={clubId}
        onSaved={refetch}
      />
      <TeamFormDialog
        open={!!editing}
        onOpenChange={(o) => !o && setEditing(null)}
        clubId={clubId}
        team={editing}
        onSaved={refetch}
      />
      <DeleteTeamDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        team={deleting}
        onDeleted={refetch}
      />
    </Card>
  );
}
