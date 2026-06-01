import type { CrestTone } from "@/features/clubs/clubs";

/**
 * Helpers to render real (API-backed) clubs in <Crest> without depending on
 * the hardcoded mock catalog in `features/clubs/clubs.ts`. The monogram and
 * tone are derived deterministically from the club's name + id.
 */

export interface CrestInfo {
  initials: string;
  short: string;
  tone: CrestTone;
}

const TONES: CrestTone[] = ["forest", "oxblood", "navy", "ochre", "slate", "cream"];

/**
 * Generic football-club tokens that carry no identifying value. Stripped when
 * deriving initials and the short display name.
 */
const GENERIC = new Set([
  "futebol",
  "clube",
  "sport",
  "associação",
  "associacao",
  "desportiva",
  "desportivo",
  "de",
  "da",
  "do",
  "e",
  "fc",
  "sc",
  "ad",
  "cd",
  "gd",
  "ac",
  "união",
  "uniao",
]);

function significantWords(name: string): string[] {
  return name
    .trim()
    .split(/\s+/)
    .filter((w) => w.length > 0 && !GENERIC.has(w.toLowerCase()));
}

export function crestForClub(club: { id: number; name: string } | undefined): CrestInfo {
  if (!club) return { initials: "?", short: "—", tone: "slate" };

  const words = significantWords(club.name);

  const initials =
    words.length > 0
      ? words
          .slice(0, 2)
          .map((w) => w[0])
          .join("")
          .toUpperCase()
          .slice(0, 3)
      : club.name.slice(0, 2).toUpperCase();

  const short = words.length > 0 ? words.join(" ") : club.name;

  const tone = TONES[club.id % TONES.length];

  return { initials, short, tone };
}
