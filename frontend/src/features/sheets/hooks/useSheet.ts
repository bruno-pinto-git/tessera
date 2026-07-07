import { useCallback, useEffect, useState } from "react";
import { ApiError } from "@/api/client";
import { listPlayersByTeam, type Player } from "@/features/players/api/playersApi";
import {
  getMatchSheet,
  getMatchSheetHistory,
  type MatchSheet,
  type MatchSheetHistory,
} from "../api/sheetApi";

function errMsg(e: unknown): string {
  return e instanceof Error ? e.message : "Erro inesperado";
}

export function useTeamRosters(homeTeamId?: number | null, awayTeamId?: number | null) {
  const [players, setPlayers] = useState<Map<number, Player>>(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const ids = [homeTeamId, awayTeamId].filter((x): x is number => x != null);
  const key = ids.join(",");

  useEffect(() => {
    if (ids.length === 0) return;
    let cancelled = false;
    void Promise.resolve().then(() => !cancelled && setLoading(true));
    Promise.all(ids.map((id) => listPlayersByTeam(id, { size: 100 })))
      .then((pages) => {
        if (cancelled) return;
        const map = new Map<number, Player>();
        for (const page of pages) for (const p of page.content) map.set(p.id, p);
        setPlayers(map);
        setError(null);
      })
      .catch((e) => !cancelled && setError(errMsg(e)))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return { players, loading, error };
}

export function useMatchSheet(matchId: number) {
  const [sheet, setSheet] = useState<MatchSheet | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [forbidden, setForbidden] = useState(false);
  const [notFound, setNotFound] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  const refetch = useCallback(() => setReloadKey((k) => k + 1), []);

  useEffect(() => {
    if (!matchId || Number.isNaN(matchId)) return;
    let cancelled = false;
    void Promise.resolve().then(() => !cancelled && setLoading(true));
    getMatchSheet(matchId)
      .then((s) => {
        if (cancelled) return;
        setSheet(s);
        setError(null);
        setForbidden(false);
        setNotFound(false);
      })
      .catch((e) => {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 403) setForbidden(true);
        else if (e instanceof ApiError && e.status === 404) setNotFound(true);
        else setError(errMsg(e));
      })
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [matchId, reloadKey]);

  return { sheet, loading, error, forbidden, notFound, refetch };
}

export function useMatchSheetHistory(matchId: number | null) {
  const [history, setHistory] = useState<MatchSheetHistory | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notAvailable, setNotAvailable] = useState(false);

  useEffect(() => {
    if (matchId == null || Number.isNaN(matchId)) {
      setLoading(false);
      setNotAvailable(true);
      return;
    }
    let cancelled = false;
    void Promise.resolve().then(() => !cancelled && setLoading(true));
    getMatchSheetHistory(matchId)
      .then((h) => {
        if (cancelled) return;
        setHistory(h);
        setNotAvailable(false);
        setError(null);
      })
      .catch((e) => {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 404) {
          setNotAvailable(true);
          setError(null);
        } else {
          setError(errMsg(e));
        }
      })
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [matchId]);

  return { history, loading, error, notAvailable };
}
