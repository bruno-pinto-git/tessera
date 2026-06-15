import type { LineupRole, OccurrenceType } from "../api/sheetApi";

export function lineupRoleLabel(r: LineupRole): string {
  return r === "STARTER" ? "Titular" : "Suplente";
}

export function occurrenceTypeLabel(t: OccurrenceType): string {
  switch (t) {
    case "GOAL":
      return "Golo";
    case "OWN_GOAL":
      return "Autogolo";
    case "YELLOW_CARD":
      return "Cartão amarelo";
    case "RED_CARD":
      return "Cartão vermelho";
    case "SUBSTITUTION":
      return "Substituição";
    case "FOUL":
      return "Falta";
  }
}

/** Emoji used in the occurrence timeline (kept simple, no icon deps). */
export function occurrenceIcon(t: OccurrenceType): string {
  switch (t) {
    case "GOAL":
      return "⚽";
    case "OWN_GOAL":
      return "🥅";
    case "YELLOW_CARD":
      return "🟨";
    case "RED_CARD":
      return "🟥";
    case "SUBSTITUTION":
      return "🔁";
    case "FOUL":
      return "⚠️";
  }
}

/** Occurrence types that count toward the scoreline. */
export const SCORING_TYPES: OccurrenceType[] = ["GOAL", "OWN_GOAL"];

export const OCCURRENCE_TYPES: OccurrenceType[] = [
  "GOAL",
  "OWN_GOAL",
  "YELLOW_CARD",
  "RED_CARD",
  "SUBSTITUTION",
  "FOUL",
];
