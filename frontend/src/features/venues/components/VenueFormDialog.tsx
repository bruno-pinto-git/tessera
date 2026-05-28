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
import { createVenue, updateVenue, type Venue } from "../api/venuesApi";

interface VenueFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  venue?: Venue | null;
  onSaved: () => void;
}

interface FormState {
  name: string;
  capacity: string;
  address: string;
}

const EMPTY: FormState = { name: "", capacity: "", address: "" };

export function VenueFormDialog({ open, onOpenChange, venue, onSaved }: VenueFormDialogProps) {
  const isEdit = !!venue;
  const [form, setForm] = useState<FormState>(EMPTY);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setError(null);
    setForm(
      venue
        ? {
            name: venue.name,
            capacity: venue.capacity.toString(),
            address: venue.address ?? "",
          }
        : EMPTY,
    );
  }, [open, venue]);

  function validate(): string | null {
    const name = form.name.trim();
    if (name.length < 2) return "O nome deve ter pelo menos 2 caracteres.";
    if (name.length > 200) return "O nome não pode exceder 200 caracteres.";
    const cap = Number(form.capacity);
    if (!Number.isInteger(cap) || cap < 0 || cap > 200000) {
      return "Capacidade deve estar entre 0 e 200000.";
    }
    if (form.address.length > 500) return "Morada não pode exceder 500 caracteres.";
    return null;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const v = validate();
    if (v) {
      setError(v);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const payload = {
        name: form.name.trim(),
        capacity: Number(form.capacity),
        address: form.address.trim() || null,
      };
      if (isEdit && venue) {
        await updateVenue(venue.id, payload);
      } else {
        await createVenue(payload);
      }
      onSaved();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) setError("Já existe um estádio com este nome.");
        else if (err.status === 403) setError("Não tens permissões para esta operação.");
        else if (
          err.status === 400 &&
          err.body &&
          typeof err.body === "object" &&
          "detail" in err.body
        ) {
          setError(String((err.body as { detail: unknown }).detail));
        } else {
          setError(err.message);
        }
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
            <DialogTitle>{isEdit ? "Editar estádio" : "Novo estádio"}</DialogTitle>
            <DialogDescription>
              {isEdit
                ? "Atualiza os dados do estádio."
                : "Adiciona um estádio à plataforma. Pode ser usado por qualquer clube."}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label htmlFor="v-name">Nome</Label>
              <Input
                id="v-name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="Estádio do Bonfim"
                required
                autoFocus
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="v-capacity">Capacidade</Label>
              <Input
                id="v-capacity"
                type="number"
                inputMode="numeric"
                min={0}
                max={200000}
                value={form.capacity}
                onChange={(e) => setForm({ ...form, capacity: e.target.value })}
                placeholder="6 500"
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="v-address">Morada</Label>
              <Input
                id="v-address"
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
                placeholder="Rua dos Casquilhos, Setúbal"
              />
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
            <Button type="submit" disabled={submitting}>
              {submitting ? "A guardar…" : isEdit ? "Guardar" : "Criar"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
