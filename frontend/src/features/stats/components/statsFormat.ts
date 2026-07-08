const eur = new Intl.NumberFormat("pt-PT", {
  style: "currency",
  currency: "EUR",
});

export function formatRevenue(revenue: string | number): string {
  const n = typeof revenue === "number" ? revenue : Number(revenue);
  return eur.format(Number.isFinite(n) ? n : 0);
}

export function formatRate(rate: string | number): string {
  const n = typeof rate === "number" ? rate : Number(rate);
  return `${Math.round((Number.isFinite(n) ? n : 0) * 100)}%`;
}

export const CHART = {
  primary: "oklch(0.62 0.19 152)",
  primarySoft: "oklch(0.62 0.19 152 / 0.25)",
  accent: "oklch(0.65 0.17 250)",
  muted: "oklch(0.7 0.02 250)",
  grid: "oklch(0.6 0.02 250 / 0.25)",
};
