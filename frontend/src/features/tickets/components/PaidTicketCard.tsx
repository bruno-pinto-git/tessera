import { useRef, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { Download, Share2 } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import { Crest } from "@/components/Crest";
import { getWalletSaveUrl } from "@/api/ticketApi";
import type { TicketView } from "../hooks/useMyTickets";

export function PaidTicketCard({ ticket }: { ticket: TicketView }) {
  const { home, away } = ticket;
  const [addingToWallet, setAddingToWallet] = useState(false);
  const [shareNote, setShareNote] = useState<string | null>(null);
  const qrRef = useRef<HTMLDivElement>(null);

  function savePdf() {
    const qr = qrRef.current?.querySelector("svg")?.outerHTML ?? "";
    const rows = [
      ["Local", ticket.venue ?? "—"],
      ["Tipo", ticket.tier],
      ["Preço", ticket.price],
      ["Bilhete", `#${ticket.id}`],
    ]
      .map(([k, v]) => `<tr><td class="k">${esc(k)}</td><td class="v">${esc(v)}</td></tr>`)
      .join("");
    const html = `<!doctype html><html lang="pt"><head><meta charset="utf-8">
      <title>Bilhete — ${esc(ticket.title)}</title><style>
      @page{margin:18mm} *{box-sizing:border-box}
      body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;color:#111;margin:0}
      .wrap{max-width:520px;margin:0 auto;border:1px solid #ddd;border-radius:12px;padding:28px}
      .brand{text-align:center;font-weight:700;letter-spacing:2px;color:#0a7a4b;margin-bottom:16px}
      h1{font-size:20px;margin:0 0 4px} .sub{color:#666;font-size:13px}
      .qr{text-align:center;margin:22px 0} .qr svg{width:220px;height:220px}
      .code{font-family:monospace;font-size:12px;text-align:center;color:#333;word-break:break-all;margin-bottom:16px}
      table{width:100%;border-collapse:collapse;font-size:13px}
      td{padding:7px 0;border-bottom:1px solid #eee} td.k{color:#666} td.v{text-align:right}
      </style></head><body><div class="wrap">
      <div class="brand">TESSERA</div>
      <h1>${esc(ticket.title)}</h1><div class="sub">${esc(ticket.day)}</div>
      <div class="qr">${qr}</div>
      <div class="code">${esc(ticket.code)}</div>
      <table>${rows}</table>
      </div></body></html>`;
    const frame = document.createElement("iframe");
    frame.setAttribute("aria-hidden", "true");
    frame.style.cssText = "position:fixed;right:0;bottom:0;width:0;height:0;border:0";
    document.body.appendChild(frame);
    const doc = frame.contentWindow?.document;
    if (!doc) {
      frame.remove();
      return;
    }
    doc.open();
    doc.write(html);
    doc.close();
    // Content is fully inline (SVG + CSS), so a short tick is enough before printing.
    window.setTimeout(() => {
      frame.contentWindow?.focus();
      frame.contentWindow?.print();
      window.setTimeout(() => frame.remove(), 1000);
    }, 300);
  }

  async function share() {
    const url = `${window.location.origin}/events/${ticket.eventId}`;
    const text = `O meu bilhete para ${ticket.title} (${ticket.day}).`;

    // Native share sheet where available (mobile, and desktop Chromium).
    if (navigator.share) {
      try {
        await navigator.share({ title: `Bilhete — ${ticket.title}`, text, url });
      } catch {
        // user dismissed the sheet — nothing to do
      }
      return;
    }

    // Fallback: copy to clipboard; always give feedback, even if the copy is blocked.
    const payload = `${text} ${url}`;
    let copied = false;
    try {
      await navigator.clipboard.writeText(payload);
      copied = true;
    } catch {
      copied = legacyCopy(payload);
    }
    setShareNote(copied ? "Link copiado para a área de transferência" : url);
    window.setTimeout(() => setShareNote(null), 4000);
  }

  async function addToWallet() {
    setAddingToWallet(true);
    try {
      const { saveUrl } = await getWalletSaveUrl(Number(ticket.id), {
        eventTitle: ticket.title,
        venue: ticket.venue,
        kickoffAt: ticket.kickoffIso,
        tierLabel: ticket.tier,
      });
      window.location.href = saveUrl;
    } catch {
      setAddingToWallet(false);
    }
  }

  return (
    <Card className="overflow-hidden">
      <div className="grid grid-cols-[1fr_auto]">
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
              <div className="font-semibold leading-tight">{ticket.title}</div>
              <div className="text-xs text-muted-foreground">{ticket.day}</div>
            </div>
          </div>

          <div className="space-y-1.5 text-xs">
            <DL k="Local" v={ticket.venue ?? "—"} />
            <DL k="Tipo" v={ticket.tier} />
            <DL k="Preço" v={ticket.price} />
          </div>

          <div className="flex items-center gap-2">
            <Button size="sm" variant="outline" className="flex-1" onClick={savePdf}>
              <Download className="size-3.5" /> Guardar PDF
            </Button>
            <Button size="sm" variant="ghost" className="flex-1" onClick={share}>
              <Share2 className="size-3.5" /> Partilhar
            </Button>
          </div>
          {shareNote && (
            <p className="-mt-2 text-center text-xs text-muted-foreground">{shareNote}</p>
          )}

          <button
            type="button"
            onClick={addToWallet}
            disabled={addingToWallet}
            className="flex h-12 w-full items-center justify-center gap-2 rounded-md bg-black text-sm font-medium text-white disabled:opacity-60"
          >
            <WalletGlyph />
            {addingToWallet ? "A preparar…" : "Adicionar ao Google Wallet"}
          </button>
        </div>

        <div
          className="relative flex flex-col items-center justify-center gap-3 px-6 py-6 bg-secondary/40 border-l"
          style={{ borderLeftStyle: "dashed" }}
        >
          <span className="absolute -left-2 top-3 size-4 rounded-full bg-background border" />
          <span className="absolute -left-2 bottom-3 size-4 rounded-full bg-background border" />
          <div ref={qrRef} className="rounded-lg bg-white p-2 border">
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

function legacyCopy(text: string): boolean {
  try {
    const ta = document.createElement("textarea");
    ta.value = text;
    ta.style.cssText = "position:fixed;top:0;left:0;opacity:0";
    document.body.appendChild(ta);
    ta.focus();
    ta.select();
    const ok = document.execCommand("copy");
    ta.remove();
    return ok;
  } catch {
    return false;
  }
}

function esc(s: string): string {
  return s.replace(
    /[&<>"]/g,
    (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c] ?? c,
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

function WalletGlyph() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="2" y="6" width="20" height="14" rx="2" stroke="white" strokeWidth="1.6" />
      <path d="M2 10h20" stroke="white" strokeWidth="1.6" />
      <circle cx="17" cy="15" r="1.4" fill="white" />
    </svg>
  );
}
