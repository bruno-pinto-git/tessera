import { useEffect, useState } from "react";
import { listMyTickets, type Ticket, type TicketStatus } from "@/api/ticketApi";
import { getEvent, type Event } from "@/features/events/api/eventsApi";
import { getMatch, type Match } from "@/features/matches/api/matchesApi";
import { getClub, type Club } from "@/features/clubs/api/clubsApi";
import { getVenue, type Venue } from "@/features/venues/api/venuesApi";
import { crestForClub, crestFromName, splitFixture, type CrestInfo } from "../ticketViews";

/**
 * Unified view model for a ticket on "Os meus bilhetes". Enriched with the
 * related event / match / club / venue resolved from the respective services.
 */
export interface TicketView {
  id: string;
  code: string;
  status: TicketStatus;
  eventId: number;
  home: CrestInfo;
  away: CrestInfo;
  /** "Home vs Away", or "Jogo cancelado" when the match no longer exists. */
  title: string;
  /** True when the ticket's match was deleted and can no longer be resolved. */
  matchRemoved: boolean;
  /** kickoffAt formatted pt-PT, e.g. "Sáb, 16/05 · 16:00". */
  day: string;
  venue: string | null;
  /** "Sócio" when the price matches the supporter price, else "Normal". */
  tier: string;
  /** Price formatted as EUR, e.g. "10,00 €". */
  price: string;
  paidAt: string | null;
  validatedAt: string | null;
  /** kickoff epoch ms for sorting; null when unresolved. */
  kickoffMs: number | null;
}

export interface UseMyTicketsResult {
  paid: TicketView[];
  pending: TicketView[];
  past: TicketView[];
  loading: boolean;
  error: string | null;
}

function formatDay(kickoffAt: string | undefined | null): { day: string; ms: number | null } {
  if (!kickoffAt) return { day: "Data por confirmar", ms: null };
  const d = new Date(kickoffAt);
  if (Number.isNaN(d.getTime())) return { day: "Data por confirmar", ms: null };
  const weekday = d.toLocaleDateString("pt-PT", { weekday: "short" }).replace(".", "");
  const date = d.toLocaleDateString("pt-PT", { day: "2-digit", month: "2-digit" });
  const time = d.toLocaleTimeString("pt-PT", { hour: "2-digit", minute: "2-digit" });
  const cap = weekday.charAt(0).toUpperCase() + weekday.slice(1);
  return { day: `${cap}, ${date} · ${time}`, ms: d.getTime() };
}

function formatEur(value: number | string): string {
  const num = typeof value === "number" ? value : Number(value);
  if (Number.isNaN(num)) return "—";
  return num.toLocaleString("pt-PT", { style: "currency", currency: "EUR" });
}

function formatDateTime(iso: string | null): string | null {
  if (!iso) return null;
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return null;
  return d.toLocaleString("pt-PT", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/** Fetch each unique id ONCE, swallowing individual failures. */
async function fetchMap<T>(ids: number[], fetcher: (id: number) => Promise<T>): Promise<Map<number, T>> {
  const map = new Map<number, T>();
  await Promise.all(
    ids.map(async (id) => {
      try {
        map.set(id, await fetcher(id));
      } catch {
        // Missing/forbidden lookups fall back to placeholders downstream.
      }
    }),
  );
  return map;
}

function uniqueDefined(values: Array<number | null | undefined>): number[] {
  return [...new Set(values.filter((v): v is number => v != null))];
}

export function useMyTickets(): UseMyTicketsResult {
  const [paid, setPaid] = useState<TicketView[]>([]);
  const [pending, setPending] = useState<TicketView[]>([]);
  const [past, setPast] = useState<TicketView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function run() {
      setLoading(true);
      setError(null);
      try {
        const page = await listMyTickets({ size: 100 });
        const tickets = page.content;

        // Events by ticket.eventId.
        const events = await fetchMap<Event>(
          uniqueDefined(tickets.map((t) => t.eventId)),
          getEvent,
        );

        // Matches by event.matchId ?? ticket.matchId.
        const matchIdFor = (t: Ticket): number | null =>
          events.get(t.eventId)?.matchId ?? t.matchId ?? null;
        const matches = await fetchMap<Match>(
          uniqueDefined(tickets.map(matchIdFor)),
          getMatch,
        );

        // Clubs by the matches' home/away club ids.
        const clubIds: Array<number | null> = [];
        for (const m of matches.values()) {
          clubIds.push(m.homeClubId, m.awayClubId);
        }
        const clubs = await fetchMap<Club>(uniqueDefined(clubIds), getClub);

        // Venues by venueId.
        const venueIds = uniqueDefined([...matches.values()].map((m) => m.venueId));
        const venues = await fetchMap<Venue>(venueIds, getVenue);

        const views: TicketView[] = tickets.map((t) => {
          const event = events.get(t.eventId);
          const matchId = matchIdFor(t);
          const match = matchId != null ? matches.get(matchId) : undefined;
          const homeClub = match?.homeClubId != null ? clubs.get(match.homeClubId) : undefined;
          const awayClub = match?.awayClubId != null ? clubs.get(match.awayClubId) : undefined;
          const venue = match?.venueId != null ? venues.get(match.venueId) : undefined;
          const { day, ms } = formatDay(match?.kickoffAt);

          // The match id is known but couldn't be resolved -> it was deleted.
          const matchRemoved = matchId != null && match === undefined;
          let home = crestForClub(homeClub);
          let away = crestForClub(awayClub);
          let title: string;
          if (matchRemoved) {
            // Rebuild the teams (names + crests) from the fixture snapshot
            // captured on the event when the box office opened, so a cancelled
            // match still shows proper crests instead of "?".
            const fixture = event?.matchLabel ? splitFixture(event.matchLabel) : null;
            if (fixture) {
              home = crestFromName(fixture.home);
              away = crestFromName(fixture.away);
              title = `${fixture.home} vs ${fixture.away}`;
            } else {
              title = event?.matchLabel?.trim() || "Jogo cancelado";
            }
          } else {
            title = `${home.short} vs ${away.short}`;
          }

          const supporter =
            event?.priceSupporter != null && Number(t.price) === Number(event.priceSupporter);

          return {
            id: String(t.id),
            code: t.code,
            status: t.status,
            eventId: t.eventId,
            home,
            away,
            title,
            matchRemoved,
            day: matchRemoved ? "Jogo removido pela organização" : day,
            venue: venue?.name ?? null,
            tier: supporter ? "Sócio" : "Normal",
            price: formatEur(t.price),
            paidAt: formatDateTime(t.paymentDate),
            validatedAt: formatDateTime(t.validationDate),
            kickoffMs: ms,
          };
        });

        if (cancelled) return;

        const byKickoff = (a: TicketView, b: TicketView, dir: 1 | -1) =>
          dir * ((a.kickoffMs ?? 0) - (b.kickoffMs ?? 0));

        // Upcoming (paid/pending) soonest-first; past most-recent-first.
        setPaid(
          views.filter((v) => v.status === "PAID").sort((a, b) => byKickoff(a, b, 1)),
        );
        setPending(
          views.filter((v) => v.status === "PENDING").sort((a, b) => byKickoff(a, b, 1)),
        );
        setPast(
          views.filter((v) => v.status === "VALIDATED").sort((a, b) => byKickoff(a, b, -1)),
        );
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : "Não foi possível carregar os bilhetes.");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    run();
    return () => {
      cancelled = true;
    };
  }, []);

  return { paid, pending, past, loading, error };
}
