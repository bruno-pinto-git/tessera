import { ChevronDown } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "@/auth/useAuth";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export function UserMenu() {
  const { authenticated, username, roles, logout, login } = useAuth();

  if (!authenticated) {
    return (
      <button
        onClick={login}
        className="inline-flex h-8 items-center rounded-md bg-primary px-3 text-sm font-medium text-primary-foreground shadow-sm hover:bg-primary/90"
      >
        Entrar
      </button>
    );
  }

  const initials = (username ?? "??")
    .split(/[\s._-]+/)
    .map((s) => s[0]?.toUpperCase() ?? "")
    .slice(0, 2)
    .join("");
  const primaryRole = roles[0] ?? "fan";
  const roleLabel =
    primaryRole === "platform-admin"
      ? "Admin"
      : primaryRole === "club-manager"
        ? "Gestor"
        : primaryRole === "staff"
          ? "Staff"
          : "Adepto";

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className="inline-flex h-8 items-center gap-2 rounded-full border border-input bg-background pl-1 pr-2.5 transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <span className="inline-flex h-6 w-6 items-center justify-center rounded-full bg-primary text-[10px] font-semibold text-primary-foreground">
            {initials}
          </span>
          <ChevronDown className="size-3 text-muted-foreground" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <div className="px-3 py-2.5 border-b border-border">
          <div className="text-sm font-medium">{username}</div>
          <div className="text-xs text-muted-foreground truncate">{roleLabel}</div>
        </div>
        <DropdownMenuItem asChild>
          <Link to="/tickets/mine">Os meus bilhetes</Link>
        </DropdownMenuItem>
        <DropdownMenuItem disabled>Definições</DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={logout} className="text-destructive focus:text-destructive">
          Sair
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
