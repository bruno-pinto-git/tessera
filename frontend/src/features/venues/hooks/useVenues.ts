import { useCallback, useEffect, useState } from "react";
import { listVenues, type PageEnvelope, type Venue } from "../api/venuesApi";

interface UseVenuesParams {
  page: number;
  size: number;
  name: string;
}

interface UseVenuesResult {
  data: PageEnvelope<Venue> | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useVenues({ page, size, name }: UseVenuesParams): UseVenuesResult {
  const [data, setData] = useState<PageEnvelope<Venue> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const refetch = useCallback(() => setReloadKey((k) => k + 1), []);

  useEffect(() => {
    let cancelled = false;

    const run = async () => {
      try {
        const res = await listVenues({ page, size, name: name || undefined });
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
