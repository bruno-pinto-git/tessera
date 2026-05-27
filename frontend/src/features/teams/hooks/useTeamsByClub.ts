import { useCallback, useEffect, useState } from "react";
import { listTeamsByClub, type Team } from "../api/teamsApi";

export function useTeamsByClub(clubId: number) {
  const [teams, setTeams] = useState<Team[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const refetch = useCallback(() => setReloadKey((k) => k + 1), []);

  useEffect(() => {
    if (!clubId || Number.isNaN(clubId)) return;
    let cancelled = false;
    const run = async () => {
      try {
        const data = await listTeamsByClub(clubId);
        if (!cancelled) {
          setTeams(data);
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

  return { teams, loading, error, refetch };
}