import type {
  NoteModerationStatus,
  NoteVisibility,
} from "@/types/notes";

export function formatNoteDate(value: string) {
  return new Intl.DateTimeFormat("en-PK", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date(value));
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
