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
import {
  createPlayer,
  DOMINANT_FEET,
  dominantFootLabel,
  POSITIONS,
  positionLabel,
  STATUSES,
  statusLabel,
  updatePlayer,
  type DominantFoot,
  type Player,
  type PlayerPosition,
  type PlayerStatus,
} from "../api/playersApi";

interface PlayerFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  teamId: number;
  /** Pass a player to edit; omit for create. */
  player?: Player | null;
  onSaved: () => void;
}

interface FormState {
  firstName: string;
  lastName: string;
  birthdate: string;
  nationality: string;
  position: PlayerPosition;
  shirtNumber: string;
  photoUrl: string;
  dominantFoot: "" | DominantFoot;
  height: string;
  weight: string;
  status: PlayerStatus;
}

const EMPTY: FormState = {
  firstName: "",
  lastName: "",
  birthdate: "",
  nationality: "",
  position: "MF",
  shirtNumber: "",
  photoUrl: "",
  dominantFoot: "",
  height: "",
  weight: "",
  status: "ACTIVE",
};

export function PlayerFormDialog({
  open,
  onOpenChange,
  teamId,
  player,
  onSaved,
}: PlayerFormDialogProps) {
  const isEdit = !!player;
  const [form, setForm] = useState<FormState>(EMPTY);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setError(null);
    setForm(
      player
        ? {
            firstName: player.firstName,
            lastName: player.lastName,
            birthdate: player.birthdate ?? "",
            nationality: player.nationality ?? "",
            position: player.position,
            shirtNumber: player.shirtNumber?.toString() ?? "",
            photoUrl: player.photoUrl ?? "",
            dominantFoot: player.dominantFoot ?? "",
            height: player.height?.toString() ?? "",
            weight: player.weight?.toString() ?? "",
            status: player.status,
          }
        : EMPTY,
    );
  }, [open, player]);

  function validate(): string | null {
    if (form.firstName.trim().length < 1) return "Primeiro nome obrigatório.";
    if (form.lastName.trim().length < 1) return "Apelido obrigatório.";
    if (form.nationality && !/^[A-Z]{3}$/.test(form.nationality.trim())) {
      return "Nacionalidade: código ISO 3166-1 alfa-3 (ex: PRT, BRA).";
    }
    if (form.shirtNumber) {
      const n = Number(form.shirtNumber);
      if (!Number.isInteger(n) || n < 1 || n > 99) return "Número de camisola entre 1 e 99.";
    }
    if (form.height) {
      const n = Number(form.height);
      if (!Number.isInteger(n) || n < 100 || n > 250) return "Altura entre 100 e 250 cm.";
    }
    if (form.weight) {
      const n = Number(form.weight);
      if (!Number.isInteger(n) || n < 30 || n > 200) return "Peso entre 30 e 200 kg.";
    }
    return null;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const err = validate();
    if (err) {
      setError(err);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const payload = {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        birthdate: form.birthdate || null,
        nationality: form.nationality.trim() || null,
        position: form.position,
        shirtNumber: form.shirtNumber ? Number(form.shirtNumber) : null,
        photoUrl: form.photoUrl.trim() || null,
        dominantFoot: form.dominantFoot || null,
        height: form.height ? Number(form.height) : null,
        weight: form.weight ? Number(form.weight) : null,
        status: form.status,
      };
      if (isEdit && player) {
        await updatePlayer(player.id, payload);
      } else {
        await createPlayer(teamId, payload);
      }
      onSaved();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.status === 409) setError("Já existe um jogador com este número de camisola.");
        else if (err.status === 403) setError("Não tens permissões para gerir esta equipa.");
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

  const selectClass =
    "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <form onSubmit={handleSubmit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>{isEdit ? "Editar jogador" : "Novo jogador"}</DialogTitle>
            <DialogDescription>Os campos opcionais podem ser deixados em branco.</DialogDescription>
          </DialogHeader>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="firstName">Primeiro nome</Label>
              <Input
                id="firstName"
                value={form.firstName}
                onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                required
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="lastName">Apelido</Label>
              <Input
                id="lastName"
                value={form.lastName}
                onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                required
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="position">Posição</Label>
              <select
                id="position"
                value={form.position}
                onChange={(e) => setForm({ ...form, position: e.target.value as PlayerPosition })}
                className={selectClass}
              >
                {POSITIONS.map((p) => (
                  <option key={p} value={p}>
                    {positionLabel(p)}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="shirtNumber">Nº camisola</Label>
              <Input
                id="shirtNumber"
                type="number"
                min={1}
                max={99}
                value={form.shirtNumber}
                onChange={(e) => setForm({ ...form, shirtNumber: e.target.value })}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="birthdate">Data de nascimento</Label>
              <Input
                id="birthdate"
                type="date"
                value={form.birthdate}
                onChange={(e) => setForm({ ...form, birthdate: e.target.value })}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="nationality">Nacionalidade (alfa-3)</Label>
              <Input
                id="nationality"
                value={form.nationality}
                maxLength={3}
                onChange={(e) => setForm({ ...form, nationality: e.target.value.toUpperCase() })}
                placeholder="PRT"
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="dominantFoot">Pé dominante</Label>
              <select
                id="dominantFoot"
                value={form.dominantFoot}
                onChange={(e) =>
                  setForm({ ...form, dominantFoot: e.target.value as DominantFoot | "" })
                }
                className={selectClass}
              >
                <option value="">—</option>
                {DOMINANT_FEET.map((f) => (
                  <option key={f} value={f}>
                    {dominantFootLabel(f)}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="status">Estado</Label>
              <select
                id="status"
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value as PlayerStatus })}
                className={selectClass}
              >
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {statusLabel(s)}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="height">Altura (cm)</Label>
              <Input
                id="height"
                type="number"
                min={100}
                max={250}
                value={form.height}
                onChange={(e) => setForm({ ...form, height: e.target.value })}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="weight">Peso (kg)</Label>
              <Input
                id="weight"
                type="number"
                min={30}
                max={200}
                value={form.weight}
                onChange={(e) => setForm({ ...form, weight: e.target.value })}
              />
            </div>

            <div className="space-y-1.5 col-span-2">
              <Label htmlFor="photoUrl">URL da fotografia</Label>
              <Input
                id="photoUrl"
                type="url"
                value={form.photoUrl}
                onChange={(e) => setForm({ ...form, photoUrl: e.target.value })}
                placeholder="https://…"
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
