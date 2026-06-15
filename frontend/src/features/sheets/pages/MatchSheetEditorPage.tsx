import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ApiError } from "@/api/client";
import { useAuth } from "@/auth/useAuth";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Lock, LockOpen } from "lucide-react";
import { getMatch, type Match } from "@/features/matches/api/matchesApi";
import { useMatchLookups } from "@/features/matches/hooks/useMatchLookups";
import { MatchStatusBadge } from "@/features/matches/components/MatchStatusBadge";
import { useMatchSheet, useTeamRosters } from "../hooks/useSheet";
import { lockMatchSheet, unlockMatchSheet } from "../api/sheetApi";
import { LineupEditor } from "../components/LineupEditor";
import { OccurrenceEditor } from "../components/OccurrenceEditor";

function errMsg(e: unknown): string {
  if (e instanceof ApiError) {
    const body = e.body as { detail?: string } | null;
    return body?.detail ?? e.message;
  }
  return e instanceof Error ? e.message : "Erro inesperado";
}

/**
 * Match-sheet editor at /matches/:matchId/sheet. Lets admins and the home
 * club's managers/staff build the lineup, record occurrences, and close
 * (lock) the sheet. The backend enforces @clubAuthz.canEditSheet, so a
 * non-authorized user gets 403s on the actions.
 */
export function MatchSheetEditorPage() {
  const navigate = useNavigate();
  const { matchId: param } = useParams<{ matchId: string }>();
  const matchId = Number(param);
  const { hasRole } = useAuth();
  const isAdmin = hasRole("platform-admin");

  const [match, setMatch] = useState<Match | null>(null);
  const [matchError, setMatchError] = useState<string | null>(null);
  const lookups = useMatchLookups();
  const { sheet, loading, error, refetch } = useMatchSheet(matchId);
  const rosters = useTeamRosters(match?.homeTeamId, match?.awayTeamId);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!matchId || Number.isNaN(matchId)) return;
    let cancelled = false;
    getMatch(matchId)
      .then((m) => !cancelled && setMatch(m))
      .catch((e) => !cancelled && setMatchError(errMsg(e)));
    return () => {
      cancelled = true;
    };
  }, [matchId]);

  const clubName = (clubId: number | null, teamId?: number) =>
    (clubId != null ? lookups.clubs.get(clubId)?.name : null) ??
    (teamId != null ? `Equipa #${teamId}` : "Equipa");
  const homeName = clubName(match?.homeClubId ?? null, match?.homeTeamId);
  const awayName = clubName(match?.awayClubId ?? null, match?.awayTeamId);

  const locked = sheet?.locked ?? false;

  async function doLock() {
    setBusy(true);
    setActionError(null);
    try {
      await lockMatchSheet(matchId);
      refetch();
    } catch (e) {
      setActionError(errMsg(e));
    } finally {
      setBusy(false);
    }
  }

  async function doUnlock() {
    setBusy(true);
    setActionError(null);
    try {
      await unlockMatchSheet(matchId);
      refetch();
    } catch (e) {
      setActionError(errMsg(e));
    } finally {
      setBusy(false);
    }
  }

  const rosterOf = (teamId?: number) =>
    teamId == null ? [] : [...rosters.players.values()].filter((p) => p.teamId === teamId);

  return (
    <div className="space-y-6">
      <header className="space-y-1">
        <button
          onClick={() => navigate(-1)}
          className="text-xs text-muted-foreground hover:text-foreground transition-colors"
        >
          ← Voltar
        </button>
        <div className="flex items-center gap-3 flex-wrap">
          <h1 className="text-2xl font-bold tracking-tight">
            {homeName} <span className="text-muted-foreground">vs</span> {awayName}
          </h1>
          {match && <MatchStatusBadge status={match.status} />}
          {locked && <Badge variant="secondary">Ficha fechada</Badge>}
        </div>
        <p className="text-sm text-muted-foreground">Ficha técnica — convocatórias e lances.</p>
      </header>

      {matchError && <p className="text-sm text-destructive">Falha a carregar o jogo: {matchError}</p>}
      {error && <p className="text-sm text-destructive">Falha a carregar a ficha: {error}</p>}
      {rosters.error && (
        <p className="text-sm text-destructive">Falha a carregar plantéis: {rosters.error}</p>
      )}
      {actionError && <p className="text-sm text-destructive">{actionError}</p>}

      {locked && (
        <div className="rounded-md border bg-muted/40 px-4 py-3 text-sm text-muted-foreground">
          A ficha está fechada e não pode ser editada
          {isAdmin ? "; como administrador podes reabri-la." : "."}
        </div>
      )}

      {loading || !sheet || !match ? (
        <p className="text-sm text-muted-foreground py-8">A carregar ficha…</p>
      ) : (
        <>
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Convocatórias</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-8 md:grid-cols-2">
              <LineupEditor
                matchId={matchId}
                teamName={homeName}
                roster={rosterOf(match.homeTeamId)}
                entries={sheet.lineup.filter((e) => e.teamId === match.homeTeamId)}
                locked={locked}
                onChanged={refetch}
              />
              <LineupEditor
                matchId={matchId}
                teamName={awayName}
                roster={rosterOf(match.awayTeamId)}
                entries={sheet.lineup.filter((e) => e.teamId === match.awayTeamId)}
                locked={locked}
                onChanged={refetch}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Lances do jogo</CardTitle>
            </CardHeader>
            <CardContent>
              <OccurrenceEditor
                matchId={matchId}
                lineup={sheet.lineup}
                players={rosters.players}
                homeTeamId={match.homeTeamId}
                awayTeamId={match.awayTeamId}
                homeName={homeName}
                awayName={awayName}
                occurrences={sheet.occurrences}
                locked={locked}
                onChanged={refetch}
              />
            </CardContent>
          </Card>

          <div className="flex justify-end gap-2">
            {!locked ? (
              <Button onClick={doLock} disabled={busy}>
                <Lock className="size-4" />
                Fechar ficha
              </Button>
            ) : (
              isAdmin && (
                <Button variant="outline" onClick={doUnlock} disabled={busy}>
                  <LockOpen className="size-4" />
                  Reabrir ficha
                </Button>
              )
            )}
          </div>
        </>
      )}
    </div>
  );
}
