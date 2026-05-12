import { useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { Check, ChevronRight, Loader2 } from "lucide-react";
import { Link } from "react-router-dom";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import type { MatchDetail, PriceTier } from "../mockMatches";
import { cn } from "@/lib/utils";

type Step = 1 | 2 | 3;
type PaymentMethod = "MBWAY" | "CARD" | "CASH";

interface PurchaseModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  match: MatchDetail;
}

/**
 * 3-step purchase flow:
 *
 *   1. choose a price tier (and toggle "sou sócio")
 *   2. pick payment method, review total
 *   3. show the QR + ticket id, plus shortcut links
 *
 * State stays local — no Redux/Zustand needed for a single modal. The
 * actual `POST /tickets` + `POST /tickets/{id}/pay` calls happen between
 * step 2 and 3 once the events catalog is wired to real data; for now the
 * step 3 surface only renders mock data.
 */
export function PurchaseModal({ open, onOpenChange, match }: PurchaseModalProps) {
  const [step, setStep] = useState<Step>(1);
  const [tier, setTier] = useState<PriceTier>(match.tiers[0]);
  const [supporter, setSupporter] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("MBWAY");
  const [submitting, setSubmitting] = useState(false);
  const [ticketCode] = useState(() =>
    "tkt-" + Math.random().toString(36).slice(2, 10) + Math.random().toString(36).slice(2, 6),
  );

  const total = supporter ? tier.socio : tier.normal;

  const reset = () => {
    setStep(1);
    setTier(match.tiers[0]);
    setSupporter(false);
    setPaymentMethod("MBWAY");
    setSubmitting(false);
  };

  async function confirmPayment() {
    setSubmitting(true);
    // TODO: real call sequence —
    //   const ticket = await createTicket({ eventId, supporter });
    //   await payTicket(ticket.id, { paymentMethod });
    // For the visual implementation we just simulate latency.
    await new Promise((r) => setTimeout(r, 600));
    setSubmitting(false);
    setStep(3);
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(o) => {
        onOpenChange(o);
        if (!o) reset();
      }}
    >
      <DialogContent className="max-w-2xl">
        <div>
          <div className="text-[11px] uppercase tracking-widest text-muted-foreground">
            Fluxo de compra
          </div>
          <DialogTitle className="mt-1">
            {match.homeClubId === "ALJ" ? "Aljustrel" : match.homeClubId} vs{" "}
            {match.awayClubId === "PRA" ? "Praiense" : match.awayClubId} · {match.date}
          </DialogTitle>
          <DialogDescription className="sr-only">
            Fluxo de compra de bilhete em 3 passos.
          </DialogDescription>
        </div>

        <Stepper step={step} />

        {step === 1 && (
          <StepChooseTier
            tiers={match.tiers}
            tier={tier}
            onTier={setTier}
            supporter={supporter}
            onSupporter={setSupporter}
            onNext={() => setStep(2)}
          />
        )}

        {step === 2 && (
          <StepPayment
            tier={tier}
            total={total}
            paymentMethod={paymentMethod}
            onPaymentMethod={setPaymentMethod}
            submitting={submitting}
            onBack={() => setStep(1)}
            onConfirm={confirmPayment}
          />
        )}

        {step === 3 && (
          <StepConfirmation
            tier={tier}
            ticketCode={ticketCode}
            onClose={() => onOpenChange(false)}
          />
        )}
      </DialogContent>
    </Dialog>
  );
}

function Stepper({ step }: { step: Step }) {
  const items = [
    { n: 1, label: "Lugar" },
    { n: 2, label: "Pagamento" },
    { n: 3, label: "QR" },
  ];
  return (
    <ol className="flex items-center gap-2 text-xs text-muted-foreground">
      {items.map((it, i) => (
        <li key={it.n} className="flex items-center gap-2">
          <span
            className={cn(
              "inline-flex h-5 w-5 items-center justify-center rounded-full font-mono text-[10px]",
              it.n === step
                ? "bg-primary text-primary-foreground"
                : it.n < step
                  ? "bg-status-validated/15 text-status-validated"
                  : "bg-muted text-muted-foreground",
            )}
          >
            {it.n < step ? <Check className="size-3" /> : it.n}
          </span>
          <span className={cn(it.n === step && "text-foreground font-medium")}>{it.label}</span>
          {i < items.length - 1 && <ChevronRight className="size-3" />}
        </li>
      ))}
    </ol>
  );
}

function StepChooseTier({
  tiers,
  tier,
  onTier,
  supporter,
  onSupporter,
  onNext,
}: {
  tiers: PriceTier[];
  tier: PriceTier;
  onTier: (t: PriceTier) => void;
  supporter: boolean;
  onSupporter: (b: boolean) => void;
  onNext: () => void;
}) {
  return (
    <div className="space-y-4">
      <div className="space-y-2">
        {tiers.map((t) => {
          const on = t.name === tier.name;
          return (
            <label
              key={t.name}
              className={cn(
                "flex items-center gap-3 rounded-md border px-3 py-2.5 cursor-pointer",
                on && "border-primary bg-primary/5",
              )}
            >
              <input
                type="radio"
                name="tier"
                className="sr-only"
                checked={on}
                onChange={() => onTier(t)}
              />
              <span
                className={cn(
                  "size-4 rounded-full border-2 grid place-items-center",
                  on ? "border-primary" : "border-input",
                )}
              >
                {on && <span className="size-2 rounded-full bg-primary" />}
              </span>
              <div className="flex-1">
                <div className="text-sm font-medium">{t.name}</div>
                {t.scarce ? (
                  <div className="text-[11px] text-status-pending">Últimos {t.left}</div>
                ) : (
                  <div className="text-[11px] text-muted-foreground">{t.description}</div>
                )}
              </div>
              <span className="font-mono text-sm">
                {supporter ? t.socio : t.normal},00 €
              </span>
            </label>
          );
        })}
      </div>
      <label className="flex items-center gap-2 text-xs text-muted-foreground">
        <input
          type="checkbox"
          className="size-3.5 rounded border-input"
          checked={supporter}
          onChange={(e) => onSupporter(e.target.checked)}
        />
        Sou sócio (–50%)
      </label>
      <div className="flex justify-end gap-2 pt-2">
        <Button onClick={onNext}>Continuar →</Button>
      </div>
    </div>
  );
}

function StepPayment({
  tier,
  total,
  paymentMethod,
  onPaymentMethod,
  submitting,
  onBack,
  onConfirm,
}: {
  tier: PriceTier;
  total: number;
  paymentMethod: PaymentMethod;
  onPaymentMethod: (p: PaymentMethod) => void;
  submitting: boolean;
  onBack: () => void;
  onConfirm: () => void;
}) {
  const options: { value: PaymentMethod; name: string; sub: string; disabled?: boolean }[] = [
    { value: "MBWAY", name: "MBWAY", sub: "Notificação no telemóvel" },
    { value: "CARD", name: "Cartão", sub: "Visa, Mastercard" },
    { value: "CASH", name: "Dinheiro à porta", sub: "Só sócios", disabled: true },
  ];

  return (
    <div className="space-y-4">
      <div className="space-y-2">
        {options.map((p) => {
          const on = p.value === paymentMethod;
          return (
            <label
              key={p.value}
              className={cn(
                "flex items-start gap-3 rounded-md border px-3 py-2.5",
                p.disabled ? "opacity-50" : "cursor-pointer",
                on && "border-primary bg-primary/5",
              )}
            >
              <input
                type="radio"
                name="payment"
                className="sr-only"
                disabled={p.disabled}
                checked={on}
                onChange={() => onPaymentMethod(p.value)}
              />
              <span
                className={cn(
                  "size-4 mt-0.5 rounded-full border-2 grid place-items-center",
                  on ? "border-primary" : "border-input",
                )}
              >
                {on && <span className="size-2 rounded-full bg-primary" />}
              </span>
              <div className="flex-1">
                <div className="text-sm font-medium">{p.name}</div>
                <div className="text-[11px] text-muted-foreground">{p.sub}</div>
              </div>
            </label>
          );
        })}
      </div>
      <div className="rounded-md border bg-muted/50 px-3 py-2.5 text-xs space-y-1">
        <div className="flex justify-between">
          <span className="text-muted-foreground">Lugar</span>
          <span>{tier.name}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-muted-foreground">A pagar</span>
          <span className="font-semibold">{total},00 €</span>
        </div>
      </div>
      <div className="flex justify-between gap-2 pt-2">
        <Button variant="ghost" onClick={onBack} disabled={submitting}>
          ← Voltar
        </Button>
        <Button onClick={onConfirm} disabled={submitting}>
          {submitting ? (
            <>
              <Loader2 className="size-4 animate-spin" />A processar…
            </>
          ) : (
            <>Confirmar pagamento</>
          )}
        </Button>
      </div>
    </div>
  );
}

function StepConfirmation({
  tier,
  ticketCode,
  onClose,
}: {
  tier: PriceTier;
  ticketCode: string;
  onClose: () => void;
}) {
  return (
    <div className="space-y-5 text-center">
      <div className="mx-auto rounded-md border bg-white p-3 w-fit">
        <QRCodeSVG value={ticketCode} size={156} />
      </div>
      <div className="space-y-1">
        <StatusBadge status="PAID" />
        <div className="font-mono text-[11px] text-muted-foreground mt-2">
          {ticketCode.slice(0, 8)}…{ticketCode.slice(-4)}
        </div>
        <div className="text-sm">{tier.name}</div>
      </div>
      <div className="flex flex-col gap-2">
        <Button asChild>
          <Link to="/tickets/mine">Ver em "Os meus bilhetes"</Link>
        </Button>
        <Button variant="outline" onClick={onClose}>
          Fechar
        </Button>
      </div>
    </div>
  );
}
