import { cn } from "@/lib/utils";

/**
 * Renders a status pill with the design-system semantic colour and the
 * Portuguese user-facing label for each backend enum value. Use this
 * everywhere a ticket / match status appears so the visual treatment
 * stays consistent.
 */
type DomainStatus =
  | "PENDING"
  | "PAID"
  | "VALIDATED"
  | "CANCELLED"
  | "SCHEDULED"
  | "LIVE"
  | "FINISHED";

const LABEL: Record<DomainStatus, string> = {
  PENDING: "Pendente",
  PAID: "Pago",
  VALIDATED: "Validado",
  CANCELLED: "Cancelado",
  SCHEDULED: "Agendado",
  LIVE: "Ao vivo",
  FINISHED: "Terminado",
};

const STYLE: Record<DomainStatus, string> = {
  PENDING: "bg-status-pending/15 text-status-pending",
  PAID: "bg-status-paid/15 text-status-paid",
  VALIDATED: "bg-status-validated/15 text-status-validated",
  CANCELLED: "bg-status-cancelled/15 text-status-cancelled",
  SCHEDULED: "bg-secondary text-muted-foreground",
  LIVE: "bg-status-paid/15 text-status-paid",
  FINISHED: "bg-status-validated/15 text-status-validated",
};

interface StatusBadgeProps {
  status: DomainStatus | string;
  size?: "sm" | "lg";
  className?: string;
}

export function StatusBadge({ status, size = "sm", className }: StatusBadgeProps) {
  const key = status as DomainStatus;
  const label = LABEL[key] ?? status;
  const variant = STYLE[key] ?? "bg-secondary text-muted-foreground";
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-md font-semibold uppercase tracking-wide",
        size === "lg" ? "px-3 py-1 text-xs" : "px-2 py-0.5 text-[11px]",
        variant,
        className,
      )}
    >
      {label}
    </span>
  );
}
