import { apiRequest } from "@/api/apiClient";
import type {
  GamificationBadge,
  GamificationProfile,
  LeaderboardPage,
  LeaderboardPeriod,
  PublicGamificationProfile,
  UserBadge,
  XpHistoryPage,
} from "@/types/gamification";

function queryString(parameters: Record<string, string | number>) {
  const query = new URLSearchParams();
  Object.entries(parameters).forEach(([key, value]) =>
    query.set(key, String(value)),
  );
  return `?${query.toString()}`;
}

const gamificationPath = "/gamification";

export function getMyGamificationProfile(signal?: AbortSignal) {
  return apiRequest<GamificationProfile>(`${gamificationPath}/me`, {
    signal,
  });
}

export function getPublicGamificationProfile(
  userId: string,
  signal?: AbortSignal,
) {
  return apiRequest<PublicGamificationProfile>(
    `${gamificationPath}/users/${userId}`,
    { signal },
  );
}

export function getLeaderboard(
  period: LeaderboardPeriod,
  page = 0,
  size = 10,
  signal?: AbortSignal,
) {
  return apiRequest<LeaderboardPage>(
    `${gamificationPath}/leaderboard${queryString({ page, period, size })}`,
    { signal },
  );
}

export function listBadges(signal?: AbortSignal) {
  return apiRequest<GamificationBadge[]>(`${gamificationPath}/badges`, {
    signal,
  });
}

export function getMyBadges(signal?: AbortSignal) {
  return apiRequest<UserBadge[]>(`${gamificationPath}/me/badges`, {
    signal,
  });
}

export function getMyXpHistory(
  page = 0,
  size = 10,
  signal?: AbortSignal,
) {
  return apiRequest<XpHistoryPage>(
    `${gamificationPath}/me/xp-history${queryString({ page, size })}`,
    { signal },
  );
}

