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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/api/client";
import { createEvent } from "@/features/events/api/eventsApi";

interface OpenBoxOfficeDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  matchId: number | null;
  defaultLabel: string;
  onCreated: () => void;
}

export function OpenBoxOfficeDialog({
  open,
  onOpenChange,
  matchId,
  defaultLabel,
  onCreated,
}: OpenBoxOfficeDialogProps) {
  const [name, setName] = useState(defaultLabel);
  const [priceNormal, setPriceNormal] = useState("8.00");
  const [priceSupporter, setPriceSupporter] = useState("4.00");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setName(defaultLabel);
    setPriceNormal("8.00");
    setPriceSupporter("4.00");
    setError(null);
  }, [open, defaultLabel]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (matchId == null) return;
    const pn = Number(priceNormal);
    const ps = Number(priceSupporter);
    if (!Number.isFinite(pn) || pn < 0) {
      setError("Preço normal inválido.");
      return;
    }
    if (!Number.isFinite(ps) || ps < 0) {
      setError("Preço de sócio inválido.");
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await createEvent({
        name: name.trim() || defaultLabel,
        matchLabel: defaultLabel,
        matchId,
        priceNormal: pn.toFixed(2),
        priceSupporter: ps.toFixed(2),
        status: "PUBLISHED",
      });
      onCreated();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
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
        <form onSubmit={handleSubmit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Abrir bilheteira</DialogTitle>
            <DialogDescription>
              Cria um evento PUBLISHED em ticket-service para este jogo. A partir desse momento o
              jogo aparece em <code>/events</code> e os adeptos podem comprar.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label htmlFor="ev-name">Designação</Label>
              <Input
                id="ev-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={defaultLabel}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="ev-normal">Preço normal (€)</Label>
                <Input
                  id="ev-normal"
                  type="number"
                  inputMode="decimal"
                  step="0.50"
                  min={0}
                  value={priceNormal}
                  onChange={(e) => setPriceNormal(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="ev-supporter">Preço sócio (€)</Label>
                <Input
                  id="ev-supporter"
                  type="number"
                  inputMode="decimal"
                  step="0.50"
                  min={0}
                  value={priceSupporter}
                  onChange={(e) => setPriceSupporter(e.target.value)}
                  required
                />
              </div>
            </div>
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
            <Button type="submit" disabled={submitting || matchId == null}>
              {submitting ? "A abrir…" : "Abrir bilheteira"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
