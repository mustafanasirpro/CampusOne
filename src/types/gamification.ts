export type LeaderboardPeriod = "ALL_TIME" | "MONTHLY" | "WEEKLY";

export type GamificationSourceType =
  | "USER"
  | "NOTE"
  | "MARKETPLACE_LISTING"
  | "DISCUSSION_QUESTION"
  | "DISCUSSION_ANSWER"
  | "EVENT"
  | "INTERNSHIP"
  | "SYSTEM";

export type GamificationActionType =
  | "PROFILE_COMPLETED"
  | "NOTE_CREATED"
  | "NOTE_DOWNLOADED"
  | "NOTE_RATED"
  | "MARKETPLACE_LISTING_CREATED"
  | "DISCUSSION_QUESTION_CREATED"
  | "DISCUSSION_ANSWER_CREATED"
  | "DISCUSSION_ANSWER_ACCEPTED"
  | "EVENT_CREATED"
  | "EVENT_JOINED"
  | "INTERNSHIP_CREATED"
  | "DAILY_LOGIN"
  | "SYSTEM_AWARD";

export interface GamificationBadge {
  active: boolean;
  category: string;
  code: string;
  description: string;
  icon: string | null;
  id: string;
  name: string;
  sortOrder: number;
  xpRequired: number;
}

export interface UserBadge {
  awardedAt: string;
  badge: GamificationBadge;
  sourceId: string | null;
  sourceType: GamificationSourceType | null;
}

export interface GamificationProfile {
  badges: UserBadge[];
  createdAt: string;
  currentStreak: number;
  fullName: string;
  lastActivityAt: string | null;
  level: number;
  longestStreak: number;
  totalXp: number;
  updatedAt: string;
  userId: string;
}

export interface PublicGamificationProfile {
  badges: GamificationBadge[];
  fullName: string;
  level: number;
  totalXp: number;
  userId: string;
}

export interface LeaderboardEntry {
  allTimeXp: number;
  fullName: string;
  level: number;
  rank: number;
  totalXpForPeriod: number;
  userId: string;
}

export interface LeaderboardPage {
  content: LeaderboardEntry[];
  first: boolean;
  last: boolean;
  page: number;
  period: LeaderboardPeriod;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface XpTransaction {
  actionType: GamificationActionType;
  createdAt: string;
  description: string | null;
  id: string;
  points: number;
  sourceId: string | null;
  sourceType: GamificationSourceType | null;
}

export interface XpHistoryPage {
  content: XpTransaction[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

