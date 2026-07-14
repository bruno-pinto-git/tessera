import { Link } from "react-router-dom";
import { ClipboardList } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/auth/useAuth";
import type { Role } from "@/auth/keycloak";

const SHEET_ROLES: Role[] = ["club-manager", "staff", "platform-admin"];

interface SheetLinkButtonProps {
  matchId: number | null | undefined;
  size?: "sm" | "default" | "lg";
  variant?: "default" | "outline" | "secondary";
  className?: string;
}

/**
 * Shortcut to a match's technical sheet. Rendered only for the roles that can
 * open it (club managers, staff, platform admins) and when a match exists;
 * renders nothing otherwise, so it is safe to drop into any public view.
 */
export function SheetLinkButton({
  matchId,
  size = "sm",
  variant = "outline",
  className,
}: SheetLinkButtonProps) {
  const { hasRole } = useAuth();

  if (matchId == null) return null;
  if (!SHEET_ROLES.some((r) => hasRole(r))) return null;

  return (
    <Button size={size} variant={variant} asChild className={className}>
      <Link to={`/matches/${matchId}/sheet`}>
        <ClipboardList className="size-3.5" />
        Ficha técnica
      </Link>
    </Button>
  );
}
