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
import { ChevronLeft, ChevronRight, Pencil, Plus, Trash2 } from "lucide-react";
import { useVenues } from "../hooks/useVenues";
import { VenueFormDialog } from "../components/VenueFormDialog";
import { DeleteVenueDialog } from "../components/DeleteVenueDialog";
import type { Venue } from "../api/venuesApi";

const PAGE_SIZE = 20;

export function VenuesAdminPage() {
  const [page, setPage] = useState(0);
  const [name, setName] = useState("");
  const [editing, setEditing] = useState<Venue | null>(null);
  const [deleting, setDeleting] = useState<Venue | null>(null);
  const [createOpen, setCreateOpen] = useState(false);

  const { data, loading, error, refetch } = useVenues({ page, size: PAGE_SIZE, name });

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
          <h1 className="text-2xl font-bold tracking-tight">Estádios</h1>
          <p className="text-sm text-muted-foreground">
            Locais onde os jogos decorrem. Partilhados entre todos os clubes.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="size-4" />
          Novo estádio
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

      {error && <p className="text-sm text-destructive">Falha a carregar estádios: {error}</p>}

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nome</TableHead>
              <TableHead className="w-32">Capacidade</TableHead>
              <TableHead>Morada</TableHead>
              <TableHead className="w-24 text-right">Ações</TableHead>
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
                    ? "Sem estádios a corresponder ao filtro."
                    : "Ainda não há estádios registados."}
                </TableCell>
              </TableRow>
            )}
            {!loading &&
              data?.content.map((v) => (
                <TableRow key={v.id}>
                  <TableCell className="font-medium">{v.name}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {v.capacity.toLocaleString("pt-PT")}
                  </TableCell>
                  <TableCell className="text-muted-foreground truncate max-w-xs">
                    {v.address ?? "—"}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setEditing(v)}
                      aria-label={`Editar ${v.name}`}
                    >
                      <Pencil className="size-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setDeleting(v)}
                      aria-label={`Eliminar ${v.name}`}
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
            {data.totalElements} estádio{data.totalElements === 1 ? "" : "s"} · página{" "}
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

      <VenueFormDialog open={createOpen} onOpenChange={setCreateOpen} onSaved={refetch} />
      <VenueFormDialog
        open={!!editing}
        onOpenChange={(o) => !o && setEditing(null)}
        venue={editing}
        onSaved={refetch}
      />
      <DeleteVenueDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        venue={deleting}
        onDeleted={refetch}
      />
    </div>
  );
}
