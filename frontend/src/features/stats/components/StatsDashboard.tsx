import { useAuth } from "@/auth/useAuth";
import { useMe } from "@/auth/useMe";
import { AdminSalesDashboard } from "./AdminSalesDashboard";
import { ManagerSalesDashboard } from "./ManagerSalesDashboard";
import { RecentResults } from "./RecentResults";

/**
 * Role-aware statistics block for the homepage:
 *   - platform-admin  → global sales KPIs + charts, then recent results
 *   - club manager     → per-club sales incl. revenue, for each managed club
 *   - club staff       → per-club sold/validated counts (NO revenue)
 *   - everyone else    → public recent results
 */
export function StatsDashboard() {
  const { hasRole } = useAuth();
  const { me } = useMe();

  if (hasRole("platform-admin")) {
    return (
      <section className="space-y-6">
        <SectionHeading title="Vendas — visão global" subtitle="Estatísticas de toda a plataforma." />
        <AdminSalesDashboard />
        <RecentResults />
      </section>
    );
  }

  const memberships = me?.clubMemberships ?? [];
  const managedClubIds = memberships.filter((m) => m.role === "MANAGER").map((m) => m.clubId);
  // Staff-only clubs (exclude any the user also manages — those get the fuller view).
  const staffClubIds = memberships
    .filter((m) => m.role === "STAFF" && !managedClubIds.includes(m.clubId))
    .map((m) => m.clubId);

  if (managedClubIds.length > 0) {
    return (
      <section className="space-y-6">
        <SectionHeading
          title="O meu clube — vendas"
          subtitle="Bilhetes e receita dos jogos em casa do(s) clube(s) que geres."
        />
        <ManagerSalesDashboard clubIds={managedClubIds} />
      </section>
    );
  }

  if (staffClubIds.length > 0) {
    return (
      <section className="space-y-6">
        <SectionHeading
          title="O meu clube — bilhetes"
          subtitle="Bilhetes vendidos e validados dos jogos em casa do(s) clube(s) onde és staff."
        />
        <ManagerSalesDashboard clubIds={staffClubIds} showRevenue={false} />
      </section>
    );
  }

  return (
    <section className="space-y-4">
      <SectionHeading title="Resultados" subtitle="Os jogos mais recentes." />
      <RecentResults />
    </section>
  );
}

function SectionHeading({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div className="space-y-1">
      <h2 className="font-display text-2xl font-bold tracking-tight">{title}</h2>
      <p className="text-sm text-muted-foreground">{subtitle}</p>
    </div>
  );
}
