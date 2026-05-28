import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ChevronRight } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import { Crest } from "@/components/Crest";
import { useEventCatalog } from "../hooks/useEventsCatalog";
import { PurchaseModal } from "../components/PurchaseModal";
import { NotFoundPage } from "@/pages/NotFoundPage";
import type { CatalogEntry } from "../lib/catalog";

export function EventDetailPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const { entry, loading, error, notFound } = useEventCatalog(id);
  const [open, setOpen] = useState(false);

  if (notFound) return <NotFoundPage />;
  if (loading) {
    return <p className="text-sm text-muted-foreground py-12 text-center">A carregar jogo…</p>;
  }
  if (error || !entry) {
    return (
      <p className="text-sm text-destructive py-12 text-center">
        Falha a carregar: {error ?? "evento indisponível"}
      </p>
    );
  }

  const { day, date, time } = formatKickoff(entry.kickoffAt);

  return (
    <>
      <div className="space-y-8">
        <Breadcrumb
          parts={[
            { label: "Jogos", to: "/events" },
            { label: `${entry.homeShort} vs ${entry.awayShort}` },
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
              <ClubBlock
                initials={entry.homeInitials}
                tone={entry.homeTone}
                name={entry.homeClubName}
                side="Casa"
              />
              <div className="text-center">
                <div className="text-[11px] uppercase tracking-widest text-muted-foreground mb-2">
                  {entry.homeCategory ?? "Jogo"}
                </div>
                <div className="text-5xl font-bold tracking-tight tabular-nums text-muted-foreground">
                  {entry.homeScore != null && entry.awayScore != null
                    ? `${entry.homeScore} – ${entry.awayScore}`
                    : "vs"}
                </div>
                {entry.matchStatus && (
                  <div className="mt-3">
                    <StatusBadge status={entry.matchStatus} />
                  </div>
                )}
              </div>
              <ClubBlock
                initials={entry.awayInitials}
                tone={entry.awayTone}
                name={entry.awayClubName}
                side="Fora"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-3 border-t divide-x text-sm">
            <Meta label="Data" value={date ? `${day}, ${date}` : "Por confirmar"} sub={time} />
            <Meta
              label="Estádio"
              value={entry.venueName ?? "Por definir"}
              sub={
                entry.venueCapacity
                  ? `${entry.venueCapacity.toLocaleString("pt-PT")} lugares`
                  : undefined
              }
            />
            <Meta
              label="Bilheteira"
              value={
                entry.eventStatus === "PUBLISHED" ? "Aberta" : labelForStatus(entry.eventStatus)
              }
              sub={`Normal ${entry.priceNormal.toFixed(2)} € · Sócio ${entry.priceSupporter.toFixed(2)} €`}
            />
          </div>
        </header>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <section className="lg:col-span-2 space-y-6">
            <PriceTable entry={entry} />
            <Indications time={time} />
          </section>

          <aside>
            <PurchaseSticky entry={entry} onBuy={() => setOpen(true)} />
          </aside>
        </div>
      </div>

      <PurchaseModal open={open} onOpenChange={setOpen} entry={entry} />
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
  tone: CatalogEntry["homeTone"];
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
      <div className="font-medium text-sm mt-0.5 truncate">{value}</div>
      {sub && <div className="text-xs text-muted-foreground truncate">{sub}</div>}
    </div>
  );
}

function PriceTable({ entry }: { entry: CatalogEntry }) {
  return (
    <Card>
      <div className="px-6 py-5 border-b">
        <h2 className="font-semibold">Tabela de preços</h2>
        <p className="text-sm text-muted-foreground">
          Sócios pagam o preço de sócio. Mostra o cartão no portão.
        </p>
      </div>
      <div className="divide-y">
        <PriceRow label="Bilhete normal" subtitle="Adulto" price={entry.priceNormal} />
        <PriceRow
          label="Sócio do clube"
          subtitle="Com cartão válido"
          price={entry.priceSupporter}
          accent
        />
      </div>
    </Card>
  );
}

function PriceRow({
  label,
  subtitle,
  price,
  accent,
}: {
  label: string;
  subtitle: string;
  price: number;
  accent?: boolean;
}) {
  return (
    <div className="px-6 py-4 flex items-center gap-4">
      <div className="flex-1">
        <div className="font-medium">{label}</div>
        <p className="text-xs text-muted-foreground">{subtitle}</p>
      </div>
      <div className={`font-mono font-semibold ${accent ? "text-primary" : ""}`}>
        {price.toFixed(2)} €
      </div>
    </div>
  );
}

function Indications({ time }: { time: string }) {
  return (
    <Card>
      <div className="px-6 py-5 border-b">
        <h2 className="font-semibold">Indicações</h2>
      </div>
      <div className="px-6 py-5 grid grid-cols-1 md:grid-cols-2 gap-x-10 gap-y-3 text-sm">
        <Row k="Apito inicial">{time || "—"}</Row>
        <Row k="Pagamento à porta">MB WAY, dinheiro</Row>
        <Row k="QR no telemóvel">Sem papel; mostra o ecrã ao portão</Row>
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

function PurchaseSticky({ entry, onBuy }: { entry: CatalogEntry; onBuy: () => void }) {
  const disabled = entry.eventStatus !== "PUBLISHED";
  return (
    <Card className="lg:sticky lg:top-6">
      <div className="px-6 pt-6 pb-4 border-b">
        <div className="text-[11px] uppercase tracking-wider text-muted-foreground">Comprar</div>
        <div className="mt-2 flex items-baseline gap-1">
          <span className="text-3xl font-bold tracking-tight">{entry.priceFrom.toFixed(2)}</span>
          <span className="text-sm text-muted-foreground">€ a partir de</span>
        </div>
        <p className="text-xs text-muted-foreground mt-1">QR no momento. Sem filas, sem papel.</p>
      </div>
      <div className="px-6 py-5 space-y-3">
        <Button size="lg" className="w-full" onClick={onBuy} disabled={disabled}>
          {disabled ? labelForStatus(entry.eventStatus) : "Comprar bilhete"}
        </Button>
      </div>
      <div className="border-t px-6 py-4 text-xs text-muted-foreground space-y-1">
        <div className="flex items-center justify-between">
          <span>Pagamento</span>
          <span className="text-foreground">MB WAY · Cartão</span>
        </div>
      </div>
    </Card>
  );
}

function labelForStatus(s: CatalogEntry["eventStatus"]): string {
  switch (s) {
    case "PUBLISHED":
      return "Aberta";
    case "DRAFT":
      return "Bilheteira em preparação";
    case "SALES_CLOSED":
      return "Bilheteira fechada";
    case "CANCELLED":
      return "Cancelado";
  }
}

function formatKickoff(iso: string | null): { day: string; date: string; time: string } {
  if (!iso) return { day: "—", date: "", time: "" };
  const d = new Date(iso);
  if (isNaN(d.getTime())) return { day: "—", date: iso, time: "" };
  const day = d.toLocaleDateString("pt-PT", { weekday: "short" }).replace(".", "");
  const date = d.toLocaleDateString("pt-PT", { day: "2-digit", month: "short" }).replace(".", "");
  const time = d.toLocaleTimeString("pt-PT", { hour: "2-digit", minute: "2-digit" });
  return {
    day: day.charAt(0).toUpperCase() + day.slice(1),
    date: date.charAt(0).toUpperCase() + date.slice(1),
    time,
  };
}
