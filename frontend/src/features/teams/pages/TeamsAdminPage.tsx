import { useState } from "react";
import { Link } from "react-router-dom";
import { Label } from "@/components/ui/label";
import { useClubs } from "@/features/clubs/hooks/useClubs";
import { TeamsSection } from "../components/TeamsSection";

const selectClass =
  "flex h-9 w-full max-w-sm rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring";

export function TeamsAdminPage() {
  const [clubId, setClubId] = useState<number | null>(null);

  const {
    data: clubsData,
    loading: clubsLoading,
    error: clubsError,
  } = useClubs({
    page: 0,
    size: 200,
    name: "",
  });
  const clubs = clubsData?.content ?? [];

  return (
    <div className="space-y-6">
      <header>
        <Link
          to="/admin"
          className="text-xs text-muted-foreground hover:text-foreground transition-colors"
        >
          ← Admin
        </Link>
        <h1 className="text-2xl font-bold tracking-tight">Equipas</h1>
        <p className="text-sm text-muted-foreground">
          Escolhe um clube para gerir as suas equipas (sénior, sub-19, feminina…).
        </p>
      </header>

      <div className="max-w-sm space-y-1.5">
        <Label htmlFor="club">Clube</Label>
        <select
          id="club"
          className={selectClass}
          value={clubId ?? ""}
          disabled={clubsLoading}
          onChange={(e) => {
            const value = e.target.value;
            setClubId(value ? Number(value) : null);
          }}
        >
          <option value="">{clubsLoading ? "A carregar…" : "Selecionar clube…"}</option>
          {clubs.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        {clubsError && (
          <p className="text-sm text-destructive">Falha a carregar clubes: {clubsError}</p>
        )}
      </div>

      {clubId ? (
        <TeamsSection clubId={clubId} canManage />
      ) : (
        <p className="text-sm text-muted-foreground">
          Seleciona um clube para ver e gerir as equipas.
        </p>
      )}
    </div>
  );
}
