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

/**
 * Crest derived from a club NAME only (no id available) — used to render a
 * cancelled-match ticket from its `matchLabel` snapshot. Initials follow the
 * same rules as {@link crestForClub}; the tone is hashed from the name so it
 * is stable and varied without an id.
 */
export function crestFromName(name: string): CrestInfo {
  const clean = name.trim();
  if (!clean) return { initials: "?", short: "—", tone: "slate" };
  const words = significantWords(clean);
  const initials =
    words.length > 0
      ? words.slice(0, 2).map((w) => w[0]).join("").toUpperCase().slice(0, 3)
      : clean.slice(0, 2).toUpperCase();
  let hash = 0;
  for (let i = 0; i < clean.length; i++) hash = (hash * 31 + clean.charCodeAt(i)) >>> 0;
  return { initials, short: words.length > 0 ? words.join(" ") : clean, tone: TONES[hash % TONES.length] };
}

/**
 * Split a "Home vs Away" fixture label into the two club names, dropping any
 * trailing "(category)" suffix. Returns null when there's no " vs " separator.
 */
export function splitFixture(label: string): { home: string; away: string } | null {
  const idx = label.indexOf(" vs ");
  if (idx < 0) return null;
  const strip = (s: string) => s.replace(/\s*\([^)]*\)\s*$/, "").trim();
  return { home: strip(label.slice(0, idx)), away: strip(label.slice(idx + 4)) };
}
