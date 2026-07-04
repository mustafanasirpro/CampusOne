import { apiRequest } from "@/api/apiClient";
import type {
  NotificationBulkAction,
  NotificationItem,
  NotificationListParameters,
  NotificationPage,
  NotificationUnreadCount,
} from "@/types/notifications";

function queryString(
  parameters: Record<string, boolean | string | number | undefined>,
) {
  const query = new URLSearchParams();
  Object.entries(parameters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  const value = query.toString();
  return value ? `?${value}` : "";
}

const notificationsPath = "/notifications";

export function listNotifications({
  page = 0,
  signal,
  size = 12,
  sort = "NEWEST",
  targetType,
  type,
  unreadOnly,
}: NotificationListParameters = {}) {
  return apiRequest<NotificationPage>(
    `${notificationsPath}${queryString({
      page,
      size,
      sort,
      targetType,
      type,
      unreadOnly,
    })}`,
    { signal },
  );
}

export function getNotificationById(
  notificationId: string,
  signal?: AbortSignal,
) {
  return apiRequest<NotificationItem>(
    `${notificationsPath}/${notificationId}`,
    { signal },
  );
}

export function getUnreadCount(signal?: AbortSignal) {
  return apiRequest<NotificationUnreadCount>(
    `${notificationsPath}/unread-count`,
    { signal },
  );
}

export function markNotificationRead(notificationId: string) {
  return apiRequest<NotificationItem>(
    `${notificationsPath}/${notificationId}/read`,
    { method: "PATCH" },
  );
}

export function markNotificationUnread(notificationId: string) {
  return apiRequest<NotificationItem>(
    `${notificationsPath}/${notificationId}/unread`,
    { method: "PATCH" },
  );
}

export function markAllNotificationsRead() {
  return apiRequest<NotificationBulkAction>(
    `${notificationsPath}/read-all`,
    { method: "PATCH" },
  );
}

export function deleteNotification(notificationId: string) {
  return apiRequest<void>(`${notificationsPath}/${notificationId}`, {
    method: "DELETE",
  });
}

