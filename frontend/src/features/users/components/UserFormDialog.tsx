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
import { createUser } from "@/api/usersApi";

interface UserFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSaved: () => void;
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

/**
 * Standalone "new user" dialog used by the admin /admin/users page. Same
 * shape as the new-user tab inside `AddMemberDialog`, but doesn't bind the
 * user to a club afterwards — the admin can later attach them to one or
 * more clubs from the club's member panel.
 */
export function UserFormDialog({ open, onOpenChange, onSaved }: UserFormDialogProps) {
  const [form, setForm] = useState<FormState>(EMPTY);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setError(null);
    setForm(EMPTY);
  }, [open]);

  function validate(): string | null {
    if (form.username.trim().length < 3) return "Username com pelo menos 3 caracteres.";
    if (!form.firstName.trim()) return "Primeiro nome obrigatório.";
    if (!form.lastName.trim()) return "Apelido obrigatório.";
    if (form.password.length < 6) return "Password com pelo menos 6 caracteres.";
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
      await createUser({
        username: form.username.trim(),
        email: form.email.trim() || undefined,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        password: form.password,
        role: form.role,
      });
      onSaved();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) setError("Já existe um utilizador com este username ou email.");
        else if (err.status === 400) setError("Pedido inválido. Verifica os campos.");
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
            <DialogTitle>Novo utilizador</DialogTitle>
            <DialogDescription>
              Cria um utilizador na plataforma. Podes atribuí-lo a um clube depois, na página
              do clube → Membros.
            </DialogDescription>
          </DialogHeader>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="u-username">Username</Label>
              <Input
                id="u-username"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                required
                autoFocus
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
                value={form.role}
                onChange={(e) =>
                  setForm({ ...form, role: e.target.value as "club-manager" | "staff" })
                }
                className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              >
                <option value="club-manager">Gestor de clube</option>
                <option value="staff">Staff</option>
              </select>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="u-password">Password inicial</Label>
              <Input
                id="u-password"
                type="text"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                required
              />
            </div>
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
              {submitting ? "A criar…" : "Criar"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}