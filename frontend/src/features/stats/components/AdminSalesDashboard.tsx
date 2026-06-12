import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { BadgeEuro, CheckCircle2, TicketCheck } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useDailyRevenue, useSalesSummary } from "../hooks/useStats";
import { CHART, formatRate, formatRevenue } from "./statsFormat";
import { StatCard } from "./StatCard";

/** Platform-admin view: global sales KPIs + revenue trend + sold/validated. */
export function AdminSalesDashboard() {
  const summary = useSalesSummary();
  const daily = useDailyRevenue(7);

  if (summary.error) {
    return <p className="text-sm text-destructive">Não foi possível carregar as estatísticas.</p>;
  }

  const s = summary.data;
  const soldVsValidated = s
    ? [
        { name: "Vendidos", value: s.sold, fill: CHART.primary },
        { name: "Validados", value: s.validated, fill: CHART.accent },
      ]
    : [];

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard
          label="Receita total"
          value={s ? formatRevenue(s.revenue) : "—"}
          icon={<BadgeEuro className="size-4 text-primary" />}
        />
        <StatCard
          label="Bilhetes vendidos"
          value={s ? s.sold.toLocaleString("pt-PT") : "—"}
          hint={s ? `${s.validated.toLocaleString("pt-PT")} validados` : undefined}
          icon={<TicketCheck className="size-4 text-primary" />}
        />
        <StatCard
          label="Taxa de validação"
          value={s ? formatRate(s.validationRate) : "—"}
          icon={<CheckCircle2 className="size-4 text-primary" />}
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-base">Receita — últimos 7 dias</CardTitle>
          </CardHeader>
          <CardContent>
            {daily.data && daily.data.some((d) => d.revenue > 0) ? (
              <ResponsiveContainer width="100%" height={240}>
                <AreaChart data={daily.data} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                  <defs>
                    <linearGradient id="rev" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor={CHART.primary} stopOpacity={0.5} />
                      <stop offset="100%" stopColor={CHART.primary} stopOpacity={0.02} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} vertical={false} />
                  <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} />
                  <YAxis tickLine={false} axisLine={false} fontSize={12} width={48} />
                  <Tooltip
                    formatter={(v) => [formatRevenue(Number(v)), "Receita"]}
                    contentStyle={tooltipStyle}
                  />
                  <Area
                    type="monotone"
                    dataKey="revenue"
                    stroke={CHART.primary}
                    strokeWidth={2}
                    fill="url(#rev)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <EmptyChart loading={daily.loading} />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Vendidos vs. validados</CardTitle>
          </CardHeader>
          <CardContent>
            {s && s.sold > 0 ? (
              <ResponsiveContainer width="100%" height={240}>
                <BarChart data={soldVsValidated} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} vertical={false} />
                  <XAxis dataKey="name" tickLine={false} axisLine={false} fontSize={12} />
                  <YAxis tickLine={false} axisLine={false} fontSize={12} width={40} />
                  <Tooltip cursor={{ fill: CHART.primarySoft }} contentStyle={tooltipStyle} />
                  <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                    {soldVsValidated.map((d) => (
                      <Cell key={d.name} fill={d.fill} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <EmptyChart loading={summary.loading} />
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

const tooltipStyle: React.CSSProperties = {
  borderRadius: 12,
  border: "1px solid var(--border)",
  background: "var(--popover)",
  color: "var(--popover-foreground)",
  fontSize: 12,
};

function EmptyChart({ loading }: { loading: boolean }) {
  return (
    <div className="flex h-[240px] items-center justify-center text-sm text-muted-foreground">
      {loading ? "A carregar…" : "Ainda sem dados de vendas."}
    </div>
  );
}
