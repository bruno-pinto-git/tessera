import { useCallback, useEffect, useState } from "react";
import { listPlayersByTeam, type PageEnvelope, type Player } from "../api/playersApi";

export function usePlayersByTeam(teamId: number, size = 50) {
  const [data, setData] = useState<PageEnvelope<Player> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const refetch = useCallback(() => setReloadKey((k) => k + 1), []);

  useEffect(() => {
    if (!teamId || Number.isNaN(teamId)) return;
    let cancelled = false;
    const run = async () => {
      try {
        const res = await listPlayersByTeam(teamId, { size });
        if (!cancelled) {
          setData(res);
          setError(null);
        }
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Erro inesperado");
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
  }, [teamId, size, reloadKey]);

  return { players: data?.content ?? [], loading, error, refetch };
}