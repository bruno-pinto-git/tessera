import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "@/auth/useAuth";
import { useMe } from "@/auth/useMe";
import { getTeam, teamCategoryLabel, type Team } from "@/features/teams/api/teamsApi";
import { getClub, type Club } from "@/features/clubs/api/clubsApi";
import { PlayersSection } from "@/features/players/components/PlayersSection";

export function TeamDetailPage() {
  const { id } = useParams<{ id: string }>();
  const teamId = Number(id);
  const { hasRole } = useAuth();
  const { me } = useMe();
  const isPlatformAdmin = hasRole("platform-admin");

  const [team, setTeam] = useState<Team | null>(null);
  const [club, setClub] = useState<Club | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!teamId || Number.isNaN(teamId)) return;
    let cancelled = false;
    const run = async () => {
      try {
        const t = await getTeam(teamId);
        if (cancelled) return;
        setTeam(t);
        const c = await getClub(t.clubId);
        if (!cancelled) setClub(c);
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Erro inesperado");
      }
    };
    void run();
    return () => {
      cancelled = true;
    };
  }, [teamId]);

  const isManagerOfClub =
    team != null &&
    me?.clubMemberships?.some((m) => m.clubId === team.clubId && m.role === "MANAGER");
  const canManage = isPlatformAdmin || !!isManagerOfClub;

  const backTo = team
    ? isPlatformAdmin
      ? `/admin/clubs/${team.clubId}`
      : `/club/${team.clubId}`
    : "/";

  return (
    <div className="space-y-8">
      <header className="space-y-1">
        <Link
          to={backTo}
          className="text-xs text-muted-foreground hover:text-foreground transition-colors"
        >
          ← {club ? `Voltar a ${club.name}` : "Voltar"}
        </Link>
        <h1 className="text-2xl font-bold tracking-tight">
          {team ? teamCategoryLabel(team.category) : "Equipa"}
        </h1>
        {club && <p className="text-sm text-muted-foreground">{club.name}</p>}
      </header>

      {error && <p className="text-sm text-destructive">Falha a carregar: {error}</p>}

      {team && !Number.isNaN(teamId) && <PlayersSection teamId={teamId} canManage={canManage} />}
    </div>
  );
}
