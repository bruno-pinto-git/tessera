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
import { Input } from "@/components/ui/input";
import { StatusBadge } from "@/components/ui/status-badge";
import type { MatchDetail, PriceTier } from "../mockMatches";
import { cn } from "@/lib/utils";
import { createTicket, getTicket, payTicket, type Ticket } from "@/api/ticketApi";
import { ApiError } from "@/api/client";

type Step = 1 | 2 | 3;
type PaymentMethod = "MBWAY" | "CARD" | "CASH";

interface PurchaseModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  match: MatchDetail;
}

/**
 * 3-step purchase flow wired to the real ticket-service:
 *
 *   1. choose a price tier (+ "sou sócio" toggle)
 *   2. pick payment method, type phone number (MBWAY), confirm
 *      → POST /tickets → POST /tickets/{id}/pay
 *      → for MBWAY: poll GET /tickets/{id} until status === PAID
 *   3. show the real QR + ticket id, plus shortcut links
 */
export function PurchaseModal({ open, onOpenChange, match }: PurchaseModalProps) {
  const [step, setStep] = useState<Step>(1);
  const [tier, setTier] = useState<PriceTier>(match.tiers[0]);
  const [supporter, setSupporter] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("MBWAY");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [awaitingMbway, setAwaitingMbway] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [ticket, setTicket] = useState<Ticket | null>(null);

  const total = supporter ? tier.socio : tier.normal;

  const reset = () => {
    setStep(1);
    setTier(match.tiers[0]);
    setSupporter(false);
    setPaymentMethod("MBWAY");
    setPhoneNumber("");
    setSubmitting(false);
    setAwaitingMbway(false);
    setError(null);
    setTicket(null);
  };

  async function confirmPayment() {
    setError(null);

    if (paymentMethod === "MBWAY" && !isValidPhone(phoneNumber)) {
      setError("Telemóvel inválido. Usa o formato 351XXXXXXXXX.");
      return;
    }

    setSubmitting(true);
    try {
      const created = await createTicket({ eventId: match.eventId, supporter });
      const paid = await payTicket(created.id, {
        paymentMethod,
        phoneNumber: paymentMethod === "MBWAY" ? normalisePhone(phoneNumber) : undefined,
      });

      if (paid.status === "PAID") {
        setTicket(paid);
        setStep(3);
      } else {
        // MBWAY: ticket is PENDING until the gateway pushes the customer's
        // phone and they accept. We poll the ticket every 2s.
        setAwaitingMbway(true);
        const finalTicket = await pollUntilResolved(paid.id);
        if (finalTicket.status === "PAID") {
          setTicket(finalTicket);
          setStep(3);
        } else {
          setError("O pagamento expirou ou foi recusado. Tenta de novo.");
        }
      }
    } catch (e) {
      setError(messageFromError(e));
    } finally {
      setSubmitting(false);
      setAwaitingMbway(false);
    }
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
            phoneNumber={phoneNumber}
            onPhoneNumber={setPhoneNumber}
            submitting={submitting}
            awaitingMbway={awaitingMbway}
            error={error}
            onBack={() => setStep(1)}
            onConfirm={confirmPayment}
          />
        )}

        {step === 3 && ticket && (
          <StepConfirmation
            tier={tier}
            ticket={ticket}
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
  phoneNumber,
  onPhoneNumber,
  submitting,
  awaitingMbway,
  error,
  onBack,
  onConfirm,
}: {
  tier: PriceTier;
  total: number;
  paymentMethod: PaymentMethod;
  onPaymentMethod: (p: PaymentMethod) => void;
  phoneNumber: string;
  onPhoneNumber: (v: string) => void;
  submitting: boolean;
  awaitingMbway: boolean;
  error: string | null;
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

      {paymentMethod === "MBWAY" && (
        <div className="space-y-1.5">
          <label htmlFor="mbway-phone" className="text-xs text-muted-foreground">
            Telemóvel MB WAY
          </label>
          <Input
            id="mbway-phone"
            inputMode="tel"
            placeholder="351912345678"
            value={phoneNumber}
            onChange={(e) => onPhoneNumber(e.target.value)}
            disabled={submitting}
          />
          <p className="text-[11px] text-muted-foreground">
            Formato internacional sem espaços. A notificação chega ao telemóvel
            registado no MB WAY.
          </p>
        </div>
      )}

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

      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}

      <div className="flex justify-between gap-2 pt-2">
        <Button variant="ghost" onClick={onBack} disabled={submitting}>
          ← Voltar
        </Button>
        <Button onClick={onConfirm} disabled={submitting}>
          {submitting ? (
            <>
              <Loader2 className="size-4 animate-spin" />
              {awaitingMbway ? "A aguardar MB WAY…" : "A processar…"}
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
  ticket,
  onClose,
}: {
  tier: PriceTier;
  ticket: Ticket;
  onClose: () => void;
}) {
  return (
    <div className="space-y-5 text-center">
      <div className="mx-auto rounded-md border bg-white p-3 w-fit">
        <QRCodeSVG value={ticket.code} size={156} />
      </div>
      <div className="space-y-1">
        <StatusBadge status={ticket.status} />
        <div className="font-mono text-[11px] text-muted-foreground mt-2">
          {ticket.code.slice(0, 8)}…{ticket.code.slice(-4)}
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

// ───── helpers ─────

const POLL_INTERVAL_MS = 2_000;
const POLL_TIMEOUT_MS = 180_000;

async function pollUntilResolved(ticketId: number): Promise<Ticket> {
  const deadline = Date.now() + POLL_TIMEOUT_MS;
  let latest = await getTicket(ticketId);
  while (latest.status === "PENDING" && Date.now() < deadline) {
    await sleep(POLL_INTERVAL_MS);
    latest = await getTicket(ticketId);
  }
  return latest;
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function isValidPhone(phone: string): boolean {
  const normalised = normalisePhone(phone);
  return /^\d{9,15}$/.test(normalised);
}

function normalisePhone(phone: string): string {
  return phone.replace(/[\s+]/g, "");
}

function messageFromError(e: unknown): string {
  if (e instanceof ApiError) {
    return `Erro ${e.status}: ${e.statusText}`;
  }
  if (e instanceof Error) {
    return e.message;
  }
  return "Erro desconhecido.";
}
