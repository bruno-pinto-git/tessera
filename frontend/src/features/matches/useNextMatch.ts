import { useEffect, useState } from "react";
import { useEventsCatalog } from "@/features/events/hooks/useEventsCatalog";

export interface NextMatch {
  eventId: number;
  homeShort: string;
  awayShort: string;
  kickoffAt: Date;
}

const WINDOW_MS = 24 * 60 * 60 * 1000;

export function useNextMatch(): NextMatch | null {
  const { entries } = useEventsCatalog();

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

export function dayLabel(target: Date, now: Date = new Date()): string {
  const sameDay =
    target.getFullYear() === now.getFullYear() &&
    target.getMonth() === now.getMonth() &&
    target.getDate() === now.getDate();
  return sameDay ? "Hoje" : "Amanhã";
}

export function formatCountdown(target: Date, now: Date = new Date()): string {
  const diffMs = target.getTime() - now.getTime();
  if (diffMs <= 0) return "agora";
  const totalMin = Math.floor(diffMs / 60_000);
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  if (h === 0) return `${m}min`;
  return `${h}h ${m.toString().padStart(2, "0")}min`;
}
