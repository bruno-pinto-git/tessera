import { Card } from "@/components/ui/card";
import { PaidTicketCard } from "../components/PaidTicketCard";
import { PendingTicketCard } from "../components/PendingTicketCard";
import { PastTicketRow } from "../components/PastTicketRow";
import { MOCK_PAID, MOCK_PENDING, MOCK_PAST } from "../mockMyTickets";
import { cn } from "@/lib/utils";

/**
 * "Os meus bilhetes" — grouped by status. Three sections:
 *
 *  - Prontos a usar (PAID) — large QR + perforated stub
 *  - Aguarda pagamento (PENDING) — countdown + "concluir pagamento" CTA
 *  - Histórico (VALIDATED / CANCELLED) — rows, faded
 *
 * TODO: swap the three constants for the result of `listMyTickets()`
 * partitioned by status. Keep the visual structure.
 */
export function MyTicketsPage() {
  const paid = MOCK_PAID;
  const pending = MOCK_PENDING;
  const past = MOCK_PAST;

  return (
    <div className="space-y-10">
      <header className="space-y-2">
        <h1 className="text-4xl font-bold tracking-tight">Os meus bilhetes</h1>
        <p className="text-sm text-muted-foreground max-w-2xl">
          Apresenta o QR à entrada do estádio. Faz uma{" "}
          <span className="text-foreground">captura de ecrã</span> se vais para uma zona sem rede —
          o QR funciona offline.
        </p>
      </header>

      <Group
        title="Prontos a usar"
        sub={`${paid.length} bilhete${paid.length === 1 ? "" : "s"} pago${paid.length === 1 ? "" : "s"}`}
      >
        {paid.length === 0 ? (
          <EmptyState text="Ainda não tens bilhetes pagos. Compra um nos próximos jogos." />
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {paid.map((t) => (
              <PaidTicketCard key={t.id} ticket={t} />
            ))}
          </div>
        )}
      </Group>

      {pending.length > 0 && (
        <Group title="Aguarda pagamento" sub="Conclui em 15 min para garantir o lugar">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {pending.map((t) => (
              <PendingTicketCard key={t.id} ticket={t} />
            ))}
          </div>
        </Group>
      )}

      <Group title="Histórico" sub="Bilhetes já usados ou cancelados" muted>
        {past.length === 0 ? (
          <EmptyState text="Ainda sem histórico." />
        ) : (
          <Card>
            <ul className="divide-y">
              {past.map((t) => (
                <PastTicketRow key={t.id} ticket={t} />
              ))}
            </ul>
          </Card>
        )}
      </Group>
    </div>
  );
}

function Group({
  title,
  sub,
  children,
  muted,
}: {
  title: string;
  sub?: string;
  children: React.ReactNode;
  muted?: boolean;
}) {
  return (
    <section className="space-y-4">
      <div>
        <h2
          className={cn("text-lg font-semibold tracking-tight", muted && "text-muted-foreground")}
        >
          {title}
        </h2>
        {sub && <p className="text-sm text-muted-foreground">{sub}</p>}
      </div>
      {children}
    </section>
  );
}

function EmptyState({ text }: { text: string }) {
  return (
    <Card>
      <p className="px-6 py-10 text-sm text-muted-foreground text-center">{text}</p>
    </Card>
  );
}
