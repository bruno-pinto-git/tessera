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
import { deleteUser, type UserSummary } from "@/api/usersApi";

interface DeleteUserDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  user: UserSummary | null;
  onDeleted: () => void;
}

export function DeleteUserDialog({ open, onOpenChange, user, onDeleted }: DeleteUserDialogProps) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleConfirm() {
    if (!user) return;
    setSubmitting(true);
    setError(null);
    try {
      await deleteUser(user.id);
      onDeleted();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        // Already gone — treat as success.
        onDeleted();
        onOpenChange(false);
      } else {
        setError(err instanceof Error ? err.message : "Erro inesperado");
      }
    } finally {
      setSubmitting(false);
    }
  }

  const display =
    [user?.firstName, user?.lastName].filter(Boolean).join(" ") || user?.username || user?.id;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Eliminar utilizador</DialogTitle>
          <DialogDescription>
            Tens a certeza que queres eliminar{" "}
            <span className="font-medium text-foreground">{display}</span>? A operação é permanente
            em Keycloak. Quaisquer atribuições a clubes deste utilizador também desaparecem.
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
