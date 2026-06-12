/** Shared formatting + chart palette for the stats dashboard. */

const eur = new Intl.NumberFormat("pt-PT", {
  style: "currency",
  currency: "EUR",
});

/** Format a decimal-string revenue (e.g. "12345.50") as `12 345,50 €`. */
export function formatRevenue(revenue: string | number): string {
  const n = typeof revenue === "number" ? revenue : Number(revenue);
  return eur.format(Number.isFinite(n) ? n : 0);
}

/** Format a 0..1 decimal-string rate as a whole-number percentage. */
export function formatRate(rate: string | number): string {
  const n = typeof rate === "number" ? rate : Number(rate);
  return `${Math.round((Number.isFinite(n) ? n : 0) * 100)}%`;
}

/** Chart colours — bold sporty palette, aligned with the app's primary green. */
export const CHART = {
  primary: "oklch(0.62 0.19 152)",
  primarySoft: "oklch(0.62 0.19 152 / 0.25)",
  accent: "oklch(0.65 0.17 250)",
  muted: "oklch(0.7 0.02 250)",
  grid: "oklch(0.6 0.02 250 / 0.25)",
};
