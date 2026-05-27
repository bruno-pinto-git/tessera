import { Link } from "react-router-dom";
import { ChevronRight } from "lucide-react";

interface MatchdayBannerProps {
  home: string;
  away: string;
  /** Human-readable countdown, e.g. "3h 22min". */
  kickoff: string;
  href: string;
}

/**
 * Thin (h-9) primary-tinted strip rendered above the header when there is
 * a match in the next 24h. Clickable; takes the user to the event detail
 * (or the user's paid ticket if one already exists).
 *
 * Visually it uses `color-mix()` so the tint follows the active theme
 * without needing a dedicated `--primary-soft` token.
 */
export function MatchdayBanner({ home, away, kickoff, href }: MatchdayBannerProps) {
  return (
    <Link
      to={href}
      className="block border-b"
      style={{
        background: "color-mix(in oklch, var(--primary) 12%, var(--background))",
        borderColor: "color-mix(in oklch, var(--primary) 30%, transparent)",
      }}
    >
      <div className="container mx-auto flex h-9 items-center justify-between gap-4 px-4 text-[13px]">
        <div className="flex items-center gap-3 min-w-0">
          <span
            className="inline-flex h-1.5 w-1.5 rounded-full"
            style={{
              background: "var(--primary)",
              boxShadow: "0 0 0 4px color-mix(in oklch, var(--primary) 20%, transparent)",
            }}
          />
          <span className="font-medium" style={{ color: "var(--primary)" }}>
            Hoje
          </span>
          <span className="text-muted-foreground">·</span>
          <span className="truncate">
            <strong className="font-medium">{home}</strong>{" "}
            <span className="text-muted-foreground">vs</span>{" "}
            <strong className="font-medium">{away}</strong>
          </span>
          <span className="text-muted-foreground hidden sm:inline">
            · começa em <span className="tabular-nums font-medium text-foreground">{kickoff}</span>
          </span>
        </div>
        <span
          className="flex items-center gap-1 text-xs font-medium shrink-0"
          style={{ color: "var(--primary)" }}
        >
          Ver detalhes
          <ChevronRight className="size-3" />
        </span>
      </div>
    </Link>
  );
}
