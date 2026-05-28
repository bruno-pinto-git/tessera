import { useCallback, useEffect, useState } from "react";
import {
  listMatches,
  type ListMatchesParams,
  type Match,
  type PageEnvelope,
} from "../api/matchesApi";

interface UseMatchesResult {
  data: PageEnvelope<Match> | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useMatches(params: ListMatchesParams): UseMatchesResult {
  const [data, setData] = useState<PageEnvelope<Match> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const refetch = useCallback(() => setReloadKey((k) => k + 1), []);

  // Stable key so the effect only refetches when filters actually change.
  const key = JSON.stringify(params);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      try {
        const res = await listMatches(params);
        if (cancelled) return;
        setData(res);
        setError(null);
      } catch (err) {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "Erro inesperado");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    void Promise.resolve().then(() => {
      if (!cancelled) setLoading(true);
    });
    void run();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, reloadKey]);

  return { data, loading, error, refetch };
}
