import type { CrestTone } from "@/features/clubs/clubs";
import type { Club } from "@/features/clubs/api/clubsApi";
import type { Venue } from "@/features/venues/api/venuesApi";
import type { Team } from "@/features/teams/api/teamsApi";
import type { Match, MatchStatus } from "@/features/matches/api/matchesApi";
import type { Event } from "../api/eventsApi";

export interface CatalogEntry {
  eventId: number;
  matchId: number | null;
  eventStatus: Event["status"];

  homeClubId: number | null;
  homeClubName: string;
  homeShort: string;
  homeInitials: string;
  homeTone: CrestTone;
  homeCategory: string | null;

  awayClubId: number | null;
  awayClubName: string;
  awayShort: string;
  awayInitials: string;
  awayTone: CrestTone;
  awayCategory: string | null;

  venueId: number | null;
  venueName: string | null;
  venueCapacity: number | null;
  kickoffAt: string | null;
  matchStatus: MatchStatus | null;
  homeScore: number | null;
  awayScore: number | null;

  priceNormal: number;
  priceSupporter: number;
  priceFrom: number;
}

const TONES: CrestTone[] = ["forest", "oxblood", "navy", "ochre", "slate", "cream"];

export function toneFromId(id: number | null): CrestTone {
  if (id == null) return "slate";
  return TONES[Math.abs(id) % TONES.length];
}

export function initialsFromName(name: string): string {
  const cleaned = name.normalize("NFD").replace(/[̀-ͯ]/g, "").toUpperCase();
  const words = cleaned
    .split(/[\s.·-]+/)
    .filter((w) => w.length > 0)
    .filter((w) => !["DE", "DA", "DO", "DOS", "DAS", "E", "DEL"].includes(w));
  if (words.length === 0) return "?";

  if (words[0].length <= 3 && words.length > 1) {
    return (words[0][0] + (words[1][0] ?? "")).slice(0, 2);
  }
  if (words.length === 1) return words[0].slice(0, 2);
  return (words[0][0] + words[1][0]).slice(0, 2);
}

export function shortFromName(name: string): string {
  const ACRONYM = /^(CD|SU|GD|AD|SC|FC|AC|UD|SU|AF)\.?$/i;
  const parts = name.split(/\s+/);
  const stripped = parts.filter((p, i) => !(i === 0 && ACRONYM.test(p)));
  return stripped.join(" ") || name;
}

export function buildEntry(input: {
  event: Event;
  match: Match | null;
  homeTeam: Team | null;
  awayTeam: Team | null;
  homeClub: Club | null;
  awayClub: Club | null;
  venue: Venue | null;
  teamCategoryLabel: (c: Team["category"]) => string;
}): CatalogEntry {
  const homeClubName = input.homeClub?.name ?? "Casa";
  const awayClubName = input.awayClub?.name ?? "Visitante";
  const priceNormal = Number(input.event.priceNormal);
  const priceSupporter = Number(input.event.priceSupporter);

  return {
    eventId: input.event.id,
    matchId: input.event.matchId ?? null,
    eventStatus: input.event.status,

    homeClubId: input.homeClub?.id ?? null,
    homeClubName,
    homeShort: shortFromName(homeClubName),
    homeInitials: initialsFromName(homeClubName),
    homeTone: toneFromId(input.homeClub?.id ?? null),
    homeCategory: input.homeTeam ? input.teamCategoryLabel(input.homeTeam.category) : null,

    awayClubId: input.awayClub?.id ?? null,
    awayClubName,
    awayShort: shortFromName(awayClubName),
    awayInitials: initialsFromName(awayClubName),
    awayTone: toneFromId(input.awayClub?.id ?? null),
    awayCategory: input.awayTeam ? input.teamCategoryLabel(input.awayTeam.category) : null,

    venueId: input.venue?.id ?? null,
    venueName: input.venue?.name ?? null,
    venueCapacity: input.venue?.capacity ?? null,
    kickoffAt: input.match?.kickoffAt ?? null,
    matchStatus: input.match?.status ?? null,
    homeScore: input.match?.homeScore ?? null,
    awayScore: input.match?.awayScore ?? null,

    priceNormal,
    priceSupporter,
    priceFrom: Math.min(priceNormal, priceSupporter),
  };
}
