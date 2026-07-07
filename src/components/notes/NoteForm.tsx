import {
  FileArchive,
  FileText,
  Save,
  Trash2,
  UploadCloud,
} from "lucide-react";
import {
  useId,
  useRef,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react";

import { Button, Card, CardContent } from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import type {
  CreateNoteRequest,
  NoteDetail,
  NoteFileType,
  NoteVisibility,
  UpdateNoteRequest,
} from "@/types/notes";
import { cn } from "@/utils/cn";
import { formatFileSize } from "./noteFormatting";

type FormErrors = Partial<
  Record<keyof NoteFormState | "file", string>
>;

interface NoteFormState {
  courseCode: string;
  courseName: string;
  description: string;
  fileType: NoteFileType;
  semester: string;
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
      onSubmit: (
        request: CreateNoteRequest,
        file: File,
      ) => Promise<void>;
    })
  | (CommonNoteFormProps & {
      initialNote: NoteDetail;
      mode: "edit";
      onSubmit: (request: UpdateNoteRequest) => Promise<void>;
    });

const MAX_PDF_SIZE_MB = 25;
const MAX_PDF_SIZE_BYTES = MAX_PDF_SIZE_MB * 1024 * 1024;
const courseCodePattern = /^[A-Za-z0-9][A-Za-z0-9._ -]{1,29}$/;

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

const semesterOptions = Array.from({ length: 8 }, (_, index) => ({
  label: `Semester ${index + 1}`,
  value: String(index + 1),
}));

function initialState(initialNote?: NoteDetail): NoteFormState {
  return {
    courseCode: initialNote?.course.courseCode ?? "",
    courseName: initialNote?.course.title ?? "",
    description: initialNote?.description ?? "",
    fileType: initialNote?.fileType ?? "PDF",
    semester: String(initialNote?.semester ?? 1),
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
  const fileInputId = useId();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const initialNote =
    props.mode === "edit" ? props.initialNote : undefined;
  const [form, setForm] = useState<NoteFormState>(() =>
    initialState(initialNote),
  );
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
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
    props.backendFieldErrors?.[`note.${key}`]?.[0];

  const selectFile = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null;
    setErrors((current) => ({ ...current, file: undefined }));
    if (!file) {
      setSelectedFile(null);
      return;
    }
    if (file.size === 0) {
      setSelectedFile(null);
      setErrors((current) => ({
        ...current,
        file: "Select a non-empty PDF file.",
      }));
      event.target.value = "";
      return;
    }
    if (
      file.type !== "application/pdf" ||
      !file.name.toLowerCase().endsWith(".pdf")
    ) {
      setSelectedFile(null);
      setErrors((current) => ({
        ...current,
        file: "Only PDF files are allowed.",
      }));
      event.target.value = "";
      return;
    }
    if (file.size > MAX_PDF_SIZE_BYTES) {
      setSelectedFile(null);
      setErrors((current) => ({
        ...current,
        file: `File size must be ${MAX_PDF_SIZE_MB} MB or less.`,
      }));
      event.target.value = "";
      return;
    }
    setSelectedFile(file);
  };

  const removeSelectedFile = () => {
    setSelectedFile(null);
    setErrors((current) => ({ ...current, file: undefined }));
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const validate = () => {
    const nextErrors: FormErrors = {};
    const tags = normalizedTags(form.tags);

    if (!courseCodePattern.test(form.courseCode.trim())) {
      nextErrors.courseCode = "Use a normal course code like CSC275.";
    }
    if (form.courseName.trim() && form.courseName.trim().length < 2) {
      nextErrors.courseName = "Course name must contain at least 2 characters.";
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
    if (props.mode === "create" && !selectedFile) {
      nextErrors.file = "Select a PDF file to upload.";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (props.isSubmitting) return;
    if (!validate()) return;

    const commonRequest = {
      courseCode: form.courseCode.trim().toUpperCase(),
      courseName: form.courseName.trim() || undefined,
      description: form.description.trim(),
      fileType: props.mode === "create" ? ("PDF" as const) : form.fileType,
      semester: Number(form.semester),
      tags: normalizedTags(form.tags),
      teacherName: form.teacherName.trim(),
      title: form.title.trim(),
      visibility: form.visibility,
    };

    if (props.mode === "create") {
      if (!selectedFile) return;
      await props.onSubmit(commonRequest, selectedFile);
      return;
    }

    const initialTags = props.initialNote.tags.map((tag) => tag.name);
    const updateRequest: UpdateNoteRequest = {};
    if (commonRequest.courseCode !== props.initialNote.course.courseCode) {
      updateRequest.courseCode = commonRequest.courseCode;
    }
    if ((commonRequest.courseName ?? "") !== props.initialNote.course.title) {
      updateRequest.courseName = commonRequest.courseName;
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
              error={errors.courseCode ?? backendError("courseCode")}
              hint="Use a normal course code like CSC275, OOP, or DBMS."
              label="Course code"
              onChange={(event) => update("courseCode", event.target.value)}
              placeholder="e.g. CSC275"
              required
              value={form.courseCode}
            />
            <FormField
              error={errors.courseName ?? backendError("courseName")}
              hint="Optional; helps students recognize the course."
              label="Course name"
              maxLength={160}
              onChange={(event) => update("courseName", event.target.value)}
              placeholder="e.g. Computer Networking"
              value={form.courseName}
            />
          </div>

          <div className="grid gap-5 md:grid-cols-2">
            <FormField
              error={errors.teacherName ?? backendError("teacherName")}
              label="Teacher name"
              maxLength={120}
              onChange={(event) => update("teacherName", event.target.value)}
              placeholder="Instructor name"
              required
              value={form.teacherName}
            />
          </div>

          <div
            className={cn(
              "grid gap-5",
              props.mode === "create"
                ? "sm:grid-cols-2"
                : "sm:grid-cols-3",
            )}
          >
            <SelectField
              error={errors.semester ?? backendError("semester")}
              label="Semester"
              onChange={(event) => update("semester", event.target.value)}
              options={semesterOptions}
              required
              value={form.semester}
            />
            {props.mode === "edit" ? (
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
            ) : null}
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
                  PDF study resource
                </h2>
                <p className="mt-1 text-sm leading-6 text-slate-500">
                  Upload one PDF up to 25 MB. CampusOne handles secure
                  storage and file metadata automatically.
                </p>
              </div>
            </div>

            <label
              className={cn(
                "grid cursor-pointer place-items-center gap-3 rounded-2xl border-2 border-dashed px-5 py-8 text-center transition",
                errors.file || backendError("file")
                  ? "border-red-300 bg-red-50 hover:border-red-400"
                  : "border-slate-300 bg-slate-50 hover:border-brand-400 hover:bg-brand-50/40",
              )}
              htmlFor={fileInputId}
            >
              <UploadCloud className="size-8 text-brand-600" />
              <span>
                <span className="block text-sm font-semibold text-slate-800">
                  Select a PDF
                </span>
                <span className="mt-1 block text-xs text-slate-500">
                  PDF only, up to 25 MB
                </span>
              </span>
              <input
                accept=".pdf,application/pdf"
                className="sr-only"
                disabled={props.isSubmitting}
                id={fileInputId}
                onChange={selectFile}
                ref={fileInputRef}
                type="file"
              />
            </label>

            {errors.file || backendError("file") ? (
              <p className="text-sm font-medium text-red-600" role="alert">
                {errors.file ?? backendError("file")}
              </p>
            ) : null}

            {selectedFile ? (
              <div className="flex flex-wrap items-center gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-white text-emerald-700 shadow-sm">
                  <FileText className="size-5" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold text-slate-900">
                    {selectedFile.name}
                  </p>
                  <p className="mt-0.5 text-xs text-slate-500">
                    {formatFileSize(selectedFile.size)} · PDF
                  </p>
                </div>
                <Button
                  aria-label={`Remove ${selectedFile.name}`}
                  disabled={props.isSubmitting}
                  onClick={removeSelectedFile}
                  size="sm"
                  variant="outline"
                >
                  <Trash2 className="size-4" />
                  Remove
                </Button>
              </div>
            ) : null}
          </CardContent>
        </Card>
      ) : (
        <Card className="border-brand-100 bg-brand-50/40">
          <CardContent className="flex items-start gap-3 p-5 text-sm leading-6 text-brand-800">
            <FileText className="mt-0.5 size-5 shrink-0" />
            <span>
              This edit keeps the existing file{" "}
              <strong>{props.initialNote.file.originalFilename}</strong>.
              File replacement is not available on the metadata update
              endpoint.
            </span>
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
