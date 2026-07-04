import { apiRequest } from "@/api/apiClient";

export interface CurrentUserIdentity {
  email: string;
  fullName: string;
  userId: string;
}

export function getCurrentUserIdentity(signal?: AbortSignal) {
  return apiRequest<CurrentUserIdentity>("/users/me", { signal });
}
