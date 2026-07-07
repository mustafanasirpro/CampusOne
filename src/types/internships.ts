export type InternshipType =
  | "FULL_TIME"
  | "PART_TIME"
  | "SUMMER"
  | "WINTER"
  | "REMOTE_INTERNSHIP";

export type InternshipWorkMode = "ONSITE" | "REMOTE" | "HYBRID";
export type InternshipStatus =
  | "PENDING_REVIEW"
  | "OPEN"
  | "CLOSED"
  | "EXPIRED"
  | "REJECTED";
export type InternshipSort =
  | "NEWEST"
  | "OLDEST"
  | "DEADLINE_ASC"
  | "DEADLINE_DESC";

export interface InternshipPoster {
  avatarUrl: string | null;
  fullName: string;
  university: string;
  userId: string;
}

export interface InternshipSummary {
  applyUrl: string;
  companyName: string;
  createdAt: string;
  currency: string | null;
  deadline: string;
  description: string;
  id: string;
  internshipType: InternshipType;
  location: string;
  ownedByCurrentUser: boolean;
  paid: boolean;
  poster: InternshipPoster;
  savedByCurrentUser: boolean;
  status: InternshipStatus;
  stipendAmount: number | null;
  title: string;
  updatedAt: string;
  workMode: InternshipWorkMode;
}

export type InternshipDetail = InternshipSummary;

export interface InternshipPage {
  content: InternshipSummary[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SavedInternshipState {
  internshipId: string;
  saved: boolean;
  savedAt: string | null;
  userId: string;
}

export interface CreateInternshipRequest {
  applyUrl: string;
  companyName: string;
  currency: string | null;
  deadline: string;
  description: string;
  internshipType: InternshipType;
  location: string;
  paid: boolean;
  stipendAmount: number | null;
  title: string;
  workMode: InternshipWorkMode;
}

export interface UpdateInternshipRequest {
  applyUrl?: string;
  companyName?: string;
  currency?: string | null;
  deadline?: string;
  description?: string;
  internshipType?: InternshipType;
  location?: string;
  paid?: boolean;
  status?: InternshipStatus;
  stipendAmount?: number | null;
  title?: string;
  workMode?: InternshipWorkMode;
}

export interface InternshipListParameters {
  internshipType?: InternshipType;
  page?: number;
  paid?: boolean;
  search?: string;
  signal?: AbortSignal;
  size?: number;
  sort?: InternshipSort;
  status?: InternshipStatus;
  workMode?: InternshipWorkMode;
}
