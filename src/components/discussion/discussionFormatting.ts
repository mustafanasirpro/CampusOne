import type {
  DiscussionCategory,
  DiscussionQuestionStatus,
} from "@/types/discussion";
import { formatDate } from "@/utils/format";

export const discussionCategoryOptions: Array<{
  label: string;
  value: DiscussionCategory;
}> = [
  { label: "General", value: "GENERAL" },
  { label: "Academic", value: "ACADEMIC" },
  { label: "Programming", value: "PROGRAMMING" },
  { label: "Exams", value: "EXAMS" },
  { label: "Career", value: "CAREER" },
  { label: "Campus", value: "CAMPUS" },
  { label: "Other", value: "OTHER" },
];

export function discussionCategoryLabel(category: DiscussionCategory) {
  return (
    discussionCategoryOptions.find((option) => option.value === category)
      ?.label ?? category
  );
}

export function discussionStatusLabel(status: DiscussionQuestionStatus) {
  return {
    CLOSED: "Closed",
    HIDDEN: "Hidden",
    OPEN: "Open",
    PENDING_REVIEW: "Pending review",
    REJECTED: "Rejected",
    RESOLVED: "Resolved",
  }[status];
}

export function formatDiscussionDate(value: string) {
  return formatDate(value);
}
