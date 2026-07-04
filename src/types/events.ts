export type EventVisibility = "PUBLIC" | "PRIVATE";
export type EventStatus = "UPCOMING" | "CANCELLED" | "COMPLETED";
export type EventSort = "NEWEST" | "OLDEST" | "UPCOMING";

export interface EventOrganizer {
  avatarUrl: string | null;
  fullName: string;
  university: string;
  userId: string;
}

export interface EventSummary {
  capacity: number;
  createdAt: string;
  description: string;
  endTime: string;
  id: string;
  joinedByCurrentUser: boolean;
  location: string;
  organizer: EventOrganizer;
  ownedByCurrentUser: boolean;
  participantCount: number;
  startTime: string;
  status: EventStatus;
  title: string;
  updatedAt: string;
  visibility: EventVisibility;
}

export type EventDetail = EventSummary;

export interface EventPage {
  content: EventSummary[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface EventParticipantState {
  eventId: string;
  joined: boolean;
  joinedAt: string | null;
  participantCount: number;
  userId: string;
}

export interface CreateEventRequest {
  capacity: number;
  description: string;
  endTime: string;
  location: string;
  startTime: string;
  title: string;
  visibility: EventVisibility;
}

export interface UpdateEventRequest {
  capacity?: number;
  description?: string;
  endTime?: string;
  location?: string;
  startTime?: string;
  status?: EventStatus;
  title?: string;
  visibility?: EventVisibility;
}

export interface EventListParameters {
  page?: number;
  search?: string;
  signal?: AbortSignal;
  size?: number;
  sort?: EventSort;
  status?: EventStatus;
}

