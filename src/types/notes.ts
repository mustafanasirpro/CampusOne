export type NoteFileType =
  | "PDF"
  | "PPT"
  | "PPTX"
  | "DOC"
  | "DOCX"
  | "IMAGE"
  | "OTHER";

export type NoteVisibility = "PUBLIC" | "CAMPUS" | "PRIVATE";
export type NoteModerationStatus =
  | "PENDING"
  | "APPROVED"
  | "REJECTED"
  | "HIDDEN";
export type NoteSort = "NEWEST" | "RATING" | "DOWNLOADS";
export type StorageProvider = "LOCAL" | "MINIO" | "S3" | "S3_COMPATIBLE";
export type FileAssetStatus =
  | "PENDING"
  | "QUARANTINED"
  | "READY"
  | "REJECTED"
  | "DELETED";

export interface CourseSummary {
  active: boolean;
  courseCode: string;
  departmentId: string;
  id: string;
  recommendedSemester: number | null;
  title: string;
}

export interface NoteUploader {
  avatarUrl: string | null;
  fullName: string;
  university: string;
  userId: string;
}

export interface NoteTag {
  id: string;
  name: string;
}

export interface NoteFileMetadata {
  createdAt: string;
  id: string;
  mimeType: string;
  originalFilename: string;
  sizeBytes: number;
  status: FileAssetStatus;
  storageProvider: StorageProvider;
}

export interface NoteSummary {
  averageRating: number;
  course: CourseSummary;
  createdAt: string;
  downloadCount: number;
  fileType: NoteFileType;
  id: string;
  moderationStatus: NoteModerationStatus;
  ratingCount: number;
  semester: number;
  tags: NoteTag[];
  teacherName: string;
  title: string;
  updatedAt: string;
  uploader: NoteUploader;
  visibility: NoteVisibility;
}

export interface NoteDetail extends NoteSummary {
  bookmarked: boolean;
  contentVersion: number;
  currentUserRating: number | null;
  description: string;
  file: NoteFileMetadata;
  moderationReason: string | null;
  publishedAt: string | null;
}

export interface NotePage {
  content: NoteSummary[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FileMetadataRequest {
  bucketName: string;
  checksumSha256?: string | null;
  expiresAt?: string | null;
  mimeType: string;
  objectKey: string;
  originalFilename: string;
  sizeBytes: number;
  storageProvider: StorageProvider;
}

export interface CreateNoteRequest {
  courseId: string;
  description: string;
  file: FileMetadataRequest;
  fileType: NoteFileType;
  semester: number;
  tags: string[];
  teacherName: string;
  title: string;
  visibility: NoteVisibility;
}

export interface UpdateNoteRequest {
  courseId?: string;
  description?: string;
  fileType?: NoteFileType;
  semester?: number;
  tags?: string[];
  teacherName?: string;
  title?: string;
  visibility?: NoteVisibility;
}

export interface NoteListParameters {
  courseId?: string;
  page?: number;
  signal?: AbortSignal;
  size?: number;
  sort?: NoteSort;
  tag?: string;
}

export interface BookmarkState {
  bookmarked: boolean;
  noteId: string;
}

export interface RatingResult {
  averageRating: number;
  noteId: string;
  rating: number;
  ratingCount: number;
}

export interface DownloadEventResult {
  downloadedAt: string;
  downloadCount: number;
  eventId: string;
  noteId: string;
}
