import { useEffect, useState } from "react";
import {
  getSalesByClub,
  getSalesRange,
  getSalesSummary,
  listMatchHistory,
  type MatchHistoryParams,
  type MatchSummary,
  type SalesByClub,
  type SalesSummary,
} from "../api/statsApi";

interface AsyncState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

const initial = <T,>(): AsyncState<T> => ({ data: null, loading: true, error: null });

function errMsg(e: unknown): string {
  return e instanceof Error ? e.message : "Erro inesperado";
}

export function useSalesSummary(enabled = true) {
  const [state, setState] = useState<AsyncState<SalesSummary>>(initial);

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;
    void Promise.resolve().then(() => !cancelled && setState((s) => ({ ...s, loading: true })));
    getSalesSummary()
      .then((data) => !cancelled && setState({ data, loading: false, error: null }))
      .catch((e) => !cancelled && setState({ data: null, loading: false, error: errMsg(e) }));
    return () => {
      cancelled = true;
    };
  }, [enabled]);

  return state;
}

export function useSalesByClub(clubId: number | null) {
  const [state, setState] = useState<AsyncState<SalesByClub>>(initial);

  useEffect(() => {
    if (clubId == null) return;
    let cancelled = false;
    void Promise.resolve().then(() => !cancelled && setState((s) => ({ ...s, loading: true })));
    getSalesByClub(clubId)
      .then((data) => !cancelled && setState({ data, loading: false, error: null }))
      .catch((e) => !cancelled && setState({ data: null, loading: false, error: errMsg(e) }));
    return () => {
      cancelled = true;
    };
  }, [clubId]);

  return state;
}

export function useMatchHistory(params: MatchHistoryParams, enabled = true) {
  const [state, setState] = useState<AsyncState<MatchSummary[]>>(initial);
  const key = JSON.stringify(params);

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;
    void Promise.resolve().then(() => !cancelled && setState((s) => ({ ...s, loading: true })));
    listMatchHistory(params)
      .then((page) => !cancelled && setState({ data: page.content, loading: false, error: null }))
      .catch((e) => !cancelled && setState({ data: null, loading: false, error: errMsg(e) }));
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, enabled]);

  return state;
}

export interface DailyPoint {
  label: string;
  date: string;
  revenue: number;
  sold: number;
}

export function useDailyRevenue(days = 7, enabled = true) {
  const [state, setState] = useState<AsyncState<DailyPoint[]>>(initial);

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;

    const today = new Date();
    const windows: { start: Date; end: Date }[] = [];
    for (let i = days - 1; i >= 0; i--) {
      const start = new Date(
        Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate() - i),
      );
      const end = new Date(start);
      end.setUTCDate(end.getUTCDate() + 1);
      windows.push({ start, end });
    }

    void Promise.resolve().then(() => !cancelled && setState((s) => ({ ...s, loading: true })));
    Promise.all(
      windows.map(async (w): Promise<DailyPoint> => {
        const s = await getSalesRange(w.start.toISOString(), w.end.toISOString());
        const dd = String(w.start.getUTCDate()).padStart(2, "0");
        const mm = String(w.start.getUTCMonth() + 1).padStart(2, "0");
        return {
          label: `${dd}/${mm}`,
          date: w.start.toISOString().slice(0, 10),
          revenue: Number(s.revenue),
          sold: s.sold,
        };
      }),
    )
      .then((data) => !cancelled && setState({ data, loading: false, error: null }))
      .catch((e) => !cancelled && setState({ data: null, loading: false, error: errMsg(e) }));

    return () => {
      cancelled = true;
    };
  }, [days, enabled]);

  return state;
}
