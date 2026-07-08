import type { Occurrence } from "../api/sheetApi";

export interface GoalTally {
  home: number;
  away: number;
}

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
      if (o.teamId === homeTeamId) away++;
      else if (o.teamId === awayTeamId) home++;
    }
  }
  return { home, away };
}

export function scoreMismatch(
  homeScore: number | null | undefined,
  awayScore: number | null | undefined,
  tally: GoalTally,
): boolean {
  if (homeScore == null || awayScore == null) return false;
  return tally.home !== homeScore || tally.away !== awayScore;
}
