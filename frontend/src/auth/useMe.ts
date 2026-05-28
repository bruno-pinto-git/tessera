import { useEffect, useState } from "react";
import { useAuth } from "@/auth/useAuth";
import { getMe, type Me } from "@/api/meApi";

/**
 * Loads the authenticated user's profile (`GET /me`) once the auth context
 * has a valid session. Re-fetches if the user logs in/out during the same
 * SPA session.
 */
export function useMe() {
  const { authenticated, token } = useAuth();
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authenticated) {
      setMe(null);
      return;
    }

    let cancelled = false;
    const run = async () => {
      try {
        const data = await getMe();
        if (!cancelled) {
          setMe(data);
          setError(null);
        }
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Erro inesperado");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    // Defer the loading flag so we don't write state synchronously inside
    // the effect (react-hooks/set-state-in-effect).
    void Promise.resolve().then(() => {
      if (!cancelled) setLoading(true);
    });
    void run();

    return () => {
      cancelled = true;
    };
  }, [authenticated, token]);

  return { me, loading, error };
}
