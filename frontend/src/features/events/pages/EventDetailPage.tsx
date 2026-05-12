import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ChevronRight } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import { Crest } from "@/components/Crest";
import { useAuth } from "@/auth/useAuth";
import { clubBy } from "@/features/clubs/clubs";
import { MOCK_MATCH_DETAIL, type MatchDetail, type PriceTier } from "../mockMatches";
import { PurchaseModal } from "../components/PurchaseModal";
import { NotFoundPage } from "@/pages/NotFoundPage";

export function EventDetailPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const match = MOCK_MATCH_DETAIL[id];
  const [open, setOpen] = useState(false);

  if (!match) return <NotFoundPage />;

  const home = clubBy(match.homeClubId);
  const away = clubBy(match.awayClubId);
  const cheapest = match.tiers.reduce(
    (acc, t) => (t.normal < acc ? t.normal : acc),
    Number.POSITIVE_INFINITY,
  );

  return (
    <>
      <div className="space-y-8">
        <Breadcrumb
          parts={[
            { label: "Jogos", to: "/events" },
            { label: match.competition.split("·")[0].trim(), to: "/events" },
            { label: `${home.short} vs ${away.short}` },
          ]}
        />

        <header className="rounded-xl border bg-card overflow-hidden">
          <div
            className="relative px-6 md:px-10 pt-10 pb-8"
            style={{
              background:
                "radial-gradient(1100px 360px at 50% -120%, oklch(0.45 0.13 152 / 0.18), transparent 60%), linear-gradient(180deg, oklch(0.99 0 0), oklch(0.97 0 0))",
            }}
          >
            <div className="flex items-center justify-between gap-6 flex-wrap">
              <ClubBlock initials={home.initials} tone={home.tone} name={home.name} side="Casa" />
              <div className="text-center">
                <div className="text-[11px] uppercase tracking-widest text-muted-foreground mb-2">
                  {match.matchday}
                </div>
                <div className="text-5xl font-bold tracking-tight tabular-nums text-muted-foreground">
                  vs
                </div>
                <div className="mt-3">
                  <StatusBadge status={match.status} />
                </div>
              </div>
              <ClubBlock initials={away.initials} tone={away.tone} name={away.name} side="Fora" />
            </div>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 border-t divide-x text-sm">
            <Meta label="Data" value={`${match.day}, ${match.date}`} sub={match.time} />
            <Meta label="Estádio" value={match.venue} sub={home.short} />
            <Meta label="Competição" value="Campeonato de Portugal" sub="Série D · J28" />
            <Meta
              label="Lotação"
              value={`${match.sold.toLocaleString("pt-PT")} / ${match.capacity.toLocaleString("pt-PT")}`}
              sub={`${Math.round((match.sold / match.capacity) * 100)}% ocupado`}
            />
          </div>
        </header>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <section className="lg:col-span-2 space-y-6">
            <PriceTable tiers={match.tiers} />
            <Indications match={match} />
          </section>

          <aside className="space-y-4">
            <PurchaseSticky cheapest={cheapest} onBuy={() => setOpen(true)} />
            <AdminSalesCard match={match} />
          </aside>
        </div>
      </div>

      <PurchaseModal open={open} onOpenChange={setOpen} match={match} />
    </>
  );
}

function Breadcrumb({ parts }: { parts: { label: string; to?: string }[] }) {
  return (
    <nav className="flex items-center gap-2 text-xs text-muted-foreground">
      {parts.map((p, i) => (
        <span key={i} className="flex items-center gap-2">
          {p.to ? (
            <Link to={p.to} className="hover:text-foreground transition-colors">
              {p.label}
            </Link>
          ) : (
            <span className="text-foreground">{p.label}</span>
          )}
          {i < parts.length - 1 && <ChevronRight className="size-3" />}
        </span>
      ))}
    </nav>
  );
}

function ClubBlock({
  initials,
  tone,
  name,
  side,
}: {
  initials: string;
  tone: ReturnType<typeof clubBy>["tone"];
  name: string;
  side: string;
}) {
  return (
    <div className="flex flex-col items-center gap-3 flex-1 text-center min-w-[120px]">
      <Crest initials={initials} tone={tone} size={96} />
      <div>
        <div className="text-xl font-bold tracking-tight">{name}</div>
        <div className="text-xs uppercase tracking-wider text-muted-foreground mt-0.5">{side}</div>
      </div>
    </div>
  );
}

function Meta({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="px-6 py-4">
      <div className="text-[11px] uppercase tracking-wider text-muted-foreground">{label}</div>
      <div className="font-medium text-sm mt-0.5">{value}</div>
      {sub && <div className="text-xs text-muted-foreground">{sub}</div>}
    </div>
  );
}

function PriceTable({ tiers }: { tiers: PriceTier[] }) {
  return (
    <Card>
      <div className="px-6 py-5 border-b">
        <h2 className="font-semibold">Tabela de preços</h2>
        <p className="text-sm text-muted-foreground">
          Escolhe o tipo de bilhete. Sócios pagam abaixo.
        </p>
      </div>
      <div className="divide-y">
        {tiers.map((t) => (
          <div key={t.name} className="px-6 py-4 flex items-center gap-4">
            <div className="flex-1">
              <div className="flex items-center gap-2">
                <span className="font-medium">{t.name}</span>
                {t.scarce && (
                  <span className="text-[11px] text-status-pending font-medium">
                    Últimos {t.left}
                  </span>
                )}
              </div>
              <p className="text-xs text-muted-foreground">{t.description}</p>
            </div>
            <div className="grid grid-cols-2 gap-6 text-right">
              <div>
                <div className="text-[11px] uppercase tracking-wider text-muted-foreground">
                  Normal
                </div>
                <div className="font-mono font-semibold">{t.normal},00 €</div>
              </div>
              <div>
                <div className="text-[11px] uppercase tracking-wider text-muted-foreground">
                  Sócio
                </div>
                <div className="font-mono font-semibold text-primary">{t.socio},00 €</div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
}

function Indications({ match }: { match: MatchDetail }) {
  return (
    <Card>
      <div className="px-6 py-5 border-b">
        <h2 className="font-semibold">Indicações</h2>
      </div>
      <div className="px-6 py-5 grid grid-cols-1 md:grid-cols-2 gap-x-10 gap-y-3 text-sm">
        <Row k="Portões abrem">{match.gatesOpen}</Row>
        <Row k="Apito inicial">{match.time}</Row>
        <Row k="Estacionamento">{match.parking}</Row>
        <Row k="Acessibilidade">{match.accessibility}</Row>
        <Row k="Pagamento à porta">MBWAY, dinheiro</Row>
        <Row k="Política">Sem reembolso após apito</Row>
      </div>
    </Card>
  );
}

function Row({ k, children }: { k: string; children: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-4">
      <span className="text-muted-foreground">{k}</span>
      <span className="text-right">{children}</span>
    </div>
  );
}

function PurchaseSticky({ cheapest, onBuy }: { cheapest: number; onBuy: () => void }) {
  return (
    <Card className="lg:sticky lg:top-6">
      <div className="px-6 pt-6 pb-4 border-b">
        <div className="text-[11px] uppercase tracking-wider text-muted-foreground">Comprar</div>
        <div className="mt-2 flex items-baseline gap-1">
          <span className="text-3xl font-bold tracking-tight">{cheapest},00</span>
          <span className="text-sm text-muted-foreground">€ a partir de</span>
        </div>
        <p className="text-xs text-muted-foreground mt-1">QR no momento. Sem filas, sem papel.</p>
      </div>
      <div className="px-6 py-5 space-y-3">
        <Button size="lg" className="w-full" onClick={onBuy}>
          Comprar bilhete
        </Button>
        <Button size="lg" variant="outline" className="w-full">
          Sou sócio
        </Button>
      </div>
      <div className="border-t px-6 py-4 text-xs text-muted-foreground space-y-1">
        <div className="flex items-center justify-between">
          <span>Pagamento</span>
          <span className="text-foreground">MBWAY · Cartão</span>
        </div>
        <div className="flex items-center justify-between">
          <span>Reembolso</span>
          <span className="text-foreground">Até 24h antes</span>
        </div>
      </div>
    </Card>
  );
}

function AdminSalesCard({ match }: { match: MatchDetail }) {
  const { hasRole } = useAuth();
  if (!hasRole("admin")) return null;
  const revenue = match.sold * 8; // rough proxy until the real query lands
  return (
    <Card>
      <div className="px-6 py-4 flex items-center justify-between border-b">
        <div>
          <div className="text-[11px] uppercase tracking-wider text-muted-foreground">
            Apenas admin
          </div>
          <div className="font-semibold text-sm">Vendas</div>
        </div>
        <Crest initials="A" tone="forest" size={28} square />
      </div>
      <div className="px-6 py-4 grid grid-cols-2 gap-3 text-sm">
        <Stat label="Pagos" value={match.sold.toLocaleString("pt-PT")} tone="paid" />
        <Stat label="Pendentes" value={match.pending.toLocaleString("pt-PT")} tone="pending" />
        <Stat label="Validados" value={match.validated.toLocaleString("pt-PT")} tone="validated" />
        <Stat label="Receita" value={`${revenue.toLocaleString("pt-PT")} €`} />
      </div>
    </Card>
  );
}

function Stat({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone?: "paid" | "pending" | "validated";
}) {
  const toneCls =
    tone === "paid"
      ? "text-status-paid"
      : tone === "pending"
        ? "text-status-pending"
        : tone === "validated"
          ? "text-status-validated"
          : "text-foreground";
  return (
    <div>
      <div className="text-[11px] uppercase tracking-wider text-muted-foreground">{label}</div>
      <div className={`font-mono font-semibold ${toneCls}`}>{value}</div>
    </div>
  );
}
