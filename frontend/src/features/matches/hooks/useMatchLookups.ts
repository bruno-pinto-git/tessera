import { useEffect, useState } from "react";
import { listClubs, type Club } from "@/features/clubs/api/clubsApi";
import { listVenues, type Venue } from "@/features/venues/api/venuesApi";
import { getTeam, type Team } from "@/features/teams/api/teamsApi";

export function useMatchLookups() {
  const [clubs, setClubs] = useState<Map<number, Club>>(new Map());
  const [venues, setVenues] = useState<Map<number, Venue>>(new Map());
  const [teamCache, setTeamCache] = useState<Map<number, Team>>(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      try {
        const [c, v] = await Promise.all([listClubs({ size: 100 }), listVenues({ size: 100 })]);
        if (cancelled) return;
        setClubs(new Map(c.content.map((cl) => [cl.id, cl])));
        setVenues(new Map(v.content.map((vn) => [vn.id, vn])));
        setError(null);
      } catch (err) {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "Erro inesperado");
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
  }, []);

  async function resolveTeam(id: number): Promise<Team> {
    const cached = teamCache.get(id);
    if (cached) return cached;
    const team = await getTeam(id);
    setTeamCache((m) => new Map(m).set(id, team));
    return team;
  }

  function getCachedTeam(id: number): Team | undefined {
    return teamCache.get(id);
  }

  return { clubs, venues, teamCache, getCachedTeam, resolveTeam, loading, error };
}
