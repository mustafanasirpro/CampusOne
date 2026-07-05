import { createContext } from "react";

import type { LoginRequest, RegisterRequest } from "@/api/authApi";
import type { AuthUser } from "@/auth/types";

export interface AuthContextValue {
  accessToken: string | null;
  authError: string | null;
  clearAuthError: () => void;
  currentUser: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (request: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  register: (request: RegisterRequest) => Promise<AuthUser>;
  syncCurrentUser: (
    update: Pick<AuthUser, "email" | "fullName">,
  ) => void;
}

export const AuthContext = createContext<AuthContextValue | undefined>(
  undefined,
);
