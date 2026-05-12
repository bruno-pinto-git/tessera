import { useMemo, useState } from "react";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { MatchCard } from "../components/MatchCard";
import { MOCK_MATCHES, type MatchListItem } from "../mockMatches";
import { clubBy } from "@/features/clubs/clubs";
import { cn } from "@/lib/utils";

const SEASONS = ["2025/26", "2024/25"];

interface Filter {
  key: string;
  label: string;
  match: (m: MatchListItem) => boolean;
}

const FILTERS: Filter[] = [
  { key: "all", label: "Todos", match: () => true },
  // "Em casa" / "Fora" are placeholders until we know the user's favourite
  // club — for now they're hard-coded against the demo club (ALJ).
  { key: "home", label: "Em casa", match: (m) => m.homeClubId === "ALJ" },
  { key: "away", label: "Fora", match: (m) => m.awayClubId === "ALJ" },
  {
    key: "this-week",
    label: "Esta semana",
    match: (m) => {
      const k = new Date(m.kickoffAt);
      const now = Date.now();
      return k.getTime() - now < 7 * 24 * 3600 * 1000;
    },
  },
];

export function EventsPage() {
  const [filter, setFilter] = useState<string>("all");
  const [query, setQuery] = useState("");
  const [season, setSeason] = useState(SEASONS[0]);

  const visible = useMemo(() => {
    const f = FILTERS.find((x) => x.key === filter) ?? FILTERS[0];
    const q = query.trim().toLowerCase();
    return MOCK_MATCHES.filter(f.match).filter((m) => {
      if (!q) return true;
      const home = clubBy(m.homeClubId);
      const away = clubBy(m.awayClubId);
      return (
        home.name.toLowerCase().includes(q) ||
        away.name.toLowerCase().includes(q) ||
        m.venue.toLowerCase().includes(q)
      );
    });
  }, [filter, query]);

  // Counts under each filter pill — recomputed when the underlying mock
  // changes (today it's a constant, but the call-site reads cleaner this
  // way once a real query lands).
  const counts = useMemo(
    () => Object.fromEntries(FILTERS.map((f) => [f.key, MOCK_MATCHES.filter(f.match).length])),
    [],
  );

  return (
    <div className="space-y-8">
      <header className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div className="space-y-2">
          <h1 className="text-4xl font-bold tracking-tight">Próximos jogos</h1>
          <p className="text-sm text-muted-foreground max-w-xl">
            Calendário dos próximos jogos com bilheteira aberta. Reserva o teu lugar e apresenta
            o QR à entrada do estádio.
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
          <select
            value={season}
            onChange={(e) => setSeason(e.target.value)}
            className="h-9 rounded-md border border-input bg-background px-3 text-sm"
          >
            {SEASONS.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
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
              <span className="ml-1.5 text-xs text-muted-foreground">{counts[f.key]}</span>
            </button>
          );
        })}
      </div>

      {visible.length === 0 ? (
        <p className="text-sm text-muted-foreground py-12 text-center">
          Não encontramos jogos com esses critérios.
        </p>
      ) : (
        <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {visible.map((m) => (
            <MatchCard key={m.id} match={m} />
          ))}
        </section>
      )}
    </div>
  );
}
