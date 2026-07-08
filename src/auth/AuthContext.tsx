import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { ApiError } from "@/api/apiClient";
import * as authApi from "@/api/authApi";
import {
  AUTH_SESSION_CHANGED_EVENT,
  clearStoredAuthSession,
  getStoredAccessToken,
  getStoredAuthUser,
  storeAuthSession,
  updateStoredAuthUser,
} from "@/auth/authStorage";
import {
  AuthContext,
  type AuthContextValue,
} from "@/auth/authContextValue";

function errorMessage(error: unknown, invalidCredentials = false) {
  if (error instanceof ApiError) {
    if (
      invalidCredentials &&
      (error.status === 401 || error.status === 403)
    ) {
      return "Invalid email or password.";
    }
    return error.message;
  }
  return "Something went wrong. Please try again.";
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState(getStoredAccessToken);
  const [currentUser, setCurrentUser] = useState(getStoredAuthUser);
  const [authError, setAuthError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const syncStoredSession = useCallback(() => {
    setAccessToken(getStoredAccessToken());
    setCurrentUser(getStoredAuthUser());
  }, []);

  useEffect(() => {
    window.addEventListener(AUTH_SESSION_CHANGED_EVENT, syncStoredSession);
    window.addEventListener("storage", syncStoredSession);
    return () => {
      window.removeEventListener(
        AUTH_SESSION_CHANGED_EVENT,
        syncStoredSession,
      );
      window.removeEventListener("storage", syncStoredSession);
    };
  }, [syncStoredSession]);

  const login = useCallback(async (request: authApi.LoginRequest) => {
    setAuthError(null);
    setIsLoading(true);
    try {
      const session = await authApi.login(request);
      storeAuthSession(session);
      setAccessToken(session.accessToken);
      setCurrentUser(session.user);
    } catch (error) {
      const message = errorMessage(error, true);
      setAuthError(message);
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const register = useCallback(
    async (request: authApi.RegisterRequest) => {
      setAuthError(null);
      setIsLoading(true);
      try {
        return await authApi.register(request);
      } catch (error) {
        const message = errorMessage(error);
        setAuthError(message);
        throw error;
      } finally {
        setIsLoading(false);
      }
    },
    [],
  );

  const logout = useCallback(async () => {
    setAuthError(null);
    setIsLoading(true);
    try {
      await authApi.logout();
    } catch (error) {
      setAuthError(errorMessage(error));
    } finally {
      clearStoredAuthSession();
      setAccessToken(null);
      setCurrentUser(null);
      setIsLoading(false);
    }
  }, []);

  const clearAuthError = useCallback(() => setAuthError(null), []);
  const syncCurrentUser = useCallback(
    (update: Pick<NonNullable<typeof currentUser>, "email" | "fullName">) => {
      updateStoredAuthUser(update);
      setCurrentUser((current) =>
        current ? { ...current, ...update } : current,
      );
    },
    [],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      accessToken,
      authError,
      clearAuthError,
      currentUser,
      isAuthenticated: Boolean(accessToken),
      isLoading,
      login,
      logout,
      register,
      syncCurrentUser,
    }),
    [
      accessToken,
      authError,
      clearAuthError,
      currentUser,
      isLoading,
      login,
      logout,
      register,
      syncCurrentUser,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
