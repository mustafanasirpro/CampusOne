import type { EventStatus } from "@/types/events";
import { formatDateTime } from "@/utils/format";

export function eventStatusLabel(status: EventStatus) {
  return {
    CANCELLED: "Cancelled",
    COMPLETED: "Completed",
    PENDING_REVIEW: "Pending review",
    REJECTED: "Rejected",
    UPCOMING: "Upcoming",
  }[status];
}

export function formatEventDateTime(value: string) {
  return formatDateTime(value);
}

export function toDateTimeInput(value: string) {
  const date = new Date(value);
  const localDate = new Date(
    date.getTime() - date.getTimezoneOffset() * 60_000,
  );
  return localDate.toISOString().slice(0, 16);
}
