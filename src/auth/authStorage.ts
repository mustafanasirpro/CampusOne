import type { AuthSession, AuthUser } from "@/auth/types";

const ACCESS_TOKEN_KEY = "campusone.accessToken";
const AUTH_USER_KEY = "campusone.authUser";

export const AUTH_SESSION_CHANGED_EVENT = "campusone:auth-session-changed";

function storageAvailable() {
  return typeof window !== "undefined" && Boolean(window.localStorage);
}

function isAuthUser(value: unknown): value is AuthUser {
  if (typeof value !== "object" || value === null) return false;

  const candidate = value as Partial<AuthUser>;
  return (
    typeof candidate.email === "string" &&
    typeof candidate.fullName === "string" &&
    Array.isArray(candidate.roles) &&
    candidate.roles.every((role) => typeof role === "string")
  );
}

export function getStoredAccessToken() {
  return storageAvailable()
    ? window.localStorage.getItem(ACCESS_TOKEN_KEY)
    : null;
}

export function getStoredAuthUser(): AuthUser | null {
  if (!storageAvailable()) return null;

  const value = window.localStorage.getItem(AUTH_USER_KEY);
  if (!value) return null;

  try {
    const parsed: unknown = JSON.parse(value);
    if (isAuthUser(parsed)) return parsed;
    window.localStorage.removeItem(AUTH_USER_KEY);
    return null;
  } catch {
    window.localStorage.removeItem(AUTH_USER_KEY);
    return null;
  }
}

export function storeAuthSession(session: AuthSession) {
  if (!storageAvailable()) return;

  window.localStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken);
  window.localStorage.setItem(AUTH_USER_KEY, JSON.stringify(session.user));
  window.dispatchEvent(new Event(AUTH_SESSION_CHANGED_EVENT));
}

export function updateStoredAuthUser(
  update: Pick<AuthUser, "email" | "fullName">,
) {
  if (!storageAvailable()) return;

  const current = getStoredAuthUser();
  if (!current) return;
  window.localStorage.setItem(
    AUTH_USER_KEY,
    JSON.stringify({ ...current, ...update }),
  );
  window.dispatchEvent(new Event(AUTH_SESSION_CHANGED_EVENT));
}

export function clearStoredAuthSession() {
  if (!storageAvailable()) return;

  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(AUTH_USER_KEY);
  window.dispatchEvent(new Event(AUTH_SESSION_CHANGED_EVENT));
}
