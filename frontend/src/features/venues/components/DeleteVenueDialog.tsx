import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ApiError } from "@/api/client";
import { deleteVenue, type Venue } from "../api/venuesApi";

interface DeleteVenueDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  venue: Venue | null;
  onDeleted: () => void;
}

export function DeleteVenueDialog({
  open,
  onOpenChange,
  venue,
  onDeleted,
}: DeleteVenueDialogProps) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleConfirm() {
    if (!venue) return;
    setSubmitting(true);
    setError(null);
    try {
      await deleteVenue(venue.id);
      onDeleted();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        onDeleted();
        onOpenChange(false);
      } else if (err instanceof ApiError && err.status === 409) {
        setError("Este estádio está a ser usado por jogos. Reatribui os jogos primeiro.");
      } else {
        setError(err instanceof Error ? err.message : "Erro inesperado");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Eliminar estádio</DialogTitle>
          <DialogDescription>
            Tens a certeza que queres eliminar{" "}
            <span className="font-medium text-foreground">{venue?.name}</span>? O registo é
            arquivado mas pode ser restaurado por um administrador.
          </DialogDescription>
        </DialogHeader>

        {error && <p className="text-sm text-destructive">{error}</p>}

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={submitting}
          >
            Cancelar
          </Button>
          <Button type="button" variant="destructive" onClick={handleConfirm} disabled={submitting}>
            {submitting ? "A eliminar…" : "Eliminar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
