import { apiRequest } from "@/api/apiClient";

export type ProfileVisibility = "PRIVATE" | "PUBLIC";
export type ThemePreference = "DARK" | "LIGHT" | "SYSTEM";

export interface UniversitySummary {
  active: boolean;
  city: string;
  id: string;
  name: string;
  shortName: string;
  website: string | null;
}

export interface DepartmentSummary {
  active: boolean;
  code: string;
  id: string;
  name: string;
  universityId: string;
}

export interface UserPreferences {
  compactMode: boolean;
  language: string;
  theme: ThemePreference;
}

export interface CurrentUserIdentity {
  avatarUrl: string | null;
  bio: string | null;
  coverImageUrl: string | null;
  department: DepartmentSummary;
  email: string;
  fullName: string;
  location: string | null;
  preferences: UserPreferences;
  semester: number;
  skills: string[];
  totalXp: number;
  university: UniversitySummary;
  userId: string;
  visibility: ProfileVisibility;
}

export interface UpdateCurrentUserRequest {
  avatarUrl?: string;
  bio?: string;
  coverImageUrl?: string;
  departmentId?: string;
  fullName?: string;
  location?: string;
  preferences?: Partial<UserPreferences>;
  semester?: number;
  universityId?: string;
  visibility?: ProfileVisibility;
}

export function getCurrentUserIdentity(signal?: AbortSignal) {
  return apiRequest<CurrentUserIdentity>("/users/me", { signal });
}

export const getCurrentUserProfile = getCurrentUserIdentity;

export function updateCurrentUser(request: UpdateCurrentUserRequest) {
  return apiRequest<CurrentUserIdentity>("/users/me", {
    body: JSON.stringify(request),
    method: "PATCH",
  });
}

export function replaceCurrentUserSkills(skills: string[]) {
  return apiRequest<CurrentUserIdentity>("/users/me/skills", {
    body: JSON.stringify({ skills }),
    method: "PUT",
  });
}
