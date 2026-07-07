import type { CrestTone } from "@/features/clubs/clubs";

export interface CrestInfo {
  initials: string;
  short: string;
  tone: CrestTone;
}

const TONES: CrestTone[] = ["forest", "oxblood", "navy", "ochre", "slate", "cream"];

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

export function splitFixture(label: string): { home: string; away: string } | null {
  const idx = label.indexOf(" vs ");
  if (idx < 0) return null;
  const strip = (s: string) => s.replace(/\s*\([^)]*\)\s*$/, "").trim();
  return { home: strip(label.slice(0, idx)), away: strip(label.slice(idx + 4)) };
}
