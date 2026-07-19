import {
  clearStoredAuthSession,
  getStoredAccessToken,
  storeAuthSession,
} from "@/auth/authStorage";
import type { AuthSession } from "@/auth/types";

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();
const deployedBackendBaseUrl =
  "https://campusone-backend-otc4.onrender.com/api/v1";
const localBackendBaseUrl = "http://localhost:8080/api/v1";

export const API_BASE_URL = (
  configuredBaseUrl ||
  (import.meta.env.PROD ? deployedBackendBaseUrl : localBackendBaseUrl)
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
  const message = payload?.message?.trim();
  const safeMessage =
    message && !/unexpected error|internal server/i.test(message)
      ? message
      : response.status >= 500
        ? "Something went wrong. Please try again."
        : "The request could not be completed.";
  return new ApiError(
    safeMessage,
    response.status,
    payload?.code,
    payload?.fieldErrors,
  );
}

function isBrowserOffline() {
  return typeof navigator !== "undefined" && navigator.onLine === false;
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

  if (isBrowserOffline()) {
    throw new ApiError(
      "You appear to be offline. Check your internet connection and try again.",
      0,
      "OFFLINE",
    );
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
    if (isBrowserOffline()) {
      throw new ApiError(
        "You appear to be offline. Check your internet connection and try again.",
        0,
        "OFFLINE",
      );
    }
    throw new ApiError(
      "We could not connect to CampusOne. Please try again.",
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

export async function apiDownload(
  path: string,
  retryOnUnauthorized = true,
): Promise<{ blob: Blob; filename: string }> {
  if (isBrowserOffline()) {
    throw new ApiError(
      "You appear to be offline. Check your internet connection and try again.",
      0,
      "OFFLINE",
    );
  }
  const headers = new Headers({ Accept: "*/*" });
  const accessToken = getStoredAccessToken();
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  let response: Response;
  try {
    response = await fetch(endpoint(path), {
      credentials: "include",
      headers,
    });
  } catch {
    throw new ApiError(
      isBrowserOffline()
        ? "You appear to be offline. Check your internet connection and try again."
        : "We could not connect to CampusOne. Please try again.",
      0,
      isBrowserOffline() ? "OFFLINE" : "NETWORK_ERROR",
    );
  }
  if (response.status === 401 && retryOnUnauthorized) {
    const refreshed = await refreshSession();
    if (refreshed) return apiDownload(path, false);
  }
  if (!response.ok) {
    const body = await parseResponseBody(response);
    throw toApiError(response, body);
  }
  const disposition = response.headers.get("content-disposition") ?? "";
  const encodedName = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  const simpleName = disposition.match(/filename="?([^";]+)"?/i)?.[1];
  const filename = encodedName
    ? decodeURIComponent(encodedName)
    : simpleName || "campusone-export";
  return { blob: await response.blob(), filename };
}
