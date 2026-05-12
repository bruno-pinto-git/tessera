import { useEffect, useState } from "react";

export interface NextMatch {
  eventId: number;
  homeShort: string;
  awayShort: string;
  /** Absolute kickoff time. */
  kickoffAt: Date;
}

/**
 * Returns the next match within the configured window (24h by default),
 * or `null` if there is no imminent match. Used by <MatchdayBanner>.
 *
 * TODO: replace the hard-coded mock with a real query against
 * match-service (`GET /api/v1/matches?status=SCHEDULED&from=...&to=...`)
 * once the catalog is wired. Polling every 60s is enough.
 */
export function useNextMatch(): NextMatch | null {
  // For now we surface a single mock match a few hours from now so the
  // banner shows up during development. Set to `null` if you want to see
  // the layout without the banner.
  const [match] = useState<NextMatch | null>(() => {
    const kickoffAt = new Date(Date.now() + 3 * 60 * 60 * 1000 + 22 * 60 * 1000);
    return {
      eventId: 1,
      homeShort: "Aljustrel",
      awayShort: "Praiense",
      kickoffAt,
    };
  });

  // Re-render every 60s so the countdown stays fresh.
  const [, setTick] = useState(0);
  useEffect(() => {
    const id = window.setInterval(() => setTick((n) => n + 1), 60_000);
    return () => window.clearInterval(id);
  }, []);

  return match;
}

/** Human-readable "Xh YYmin" formatter for the banner. */
export function formatCountdown(target: Date, now: Date = new Date()): string {
  const diffMs = target.getTime() - now.getTime();
  if (diffMs <= 0) return "agora";
  const totalMin = Math.floor(diffMs / 60_000);
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  if (h === 0) return `${m}min`;
  return `${h}h ${m.toString().padStart(2, "0")}min`;
}
