import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/api/client";
import { createUser, updateUser, type UserSummary } from "@/api/usersApi";

interface UserFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSaved: () => void;
  /** When provided, the dialog edits this user instead of creating one. */
  user?: UserSummary | null;
}

interface FormState {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  password: string;
  role: "club-manager" | "staff";
}

const EMPTY: FormState = {
  username: "",
  email: "",
  firstName: "",
  lastName: "",
  password: "",
  role: "club-manager",
};

const selectClass =
  "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring";

/** Picks the editable role to preselect; defaults to club-manager. */
function manageableRoleOf(user?: UserSummary | null): "club-manager" | "staff" {
  return user?.roles?.includes("staff") ? "staff" : "club-manager";
}

/**
 * Create / edit user dialog used by the admin /admin/users page.
 *
 * Create mode: the initial password is always *temporary* — Keycloak forces
 * the user to choose a new one on first login. The user can be attached to a
 * club afterwards from the club's member panel.
 *
 * Edit mode: profile fields + role + account status are editable; an optional
 * new (temporary) password can be set, and "force password change" adds the
 * UPDATE_PASSWORD required action. Platform admins keep their role (we only
 * manage club-manager / staff here).
 */
export function UserFormDialog({ open, onOpenChange, onSaved, user }: UserFormDialogProps) {
  const isEdit = !!user;
  const isAdmin = !!user?.roles?.includes("platform-admin");
  const [form, setForm] = useState<FormState>(EMPTY);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setError(null);
    if (user) {
      setForm({
        username: user.username ?? "",
        email: user.email ?? "",
        firstName: user.firstName ?? "",
        lastName: user.lastName ?? "",
        password: "",
        role: manageableRoleOf(user),
      });
    } else {
      setForm(EMPTY);
    }
  }, [open, user]);

  function validate(): string | null {
    if (!isEdit && form.username.trim().length < 3)
      return "Username com pelo menos 3 caracteres.";
    if (!form.firstName.trim()) return "Primeiro nome obrigatório.";
    if (!form.lastName.trim()) return "Apelido obrigatório.";
    // Password is only set on create (temporary). Editing never touches it.
    if (!isEdit && form.password.length < 6) return "Password com pelo menos 6 caracteres.";
    return null;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const err = validate();
    if (err) {
      setError(err);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      if (isEdit && user) {
        await updateUser(user.id, {
          email: form.email.trim() || undefined,
          firstName: form.firstName.trim(),
          lastName: form.lastName.trim(),
          // Don't touch the role of a platform-admin.
          role: isAdmin ? undefined : form.role,
        });
      } else {
        await createUser({
          username: form.username.trim(),
          email: form.email.trim() || undefined,
          firstName: form.firstName.trim(),
          lastName: form.lastName.trim(),
          password: form.password,
          role: form.role,
        });
      }
      onSaved();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) setError("Já existe um utilizador com este username ou email.");
        else if (err.status === 400) setError("Pedido inválido. Verifica os campos.");
        else if (err.status === 404) setError("Utilizador já não existe.");
        else setError(err.message);
      } else {
        setError(err instanceof Error ? err.message : "Erro inesperado");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>{isEdit ? "Editar utilizador" : "Novo utilizador"}</DialogTitle>
            <DialogDescription>
              {isEdit
                ? "Atualiza os dados e o tipo do utilizador. O estado da conta e a troca de password gerem-se pelos botões na lista."
                : "Cria um utilizador na plataforma. A password definida é temporária — o utilizador terá de a trocar no primeiro login."}
            </DialogDescription>
          </DialogHeader>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="u-username">Username</Label>
              <Input
                id="u-username"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                required={!isEdit}
                disabled={isEdit}
                autoFocus={!isEdit}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="u-email">Email</Label>
              <Input
                id="u-email"
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="u-firstName">Primeiro nome</Label>
              <Input
                id="u-firstName"
                value={form.firstName}
                onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                required
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="u-lastName">Apelido</Label>
              <Input
                id="u-lastName"
                value={form.lastName}
                onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                required
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="u-role">Tipo</Label>
              <select
                id="u-role"
                value={isAdmin ? "platform-admin" : form.role}
                disabled={isAdmin}
                onChange={(e) =>
                  setForm({ ...form, role: e.target.value as "club-manager" | "staff" })
                }
                className={selectClass}
              >
                {isAdmin && <option value="platform-admin">Admin (gerido no realm)</option>}
                <option value="club-manager">Gestor</option>
                <option value="staff">Staff</option>
              </select>
            </div>
            {!isEdit && (
              <div className="space-y-1.5">
                <Label htmlFor="u-password">Password inicial (temporária)</Label>
                <Input
                  id="u-password"
                  type="text"
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  required
                />
              </div>
            )}
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={submitting}
            >
              Cancelar
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting
                ? isEdit
                  ? "A guardar…"
                  : "A criar…"
                : isEdit
                  ? "Guardar"
                  : "Criar"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
