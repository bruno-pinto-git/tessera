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
import { Ban, CircleCheck, KeyRound, Pencil, Plus, Trash2 } from "lucide-react";
import { useUsers } from "../hooks/useUsers";
import { UserFormDialog } from "../components/UserFormDialog";
import { DeleteUserDialog } from "../components/DeleteUserDialog";
import { ForcePasswordResetDialog } from "../components/ForcePasswordResetDialog";
import { updateUser, type UserSummary } from "@/api/usersApi";

const ROLE_LABELS: Record<string, string> = {
  "platform-admin": "Admin",
  staff: "Staff",
  "club-manager": "Gestor",
  fan: "Adepto",
};

function RoleBadge({ roles }: { roles: string[] }) {
  const order = ["platform-admin", "staff", "club-manager", "fan"];
  const top = order.find((r) => roles.includes(r));
  if (!top) return <span className="text-muted-foreground">—</span>;
  return <Badge variant={top === "platform-admin" ? "default" : "secondary"}>{ROLE_LABELS[top]}</Badge>;
}

export function UsersAdminPage() {
  const [search, setSearch] = useState("");
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<UserSummary | null>(null);
  const [resetting, setResetting] = useState<UserSummary | null>(null);
  const [deleting, setDeleting] = useState<UserSummary | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const { users, loading, error, refetch } = useUsers({ search });

  async function toggleEnabled(u: UserSummary) {
    const enable = u.enabled === false;
    setTogglingId(u.id);
    setActionError(null);
    try {
      await updateUser(u.id, { enabled: enable });
      refetch();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Erro ao alterar o estado.");
    } finally {
      setTogglingId(null);
    }
  }

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
            Gere os utilizadores da plataforma. Para atribuir um utilizador a um clube, abre a
            página do clube e vai a Membros.
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
      {actionError && <p className="text-sm text-destructive">{actionError}</p>}

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Username</TableHead>
              <TableHead>Nome</TableHead>
              <TableHead>Email</TableHead>
              <TableHead className="w-32">Tipo</TableHead>
              <TableHead className="w-24">Estado</TableHead>
              <TableHead className="w-40 text-right">Ações</TableHead>
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
            {!loading && users.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
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
                      <RoleBadge roles={u.roles ?? []} />
                    </TableCell>
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
                        onClick={() => toggleEnabled(u)}
                        disabled={togglingId === u.id}
                        aria-label={`${u.enabled === false ? "Ativar" : "Desativar"} ${u.username}`}
                        title={u.enabled === false ? "Ativar conta" : "Desativar conta"}
                      >
                        {u.enabled === false ? (
                          <CircleCheck className="size-4 text-primary" />
                        ) : (
                          <Ban className="size-4" />
                        )}
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => setResetting(u)}
                        aria-label={`Forçar troca de password ${u.username}`}
                        title="Forçar troca de password"
                      >
                        <KeyRound className="size-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => setEditing(u)}
                        aria-label={`Editar ${u.username}`}
                        title="Editar"
                      >
                        <Pencil className="size-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => setDeleting(u)}
                        aria-label={`Eliminar ${u.username}`}
                        title="Eliminar"
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
      <UserFormDialog
        open={!!editing}
        onOpenChange={(o) => !o && setEditing(null)}
        onSaved={refetch}
        user={editing}
      />
      <ForcePasswordResetDialog
        open={!!resetting}
        onOpenChange={(o) => !o && setResetting(null)}
        user={resetting}
        onDone={refetch}
      />
      <DeleteUserDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        user={deleting}
        onDeleted={refetch}
      />
    </div>
  );
}
