import type { NotificationType } from "@/types/notifications";
import { formatDateTime } from "@/utils/format";

export const notificationTypeOptions: Array<{
  label: string;
  value: NotificationType;
}> = [
  { label: "System", value: "SYSTEM" },
  { label: "Reminders", value: "USER_REMINDER" },
  { label: "Discussion replies", value: "DISCUSSION_REPLY" },
  { label: "Accepted answers", value: "DISCUSSION_ACCEPTED" },
  { label: "Marketplace", value: "MARKETPLACE_UPDATE" },
  { label: "Event updates", value: "EVENT_UPDATE" },
  { label: "Event reminders", value: "EVENT_REMINDER" },
  { label: "Internships", value: "INTERNSHIP_POSTED" },
  { label: "Note activity", value: "NOTE_ACTIVITY" },
  { label: "Admin messages", value: "ADMIN_MESSAGE" },
];

export function notificationTypeLabel(type: NotificationType) {
  return notificationTypeOptions.find((option) => option.value === type)?.label ?? type;
}

export function formatNotificationDate(value: string) {
  return formatDateTime(value);
}
