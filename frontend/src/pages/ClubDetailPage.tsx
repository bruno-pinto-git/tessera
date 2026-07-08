import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ClipboardList } from "lucide-react";
import { useAuth } from "@/auth/useAuth";
import { useMe } from "@/auth/useMe";
import { getClub, type Club } from "@/features/clubs/api/clubsApi";
import { MembersSection } from "@/features/members/components/MembersSection";
import { TeamsSection } from "@/features/teams/components/TeamsSection";
import { MatchesSection } from "@/features/matches/components/MatchesSection";

export function ClubDetailPage() {
  const { id } = useParams<{ id: string }>();
  const clubId = Number(id);
  const { hasRole } = useAuth();
  const { me } = useMe();
  const isPlatformAdmin = hasRole("platform-admin");
  const [club, setClub] = useState<Club | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!clubId || Number.isNaN(clubId)) return;
    let cancelled = false;
    const run = async () => {
      try {
        const c = await getClub(clubId);
        if (!cancelled) setClub(c);
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Erro inesperado");
      }
    };
    void run();
    return () => {
      cancelled = true;
    };
  }, [clubId]);

  const isManagerOfThis = me?.clubMemberships?.some(
    (m) => m.clubId === clubId && m.role === "MANAGER",
  );
  const isMemberOfThis = me?.clubMemberships?.some((m) => m.clubId === clubId);
  const canManage = isPlatformAdmin || !!isManagerOfThis;
  const backTo = isPlatformAdmin ? "/admin/clubs" : isMemberOfThis ? "/club" : "/admin/clubs";

  return (
    <div className="space-y-8">
      <header className="space-y-1">
        <Link
          to={backTo}
          className="text-xs text-muted-foreground hover:text-foreground transition-colors"
        >
          ← Voltar
        </Link>
        <h1 className="text-2xl font-bold tracking-tight">{club?.name ?? "Clube"}</h1>
        {club?.foundedYear && (
          <p className="text-sm text-muted-foreground">Fundado em {club.foundedYear}</p>
        )}
      </header>

      {error && <p className="text-sm text-destructive">Falha a carregar: {error}</p>}

      {!Number.isNaN(clubId) && (isPlatformAdmin || isMemberOfThis) && (
        <MembersSection clubId={clubId} canManage={canManage} managerMode={!isPlatformAdmin} />
      )}

      {!Number.isNaN(clubId) && <TeamsSection clubId={clubId} canManage={canManage} />}

      {!Number.isNaN(clubId) && <MatchesSection clubId={clubId} canManage={canManage} />}

      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <ClipboardList className="size-5 text-primary" />
            <CardTitle className="text-base">Fichas técnicas</CardTitle>
          </div>
          <CardDescription>Convocatórias e lances dos jogos.</CardDescription>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          Abre a ficha técnica de cada jogo pelo botão{" "}
          <ClipboardList className="inline size-4 align-text-bottom" /> na lista de{" "}
          <span className="font-medium text-foreground">Jogos &amp; Bilheteira</span> acima.
        </CardContent>
      </Card>
    </div>
  );
}
