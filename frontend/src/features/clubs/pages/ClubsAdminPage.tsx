import { useState } from "react";
import { Link } from "react-router-dom";
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
import { ChevronLeft, ChevronRight, ExternalLink, Pencil, Plus, Trash2 } from "lucide-react";
import { useClubs } from "../hooks/useClubs";
import { ClubFormDialog } from "../components/ClubFormDialog";
import { DeleteClubDialog } from "../components/DeleteClubDialog";
import type { Club } from "../api/clubsApi";

const PAGE_SIZE = 20;

export function ClubsAdminPage() {
  const [page, setPage] = useState(0);
  const [name, setName] = useState("");
  const [editing, setEditing] = useState<Club | null>(null);
  const [deleting, setDeleting] = useState<Club | null>(null);
  const [createOpen, setCreateOpen] = useState(false);

  const { data, loading, error, refetch } = useClubs({ page, size: PAGE_SIZE, name });

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
          <h1 className="text-2xl font-bold tracking-tight">Clubes</h1>
          <p className="text-sm text-muted-foreground">Gere os clubes registados na plataforma.</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="size-4" />
          Novo clube
        </Button>
      </header>

      <Input
        placeholder="Procurar por nome…"
        value={name}
        onChange={(e) => {
          setName(e.target.value);
          setPage(0);
        }}
        className="max-w-sm"
      />

      {error && <p className="text-sm text-destructive">Falha a carregar clubes: {error}</p>}

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nome</TableHead>
              <TableHead className="w-32">Fundado</TableHead>
              <TableHead>Emblema</TableHead>
              <TableHead className="w-32 text-right">Ações</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading && (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-muted-foreground py-8">
                  A carregar…
                </TableCell>
              </TableRow>
            )}
            {!loading && data && data.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-muted-foreground py-8">
                  {name
                    ? "Sem clubes a corresponder ao filtro."
                    : "Ainda não há clubes registados."}
                </TableCell>
              </TableRow>
            )}
            {!loading &&
              data?.content.map((club) => (
                <TableRow key={club.id}>
                  <TableCell className="font-medium">{club.name}</TableCell>
                  <TableCell className="text-muted-foreground">{club.foundedYear ?? "—"}</TableCell>
                  <TableCell className="text-muted-foreground truncate max-w-xs">
                    {club.crestUrl ?? "—"}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="icon"
                      asChild
                      aria-label={`Abrir ${club.name}`}
                    >
                      <Link to={`/admin/clubs/${club.id}`}>
                        <ExternalLink className="size-4" />
                      </Link>
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setEditing(club)}
                      aria-label={`Editar ${club.name}`}
                    >
                      <Pencil className="size-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setDeleting(club)}
                      aria-label={`Eliminar ${club.name}`}
                    >
                      <Trash2 className="size-4 text-destructive" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
          </TableBody>
        </Table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm">
          <p className="text-muted-foreground">
            {data.totalElements} clube{data.totalElements === 1 ? "" : "s"} · página {data.page + 1}{" "}
            de {data.totalPages}
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

      <ClubFormDialog open={createOpen} onOpenChange={setCreateOpen} onSaved={refetch} />
      <ClubFormDialog
        open={!!editing}
        onOpenChange={(open) => !open && setEditing(null)}
        club={editing}
        onSaved={refetch}
      />
      <DeleteClubDialog
        open={!!deleting}
        onOpenChange={(open) => !open && setDeleting(null)}
        club={deleting}
        onDeleted={refetch}
      />
    </div>
  );
}
