import { useEffect, useState } from "react";
import { useEventsCatalog } from "@/features/events/hooks/useEventsCatalog";

export interface NextMatch {
  eventId: number;
  homeShort: string;
  awayShort: string;
  /** Absolute kickoff time. */
  kickoffAt: Date;
}

/** Only surface a match if it kicks off within this window. */
const WINDOW_MS = 24 * 60 * 60 * 1000;

/**
 * Returns the next PUBLISHED + SCHEDULED match kicking off within the next 24h,
 * or `null` if there is none. Derived from the real events catalog
 * (`useEventsCatalog`), which joins events -> match -> teams/clubs and already
 * resolves short club names — so this stays a thin selector over real data.
 *
 * Used by <MatchdayBanner>. Re-evaluates every 60s so the banner appears /
 * disappears and the countdown stays fresh without a page reload.
 */
export function useNextMatch(): NextMatch | null {
  const { entries } = useEventsCatalog();

  // Tick every 60s so the window re-evaluates and the countdown refreshes.
  const [, setTick] = useState(0);
  useEffect(() => {
    const id = window.setInterval(() => setTick((n) => n + 1), 60_000);
    return () => window.clearInterval(id);
  }, []);

  const now = Date.now();
  const next = entries
    .filter((e) => e.matchStatus === "SCHEDULED" && e.kickoffAt != null)
    .map((e) => ({ entry: e, ts: new Date(e.kickoffAt as string).getTime() }))
    .filter(({ ts }) => Number.isFinite(ts) && ts > now && ts - now <= WINDOW_MS)
    .sort((a, b) => a.ts - b.ts)[0];

  if (!next) return null;
  return {
    eventId: next.entry.eventId,
    homeShort: next.entry.homeShort,
    awayShort: next.entry.awayShort,
    kickoffAt: new Date(next.entry.kickoffAt as string),
  };
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
