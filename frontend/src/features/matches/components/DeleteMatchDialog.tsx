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
import { deleteMatch, type Match } from "../api/matchesApi";

interface DeleteMatchDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  match: Match | null;
  label: string;
  onDeleted: () => void;
}

export function DeleteMatchDialog({
  open,
  onOpenChange,
  match,
  label,
  onDeleted,
}: DeleteMatchDialogProps) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleConfirm() {
    if (!match) return;
    setSubmitting(true);
    setError(null);
    try {
      await deleteMatch(match.id);
      onDeleted();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        onDeleted();
        onOpenChange(false);
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
          <DialogTitle>Eliminar jogo</DialogTitle>
          <DialogDescription>
            Tens a certeza que queres eliminar{" "}
            <span className="font-medium text-foreground">{label}</span>? O registo é arquivado
            (soft delete).
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
