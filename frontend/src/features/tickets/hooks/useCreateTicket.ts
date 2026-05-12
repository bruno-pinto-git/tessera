import { useState } from "react";
import { createTicket } from "@/api/ticketApi";
import { ApiError } from "@/api/client";
import type { Ticket } from "../types";

export function useCreateTicket() {
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function generate(eventId: number, supporter: boolean) {
    setLoading(true);
    setError(null);
    try {
      const result = await createTicket({ eventId, supporter });
      setTicket(result);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(`${err.status} — ${err.statusText}`);
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError("Erro desconhecido");
      }
    } finally {
      setLoading(false);
    }
  }

  return { ticket, loading, error, generate };
}
