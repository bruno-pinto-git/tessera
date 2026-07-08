import { useEffect, useRef, useState, type ReactNode } from "react";
import { keycloak, type Role } from "./keycloak";
import { AuthContext, type AuthContextValue } from "./AuthContext";

const ROLE_VALUES: Role[] = ["platform-admin", "club-manager", "staff", "fan"];

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [token, setToken] = useState<string | undefined>(undefined);
  const initStarted = useRef(false);

  useEffect(() => {
    if (initStarted.current) return;
    initStarted.current = true;

    keycloak
      .init({
        onLoad: "check-sso",
        pkceMethod: "S256",
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      })
      .then((isAuth) => {
        setAuthenticated(isAuth);
        setToken(keycloak.token);
        setInitialized(true);
      })
      .catch(() => {
        setInitialized(true);
      });

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).then(() => setToken(keycloak.token));
    };

    keycloak.onAuthSuccess = () => {
      setAuthenticated(true);
      setToken(keycloak.token);
    };

    keycloak.onAuthLogout = () => {
      setAuthenticated(false);
      setToken(undefined);
    };
  }, []);

  const realmRoles = (keycloak.tokenParsed?.realm_access?.roles ?? []) as string[];
  const roles = ROLE_VALUES.filter((r) => realmRoles.includes(r));

  const value: AuthContextValue = {
    initialized,
    authenticated,
    username: keycloak.tokenParsed?.preferred_username as string | undefined,
    roles,
    hasRole: (role) => realmRoles.includes(role),
    login: () => keycloak.login(),
    logout: () => keycloak.logout({ redirectUri: window.location.origin }),
    token,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
