import { useEffect, useState } from "react";
import { searchUsers, type UserSummary } from "@/api/usersApi";

interface UseUsersParams {
  search: string;
  max?: number;
}

interface UseUsersResult {
  users: UserSummary[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useUsers({ search, max = 50 }: UseUsersParams): UseUsersResult {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    const run = async () => {
      try {
        const res = await searchUsers({ search: search || undefined, max });
        if (cancelled) return;
        setUsers(res);
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

    const handle = window.setTimeout(() => {
      if (!cancelled) void run();
    }, 250);

    return () => {
      cancelled = true;
      window.clearTimeout(handle);
    };
  }, [search, max, reloadKey]);

  return {
    users,
    loading,
    error,
    refetch: () => setReloadKey((k) => k + 1),
  };
}
