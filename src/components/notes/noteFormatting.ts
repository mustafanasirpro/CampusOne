import type {
  NoteModerationStatus,
  NoteVisibility,
} from "@/types/notes";
import { formatDate } from "@/utils/format";

export function formatNoteDate(value: string) {
  return formatDate(value);
}

export function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function moderationLabel(status: NoteModerationStatus) {
  return {
    APPROVED: "Approved",
    HIDDEN: "Hidden",
    PENDING: "Pending review",
    REJECTED: "Rejected",
  }[status];
}

export function visibilityLabel(visibility: NoteVisibility) {
  return {
    CAMPUS: "Campus",
    PRIVATE: "Private",
    PUBLIC: "Public",
  }[visibility];
}
