import type {
  ModerationActionType,
  ModerationTargetType,
  ReportReason,
  ReportStatus,
} from "@/types/moderation";
import { formatDateTime } from "@/utils/format";

export const moderationTargetOptions: Array<{
  label: string;
  value: ModerationTargetType;
}> = [
  { label: "Note", value: "NOTE" },
  { label: "Marketplace listing", value: "MARKETPLACE_LISTING" },
  { label: "Discussion question", value: "DISCUSSION_QUESTION" },
  { label: "Discussion answer", value: "DISCUSSION_ANSWER" },
  { label: "Event", value: "EVENT" },
  { label: "Internship", value: "INTERNSHIP" },
  { label: "AI generated item", value: "AI_GENERATED_ITEM" },
  { label: "User profile", value: "USER_PROFILE" },
  { label: "System", value: "SYSTEM" },
];

export const reportReasonOptions: Array<{
  label: string;
  value: ReportReason;
}> = [
  { label: "Spam", value: "SPAM" },
  { label: "Harassment", value: "HARASSMENT" },
  { label: "Hate speech", value: "HATE_SPEECH" },
  { label: "Misinformation", value: "MISINFORMATION" },
  { label: "Inappropriate content", value: "INAPPROPRIATE_CONTENT" },
  { label: "Scam", value: "SCAM" },
  { label: "Duplicate", value: "DUPLICATE" },
  { label: "Privacy concern", value: "PRIVACY_CONCERN" },
  { label: "Other", value: "OTHER" },
];

export const reportStatusOptions: Array<{
  label: string;
  value: ReportStatus;
}> = [
  { label: "Pending", value: "PENDING" },
  { label: "Under review", value: "UNDER_REVIEW" },
  { label: "Resolved", value: "RESOLVED" },
  { label: "Dismissed", value: "DISMISSED" },
];

export const moderationActionOptions: Array<{
  label: string;
  value: ModerationActionType;
}> = [
  { label: "Report reviewed", value: "REPORT_REVIEWED" },
  { label: "Report resolved", value: "REPORT_RESOLVED" },
  { label: "Report dismissed", value: "REPORT_DISMISSED" },
  { label: "Content approved", value: "CONTENT_APPROVED" },
  { label: "Content rejected", value: "CONTENT_REJECTED" },
  { label: "Content warning", value: "CONTENT_WARNING" },
  { label: "Content hidden", value: "CONTENT_HIDDEN" },
  { label: "Content restored", value: "CONTENT_RESTORED" },
  { label: "User warning", value: "USER_WARNING" },
  { label: "User restricted", value: "USER_RESTRICTED" },
  { label: "User restored", value: "USER_RESTORED" },
  { label: "System note", value: "SYSTEM_NOTE" },
];

export function formatModerationDate(value: string | null) {
  return formatDateTime(value, "Not available");
}

export function shortModerationId(value: string) {
  return `${value.slice(0, 8)}…`;
}

export function targetTypeLabel(value: ModerationTargetType) {
  return (
    moderationTargetOptions.find((option) => option.value === value)?.label ??
    value
  );
}

export function reportReasonLabel(value: ReportReason) {
  return (
    reportReasonOptions.find((option) => option.value === value)?.label ?? value
  );
}

export function actionTypeLabel(value: ModerationActionType) {
  return (
    moderationActionOptions.find((option) => option.value === value)?.label ??
    value
  );
}

export function isUuid(value: string) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
    value,
  );
}
