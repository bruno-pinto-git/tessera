import { useEffect, type ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "./useAuth";
import type { Role } from "./keycloak";

interface ProtectedRouteProps {
  children: ReactNode;
  roles?: Role[];
}

export function ProtectedRoute({ children, roles }: ProtectedRouteProps) {
  const { initialized, authenticated, hasRole, login } = useAuth();

  useEffect(() => {
    if (initialized && !authenticated) {
      login();
    }
  }, [initialized, authenticated, login]);

  if (!initialized) {
    return <p className="text-sm text-muted-foreground p-4">A carregar…</p>;
  }

  if (!authenticated) {
    return <p className="text-sm text-muted-foreground p-4">A redirecionar para o login…</p>;
  }

  if (roles && roles.length > 0 && !roles.some((r) => hasRole(r))) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <>{children}</>;
}
