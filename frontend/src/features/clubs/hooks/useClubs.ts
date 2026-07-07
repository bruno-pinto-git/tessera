import { useCallback, useEffect, useState } from "react";
import { listClubs, type Club, type PageEnvelope } from "../api/clubsApi";

interface UseClubsParams {
  page: number;
  size: number;
  name: string;
}

interface UseClubsResult {
  data: PageEnvelope<Club> | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useClubs({ page, size, name }: UseClubsParams): UseClubsResult {
  const [data, setData] = useState<PageEnvelope<Club> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const refetch = useCallback(() => setReloadKey((k) => k + 1), []);

  useEffect(() => {
    let cancelled = false;

    const run = async () => {
      try {
        const res = await listClubs({ page, size, name: name || undefined });
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
  }, [page, size, name, reloadKey]);

  return { data, loading, error, refetch };
}
