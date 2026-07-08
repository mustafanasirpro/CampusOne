import { apiRequest } from "@/api/apiClient";
import type { AuthSession, AuthUser } from "@/auth/types";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  departmentId: string;
  email: string;
  fullName: string;
  password: string;
  semester: number;
  universityId: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  newPassword: string;
  token: string;
}

export interface PasswordResetResponse {
  message: string;
}

export function login(request: LoginRequest) {
  return apiRequest<AuthSession>("/auth/login", {
    attachAccessToken: false,
    body: JSON.stringify(request),
    method: "POST",
    retryOnUnauthorized: false,
  });
}

export function register(request: RegisterRequest) {
  return apiRequest<AuthUser>("/auth/register", {
    attachAccessToken: false,
    body: JSON.stringify(request),
    method: "POST",
    retryOnUnauthorized: false,
  });
}

export function forgotPassword(request: ForgotPasswordRequest) {
  return apiRequest<PasswordResetResponse>("/auth/forgot-password", {
    attachAccessToken: false,
    body: JSON.stringify(request),
    method: "POST",
    retryOnUnauthorized: false,
  });
}

export function resetPassword(request: ResetPasswordRequest) {
  return apiRequest<PasswordResetResponse>("/auth/reset-password", {
    attachAccessToken: false,
    body: JSON.stringify(request),
    method: "POST",
    retryOnUnauthorized: false,
  });
}

export function logout() {
  return apiRequest<void>("/auth/logout", {
    method: "POST",
    retryOnUnauthorized: false,
  });
}
