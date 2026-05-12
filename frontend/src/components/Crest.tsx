import type { CrestTone } from "@/features/clubs/clubs";
import { cn } from "@/lib/utils";

/**
 * Monogram placeholder for a club crest. Renders a tonal background with
 * the club's initials in a mono font. Lives here (not in `components/ui/`)
 * because it carries domain semantics — when real logo assets land, swap
 * this for an `<img>` with a fallback to the monogram.
 */
const TONE_BG: Record<CrestTone, string> = {
  forest: "oklch(0.45 0.13 152)",
  oxblood: "oklch(0.38 0.13 25)",
  navy: "oklch(0.36 0.12 254)",
  ochre: "oklch(0.55 0.13 75)",
  slate: "oklch(0.3 0.02 240)",
  cream: "oklch(0.92 0.04 80)",
};

const TONE_FG: Record<CrestTone, string> = {
  forest: "oklch(0.99 0 0)",
  oxblood: "oklch(0.99 0 0)",
  navy: "oklch(0.99 0 0)",
  ochre: "oklch(0.99 0 0)",
  slate: "oklch(0.99 0 0)",
  cream: "oklch(0.25 0.05 30)",
};

interface CrestProps {
  initials: string;
  tone?: CrestTone;
  size?: number;
  square?: boolean;
  className?: string;
}

export function Crest({
  initials,
  tone = "forest",
  size = 40,
  square = false,
  className,
}: CrestProps) {
  return (
    <div
      className={cn(
        "inline-flex shrink-0 items-center justify-center font-bold leading-none",
        className,
      )}
      style={{
        width: size,
        height: size,
        background: TONE_BG[tone],
        color: TONE_FG[tone],
        borderRadius: square ? size * 0.18 : 9999,
        fontSize: Math.round(size * 0.4),
        letterSpacing: "-0.02em",
        fontFamily: "'JetBrains Mono', ui-monospace, monospace",
      }}
    >
      {initials}
    </div>
  );
}
