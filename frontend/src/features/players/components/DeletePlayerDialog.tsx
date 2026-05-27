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
import { deletePlayer, type Player } from "../api/playersApi";

interface DeletePlayerDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  player: Player | null;
  onDeleted: () => void;
}

export function DeletePlayerDialog({ open, onOpenChange, player, onDeleted }: DeletePlayerDialogProps) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleConfirm() {
    if (!player) return;
    setSubmitting(true);
    setError(null);
    try {
      await deletePlayer(player.id);
      onDeleted();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError && err.status === 403) {
        setError("Não tens permissões para gerir esta equipa.");
      } else {
        setError(err instanceof Error ? err.message : "Erro inesperado.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  const name = player ? `${player.firstName} ${player.lastName}` : "";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Eliminar jogador</DialogTitle>
          <DialogDescription>
            Eliminar <span className="font-medium text-foreground">{name}</span>? O registo é
            arquivado e pode ser restaurado.
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