import {
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
import { useMatchLookups } from "@/features/matches/hooks/useMatchLookups";
import { useSalesByClub } from "../hooks/useStats";
import { CHART, formatRate, formatRevenue } from "./statsFormat";
import { StatCard } from "./StatCard";
import { RecentResults } from "./RecentResults";

/**
 * Club-scoped sales, one block per club. Managers see revenue; staff don't
 * (the backend returns `revenue: null` for them), so pass `showRevenue={false}`.
 */
export function ManagerSalesDashboard({
  clubIds,
  showRevenue = true,
}: {
  clubIds: number[];
  showRevenue?: boolean;
}) {
  if (clubIds.length === 0) return null;
  return (
    <div className="space-y-8">
      {clubIds.map((id) => (
        <ManagerClubBlock key={id} clubId={id} showRevenue={showRevenue} />
      ))}
    </div>
  );
}

function ManagerClubBlock({ clubId, showRevenue }: { clubId: number; showRevenue: boolean }) {
  const { data: s, loading, error } = useSalesByClub(clubId);
  const { clubs } = useMatchLookups();
  const name = clubs.get(clubId)?.name ?? `Clube #${clubId}`;

  const soldVsValidated = s
    ? [
        { name: "Vendidos", value: s.sold, fill: CHART.primary },
        { name: "Validados", value: s.validated, fill: CHART.accent },
      ]
    : [];

  return (
    <div className="space-y-4">
      <h3 className="font-display text-lg font-semibold tracking-tight">{name}</h3>

      {error ? (
        <p className="text-sm text-destructive">Não foi possível carregar as vendas deste clube.</p>
      ) : (
        <>
          <div className={`grid gap-4 ${showRevenue ? "sm:grid-cols-3" : "sm:grid-cols-2"}`}>
            {showRevenue && (
              <StatCard
                label="Receita do clube"
                value={s?.revenue != null ? formatRevenue(s.revenue) : loading ? "…" : "—"}
                icon={<BadgeEuro className="size-4 text-primary" />}
              />
            )}
            <StatCard
              label="Bilhetes vendidos"
              value={s ? s.sold.toLocaleString("pt-PT") : loading ? "…" : "—"}
              hint={s ? `${s.validated.toLocaleString("pt-PT")} validados` : undefined}
              icon={<TicketCheck className="size-4 text-primary" />}
            />
            <StatCard
              label="Taxa de validação"
              value={s ? formatRate(s.validationRate) : loading ? "…" : "—"}
              icon={<CheckCircle2 className="size-4 text-primary" />}
            />
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Vendidos vs. validados</CardTitle>
              </CardHeader>
              <CardContent>
                {s && s.sold > 0 ? (
                  <ResponsiveContainer width="100%" height={220}>
                    <BarChart
                      data={soldVsValidated}
                      margin={{ top: 8, right: 8, left: -8, bottom: 0 }}
                    >
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
                  <div className="flex h-[220px] items-center justify-center text-sm text-muted-foreground">
                    {loading ? "A carregar…" : "Ainda sem vendas para este clube."}
                  </div>
                )}
              </CardContent>
            </Card>

            <RecentResults clubId={clubId} title="Últimos jogos do clube" />
          </div>
        </>
      )}
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
