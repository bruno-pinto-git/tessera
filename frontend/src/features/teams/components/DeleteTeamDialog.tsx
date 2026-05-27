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
import { deleteTeam, teamCategoryLabel, type Team } from "../api/teamsApi";

interface DeleteTeamDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  team: Team | null;
  onDeleted: () => void;
}

export function DeleteTeamDialog({ open, onOpenChange, team, onDeleted }: DeleteTeamDialogProps) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleConfirm() {
    if (!team) return;
    setSubmitting(true);
    setError(null);
    try {
      await deleteTeam(team.id);
      onDeleted();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError("A equipa tem dados associados e não pode ser eliminada já.");
      } else if (err instanceof ApiError && err.status === 403) {
        setError("Não tens permissões para gerir este clube.");
      } else {
        setError(err instanceof Error ? err.message : "Erro inesperado.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Eliminar equipa</DialogTitle>
          <DialogDescription>
            Eliminar a equipa{" "}
            <span className="font-medium text-foreground">
              {team ? teamCategoryLabel(team.category) : ""}
            </span>
            ? O registo é arquivado e pode ser restaurado.
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