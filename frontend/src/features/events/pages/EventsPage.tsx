import { useMemo, useState } from "react";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { MatchCard } from "../components/MatchCard";
import { useEventsCatalog } from "../hooks/useEventsCatalog";
import type { CatalogEntry } from "../lib/catalog";
import { cn } from "@/lib/utils";

interface Filter {
  key: string;
  label: string;
  match: (e: CatalogEntry) => boolean;
}

const FILTERS: Filter[] = [
  { key: "all", label: "Todos", match: () => true },
  {
    key: "this-week",
    label: "Esta semana",
    match: (e) => {
      if (!e.kickoffAt) return false;
      const t = new Date(e.kickoffAt).getTime();
      const now = Date.now();
      return t - now < 7 * 24 * 3600 * 1000 && t > now;
    },
  },
  {
    key: "upcoming",
    label: "Por jogar",
    match: (e) =>
      e.matchStatus === "SCHEDULED" ||
      e.matchStatus === "LIVE" ||
      e.matchStatus === "POSTPONED" ||
      e.matchStatus == null,
  },
];

export function EventsPage() {
  const [filter, setFilter] = useState<string>("all");
  const [query, setQuery] = useState("");
  const { entries, loading, error } = useEventsCatalog();

  const visible = useMemo(() => {
    const f = FILTERS.find((x) => x.key === filter) ?? FILTERS[0];
    const q = query.trim().toLowerCase();
    return entries.filter(f.match).filter((e) => {
      if (!q) return true;
      return (
        e.homeClubName.toLowerCase().includes(q) ||
        e.awayClubName.toLowerCase().includes(q) ||
        (e.venueName?.toLowerCase().includes(q) ?? false)
      );
    });
  }, [entries, filter, query]);

  const counts = useMemo(
    () => Object.fromEntries(FILTERS.map((f) => [f.key, entries.filter(f.match).length])),
    [entries],
  );

  return (
    <div className="space-y-8">
      <header className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div className="space-y-2">
          <h1 className="text-4xl font-bold tracking-tight">Próximos jogos</h1>
          <p className="text-sm text-muted-foreground max-w-xl">
            Calendário dos próximos jogos com bilheteira aberta. Reserva o teu lugar e apresenta o
            QR à entrada do estádio.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground size-4" />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Procurar clube, estádio…"
              className="h-9 w-64 pl-9"
            />
          </div>
        </div>
      </header>

      <div className="flex items-center gap-2 border-b -mb-px pb-px">
        {FILTERS.map((f) => {
          const active = f.key === filter;
          return (
            <button
              key={f.key}
              onClick={() => setFilter(f.key)}
              className={cn(
                "h-9 px-3 text-sm rounded-t-md -mb-px transition-colors",
                active
                  ? "text-foreground font-medium border-b-2 border-primary"
                  : "text-muted-foreground hover:text-foreground",
              )}
            >
              {f.label}
              <span className="ml-1.5 text-xs text-muted-foreground">{counts[f.key] ?? 0}</span>
            </button>
          );
        })}
      </div>

      {loading && (
        <p className="text-sm text-muted-foreground py-12 text-center">A carregar jogos…</p>
      )}
      {error && (
        <p className="text-sm text-destructive py-12 text-center">
          Falha a carregar jogos: {error}
        </p>
      )}
      {!loading && !error && visible.length === 0 && (
        <p className="text-sm text-muted-foreground py-12 text-center">
          {entries.length === 0
            ? "Ainda não há jogos com bilheteira aberta. Volta em breve."
            : "Não encontramos jogos com esses critérios."}
        </p>
      )}
      {!loading && !error && visible.length > 0 && (
        <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {visible.map((e) => (
            <MatchCard key={e.eventId} entry={e} />
          ))}
        </section>
      )}
    </div>
  );
}
