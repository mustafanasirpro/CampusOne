import type { EventStatus } from "@/types/events";

export function eventStatusLabel(status: EventStatus) {
  return {
    CANCELLED: "Cancelled",
    COMPLETED: "Completed",
    UPCOMING: "Upcoming",
  }[status];
}

export function formatEventDateTime(value: string) {
  return new Intl.DateTimeFormat("en-PK", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function toDateTimeInput(value: string) {
  const date = new Date(value);
  const localDate = new Date(
    date.getTime() - date.getTimezoneOffset() * 60_000,
  );
  return localDate.toISOString().slice(0, 16);
}

