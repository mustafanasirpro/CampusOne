import { FileArchive, Save } from "lucide-react";
import { useState, type FormEvent } from "react";

import { Button, Card, CardContent } from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import type {
  CreateNoteRequest,
  NoteDetail,
  NoteFileType,
  NoteVisibility,
  StorageProvider,
  UpdateNoteRequest,
} from "@/types/notes";
import { cn } from "@/utils/cn";

type FormErrors = Partial<Record<keyof NoteFormState, string>>;

interface NoteFormState {
  bucketName: string;
  checksumSha256: string;
  courseId: string;
  description: string;
  expiresAt: string;
  fileType: NoteFileType;
  mimeType: string;
  objectKey: string;
  originalFilename: string;
  semester: string;
  sizeBytes: string;
  storageProvider: StorageProvider;
  tags: string;
  teacherName: string;
  title: string;
  visibility: NoteVisibility;
}

interface CommonNoteFormProps {
  backendFieldErrors?: Record<string, string[]>;
  isSubmitting: boolean;
  submitLabel: string;
}

type NoteFormProps =
  | (CommonNoteFormProps & {
      mode: "create";
      onSubmit: (request: CreateNoteRequest) => Promise<void>;
    })
  | (CommonNoteFormProps & {
      initialNote: NoteDetail;
      mode: "edit";
      onSubmit: (request: UpdateNoteRequest) => Promise<void>;
    });

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const checksumPattern = /^[0-9a-f]{64}$/i;
const mimeTypePattern = /^[^/\s]+\/[^/\s]+$/;

const fileTypeOptions = [
  "PDF",
  "PPT",
  "PPTX",
  "DOC",
  "DOCX",
  "IMAGE",
  "OTHER",
].map((value) => ({ label: value, value }));

const visibilityOptions = [
  { label: "Public", value: "PUBLIC" },
  { label: "Campus only", value: "CAMPUS" },
  { label: "Private", value: "PRIVATE" },
];

const storageOptions = [
  { label: "Local metadata", value: "LOCAL" },
  { label: "MinIO", value: "MINIO" },
  { label: "Amazon S3", value: "S3" },
  { label: "S3 compatible", value: "S3_COMPATIBLE" },
];

const semesterOptions = Array.from({ length: 8 }, (_, index) => ({
  label: `Semester ${index + 1}`,
  value: String(index + 1),
}));

function initialState(initialNote?: NoteDetail): NoteFormState {
  return {
    bucketName: "",
    checksumSha256: "",
    courseId: initialNote?.course.id ?? "",
    description: initialNote?.description ?? "",
    expiresAt: "",
    fileType: initialNote?.fileType ?? "PDF",
    mimeType: "",
    objectKey: "",
    originalFilename: "",
    semester: String(initialNote?.semester ?? 1),
    sizeBytes: "",
    storageProvider: "LOCAL",
    tags: initialNote?.tags.map((tag) => tag.name).join(", ") ?? "",
    teacherName: initialNote?.teacherName ?? "",
    title: initialNote?.title ?? "",
    visibility: initialNote?.visibility ?? "PUBLIC",
  };
}

function TextAreaField({
  error,
  label,
  maxLength,
  onChange,
  value,
}: {
  error?: string;
  label: string;
  maxLength: number;
  onChange: (value: string) => void;
  value: string;
}) {
  return (
    <label className="grid gap-1.5">
      <span className="text-sm font-semibold text-slate-700">
        {label}
        <span aria-hidden="true" className="ml-1 text-red-500">
          *
        </span>
      </span>
      <textarea
        aria-invalid={Boolean(error)}
        className={cn(
          "min-h-36 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:ring-4",
          error
            ? "border-red-300 focus:border-red-400 focus:ring-red-100"
            : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
        )}
        maxLength={maxLength}
        onChange={(event) => onChange(event.target.value)}
        required
        value={value}
      />
      <span className="flex justify-between gap-3 text-xs">
        <span className={error ? "font-medium text-red-600" : "text-slate-400"}>
          {error ?? "Describe what students will find in this note."}
        </span>
        <span className="shrink-0 text-slate-400">
          {value.length}/{maxLength}
        </span>
      </span>
    </label>
  );
}

export function NoteForm(props: NoteFormProps) {
  const initialNote =
    props.mode === "edit" ? props.initialNote : undefined;
  const [form, setForm] = useState<NoteFormState>(() =>
    initialState(initialNote),
  );
  const [errors, setErrors] = useState<FormErrors>({});

  const update = <Key extends keyof NoteFormState>(
    key: Key,
    value: NoteFormState[Key],
  ) => {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: undefined }));
  };

  const backendError = (key: string) =>
    props.backendFieldErrors?.[key]?.[0] ??
    props.backendFieldErrors?.[`file.${key}`]?.[0];

  const validate = () => {
    const nextErrors: FormErrors = {};
    const tags = normalizedTags(form.tags);

    if (!uuidPattern.test(form.courseId.trim())) {
      nextErrors.courseId = "Enter a valid course UUID.";
    }
    if (form.title.trim().length < 5) {
      nextErrors.title = "Title must contain at least 5 characters.";
    }
    if (form.description.trim().length < 10) {
      nextErrors.description =
        "Description must contain at least 10 characters.";
    }
    if (form.teacherName.trim().length < 2) {
      nextErrors.teacherName =
        "Teacher name must contain at least 2 characters.";
    }
    if (tags.length > 10) {
      nextErrors.tags = "Use no more than 10 tags.";
    } else if (tags.some((tag) => tag.length < 2 || tag.length > 40)) {
      nextErrors.tags = "Each tag must contain 2 to 40 characters.";
    }

    if (props.mode === "create") {
      if (form.bucketName.trim().length < 3) {
        nextErrors.bucketName =
          "Bucket name must contain at least 3 characters.";
      }
      if (!form.objectKey.trim()) {
        nextErrors.objectKey = "Enter the storage object key.";
      }
      if (!form.originalFilename.trim()) {
        nextErrors.originalFilename = "Enter the original filename.";
      }
      if (!mimeTypePattern.test(form.mimeType.trim())) {
        nextErrors.mimeType =
          "Use a valid MIME type such as application/pdf.";
      }
      if (!Number.isFinite(Number(form.sizeBytes)) || Number(form.sizeBytes) <= 0) {
        nextErrors.sizeBytes = "File size must be greater than zero.";
      }
      if (
        form.checksumSha256.trim() &&
        !checksumPattern.test(form.checksumSha256.trim())
      ) {
        nextErrors.checksumSha256 =
          "Checksum must contain exactly 64 hexadecimal characters.";
      }
      if (form.expiresAt) {
        const expiresAt = new Date(form.expiresAt);
        if (Number.isNaN(expiresAt.getTime())) {
          nextErrors.expiresAt = "Choose a valid expiry date.";
        } else if (expiresAt.getTime() <= Date.now()) {
          nextErrors.expiresAt = "Expiry must be in the future.";
        }
      }
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!validate()) return;

    const commonRequest = {
      courseId: form.courseId.trim(),
      description: form.description.trim(),
      fileType: form.fileType,
      semester: Number(form.semester),
      tags: normalizedTags(form.tags),
      teacherName: form.teacherName.trim(),
      title: form.title.trim(),
      visibility: form.visibility,
    };

    if (props.mode === "create") {
      await props.onSubmit({
        ...commonRequest,
        file: {
          bucketName: form.bucketName.trim(),
          checksumSha256:
            form.checksumSha256.trim().toLowerCase() || null,
          expiresAt: form.expiresAt
            ? new Date(form.expiresAt).toISOString()
            : null,
          mimeType: form.mimeType.trim().toLowerCase(),
          objectKey: form.objectKey.trim(),
          originalFilename: form.originalFilename.trim(),
          sizeBytes: Number(form.sizeBytes),
          storageProvider: form.storageProvider,
        },
      });
    } else {
      const initialTags = props.initialNote.tags.map((tag) => tag.name);
      const updateRequest: UpdateNoteRequest = {};
      if (commonRequest.courseId !== props.initialNote.course.id) {
        updateRequest.courseId = commonRequest.courseId;
      }
      if (commonRequest.title !== props.initialNote.title) {
        updateRequest.title = commonRequest.title;
      }
      if (commonRequest.description !== props.initialNote.description) {
        updateRequest.description = commonRequest.description;
      }
      if (commonRequest.teacherName !== props.initialNote.teacherName) {
        updateRequest.teacherName = commonRequest.teacherName;
      }
      if (commonRequest.semester !== props.initialNote.semester) {
        updateRequest.semester = commonRequest.semester;
      }
      if (commonRequest.fileType !== props.initialNote.fileType) {
        updateRequest.fileType = commonRequest.fileType;
      }
      if (commonRequest.visibility !== props.initialNote.visibility) {
        updateRequest.visibility = commonRequest.visibility;
      }
      if (!sameTags(commonRequest.tags, initialTags)) {
        updateRequest.tags = commonRequest.tags;
      }
      await props.onSubmit(updateRequest);
    }
  };

  return (
    <form className="grid gap-6" noValidate onSubmit={handleSubmit}>
      <Card>
        <CardContent className="grid gap-5 p-5 sm:p-6">
          <div>
            <h2 className="text-lg font-semibold text-slate-950">
              Note information
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              New and materially edited notes are submitted for moderation.
            </p>
          </div>

          <FormField
            error={errors.title ?? backendError("title")}
            label="Note title"
            maxLength={160}
            onChange={(event) => update("title", event.target.value)}
            placeholder="Object-Oriented Programming final revision notes"
            required
            value={form.title}
          />

          <TextAreaField
            error={errors.description ?? backendError("description")}
            label="Description"
            maxLength={2000}
            onChange={(value) => update("description", value)}
            value={form.description}
          />

          <div className="grid gap-5 md:grid-cols-2">
            <FormField
              error={errors.courseId ?? backendError("courseId")}
              hint="No public course-directory API is available yet."
              label="Course ID"
              onChange={(event) => update("courseId", event.target.value)}
              placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
              required
              value={form.courseId}
            />
            <FormField
              error={errors.teacherName ?? backendError("teacherName")}
              label="Teacher name"
              maxLength={120}
              onChange={(event) => update("teacherName", event.target.value)}
              placeholder="Dr. Ayesha Malik"
              required
              value={form.teacherName}
            />
          </div>

          <div className="grid gap-5 sm:grid-cols-3">
            <SelectField
              error={errors.semester ?? backendError("semester")}
              label="Semester"
              onChange={(event) => update("semester", event.target.value)}
              options={semesterOptions}
              required
              value={form.semester}
            />
            <SelectField
              error={errors.fileType ?? backendError("fileType")}
              label="File type"
              onChange={(event) =>
                update("fileType", event.target.value as NoteFileType)
              }
              options={fileTypeOptions}
              required
              value={form.fileType}
            />
            <SelectField
              error={errors.visibility ?? backendError("visibility")}
              label="Visibility"
              onChange={(event) =>
                update("visibility", event.target.value as NoteVisibility)
              }
              options={visibilityOptions}
              required
              value={form.visibility}
            />
          </div>

          <FormField
            error={errors.tags ?? backendError("tags")}
            hint="Comma-separated; maximum 10 tags, 2–40 characters each."
            label="Tags"
            onChange={(event) => update("tags", event.target.value)}
            placeholder="java, oop, final-exam"
            value={form.tags}
          />
        </CardContent>
      </Card>

      {props.mode === "create" ? (
        <Card>
          <CardContent className="grid gap-5 p-5 sm:p-6">
            <div className="flex items-start gap-3">
              <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600">
                <FileArchive className="size-5" />
              </span>
              <div>
                <h2 className="text-lg font-semibold text-slate-950">
                  File metadata
                </h2>
                <p className="mt-1 text-sm leading-6 text-slate-500">
                  Binary upload is not enabled yet. Enter metadata for an
                  object that will later be managed by CampusOne storage.
                </p>
              </div>
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <SelectField
                error={
                  errors.storageProvider ??
                  backendError("storageProvider")
                }
                label="Storage provider"
                onChange={(event) =>
                  update(
                    "storageProvider",
                    event.target.value as StorageProvider,
                  )
                }
                options={storageOptions}
                required
                value={form.storageProvider}
              />
              <FormField
                error={errors.bucketName ?? backendError("bucketName")}
                label="Bucket name"
                maxLength={100}
                onChange={(event) =>
                  update("bucketName", event.target.value)
                }
                placeholder="campusone-notes"
                required
                value={form.bucketName}
              />
            </div>

            <FormField
              error={errors.objectKey ?? backendError("objectKey")}
              label="Object key"
              maxLength={1024}
              onChange={(event) => update("objectKey", event.target.value)}
              placeholder="notes/2026/oop-final-revision.pdf"
              required
              value={form.objectKey}
            />

            <div className="grid gap-5 sm:grid-cols-2">
              <FormField
                error={
                  errors.originalFilename ??
                  backendError("originalFilename")
                }
                label="Original filename"
                maxLength={255}
                onChange={(event) =>
                  update("originalFilename", event.target.value)
                }
                placeholder="oop-final-revision.pdf"
                required
                value={form.originalFilename}
              />
              <FormField
                error={errors.mimeType ?? backendError("mimeType")}
                label="MIME type"
                maxLength={127}
                onChange={(event) => update("mimeType", event.target.value)}
                placeholder="application/pdf"
                required
                value={form.mimeType}
              />
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <FormField
                error={errors.sizeBytes ?? backendError("sizeBytes")}
                label="File size in bytes"
                min={1}
                onChange={(event) => update("sizeBytes", event.target.value)}
                placeholder="1048576"
                required
                type="number"
                value={form.sizeBytes}
              />
              <FormField
                error={errors.expiresAt ?? backendError("expiresAt")}
                hint="Optional; must be in the future."
                label="Metadata expiry"
                onChange={(event) => update("expiresAt", event.target.value)}
                type="datetime-local"
                value={form.expiresAt}
              />
            </div>

            <FormField
              error={
                errors.checksumSha256 ??
                backendError("checksumSha256")
              }
              hint="Optional 64-character SHA-256 checksum."
              label="SHA-256 checksum"
              maxLength={64}
              onChange={(event) =>
                update("checksumSha256", event.target.value)
              }
              placeholder="64 hexadecimal characters"
              value={form.checksumSha256}
            />
          </CardContent>
        </Card>
      ) : (
        <Card className="border-brand-100 bg-brand-50/40">
          <CardContent className="p-5 text-sm leading-6 text-brand-800">
            The backend does not allow replacing file metadata through the
            note update endpoint. This edit updates descriptive fields only.
          </CardContent>
        </Card>
      )}

      <div className="flex justify-end">
        <Button
          className="w-full sm:w-auto"
          loading={props.isSubmitting}
          size="lg"
          type="submit"
        >
          <Save className="size-4" />
          {props.isSubmitting ? "Saving note" : props.submitLabel}
        </Button>
      </div>
    </form>
  );
}

function normalizedTags(value: string) {
  const seen = new Set<string>();
  return value
    .split(",")
    .map((tag) => tag.trim())
    .filter((tag) => {
      const key = tag.toLowerCase();
      if (!tag || seen.has(key)) return false;
      seen.add(key);
      return true;
    });
}

function sameTags(first: string[], second: string[]) {
  if (first.length !== second.length) return false;
  return first.every(
    (tag, index) =>
      tag.toLowerCase() === second[index]?.toLowerCase(),
  );
}
