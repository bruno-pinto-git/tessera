import { useEffect, useState } from "react";
import { listClubs, type Club } from "@/features/clubs/api/clubsApi";
import { listVenues, type Venue } from "@/features/venues/api/venuesApi";
import { getTeam, type Team } from "@/features/teams/api/teamsApi";

/**
 * Pre-loads all clubs + venues (cheap — small sets in our domain) and
 * exposes a memoized team lookup that resolves team ids on demand and
 * caches the results.
 *
 * Used by the matches admin list and form: the list needs to render team
 * names (which means resolving each match's home/away team ids); the form
 * needs the cascading club -> team picker.
 */
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
        // Single pass — load everything once. We assume the catalogues are
        // small enough for a school project; the API will reject pagination
        // requests above its max so we cap at 100.
        const [c, v] = await Promise.all([
          listClubs({ size: 100 }),
          listVenues({ size: 100 }),
        ]);
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

  /**
   * Lazy load + memoize team lookup. Returns the team if cached,
   * otherwise fires a fetch and updates the cache when it arrives.
   * Callers re-render on cache update (via setTeamCache).
   */
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