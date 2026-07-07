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
import { listTeamsByClub, teamCategoryLabel, type Team } from "@/features/teams/api/teamsApi";
import { createMatch, updateMatch, type Match } from "../api/matchesApi";
import type { Club } from "@/features/clubs/api/clubsApi";
import type { Venue } from "@/features/venues/api/venuesApi";

interface MatchFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  match?: Match | null;
  clubs: Map<number, Club>;
  venues: Map<number, Venue>;
  knownTeams: Map<number, Team>;
  onSaved: () => void;
  lockHomeClubId?: number;
}

interface FormState {
  homeClubId: string;
  homeTeamId: string;
  awayClubId: string;
  awayTeamId: string;
  venueId: string;
  kickoffAt: string;
  refereeName: string;
}

const EMPTY: FormState = {
  homeClubId: "",
  homeTeamId: "",
  awayClubId: "",
  awayTeamId: "",
  venueId: "",
  kickoffAt: "",
  refereeName: "",
};

export function MatchFormDialog({
  open,
  onOpenChange,
  match,
  clubs,
  venues,
  knownTeams,
  onSaved,
  lockHomeClubId,
}: MatchFormDialogProps) {
  const isEdit = !!match;
  const homeClubLocked = !isEdit && lockHomeClubId != null;
  const [form, setForm] = useState<FormState>(EMPTY);
  const [homeTeams, setHomeTeams] = useState<Team[]>([]);
  const [awayTeams, setAwayTeams] = useState<Team[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setError(null);
    if (match) {
      const homeTeam = knownTeams.get(match.homeTeamId);
      const awayTeam = knownTeams.get(match.awayTeamId);
      setForm({
        homeClubId: homeTeam ? String(homeTeam.clubId) : "",
        homeTeamId: String(match.homeTeamId),
        awayClubId: awayTeam ? String(awayTeam.clubId) : "",
        awayTeamId: String(match.awayTeamId),
        venueId: match.venueId != null ? String(match.venueId) : "",
        kickoffAt: match.kickoffAt ? toDatetimeLocal(match.kickoffAt) : "",
        refereeName: match.refereeName ?? "",
      });
    } else {
      setForm(
        homeClubLocked ? { ...EMPTY, homeClubId: String(lockHomeClubId) } : EMPTY,
      );
      setHomeTeams([]);
      setAwayTeams([]);
    }
  }, [open, match, knownTeams, homeClubLocked, lockHomeClubId]);

  useEffect(() => {
    if (!form.homeClubId) {
      setHomeTeams([]);
      return;
    }
    let cancelled = false;
    void listTeamsByClub(Number(form.homeClubId)).then((ts) => {
      if (!cancelled) setHomeTeams(ts);
    });
    return () => {
      cancelled = true;
    };
  }, [form.homeClubId]);

  useEffect(() => {
    if (!form.awayClubId) {
      setAwayTeams([]);
      return;
    }
    let cancelled = false;
    void listTeamsByClub(Number(form.awayClubId)).then((ts) => {
      if (!cancelled) setAwayTeams(ts);
    });
    return () => {
      cancelled = true;
    };
  }, [form.awayClubId]);

  function validate(): string | null {
    if (!form.homeTeamId) return "Seleciona a equipa da casa.";
    if (!form.awayTeamId) return "Seleciona a equipa visitante.";
    if (form.homeTeamId === form.awayTeamId) {
      return "As duas equipas têm de ser diferentes.";
    }
    if (!form.kickoffAt) return "Define a data e hora do jogo.";
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
      const kickoffIso = new Date(form.kickoffAt).toISOString();

      if (isEdit && match) {
        await updateMatch(match.id, {
          venueId: form.venueId ? Number(form.venueId) : null,
          kickoffAt: kickoffIso,
          refereeName: form.refereeName.trim() || null,
        });
      } else {
        await createMatch({
          homeTeamId: Number(form.homeTeamId),
          awayTeamId: Number(form.awayTeamId),
          venueId: form.venueId ? Number(form.venueId) : null,
          kickoffAt: kickoffIso,
          refereeName: form.refereeName.trim() || null,
        });
      }
      onSaved();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof ApiError) {
        if (
          err.status === 400 &&
          err.body &&
          typeof err.body === "object" &&
          "detail" in err.body
        ) {
          setError(String((err.body as { detail: unknown }).detail));
        } else if (err.status === 409) {
          setError("Conflito — verifica as datas e equipas.");
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

  const sortedClubs = Array.from(clubs.values()).sort((a, b) => a.name.localeCompare(b.name));
  const sortedVenues = Array.from(venues.values()).sort((a, b) => a.name.localeCompare(b.name));

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <form onSubmit={handleSubmit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>{isEdit ? "Editar jogo" : "Novo jogo"}</DialogTitle>
            <DialogDescription>
              {isEdit
                ? "Atualiza dados do jogo. As equipas não se alteram aqui — cria um novo jogo se houver mudança."
                : "Agenda um novo jogo. Escolhe clube e equipa de cada lado."}
            </DialogDescription>
          </DialogHeader>

          <div className="grid grid-cols-2 gap-4">
            <fieldset className="space-y-3" disabled={isEdit}>
              <legend className="text-xs uppercase tracking-wider text-muted-foreground">
                Casa
              </legend>
              <div className="space-y-1.5">
                <Label htmlFor="m-home-club">Clube</Label>
                <Selector
                  id="m-home-club"
                  value={form.homeClubId}
                  onChange={(v) => setForm({ ...form, homeClubId: v, homeTeamId: "" })}
                  disabled={homeClubLocked}
                >
                  <option value="">— Escolhe —</option>
                  {sortedClubs.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </Selector>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="m-home-team">Equipa</Label>
                <Selector
                  id="m-home-team"
                  value={form.homeTeamId}
                  onChange={(v) => setForm({ ...form, homeTeamId: v })}
                  disabled={!form.homeClubId}
                >
                  <option value="">— Escolhe —</option>
                  {homeTeams.map((t) => (
                    <option key={t.id} value={t.id}>
                      {teamCategoryLabel(t.category)}
                    </option>
                  ))}
                </Selector>
              </div>
            </fieldset>

            <fieldset className="space-y-3" disabled={isEdit}>
              <legend className="text-xs uppercase tracking-wider text-muted-foreground">
                Visitante
              </legend>
              <div className="space-y-1.5">
                <Label htmlFor="m-away-club">Clube</Label>
                <Selector
                  id="m-away-club"
                  value={form.awayClubId}
                  onChange={(v) => setForm({ ...form, awayClubId: v, awayTeamId: "" })}
                >
                  <option value="">— Escolhe —</option>
                  {sortedClubs.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </Selector>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="m-away-team">Equipa</Label>
                <Selector
                  id="m-away-team"
                  value={form.awayTeamId}
                  onChange={(v) => setForm({ ...form, awayTeamId: v })}
                  disabled={!form.awayClubId}
                >
                  <option value="">— Escolhe —</option>
                  {awayTeams.map((t) => (
                    <option key={t.id} value={t.id}>
                      {teamCategoryLabel(t.category)}
                    </option>
                  ))}
                </Selector>
              </div>
            </fieldset>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="m-venue">Estádio</Label>
            <Selector
              id="m-venue"
              value={form.venueId}
              onChange={(v) => setForm({ ...form, venueId: v })}
            >
              <option value="">— Sem estádio definido —</option>
              {sortedVenues.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.name} ({v.capacity.toLocaleString("pt-PT")})
                </option>
              ))}
            </Selector>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="m-kickoff">Data e hora</Label>
              <Input
                id="m-kickoff"
                type="datetime-local"
                value={form.kickoffAt}
                onChange={(e) => setForm({ ...form, kickoffAt: e.target.value })}
                required
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="m-referee">Árbitro</Label>
              <Input
                id="m-referee"
                value={form.refereeName}
                onChange={(e) => setForm({ ...form, refereeName: e.target.value })}
                placeholder="Opcional"
                maxLength={200}
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

function Selector({
  id,
  value,
  onChange,
  children,
  disabled,
}: {
  id: string;
  value: string;
  onChange: (value: string) => void;
  children: React.ReactNode;
  disabled?: boolean;
}) {
  return (
    <select
      id={id}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      disabled={disabled}
      className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
    >
      {children}
    </select>
  );
}

function toDatetimeLocal(iso: string): string {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return "";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
