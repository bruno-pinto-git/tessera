import { useState } from "react";
import { Check, X, Search } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { StatusBadge } from "@/components/ui/status-badge";
import { Crest } from "@/components/Crest";
import { clubBy } from "@/features/clubs/clubs";
import { validateTicket } from "@/api/ticketApi";
import { ApiError } from "@/api/client";
import { cn } from "@/lib/utils";

/**
 * Gate-scanner experience for staff. Three visual states:
 *
 *  - idle       : camera viewport with reticle + manual fallback input
 *  - success    : full-screen green confirmation
 *  - reject     : full-screen red rejection with reason
 *
 * The current implementation skips real camera scanning (would need
 * getUserMedia + a QR-decoder library like jsQR or zxing-js). Manual
 * input is enough to demo the API + visual states end-to-end. Wiring
 * camera frames into the same `validate(code)` call is incremental.
 */
type State =
  | { kind: "idle" }
  | { kind: "success"; ticketId: number; code: string }
  | { kind: "reject"; reason: string; status?: number };

export function ValidatePage() {
  const [state, setState] = useState<State>({ kind: "idle" });
  const [manualCode, setManualCode] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [counters, setCounters] = useState({ ok: 312, err: 4 });

  async function submit(code: string) {
    setSubmitting(true);
    try {
      const ticket = await validateTicket({ code });
      setState({ kind: "success", ticketId: ticket.id, code: ticket.code });
      setCounters((c) => ({ ...c, ok: c.ok + 1 }));
    } catch (err) {
      let reason = "Erro desconhecido";
      let status: number | undefined;
      if (err instanceof ApiError) {
        status = err.status;
        if (err.status === 404) reason = "Bilhete não encontrado";
        else if (err.status === 409) reason = "Bilhete já utilizado";
        else if (err.status === 400) reason = "Código inválido";
        else reason = `Erro ${err.status}`;
      } else if (err instanceof Error) {
        reason = err.message;
      }
      setState({ kind: "reject", reason, status });
      setCounters((c) => ({ ...c, err: c.err + 1 }));
    } finally {
      setSubmitting(false);
      setManualCode("");
    }
  }

  const reset = () => setState({ kind: "idle" });

  return (
    <div className="mx-auto max-w-md">
      <Card className="relative flex flex-col overflow-hidden h-[640px]">
        <GateHeader />

        <div className="flex-1 relative overflow-hidden">
          {state.kind === "idle" && (
            <IdleView
              manualCode={manualCode}
              onManualCode={setManualCode}
              onSubmit={submit}
              submitting={submitting}
            />
          )}
          {state.kind === "success" && <SuccessView ticketId={state.ticketId} code={state.code} />}
          {state.kind === "reject" && <RejectView reason={state.reason} status={state.status} />}
        </div>

        {state.kind !== "idle" && (
          <div className="border-t bg-background p-3 grid grid-cols-2 gap-2">
            <Button variant="outline" size="lg" onClick={reset}>
              Detalhes
            </Button>
            <Button size="lg" onClick={reset}>
              Próximo
            </Button>
          </div>
        )}

        <GateFooterCounter ok={counters.ok} err={counters.err} />
      </Card>
    </div>
  );
}

function GateHeader() {
  // For the visual demo we pin a fixed match. In production this would be
  // selected on shift start (which gate the staff is assigned to).
  const home = clubBy("ALJ");
  const away = clubBy("PRA");
  const now = new Date();
  const time = `${now.getHours().toString().padStart(2, "0")}:${now.getMinutes().toString().padStart(2, "0")}`;
  return (
    <div className="flex items-center gap-3 px-4 py-3 bg-background border-b">
      <Crest initials={home.initials} tone={home.tone} size={28} />
      <Crest initials={away.initials} tone={away.tone} size={28} />
      <div className="flex-1 min-w-0">
        <div className="text-[13px] font-semibold leading-tight truncate">
          {home.short} vs {away.short}
        </div>
        <div className="text-[11px] text-muted-foreground">Sáb 16 Mai · Portão B</div>
      </div>
      <span className="text-[11px] text-muted-foreground font-mono tabular-nums">{time}</span>
    </div>
  );
}

function GateFooterCounter({ ok, err }: { ok: number; err: number }) {
  return (
    <div className="border-t grid grid-cols-2">
      <div className="px-4 py-3 border-r">
        <div className="text-[10px] uppercase tracking-wider text-muted-foreground">Validados</div>
        <div className="font-mono text-lg font-semibold text-status-validated">{ok}</div>
      </div>
      <div className="px-4 py-3">
        <div className="text-[10px] uppercase tracking-wider text-muted-foreground">Rejeitados</div>
        <div className="font-mono text-lg font-semibold text-status-cancelled">{err}</div>
      </div>
    </div>
  );
}

function IdleView({
  manualCode,
  onManualCode,
  onSubmit,
  submitting,
}: {
  manualCode: string;
  onManualCode: (v: string) => void;
  onSubmit: (code: string) => void;
  submitting: boolean;
}) {
  return (
    <div className="relative h-full flex flex-col">
      <div
        className="relative flex-1 overflow-hidden"
        style={{
          background:
            "radial-gradient(60% 50% at 50% 45%, oklch(0.25 0.01 240), oklch(0.12 0 0))",
        }}
      >
        <div
          className="absolute inset-0 opacity-20"
          style={{
            background:
              "repeating-linear-gradient(45deg, transparent 0 3px, rgba(255,255,255,0.04) 3px 6px)",
          }}
        />

        <div
          className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2"
          style={{ width: 220, height: 220 }}
        >
          <Corner pos="tl" />
          <Corner pos="tr" />
          <Corner pos="bl" />
          <Corner pos="br" />
          <div
            className="absolute inset-x-0 top-1/2 h-px"
            style={{
              background:
                "linear-gradient(90deg, transparent, oklch(0.55 0.16 152) 50%, transparent)",
              boxShadow: "0 0 12px oklch(0.55 0.16 152 / 0.7)",
            }}
          />
        </div>

        <div className="absolute inset-x-0 bottom-6 px-6 text-center">
          <div className="text-white/90 text-sm font-medium">A apontar a câmara…</div>
          <div className="text-white/60 text-xs mt-1">Aproxima o QR do bilhete</div>
        </div>
      </div>

      <form
        className="p-3 border-t bg-background flex items-center gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          if (manualCode.trim()) onSubmit(manualCode.trim());
        }}
      >
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground size-3.5" />
          <Input
            value={manualCode}
            onChange={(e) => onManualCode(e.target.value)}
            placeholder="Inserir código manualmente…"
            className="pl-9"
          />
        </div>
        <Button size="sm" type="submit" disabled={submitting || !manualCode.trim()}>
          {submitting ? "…" : "Validar"}
        </Button>
      </form>
    </div>
  );
}

function Corner({ pos }: { pos: "tl" | "tr" | "bl" | "br" }) {
  const styles: Record<typeof pos, React.CSSProperties> = {
    tl: {
      top: 0,
      left: 0,
      borderTop: "3px solid white",
      borderLeft: "3px solid white",
      borderRadius: "12px 0 0 0",
    },
    tr: {
      top: 0,
      right: 0,
      borderTop: "3px solid white",
      borderRight: "3px solid white",
      borderRadius: "0 12px 0 0",
    },
    bl: {
      bottom: 0,
      left: 0,
      borderBottom: "3px solid white",
      borderLeft: "3px solid white",
      borderRadius: "0 0 0 12px",
    },
    br: {
      bottom: 0,
      right: 0,
      borderBottom: "3px solid white",
      borderRight: "3px solid white",
      borderRadius: "0 0 12px 0",
    },
  };
  return <div className="absolute size-7" style={styles[pos]} />;
}

function SuccessView({ ticketId, code }: { ticketId: number; code: string }) {
  const time = new Date().toLocaleTimeString("pt-PT");
  return (
    <div
      className={cn(
        "h-full flex flex-col items-center justify-center px-6 text-center",
        "animate-in fade-in zoom-in-95 duration-200",
      )}
      style={{
        background: "linear-gradient(180deg, oklch(0.96 0.04 152), oklch(0.88 0.09 152))",
        color: "oklch(0.2 0.06 152)",
      }}
    >
      <div
        className="rounded-full flex items-center justify-center mb-6"
        style={{
          width: 144,
          height: 144,
          background: "white",
          boxShadow: "0 12px 40px oklch(0.45 0.13 152 / 0.25)",
        }}
      >
        <Check style={{ color: "oklch(0.45 0.13 152)" }} className="size-20" />
      </div>
      <div className="text-[11px] uppercase tracking-widest font-semibold opacity-70">
        Bilhete válido
      </div>
      <div className="text-4xl font-bold tracking-tight mt-1">Entra!</div>

      <Card className="mt-8 w-full text-left">
        <div className="px-5 py-4 space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-muted-foreground">Bilhete</span>
            <span className="font-mono">#{ticketId}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Código</span>
            <span className="font-mono text-[11px]">
              {code.slice(0, 8)}…{code.slice(-4)}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Validado às</span>
            <span className="font-mono">{time}</span>
          </div>
        </div>
      </Card>
    </div>
  );
}

function RejectView({ reason, status }: { reason: string; status?: number }) {
  return (
    <div
      className={cn(
        "h-full flex flex-col items-center justify-center px-6 text-center",
        "animate-in fade-in zoom-in-95 duration-200",
      )}
      style={{
        background: "linear-gradient(180deg, oklch(0.96 0.04 27), oklch(0.86 0.13 27))",
        color: "oklch(0.25 0.1 27)",
      }}
    >
      <div
        className="rounded-full flex items-center justify-center mb-6"
        style={{
          width: 144,
          height: 144,
          background: "white",
          boxShadow: "0 12px 40px oklch(0.6 0.18 27 / 0.3)",
        }}
      >
        <X style={{ color: "oklch(0.6 0.18 27)" }} className="size-20" />
      </div>
      <div className="text-[11px] uppercase tracking-widest font-semibold opacity-70">
        Não pode entrar
      </div>
      <div className="text-3xl font-bold tracking-tight mt-1">{reason}</div>
      {status && (
        <div className="text-sm mt-2 max-w-xs opacity-80">
          Chama um responsável se houver dúvidas.
        </div>
      )}
      <Card className="mt-8 w-full text-left">
        <div className="px-5 py-4 space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-muted-foreground">Motivo</span>
            <span>{reason}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Estado</span>
            <StatusBadge status={status === 409 ? "VALIDATED" : "CANCELLED"} />
          </div>
        </div>
      </Card>
    </div>
  );
}
