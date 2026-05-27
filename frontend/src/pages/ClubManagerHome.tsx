import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useMe } from "@/auth/useMe";
import { getClub, type Club } from "@/features/clubs/api/clubsApi";
import { ApiError } from "@/api/client";

/**
 * Landing page for users with `club-manager`. Fetches /me, filters the
 * clubMemberships down to MANAGER entries, and resolves each one against
 * the real club record. From there the user can drill into a per-club
 * management area (teams / players / sheets — still under construction).
 */
export function ClubManagerHome() {
  const { me, loading: meLoading, error: meError } = useMe();
  const [clubs, setClubs] = useState<Club[]>([]);
  const [loadingClubs, setLoadingClubs] = useState(false);
  const [missingIds, setMissingIds] = useState<number[]>([]);

  const managedIds = (me?.clubMemberships ?? [])
    .filter((m) => m.role === "MANAGER")
    .map((m) => m.clubId);
  const managedKey = managedIds.join(",");

  useEffect(() => {
    if (managedIds.length === 0) {
      setClubs([]);
      setMissingIds([]);
      return;
    }
    let cancelled = false;

    const run = async () => {
      const results = await Promise.allSettled(managedIds.map((id) => getClub(id)));
      if (cancelled) return;
      const ok: Club[] = [];
      const missing: number[] = [];
      results.forEach((r, i) => {
        if (r.status === "fulfilled") {
          ok.push(r.value);
        } else if (r.reason instanceof ApiError && r.reason.status === 404) {
          missing.push(managedIds[i]);
        }
      });
      setClubs(ok);
      setMissingIds(missing);
      setLoadingClubs(false);
    };

    void Promise.resolve().then(() => {
      if (!cancelled) setLoadingClubs(true);
    });
    void run();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [managedKey]);

  return (
    <div className="space-y-8">
      <header className="space-y-1">
        <h1 className="text-2xl font-bold tracking-tight">O meu clube</h1>
        <p className="text-sm text-muted-foreground">
          {me?.firstName ? `Olá ${me.firstName}. ` : null}
          Gere abaixo os clubes que te foram atribuídos.
        </p>
      </header>

      {meLoading && <p className="text-sm text-muted-foreground">A carregar perfil…</p>}
      {meError && <p className="text-sm text-destructive">Falha a carregar perfil: {meError}</p>}

      {me && managedIds.length === 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Sem clubes atribuídos</CardTitle>
            <CardDescription>
              Ainda não foste associado a nenhum clube. Pede ao administrador da plataforma para te
              adicionar ao grupo <code>/clubs/&lt;id&gt;/managers</code> no Keycloak.
            </CardDescription>
          </CardHeader>
        </Card>
      )}

      {loadingClubs && managedIds.length > 0 && (
        <p className="text-sm text-muted-foreground">A carregar clubes…</p>
      )}

      {clubs.length > 0 && (
        <section className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {clubs.map((club) => (
            <Link to={`/club/${club.id}`} key={club.id}>
              <Card className="transition-colors hover:border-primary/40 hover:bg-muted/30 cursor-pointer">
                <CardHeader>
                  <CardTitle className="text-base">{club.name}</CardTitle>
                  <CardDescription>
                    {club.foundedYear
                      ? `Fundado em ${club.foundedYear}`
                      : "Ano de fundação por preencher"}
                  </CardDescription>
                </CardHeader>
                <CardContent className="text-xs text-muted-foreground">Abrir →</CardContent>
              </Card>
            </Link>
          ))}
        </section>
      )}

      {missingIds.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Memberships órfãos</CardTitle>
            <CardDescription>
              Estes ids de clube vêm no teu token mas já não existem na plataforma (talvez tenham
              sido eliminados): {missingIds.join(", ")}. Pede ao admin para limpar.
            </CardDescription>
          </CardHeader>
        </Card>
      )}
    </div>
  );
}