import { QRCodeSVG } from "qrcode.react";
import { Download, Share2 } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import { Crest } from "@/components/Crest";
import type { TicketView } from "../hooks/useMyTickets";

export function PaidTicketCard({ ticket }: { ticket: TicketView }) {
  const { home, away } = ticket;

  return (
    <Card className="overflow-hidden">
      <div className="grid grid-cols-[1fr_auto]">
        {/* Left: match info */}
        <div className="p-6 space-y-5">
          <div className="flex items-center justify-between">
            <StatusBadge status="PAID" />
            <span className="text-[11px] text-muted-foreground font-mono">#{ticket.id}</span>
          </div>

          <div className="flex items-center gap-3">
            <Crest initials={home.initials} tone={home.tone} size={36} />
            <div className="text-sm text-muted-foreground">vs</div>
            <Crest initials={away.initials} tone={away.tone} size={36} />
            <div className="ml-2">
              <div className="font-semibold leading-tight">
                {home.short} <span className="text-muted-foreground font-normal">vs</span>{" "}
                {away.short}
              </div>
              <div className="text-xs text-muted-foreground">{ticket.day}</div>
            </div>
          </div>

          <div className="space-y-1.5 text-xs">
            <DL k="Local" v={ticket.venue ?? "—"} />
            <DL k="Tipo" v={ticket.tier} />
            <DL k="Preço" v={ticket.price} />
          </div>

          <div className="flex items-center gap-2">
            <Button size="sm" variant="outline" className="flex-1">
              <Download className="size-3.5" /> Guardar PDF
            </Button>
            <Button size="sm" variant="ghost" className="flex-1">
              <Share2 className="size-3.5" /> Partilhar
            </Button>
          </div>
        </div>

        {/* Right: perforated stub + QR */}
        <div
          className="relative flex flex-col items-center justify-center gap-3 px-6 py-6 bg-secondary/40 border-l"
          style={{ borderLeftStyle: "dashed" }}
        >
          <span className="absolute -left-2 top-3 size-4 rounded-full bg-background border" />
          <span className="absolute -left-2 bottom-3 size-4 rounded-full bg-background border" />
          <div className="rounded-lg bg-white p-2 border">
            <QRCodeSVG value={ticket.code} size={156} />
          </div>
          <div className="text-center">
            <div className="text-[10px] uppercase tracking-widest text-muted-foreground">
              Código
            </div>
            <div className="font-mono text-[11px] mt-0.5">
              {ticket.code.slice(0, 8)}…{ticket.code.slice(-4)}
            </div>
          </div>
          {ticket.paidAt && (
            <div className="text-[10px] text-muted-foreground">Pago em {ticket.paidAt}</div>
          )}
        </div>
      </div>
    </Card>
  );
}

function DL({ k, v }: { k: string; v: string }) {
  return (
    <div className="flex justify-between gap-4">
      <span className="text-muted-foreground">{k}</span>
      <span className="text-right">{v}</span>
    </div>
  );
}
