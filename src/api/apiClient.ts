import {
  clearStoredAuthSession,
  getStoredAccessToken,
  storeAuthSession,
} from "@/auth/authStorage";
import type { AuthSession } from "@/auth/types";

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();

export const API_BASE_URL = (
  configuredBaseUrl || "http://localhost:8080/api/v1"
).replace(/\/+$/, "");

interface ErrorPayload {
  code?: string;
  fieldErrors?: Record<string, string[]>;
  message?: string;
}

interface ApiRequestOptions extends Omit<RequestInit, "headers"> {
  attachAccessToken?: boolean;
  headers?: HeadersInit;
  retryOnUnauthorized?: boolean;
}

export class ApiError extends Error {
  readonly code?: string;
  readonly fieldErrors: Record<string, string[]>;
  readonly status: number;

  constructor(
    message: string,
    status: number,
    code?: string,
    fieldErrors: Record<string, string[]> = {},
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

let refreshRequest: Promise<AuthSession | null> | null = null;

function endpoint(path: string) {
  return `${API_BASE_URL}/${path.replace(/^\/+/, "")}`;
}

async function parseResponseBody(response: Response): Promise<unknown> {
  if (response.status === 204) return undefined;

  const contentType = response.headers.get("content-type") ?? "";
  if (
    contentType.includes("application/json") ||
    contentType.includes("+json")
  ) {
    return response.json();
  }

  const text = await response.text();
  return text || undefined;
}

function toApiError(response: Response, body: unknown) {
  const payload =
    typeof body === "object" && body !== null
      ? (body as ErrorPayload)
      : undefined;
  return new ApiError(
    payload?.message ||
      (response.status >= 500
        ? "CampusOne is temporarily unavailable. Please try again."
        : "The request could not be completed."),
    response.status,
    payload?.code,
    payload?.fieldErrors,
  );
}

async function refreshSession() {
  if (refreshRequest) return refreshRequest;

  refreshRequest = (async () => {
    try {
      const response = await fetch(endpoint("/auth/refresh"), {
        credentials: "include",
        method: "POST",
        headers: { Accept: "application/json" },
      });
      if (!response.ok) {
        clearStoredAuthSession();
        return null;
      }

      const session = (await response.json()) as AuthSession;
      storeAuthSession(session);
      return session;
    } catch {
      clearStoredAuthSession();
      return null;
    } finally {
      refreshRequest = null;
    }
  })();

  return refreshRequest;
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const {
    attachAccessToken = true,
    headers: suppliedHeaders,
    retryOnUnauthorized = true,
    ...requestOptions
  } = options;
  const headers = new Headers(suppliedHeaders);
  headers.set("Accept", "application/json");

  if (
    requestOptions.body &&
    !(requestOptions.body instanceof FormData) &&
    !headers.has("Content-Type")
  ) {
    headers.set("Content-Type", "application/json");
  }

  const accessToken = attachAccessToken ? getStoredAccessToken() : null;
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  let response: Response;
  try {
    response = await fetch(endpoint(path), {
      ...requestOptions,
      credentials: "include",
      headers,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw error;
    }
    throw new ApiError(
      "Unable to reach CampusOne. Check that the backend is running.",
      0,
      "NETWORK_ERROR",
    );
  }

  if (
    response.status === 401 &&
    attachAccessToken &&
    retryOnUnauthorized
  ) {
    const refreshedSession = await refreshSession();
    if (refreshedSession) {
      return apiRequest<T>(path, {
        ...options,
        retryOnUnauthorized: false,
      });
    }
  }

  const body = await parseResponseBody(response);
  if (!response.ok) {
    if (response.status === 401) clearStoredAuthSession();
    throw toApiError(response, body);
  }

  return body as T;
}
