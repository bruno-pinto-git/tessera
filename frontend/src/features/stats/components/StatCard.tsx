import { Card, CardContent } from "@/components/ui/card";

/** Compact KPI tile: a label, a big value, and an optional sub-line. */
export function StatCard({
  label,
  value,
  hint,
  icon,
}: {
  label: string;
  value: React.ReactNode;
  hint?: React.ReactNode;
  icon?: React.ReactNode;
}) {
  return (
    <Card>
      <CardContent className="p-5">
        <div className="flex items-center justify-between gap-2">
          <span className="text-sm text-muted-foreground">{label}</span>
          {icon}
        </div>
        <div className="mt-2 font-display text-3xl font-bold tracking-tight">{value}</div>
        {hint != null && <div className="mt-1 text-xs text-muted-foreground">{hint}</div>}
      </CardContent>
    </Card>
  );
}
