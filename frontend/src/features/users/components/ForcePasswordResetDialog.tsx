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
import { updateUser, type UserSummary } from "@/api/usersApi";

interface ForcePasswordResetDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  user: UserSummary | null;
  onDone: () => void;
}

/**
 * Confirmation for forcing a password change on next login. Adds the
 * Keycloak `UPDATE_PASSWORD` required action to the user — they keep their
 * current password until they log in, then must choose a new one.
 */
export function ForcePasswordResetDialog({
  open,
  onOpenChange,
  user,
  onDone,
}: ForcePasswordResetDialogProps) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleConfirm() {
    if (!user) return;
    setSubmitting(true);
    setError(null);
    try {
      await updateUser(user.id, { forcePasswordReset: true });
      onDone();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setError("Utilizador já não existe.");
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
          <DialogTitle>Forçar troca de password</DialogTitle>
          <DialogDescription>
            <span className="font-medium text-foreground">{display}</span> terá de escolher uma nova
            password no próximo login. A password atual continua válida até lá.
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
          <Button type="button" onClick={handleConfirm} disabled={submitting}>
            {submitting ? "A aplicar…" : "Forçar troca"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
