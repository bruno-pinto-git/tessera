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
import { deleteClub, type Club } from "../api/clubsApi";

interface DeleteClubDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  club: Club | null;
  onDeleted: () => void;
}

export function DeleteClubDialog({ open, onOpenChange, club, onDeleted }: DeleteClubDialogProps) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleConfirm() {
    if (!club) return;
    setSubmitting(true);
    setError(null);
    try {
      await deleteClub(club.id);
      onDeleted();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError("Este clube tem equipas ou jogos associados e não pode ser eliminado.");
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError("Erro inesperado.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Eliminar clube</DialogTitle>
          <DialogDescription>
            Tens a certeza que queres eliminar{" "}
            <span className="font-medium text-foreground">{club?.name}</span>? O registo é arquivado
            mas pode ser restaurado por um administrador.
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
