import { useCreateTicket } from "../hooks/useCreateTicket";
import { TicketCard } from "../components/TicketCard";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Loader2 } from "lucide-react";

// Temporary seed: until the events catalog UI is built, the dev sandbox
// uses event id 1 (created by the smoke test `09-tickets.http`).
const SEED_EVENT_ID = 1;

export function TicketsPage() {
  const { ticket, loading, error, generate } = useCreateTicket();

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <header className="space-y-1">
        <h1 className="text-2xl font-bold tracking-tight">Bilhetes — sandbox</h1>
        <p className="text-sm text-muted-foreground">
          Página temporária para testar a criação de bilhetes contra o ticket-service. Substituir
          pelo écran de compra real assim que houver UI para escolher um evento.
        </p>
      </header>

      <Card>
        <CardHeader>
          <CardTitle>Gerar bilhete de teste</CardTitle>
          <CardDescription>
            Cria um bilhete em <span className="font-mono">PENDING</span> para o evento{" "}
            <span className="font-mono">#{SEED_EVENT_ID}</span>.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Button onClick={() => generate(SEED_EVENT_ID, false)} disabled={loading}>
            {loading ? (
              <>
                <Loader2 className="size-4 animate-spin" />A gerar...
              </>
            ) : (
              "Gerar bilhete"
            )}
          </Button>
          {error && (
            <p className="text-sm text-destructive" role="alert">
              {error}
            </p>
          )}
        </CardContent>
      </Card>

      {ticket && <TicketCard ticket={ticket} />}
    </div>
  );
}
