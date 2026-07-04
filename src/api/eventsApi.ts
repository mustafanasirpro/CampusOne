import { apiRequest } from "@/api/apiClient";
import type {
  CreateEventRequest,
  EventDetail,
  EventListParameters,
  EventPage,
  EventParticipantState,
  UpdateEventRequest,
} from "@/types/events";

function queryString(parameters: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(parameters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  const value = query.toString();
  return value ? `?${value}` : "";
}

const eventsPath = "/events";

export function listEvents({
  page = 0,
  search,
  signal,
  size = 12,
  sort = "UPCOMING",
  status,
}: EventListParameters = {}) {
  return apiRequest<EventPage>(
    `${eventsPath}${queryString({ page, search, size, sort, status })}`,
    { signal },
  );
}

export function getMyEvents({
  page = 0,
  signal,
  size = 12,
  sort = "UPCOMING",
}: Pick<EventListParameters, "page" | "signal" | "size" | "sort"> = {}) {
  return apiRequest<EventPage>(
    `${eventsPath}/my${queryString({ page, size, sort })}`,
    { signal },
  );
}

export function getJoinedEvents({
  page = 0,
  signal,
  size = 12,
  sort = "UPCOMING",
}: Pick<EventListParameters, "page" | "signal" | "size" | "sort"> = {}) {
  return apiRequest<EventPage>(
    `${eventsPath}/joined${queryString({ page, size, sort })}`,
    { signal },
  );
}

export function getEventById(eventId: string, signal?: AbortSignal) {
  return apiRequest<EventDetail>(`${eventsPath}/${eventId}`, { signal });
}

export function createEvent(request: CreateEventRequest) {
  return apiRequest<EventDetail>(eventsPath, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function updateEvent(eventId: string, request: UpdateEventRequest) {
  return apiRequest<EventDetail>(`${eventsPath}/${eventId}`, {
    body: JSON.stringify(request),
    method: "PATCH",
  });
}

export function deleteEvent(eventId: string) {
  return apiRequest<void>(`${eventsPath}/${eventId}`, { method: "DELETE" });
}

export function joinEvent(eventId: string) {
  return apiRequest<EventParticipantState>(
    `${eventsPath}/${eventId}/participants`,
    { method: "POST" },
  );
}

export function leaveEvent(eventId: string) {
  return apiRequest<void>(`${eventsPath}/${eventId}/participants`, {
    method: "DELETE",
  });
}

export function getMyParticipantState(
  eventId: string,
  signal?: AbortSignal,
) {
  return apiRequest<EventParticipantState>(
    `${eventsPath}/${eventId}/participants/me`,
    { signal },
  );
}

