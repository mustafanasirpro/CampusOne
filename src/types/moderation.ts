export type ModeratorRole = "ADMIN" | "MODERATOR";

export type ModerationTargetType =
  | "NOTE"
  | "MARKETPLACE_LISTING"
  | "DISCUSSION_QUESTION"
  | "DISCUSSION_ANSWER"
  | "EVENT"
  | "INTERNSHIP"
  | "AI_GENERATED_ITEM"
  | "USER_PROFILE"
  | "SYSTEM";

export type ReportReason =
  | "SPAM"
  | "HARASSMENT"
  | "HATE_SPEECH"
  | "MISINFORMATION"
  | "INAPPROPRIATE_CONTENT"
  | "SCAM"
  | "DUPLICATE"
  | "PRIVACY_CONCERN"
  | "OTHER";

export type ReportStatus =
  | "PENDING"
  | "UNDER_REVIEW"
  | "RESOLVED"
  | "DISMISSED";

export type ModerationActionType =
  | "REPORT_REVIEWED"
  | "REPORT_RESOLVED"
  | "REPORT_DISMISSED"
  | "CONTENT_WARNING"
  | "CONTENT_HIDDEN"
  | "CONTENT_RESTORED"
  | "USER_WARNING"
  | "USER_RESTRICTED"
  | "USER_RESTORED"
  | "SYSTEM_NOTE";

export type ModerationSort = "NEWEST" | "OLDEST";

export interface ModeratorStatus {
  activeModerator: boolean;
  assignedAt: string | null;
  revokedAt: string | null;
  role: ModeratorRole | null;
}

export interface ReporterSummary {
  fullName: string;
  userId: string;
}

export interface ModeratorSummary {
  fullName: string;
  userId: string;
}

export interface ContentReportSummary {
  createdAt: string;
  id: string;
  reason: ReportReason;
  status: ReportStatus;
  targetId: string;
  targetType: ModerationTargetType;
  updatedAt: string;
}

export interface ContentReportDetail extends ContentReportSummary {
  details: string | null;
  reporter: ReporterSummary;
  resolutionNote: string | null;
  reviewedAt: string | null;
  reviewedBy: ModeratorSummary | null;
}

export interface ContentReportPage {
  content: ContentReportSummary[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ModerationAction {
  actionType: ModerationActionType;
  createdAt: string;
  id: string;
  metadata: unknown;
  moderator: ModeratorSummary;
  reason: string | null;
  reportId: string | null;
  targetId: string;
  targetType: ModerationTargetType;
}

export interface ModerationActionPage {
  content: ModerationAction[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateReportRequest {
  details?: string | null;
  reason: ReportReason;
  targetId: string;
  targetType: ModerationTargetType;
}

export interface MyReportListParameters {
  page?: number;
  signal?: AbortSignal;
  size?: number;
  sort?: ModerationSort;
  status?: ReportStatus;
  targetType?: ModerationTargetType;
}

export interface AdminReportListParameters extends MyReportListParameters {
  reason?: ReportReason;
  reporterUserId?: string;
}

export interface ModerationActionListParameters {
  actionType?: ModerationActionType;
  moderatorUserId?: string;
  page?: number;
  reportId?: string;
  signal?: AbortSignal;
  size?: number;
  sort?: ModerationSort;
  targetType?: ModerationTargetType;
}
