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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ApiError } from "@/api/client";
import { addMember, type ClubMembershipRole } from "@/api/membersApi";
import { createUser, searchUsers, type UserSummary } from "@/api/usersApi";

interface AddMemberDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  clubId: number;
  role: ClubMembershipRole;
  onAdded: () => void;
}

export function AddMemberDialog({ open, onOpenChange, clubId, role, onAdded }: AddMemberDialogProps) {
  const title = role === "MANAGER" ? "Adicionar gestor" : "Adicionar staff";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>
            Atribui um utilizador existente ao clube, ou cria um novo utilizador.
          </DialogDescription>
        </DialogHeader>

        <Tabs defaultValue="existing" className="w-full">
          <TabsList className="grid grid-cols-2 w-full">
            <TabsTrigger value="existing">Utilizador existente</TabsTrigger>
            <TabsTrigger value="new">Criar novo</TabsTrigger>
          </TabsList>
          <TabsContent value="existing">
            <ExistingUserPanel
              clubId={clubId}
              role={role}
              onAdded={() => {
                onAdded();
                onOpenChange(false);
              }}
            />
          </TabsContent>
          <TabsContent value="new">
            <NewUserPanel
              clubId={clubId}
              role={role}
              onAdded={() => {
                onAdded();
                onOpenChange(false);
              }}
            />
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}

// -----------------------------------------------------------------------------

function ExistingUserPanel({
  clubId,
  role,
  onAdded,
}: {
  clubId: number;
  role: ClubMembershipRole;
  onAdded: () => void;
}) {
  const [query, setQuery] = useState("");
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState<string | null>(null);

  // Debounce-free search: fire on Enter or click.
  async function doSearch() {
    setError(null);
    try {
      const res = await searchUsers({ search: query || undefined, max: 20 });
      setUsers(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro inesperado");
    } finally {
      setSearching(false);
    }
  }

  useEffect(() => {
    // Initial load — first 20 users in the realm.
    void Promise.resolve().then(() => setSearching(true));
    void doSearch();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function pick(user: UserSummary) {
    setSubmitting(user.id);
    setError(null);
    try {
      await addMember(clubId, { userId: user.id, role });
      onAdded();
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError("Este utilizador já está atribuído.");
      } else {
        setError(err instanceof Error ? err.message : "Erro inesperado");
      }
    } finally {
      setSubmitting(null);
    }
  }

  return (
    <div className="space-y-4 pt-3">
      <div className="flex gap-2">
        <Input
          placeholder="Procurar por nome, username, email…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              setSearching(true);
              void doSearch();
            }
          }}
        />
        <Button
          type="button"
          variant="outline"
          onClick={() => {
            setSearching(true);
            void doSearch();
          }}
        >
          Procurar
        </Button>
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <div className="rounded-md border max-h-72 overflow-auto">
        {searching && (
          <p className="text-sm text-muted-foreground p-3">A procurar…</p>
        )}
        {!searching && users.length === 0 && (
          <p className="text-sm text-muted-foreground p-3">Sem resultados.</p>
        )}
        {!searching &&
          users.map((u) => {
            const fullName = [u.firstName, u.lastName].filter(Boolean).join(" ");
            return (
              <div
                key={u.id}
                className="flex items-center justify-between gap-2 px-3 py-2 border-b last:border-b-0"
              >
                <div className="min-w-0">
                  <div className="text-sm font-medium truncate">
                    {fullName || u.username || u.email || u.id}
                  </div>
                  <div className="text-xs text-muted-foreground truncate">
                    {u.username}
                    {u.email ? ` · ${u.email}` : ""}
                  </div>
                </div>
                <Button
                  size="sm"
                  onClick={() => pick(u)}
                  disabled={submitting === u.id}
                >
                  {submitting === u.id ? "A adicionar…" : "Adicionar"}
                </Button>
              </div>
            );
          })}
      </div>
    </div>
  );
}

// -----------------------------------------------------------------------------

function NewUserPanel({
  clubId,
  role,
  onAdded,
}: {
  clubId: number;
  role: ClubMembershipRole;
  onAdded: () => void;
}) {
  const [form, setForm] = useState({
    username: "",
    email: "",
    firstName: "",
    lastName: "",
    password: "",
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
      // The realm role for new users is determined by the club role:
      // MANAGER -> realm role "club-manager", STAFF -> "staff".
      const realmRole = role === "MANAGER" ? "club-manager" : "staff";
      const user = await createUser({
        username: form.username.trim(),
        email: form.email.trim() || undefined,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        password: form.password,
        role: realmRole,
      });
      await addMember(clubId, { userId: user.id, role });
      onAdded();
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setError("Já existe um utilizador com este username ou email.");
        } else if (err.status === 400) {
          setError("Pedido inválido. Verifica os campos.");
        } else {
          setError(err.message);
        }
      } else {
        setError(err instanceof Error ? err.message : "Erro inesperado");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3 pt-3">
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="new-username">Username</Label>
          <Input
            id="new-username"
            value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })}
            required
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="new-email">Email</Label>
          <Input
            id="new-email"
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="new-firstName">Primeiro nome</Label>
          <Input
            id="new-firstName"
            value={form.firstName}
            onChange={(e) => setForm({ ...form, firstName: e.target.value })}
            required
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="new-lastName">Apelido</Label>
          <Input
            id="new-lastName"
            value={form.lastName}
            onChange={(e) => setForm({ ...form, lastName: e.target.value })}
            required
          />
        </div>
        <div className="space-y-1.5 col-span-2">
          <Label htmlFor="new-password">Password inicial</Label>
          <Input
            id="new-password"
            type="text"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
          />
          <p className="text-xs text-muted-foreground">
            O utilizador pode mudar a password depois no perfil dele.
          </p>
        </div>
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <DialogFooter>
        <Button type="submit" disabled={submitting}>
          {submitting ? "A criar…" : "Criar e adicionar"}
        </Button>
      </DialogFooter>
    </form>
  );
}