import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Plus, Trash2 } from "lucide-react";
import {
  listMembers,
  removeMember,
  type ClubMember,
  type ClubMembers,
  type ClubMembershipRole,
} from "@/api/membersApi";
import { ApiError } from "@/api/client";
import { AddMemberDialog } from "./AddMemberDialog";

interface MembersSectionProps {
  clubId: number;
}

/**
 * Members panel embedded inside the platform-admin's club detail page.
 * Lists managers and staff side by side, allows adding (existing or new
 * user) and removing. Removing only revokes the Keycloak group membership
 * — it doesn't delete the user.
 */
export function MembersSection({ clubId }: MembersSectionProps) {
  const [data, setData] = useState<ClubMembers | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [addingRole, setAddingRole] = useState<ClubMembershipRole | null>(null);

  const refetch = useCallback(() => setReloadKey((k) => k + 1), []);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      try {
        const res = await listMembers(clubId);
        if (!cancelled) {
          setData(res);
          setError(null);
        }
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Erro inesperado");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    void Promise.resolve().then(() => {
      if (!cancelled) setLoading(true);
    });
    void run();
    return () => {
      cancelled = true;
    };
  }, [clubId, reloadKey]);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Membros</CardTitle>
        <CardDescription>
          Gestores podem editar dados do clube; staff valida bilhetes e preenche fichas.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {loading && <p className="text-sm text-muted-foreground">A carregar…</p>}
        {error && <p className="text-sm text-destructive">Falha a carregar membros: {error}</p>}

        {data && (
          <div className="grid gap-6 md:grid-cols-2">
            <MemberList
              title="Gestores"
              role="MANAGER"
              clubId={clubId}
              members={data.managers}
              onAddClick={() => setAddingRole("MANAGER")}
              onChanged={refetch}
            />
            <MemberList
              title="Staff"
              role="STAFF"
              clubId={clubId}
              members={data.staff}
              onAddClick={() => setAddingRole("STAFF")}
              onChanged={refetch}
            />
          </div>
        )}
      </CardContent>

      <AddMemberDialog
        open={addingRole !== null}
        onOpenChange={(open) => !open && setAddingRole(null)}
        clubId={clubId}
        role={addingRole ?? "MANAGER"}
        onAdded={refetch}
      />
    </Card>
  );
}

function MemberList({
  title,
  role,
  clubId,
  members,
  onAddClick,
  onChanged,
}: {
  title: string;
  role: ClubMembershipRole;
  clubId: number;
  members: ClubMember[];
  onAddClick: () => void;
  onChanged: () => void;
}) {
  const [removing, setRemoving] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleRemove(m: ClubMember) {
    setRemoving(m.userId);
    setError(null);
    try {
      await removeMember(clubId, m.userId, role);
      onChanged();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Erro ao remover");
    } finally {
      setRemoving(null);
    }
  }

  return (
    <section className="space-y-2">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold">{title}</h3>
        <Button size="sm" variant="outline" onClick={onAddClick}>
          <Plus className="size-4" />
          Adicionar
        </Button>
      </div>

      {error && <p className="text-xs text-destructive">{error}</p>}

      <div className="rounded-md border divide-y">
        {members.length === 0 && (
          <p className="text-sm text-muted-foreground p-3">Sem membros.</p>
        )}
        {members.map((m) => {
          const fullName = [m.firstName, m.lastName].filter(Boolean).join(" ");
          return (
            <div key={m.userId} className="flex items-center justify-between gap-2 px-3 py-2">
              <div className="min-w-0">
                <div className="text-sm font-medium truncate">
                  {fullName || m.username || m.userId}
                </div>
                <div className="text-xs text-muted-foreground truncate">
                  {m.username}
                  {m.email ? ` · ${m.email}` : ""}
                </div>
              </div>
              <Button
                size="icon"
                variant="ghost"
                onClick={() => handleRemove(m)}
                disabled={removing === m.userId}
                aria-label={`Remover ${m.username}`}
              >
                <Trash2 className="size-4 text-destructive" />
              </Button>
            </div>
          );
        })}
      </div>
    </section>
  );
}