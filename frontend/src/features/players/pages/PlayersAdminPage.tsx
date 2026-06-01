import { useState } from "react";
import { Link } from "react-router-dom";
import { Label } from "@/components/ui/label";
import { useClubs } from "@/features/clubs/hooks/useClubs";
import { useTeamsByClub } from "@/features/teams/hooks/useTeamsByClub";
import { teamCategoryLabel } from "@/features/teams/api/teamsApi";
import { PlayersSection } from "../components/PlayersSection";

const selectClass =
  "flex h-9 w-full max-w-sm rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring";

export function PlayersAdminPage() {
  const [clubId, setClubId] = useState<number | null>(null);
  const [teamId, setTeamId] = useState<number | null>(null);

  const {
    data: clubsData,
    loading: clubsLoading,
    error: clubsError,
  } = useClubs({
    page: 0,
    size: 200,
    name: "",
  });
  const clubs = clubsData?.content ?? [];

  const { teams, loading: teamsLoading, error: teamsError } = useTeamsByClub(clubId ?? 0);

  return (
    <div className="space-y-6">
      <header>
        <Link
          to="/admin"
          className="text-xs text-muted-foreground hover:text-foreground transition-colors"
        >
          ← Admin
        </Link>
        <h1 className="text-2xl font-bold tracking-tight">Jogadores</h1>
        <p className="text-sm text-muted-foreground">
          Escolhe um clube e uma equipa para gerir o respetivo plantel.
        </p>
      </header>

      <div className="grid gap-4 sm:grid-cols-2 max-w-2xl">
        <div className="space-y-1.5">
          <Label htmlFor="club">Clube</Label>
          <select
            id="club"
            className={selectClass}
            value={clubId ?? ""}
            disabled={clubsLoading}
            onChange={(e) => {
              const value = e.target.value;
              setClubId(value ? Number(value) : null);
              setTeamId(null);
            }}
          >
            <option value="">{clubsLoading ? "A carregar…" : "Selecionar clube…"}</option>
            {clubs.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
          {clubsError && (
            <p className="text-sm text-destructive">Falha a carregar clubes: {clubsError}</p>
          )}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="team">Equipa</Label>
          <select
            id="team"
            className={selectClass}
            value={teamId ?? ""}
            disabled={!clubId || teamsLoading}
            onChange={(e) => {
              const value = e.target.value;
              setTeamId(value ? Number(value) : null);
            }}
          >
            <option value="">
              {!clubId
                ? "Escolhe um clube primeiro"
                : teamsLoading
                  ? "A carregar…"
                  : teams.length === 0
                    ? "Sem equipas neste clube"
                    : "Selecionar equipa…"}
            </option>
            {teams.map((t) => (
              <option key={t.id} value={t.id}>
                {teamCategoryLabel(t.category)}
              </option>
            ))}
          </select>
          {teamsError && (
            <p className="text-sm text-destructive">Falha a carregar equipas: {teamsError}</p>
          )}
        </div>
      </div>

      {teamId ? (
        <PlayersSection teamId={teamId} canManage />
      ) : (
        <p className="text-sm text-muted-foreground">
          Seleciona uma equipa para ver e gerir os jogadores.
        </p>
      )}
    </div>
  );
}
