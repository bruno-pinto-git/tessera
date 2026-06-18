import type { Occurrence } from "../api/sheetApi";

export interface GoalTally {
  home: number;
  away: number;
}

/**
 * Goals derived from the sheet's occurrences. A GOAL counts for the scorer's
 * team; an OWN_GOAL counts for the opponent.
 */
export function tallyGoals(
  occurrences: Occurrence[],
  homeTeamId: number,
  awayTeamId: number,
): GoalTally {
  let home = 0;
  let away = 0;
  for (const o of occurrences) {
    if (o.type === "GOAL") {
      if (o.teamId === homeTeamId) home++;
      else if (o.teamId === awayTeamId) away++;
    } else if (o.type === "OWN_GOAL") {
      // Own goal counts for the opposing team.
      if (o.teamId === homeTeamId) away++;
      else if (o.teamId === awayTeamId) home++;
    }
  }
  return { home, away };
}

/**
 * True when an official scoreline is set and the goals recorded on the sheet
 * don't add up to it. Returns false when there's no official score yet (so we
 * don't nag about a sheet for a match that hasn't finished).
 */
export function scoreMismatch(
  homeScore: number | null | undefined,
  awayScore: number | null | undefined,
  tally: GoalTally,
): boolean {
  if (homeScore == null || awayScore == null) return false;
  return tally.home !== homeScore || tally.away !== awayScore;
}
