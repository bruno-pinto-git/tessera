import { useEffect, useState } from "react";
import { listEvents, type Event } from "@/features/events/api/eventsApi";

/**
 * Which matches already have a box office (an event). The club's matches come
 * from the match-service and carry no box-office info, so we cross-reference
 * the ticket-service events by matchId. Best-effort: a failure just leaves the
 * map empty, and the "open box office" action falls back to the server's 409.
 */
export function useBoxOffices() {
  const [byMatch, setByMatch] = useState<Map<number, Event>>(new Map());
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    listEvents({ size: 200 })
      .then((page) => {
        if (cancelled) return;
        const m = new Map<number, Event>();
        for (const e of page.content) {
          if (e.matchId != null) m.set(e.matchId, e);
        }
        setByMatch(m);
      })
      .catch(() => {
        /* non-fatal — the 409 guard on creation still protects duplicates */
      });
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  return { byMatch, refetch: () => setReloadKey((k) => k + 1) };
}
