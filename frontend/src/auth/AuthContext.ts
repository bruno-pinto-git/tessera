import { createContext } from "react";
import type { Role } from "./keycloak";

export interface AuthContextValue {
  initialized: boolean;
  authenticated: boolean;
  username?: string;
  roles: Role[];
  hasRole: (role: Role) => boolean;
  login: () => void;
  logout: () => void;
  token?: string;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);
