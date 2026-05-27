import { useState } from "react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Plus, Trash2 } from "lucide-react";
import { useUsers } from "../hooks/useUsers";
import { UserFormDialog } from "../components/UserFormDialog";
import { DeleteUserDialog } from "../components/DeleteUserDialog";
import type { UserSummary } from "@/api/usersApi";

export function UsersAdminPage() {
  const [search, setSearch] = useState("");
  const [createOpen, setCreateOpen] = useState(false);
  const [deleting, setDeleting] = useState<UserSummary | null>(null);
  const { users, loading, error, refetch } = useUsers({ search });

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
          <h1 className="text-2xl font-bold tracking-tight">Utilizadores</h1>
          <p className="text-sm text-muted-foreground">
            Gere os utilizadores da plataforma. Para atribuir um utilizador a um clube, abre
            a página do clube e vai a Membros.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="size-4" />
          Novo utilizador
        </Button>
      </header>

      <Input
        placeholder="Procurar por username, nome ou email…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="max-w-sm"
      />

      {error && <p className="text-sm text-destructive">Falha a carregar: {error}</p>}

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Username</TableHead>
              <TableHead>Nome</TableHead>
              <TableHead>Email</TableHead>
              <TableHead className="w-24">Estado</TableHead>
              <TableHead className="w-20 text-right">Ações</TableHead>
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
            {!loading && users.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-muted-foreground py-8">
                  {search
                    ? "Sem utilizadores a corresponder ao filtro."
                    : "Ainda não há utilizadores."}
                </TableCell>
              </TableRow>
            )}
            {!loading &&
              users.map((u) => {
                const fullName = [u.firstName, u.lastName].filter(Boolean).join(" ");
                return (
                  <TableRow key={u.id}>
                    <TableCell className="font-medium">{u.username ?? "—"}</TableCell>
                    <TableCell className="text-muted-foreground">{fullName || "—"}</TableCell>
                    <TableCell className="text-muted-foreground">{u.email ?? "—"}</TableCell>
                    <TableCell>
                      {u.enabled === false ? (
                        <Badge variant="secondary">Desativado</Badge>
                      ) : (
                        <Badge>Activo</Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => setDeleting(u)}
                        aria-label={`Eliminar ${u.username}`}
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

      <UserFormDialog open={createOpen} onOpenChange={setCreateOpen} onSaved={refetch} />
      <DeleteUserDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        user={deleting}
        onDeleted={refetch}
      />
    </div>
  );
}