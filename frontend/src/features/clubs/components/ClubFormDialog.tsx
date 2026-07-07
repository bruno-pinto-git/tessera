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
import { createClub, updateClub, type Club } from "../api/clubsApi";

interface ClubFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  club?: Club | null;
  onSaved: () => void;
}

interface FormState {
  name: string;
  foundedYear: string;
  crestUrl: string;
}

const EMPTY: FormState = { name: "", foundedYear: "", crestUrl: "" };

export function ClubFormDialog({ open, onOpenChange, club, onSaved }: ClubFormDialogProps) {
  const isEdit = !!club;
  const [form, setForm] = useState<FormState>(EMPTY);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setError(null);
    setForm(
      club
        ? {
            name: club.name,
            foundedYear: club.foundedYear?.toString() ?? "",
            crestUrl: club.crestUrl ?? "",
          }
        : EMPTY,
    );
  }, [open, club]);

  function validate(): string | null {
    const name = form.name.trim();
    if (name.length < 2) return "O nome deve ter pelo menos 2 caracteres.";
    if (name.length > 200) return "O nome não pode exceder 200 caracteres.";
    if (form.foundedYear) {
      const y = Number(form.foundedYear);
      if (!Number.isInteger(y) || y < 1850 || y > 2100) {
        return "Ano de fundação deve estar entre 1850 e 2100.";
      }
    }
    if (form.crestUrl && !/^https?:\/\//.test(form.crestUrl.trim())) {
      return "URL do emblema deve começar por http:// ou https://.";
    }
    return null;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const payload = {
        name: form.name.trim(),
        foundedYear: form.foundedYear ? Number(form.foundedYear) : null,
        crestUrl: form.crestUrl.trim() || null,
      };
      if (isEdit && club) {
        await updateClub(club.id, payload);
      } else {
        await createClub(payload);
      }
      onSaved();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(formatApiError(err));
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
        <form onSubmit={handleSubmit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>{isEdit ? "Editar clube" : "Novo clube"}</DialogTitle>
            <DialogDescription>
              {isEdit
                ? "Atualiza os dados do clube. Apenas os campos preenchidos são alterados."
                : "Cria um novo clube na plataforma."}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3">
            <div className="space-y-1.5">
              <Label htmlFor="name">Nome</Label>
              <Input
                id="name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="SU 1.º Dezembro"
                required
                autoFocus
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="foundedYear">Ano de fundação</Label>
              <Input
                id="foundedYear"
                type="number"
                inputMode="numeric"
                min={1850}
                max={2100}
                value={form.foundedYear}
                onChange={(e) => setForm({ ...form, foundedYear: e.target.value })}
                placeholder="1953"
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="crestUrl">URL do emblema</Label>
              <Input
                id="crestUrl"
                type="url"
                value={form.crestUrl}
                onChange={(e) => setForm({ ...form, crestUrl: e.target.value })}
                placeholder="https://exemplo.com/crest.png"
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

function formatApiError(err: ApiError): string {
  if (err.status === 401) return "Sessão expirada. Por favor faz login outra vez.";
  if (err.status === 403) return "Não tens permissões para esta operação.";
  if (err.status === 409) return "Já existe um clube com este nome.";
  if (err.status === 400 && err.body && typeof err.body === "object" && "detail" in err.body) {
    return String((err.body as { detail: unknown }).detail);
  }
  return err.message;
}
