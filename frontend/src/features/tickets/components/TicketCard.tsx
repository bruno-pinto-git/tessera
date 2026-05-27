import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StatusBadge } from "@/components/ui/status-badge";
import type { Ticket } from "../types";

interface TicketCardProps {
  ticket: Ticket;
}

function formatPrice(price: Ticket["price"]) {
  const num = typeof price === "number" ? price : parseFloat(price);
  if (Number.isNaN(num)) return String(price);
  return num.toLocaleString("pt-PT", { style: "currency", currency: "EUR" });
}

export function TicketCard({ ticket }: TicketCardProps) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-2">
        <CardTitle className="text-base">Bilhete #{ticket.id}</CardTitle>
        <StatusBadge status={ticket.status} />
      </CardHeader>
      <CardContent className="space-y-3 text-sm">
        <Row label="Código">
          <code className="font-mono text-xs">{ticket.code}</code>
        </Row>
        <Row label="Preço">{formatPrice(ticket.price)}</Row>
        <Row label="Criado">{new Date(ticket.createdAt).toLocaleString("pt-PT")}</Row>
        {ticket.paymentDate && (
          <Row label="Pago em">{new Date(ticket.paymentDate).toLocaleString("pt-PT")}</Row>
        )}
        {ticket.validationDate && (
          <Row label="Validado em">{new Date(ticket.validationDate).toLocaleString("pt-PT")}</Row>
        )}
      </CardContent>
    </Card>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right">{children}</span>
    </div>
  );
}
