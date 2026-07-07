import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/api/client";
import {
  createTeam,
  TEAM_CATEGORIES,
  teamCategoryLabel,
  updateTeam,
  type Team,
  type TeamCategory,
} from "../api/teamsApi";

interface TeamFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  clubId: number;
  team?: Team | null;
  onSaved: () => void;
}

export function TeamFormDialog({ open, onOpenChange, clubId, team, onSaved }: TeamFormDialogProps) {
  const isEdit = !!team;
  const [category, setCategory] = useState<TeamCategory>("SENIOR_M");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setError(null);
    setCategory(team?.category ?? "SENIOR_M");
  }, [open, team]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      if (isEdit && team) {
        await updateTeam(team.id, { category });
      } else {
        await createTeam(clubId, { category });
      }
      onSaved();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) setError("Já existe uma equipa desta categoria neste clube.");
        else if (err.status === 403) setError("Não tens permissões para gerir este clube.");
        else setError(err.message);
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
        <form onSubmit={handleSubmit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>{isEdit ? "Editar equipa" : "Nova equipa"}</DialogTitle>
            <DialogDescription>
              Cada clube tem no máximo uma equipa por categoria.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-1.5">
            <Label htmlFor="team-category">Categoria</Label>
            <select
              id="team-category"
              value={category}
              onChange={(e) => setCategory(e.target.value as TeamCategory)}
              className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              {TEAM_CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {teamCategoryLabel(c)}
                </option>
              ))}
            </select>
          </div>

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
            <Button type="submit" disabled={submitting}>
              {submitting ? "A guardar…" : isEdit ? "Guardar" : "Criar"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
