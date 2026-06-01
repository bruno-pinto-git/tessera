import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ClipboardList } from "lucide-react";
import { useAuth } from "@/auth/useAuth";
import { useMe } from "@/auth/useMe";
import { getClub, type Club } from "@/features/clubs/api/clubsApi";
import { MembersSection } from "@/features/members/components/MembersSection";
import { TeamsSection } from "@/features/teams/components/TeamsSection";
import { MatchesSection } from "@/features/matches/components/MatchesSection";

/**
 * Per-club management area opened from `/club/<id>` (managers) or
 * `/admin/clubs/<id>` (admins). Shows the club header, its members
 * (admin-only) and its teams. Players are managed per team — open a team
 * from the teams panel to reach its squad. Match sheets are still under
 * construction. The scope-aware backend `@PreAuthorize` checks are in
 * place, so management actions "just work" for both roles.
 */
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
  // Manager OR staff of this club — staff get the same dashboard, read-only.
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

      {/* Members panel - admins, managers and staff of this club. Managers see
          a scoped mode (staff-only editing); staff get it read-only. */}
      {!Number.isNaN(clubId) && (isPlatformAdmin || isMemberOfThis) && (
        <MembersSection clubId={clubId} canManage={canManage} managerMode={!isPlatformAdmin} />
      )}

      {/* Teams panel - visible to everyone reading the page, manage actions
          gated by canManage. */}
      {!Number.isNaN(clubId) && <TeamsSection clubId={clubId} canManage={canManage} />}

      {/* Jogos & Bilheteira - home matches for this club. */}
      {!Number.isNaN(clubId) && <MatchesSection clubId={clubId} canManage={canManage} />}

      <section className="grid gap-4 md:grid-cols-2">
        <SectionCard
          icon={<ClipboardList className="size-5 text-primary" />}
          title="Fichas técnicas"
          description="Convocatórias e ocorrências dos jogos."
        />
      </section>
    </div>
  );
}

function SectionCard({
  icon,
  title,
  description,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
}) {
  return (
    <Card className="opacity-60">
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            {icon}
            <CardTitle className="text-base">{title}</CardTitle>
          </div>
          <Badge variant="secondary">Em breve</Badge>
        </div>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="text-xs text-muted-foreground">
        UI em construção. Os endpoints já estão scoped a este clube.
      </CardContent>
    </Card>
  );
}
