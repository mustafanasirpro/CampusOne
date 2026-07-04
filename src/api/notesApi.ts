import { apiRequest } from "@/api/apiClient";
import type {
  BookmarkState,
  CreateNoteRequest,
  DownloadEventResult,
  NoteDetail,
  NoteListParameters,
  NotePage,
  NoteSort,
  RatingResult,
  UpdateNoteRequest,
} from "@/types/notes";

function queryString(parameters: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(parameters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      query.set(key, String(value));
    }
  });
  const value = query.toString();
  return value ? `?${value}` : "";
}

export function listNotes({
  courseId,
  page = 0,
  signal,
  size = 12,
  sort = "NEWEST",
  tag,
}: NoteListParameters = {}) {
  return apiRequest<NotePage>(
    `/notes${queryString({ courseId, page, size, sort, tag })}`,
    { signal },
  );
}

export function getMyNotes({
  page = 0,
  signal,
  size = 12,
  sort = "NEWEST",
}: Omit<NoteListParameters, "courseId" | "tag"> = {}) {
  return apiRequest<NotePage>(
    `/notes/my${queryString({ page, size, sort })}`,
    { signal },
  );
}

export function getNoteById(noteId: string, signal?: AbortSignal) {
  return apiRequest<NoteDetail>(`/notes/${noteId}`, { signal });
}

export function createNote(request: CreateNoteRequest) {
  return apiRequest<NoteDetail>("/notes", {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function updateNote(noteId: string, request: UpdateNoteRequest) {
  return apiRequest<NoteDetail>(`/notes/${noteId}`, {
    body: JSON.stringify(request),
    method: "PATCH",
  });
}

export function deleteNote(noteId: string) {
  return apiRequest<void>(`/notes/${noteId}`, { method: "DELETE" });
}

export function bookmarkNote(noteId: string) {
  return apiRequest<BookmarkState>(`/notes/${noteId}/bookmark`, {
    method: "POST",
  });
}

export function unbookmarkNote(noteId: string) {
  return apiRequest<BookmarkState>(`/notes/${noteId}/bookmark`, {
    method: "DELETE",
  });
}

export function rateNote(noteId: string, rating: number) {
  return apiRequest<RatingResult>(`/notes/${noteId}/rating`, {
    body: JSON.stringify({ rating }),
    method: "PUT",
  });
}

export function recordNoteDownload(noteId: string) {
  return apiRequest<DownloadEventResult>(
    `/notes/${noteId}/download-events`,
    { method: "POST" },
  );
}

export const noteSortOptions: Array<{ label: string; value: NoteSort }> = [
  { label: "Newest first", value: "NEWEST" },
  { label: "Highest rated", value: "RATING" },
  { label: "Most downloaded", value: "DOWNLOADS" },
];
