export type NotificationType =
  | "SYSTEM"
  | "USER_REMINDER"
  | "DISCUSSION_REPLY"
  | "DISCUSSION_ACCEPTED"
  | "MARKETPLACE_UPDATE"
  | "EVENT_UPDATE"
  | "EVENT_REMINDER"
  | "INTERNSHIP_POSTED"
  | "NOTE_ACTIVITY"
  | "LOST_FOUND_UPDATE"
  | "ADMIN_MESSAGE";

export type NotificationTargetType =
  | "SYSTEM"
  | "USER"
  | "NOTE"
  | "MARKETPLACE_LISTING"
  | "DISCUSSION_QUESTION"
  | "DISCUSSION_ANSWER"
  | "EVENT"
  | "INTERNSHIP"
  | "LOST_FOUND_ITEM"
  | "LOST_FOUND_CLAIM"
  | "LOST_FOUND_MATCH";

export type NotificationSort = "NEWEST" | "OLDEST";

export interface NotificationItem {
  actionUrl: string | null;
  createdAt: string;
  id: string;
  isRead: boolean;
  message: string;
  readAt: string | null;
  targetId: string | null;
  targetType: NotificationTargetType | null;
  title: string;
  type: NotificationType;
  updatedAt: string;
}

export interface NotificationPage {
  content: NotificationItem[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface NotificationUnreadCount {
  unreadCount: number;
}

export interface NotificationBulkAction {
  updatedCount: number;
}

export interface NotificationListParameters {
  page?: number;
  signal?: AbortSignal;
  size?: number;
  sort?: NotificationSort;
  targetType?: NotificationTargetType;
  type?: NotificationType;
  unreadOnly?: boolean;
}
