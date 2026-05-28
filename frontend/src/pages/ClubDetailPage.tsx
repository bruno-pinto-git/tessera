import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { UserSquare2, ClipboardList } from "lucide-react";
import { useAuth } from "@/auth/useAuth";
import { useMe } from "@/auth/useMe";
import { getClub, type Club } from "@/features/clubs/api/clubsApi";
import { MembersSection } from "@/features/members/components/MembersSection";
import { TeamsSection } from "@/features/teams/components/TeamsSection";

/**
 * Per-club management area opened from `/club/<id>` (managers) or
 * `/admin/clubs/<id>` (admins — future). Phase 2 stub: shows the club
 * header and a dashboard of sections (Teams / Players / Sheets) that are
 * still under construction. The scope-aware backend `@PreAuthorize`
 * checks are in place, so when these UIs land they "just work" for both
 * roles.
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
  const backTo = isPlatformAdmin ? "/admin/clubs" : isManagerOfThis ? "/club" : "/admin/clubs";

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

      {/* Members panel - platform-admin only. */}
      {isPlatformAdmin && !Number.isNaN(clubId) && <MembersSection clubId={clubId} />}

      {/* Teams panel - visible to everyone reading the page, manage actions
          gated by canManage. */}
      {!Number.isNaN(clubId) && (
        <TeamsSection clubId={clubId} canManage={isPlatformAdmin || !!isManagerOfThis} />
      )}

      <section className="grid gap-4 md:grid-cols-2">
        <SectionCard
          icon={<UserSquare2 className="size-5 text-primary" />}
          title="Jogadores"
          description="Plantéis das equipas, com cartões e estatísticas."
        />
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
