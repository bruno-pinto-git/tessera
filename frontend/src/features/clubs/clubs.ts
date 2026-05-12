/**
 * Mock club catalog used while the match-service `GET /api/v1/clubs`
 * endpoint isn't wired yet. Each entry describes how to render the club's
 * monogram in <Crest> without depending on real logo assets.
 *
 * When the API integration lands:
 *   1. Replace the static `CLUBS` map with the result of a query against
 *      match-service.
 *   2. Add a real `logoUrl` field. <Crest> already falls back to the
 *      initials when the URL is missing, so the migration is incremental.
 *   3. Drop the `tone` field — the avatar fallback will pick the colour
 *      from `--primary` and the design will lean on actual crests.
 */

export type CrestTone = "forest" | "oxblood" | "navy" | "ochre" | "slate" | "cream";

export interface Club {
  id: string;
  name: string;
  short: string;
  initials: string;
  tone: CrestTone;
}

export const CLUBS: Record<string, Club> = {
  ALJ: { id: "ALJ", name: "CD Aljustrel", short: "Aljustrel", initials: "AL", tone: "forest" },
  PRA: { id: "PRA", name: "SC Praiense", short: "Praiense", initials: "PR", tone: "navy" },
  LOU: { id: "LOU", name: "GD Lousada", short: "Lousada", initials: "LO", tone: "oxblood" },
  TAB: { id: "TAB", name: "AD Tabuadense", short: "Tabuadense", initials: "TB", tone: "ochre" },
  TIR: { id: "TIR", name: "FC Tirsense", short: "Tirsense", initials: "TI", tone: "slate" },
  CAM: { id: "CAM", name: "AD Camacha", short: "Camacha", initials: "CM", tone: "cream" },
  MAR: { id: "MAR", name: "SC Marinhense", short: "Marinhense", initials: "MA", tone: "forest" },
  UNC: { id: "UNC", name: "AC União Coimbra", short: "Coimbra", initials: "UC", tone: "oxblood" },
  MOR: { id: "MOR", name: "CD Mortágua", short: "Mortágua", initials: "MO", tone: "navy" },
};

export function clubBy(id: string): Club {
  const c = CLUBS[id];
  if (!c) throw new Error(`Unknown club id: ${id}`);
  return c;
}
