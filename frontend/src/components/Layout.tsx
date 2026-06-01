import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "@/auth/useAuth";
import { Separator } from "@/components/ui/separator";
import { MatchdayBanner } from "@/components/MatchdayBanner";
import { UserMenu } from "@/components/UserMenu";
import { ThemeToggle } from "@/components/ThemeToggle";
import { useNextMatch, formatCountdown } from "@/features/matches/useNextMatch";
import { cn } from "@/lib/utils";

export function Layout() {
  const { authenticated, hasRole } = useAuth();
  const next = useNextMatch();

  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground">
      {next && (
        <MatchdayBanner
          home={next.homeShort}
          away={next.awayShort}
          kickoff={formatCountdown(next.kickoffAt)}
          href={`/events/${next.eventId}`}
        />
      )}

      <header className="border-b">
        <div className="container mx-auto flex h-14 items-center justify-between px-4">
          <nav className="flex items-center gap-6">
            <Link to="/" className="text-base font-semibold tracking-tight">
              Tessera
            </Link>
            <Separator orientation="vertical" className="h-5" />
            <ul className="flex items-center gap-5 text-sm">
              <li>
                <NavItem to="/events">Jogos</NavItem>
              </li>
              {authenticated && (
                <li>
                  <NavItem to="/tickets/mine">Os meus bilhetes</NavItem>
                </li>
              )}
              {(hasRole("club-manager") || hasRole("staff")) && (
                <li>
                  <NavItem to="/club">O meu clube</NavItem>
                </li>
              )}
              {hasRole("platform-admin") && (
                <li>
                  <NavItem to="/admin">Admin</NavItem>
                </li>
              )}
            </ul>
          </nav>
          <div className="flex items-center gap-2">
            <ThemeToggle />
            <UserMenu />
          </div>
        </div>
      </header>

      <main className="flex-1">
        <div className="container mx-auto px-4 py-8">
          <Outlet />
        </div>
      </main>

      <footer className="border-t">
        <div className="container mx-auto px-4 py-4 text-xs text-muted-foreground">
          Tessera · ISEL · Projeto e Seminário 2025/2026
        </div>
      </footer>
    </div>
  );
}

function NavItem({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        cn(
          "transition-colors hover:text-foreground",
          isActive ? "text-foreground font-medium" : "text-muted-foreground",
        )
      }
    >
      {children}
    </NavLink>
  );
}
