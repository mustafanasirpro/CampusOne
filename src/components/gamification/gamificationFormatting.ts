import type {
  GamificationActionType,
  LeaderboardPeriod,
} from "@/types/gamification";

export function leaderboardPeriodLabel(period: LeaderboardPeriod) {
  return {
    ALL_TIME: "All time",
    MONTHLY: "This month",
    WEEKLY: "This week",
  }[period];
}

export function gamificationActionLabel(action: GamificationActionType) {
  return action
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

export function formatGamificationDate(value: string) {
  return new Intl.DateTimeFormat("en-PK", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

