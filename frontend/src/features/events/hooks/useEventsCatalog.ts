import { useEffect, useState } from "react";
import { listEvents, getEvent, type Event } from "../api/eventsApi";
import { getMatch, type Match } from "@/features/matches/api/matchesApi";
import { listClubs, type Club } from "@/features/clubs/api/clubsApi";
import { listVenues, type Venue } from "@/features/venues/api/venuesApi";
import { getTeam, teamCategoryLabel, type Team } from "@/features/teams/api/teamsApi";
import { buildEntry, type CatalogEntry } from "../lib/catalog";

interface UseCatalogResult {
  entries: CatalogEntry[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

/**
 * Loads PUBLISHED events from ticket-service, then hydrates each with its
 * match (kickoff, teams, venue) and the club / venue / team caches it
 * needs to render. Returns flat `CatalogEntry`s consumed directly by the
 * EventsPage card grid.
 *
 * Filtering is client-side because the backend doesn't (yet) accept a
 * `status` query param on `/events`. Cheap for academic-scale data.
 */
export function useEventsCatalog(): UseCatalogResult {
  const [entries, setEntries] = useState<CatalogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    const run = async () => {
      try {
        // 1. Bootstrap caches + event list in parallel.
        const [eventsPage, clubsPage, venuesPage] = await Promise.all([
          listEvents({ size: 100 }),
          listClubs({ size: 200 }),
          listVenues({ size: 100 }),
        ]);
        if (cancelled) return;

        const clubMap = new Map<number, Club>(clubsPage.content.map((c) => [c.id, c]));
        const venueMap = new Map<number, Venue>(venuesPage.content.map((v) => [v.id, v]));

        // 2. Keep only events with a linked match and status PUBLISHED.
        const published = eventsPage.content.filter(
          (e) => e.status === "PUBLISHED" && e.matchId != null,
        );

        // 3. Fetch all matches in parallel.
        const matchResults = await Promise.allSettled(
          published.map((e) => getMatch(e.matchId as number)),
        );
        if (cancelled) return;

        // 4. Collect unique team ids referenced by the matches, fetch in parallel.
        const teamIds = new Set<number>();
        matchResults.forEach((r) => {
          if (r.status === "fulfilled") {
            teamIds.add(r.value.homeTeamId);
            teamIds.add(r.value.awayTeamId);
          }
        });
        const teamPairs = await Promise.allSettled(
          Array.from(teamIds).map(async (id) => [id, await getTeam(id)] as const),
        );
        if (cancelled) return;
        const teamMap = new Map<number, Team>(
          teamPairs.flatMap((r) => (r.status === "fulfilled" ? [r.value] : [])),
        );

        // 5. Stitch.
        const built: CatalogEntry[] = published.map((event, i) => {
          const matchRes = matchResults[i];
          const match: Match | null = matchRes.status === "fulfilled" ? matchRes.value : null;
          const homeTeam = match ? (teamMap.get(match.homeTeamId) ?? null) : null;
          const awayTeam = match ? (teamMap.get(match.awayTeamId) ?? null) : null;
          const homeClub = homeTeam ? (clubMap.get(homeTeam.clubId) ?? null) : null;
          const awayClub = awayTeam ? (clubMap.get(awayTeam.clubId) ?? null) : null;
          const venue =
            match && match.venueId != null ? (venueMap.get(match.venueId) ?? null) : null;

          return buildEntry({
            event,
            match,
            homeTeam,
            awayTeam,
            homeClub,
            awayClub,
            venue,
            teamCategoryLabel,
          });
        });

        // 6. Sort by kickoff ascending (upcoming first), with no-kickoff at the end.
        built.sort((a, b) => {
          if (a.kickoffAt == null) return 1;
          if (b.kickoffAt == null) return -1;
          return a.kickoffAt.localeCompare(b.kickoffAt);
        });

        if (!cancelled) {
          setEntries(built);
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
  }, [reloadKey]);

  return {
    entries,
    loading,
    error,
    refetch: () => setReloadKey((k) => k + 1),
  };
}

// -----------------------------------------------------------------------------

/**
 * Single-event variant for `EventDetailPage`. Same join, but scoped to one
 * event id. Returns `null` while loading or if the event doesn't exist
 * (the caller renders 404).
 */
export function useEventCatalog(eventId: number) {
  const [entry, setEntry] = useState<CatalogEntry | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!eventId || Number.isNaN(eventId)) return;
    let cancelled = false;

    const run = async () => {
      try {
        setNotFound(false);
        const event: Event = await getEvent(eventId);
        if (cancelled) return;

        const match: Match | null =
          event.matchId != null ? await getMatch(event.matchId).catch(() => null) : null;

        const [homeTeam, awayTeam] = match
          ? await Promise.all([
              getTeam(match.homeTeamId).catch(() => null),
              getTeam(match.awayTeamId).catch(() => null),
            ])
          : [null, null];

        const [homeClub, awayClub, venue] = await Promise.all([
          homeTeam
            ? listClubs({ size: 200 }).then(
                (p) => p.content.find((c) => c.id === homeTeam.clubId) ?? null,
              )
            : null,
          awayTeam
            ? listClubs({ size: 200 }).then(
                (p) => p.content.find((c) => c.id === awayTeam.clubId) ?? null,
              )
            : null,
          match && match.venueId != null
            ? listVenues({ size: 100 }).then(
                (p) => p.content.find((v) => v.id === match.venueId) ?? null,
              )
            : null,
        ]);

        if (cancelled) return;

        setEntry(
          buildEntry({
            event,
            match,
            homeTeam,
            awayTeam,
            homeClub,
            awayClub,
            venue,
            teamCategoryLabel,
          }),
        );
        setError(null);
      } catch (err) {
        if (cancelled) return;
        // 404 from the event lookup means the id doesn't exist.
        const status = (err as { status?: number })?.status;
        if (status === 404) {
          setNotFound(true);
        } else {
          setError(err instanceof Error ? err.message : "Erro inesperado");
        }
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
  }, [eventId]);

  return { entry, loading, error, notFound };
}
