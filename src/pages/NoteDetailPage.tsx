import {
  ArrowDownToLine,
  ArrowLeft,
  Bookmark,
  CalendarDays,
  Edit3,
  FileText,
  GraduationCap,
  ShieldCheck,
  Star,
  Trash2,
  UserRound,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  bookmarkNote,
  deleteNote,
  getNoteById,
  getNoteManagementStatus,
  rateNote,
  recordNoteDownload,
  unbookmarkNote,
} from "@/api/notesApi";
import {
  Badge,
  Button,
  Card,
  CardContent,
  ErrorMessage,
  LoadingSpinner,
  useToast,
} from "@/components/common";
import {
  formatFileSize,
  formatNoteDate,
  moderationLabel,
  NoteRating,
  visibilityLabel,
} from "@/components/notes";
import { paths } from "@/routes/paths";
import type { NoteDetail } from "@/types/notes";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

function actionErrorMessage(error: unknown) {
  return error instanceof ApiError
    ? error.message
    : "The note action could not be completed.";
}

export function NoteDetailPage() {
  const { noteId } = useParams<{ noteId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [note, setNote] = useState<NoteDetail | null>(null);
  const [canManage, setCanManage] = useState(false);
  const [error, setError] = useState<string | null>(() =>
    noteId ? null : "The note ID is missing.",
  );
  const [actionError, setActionError] = useState<string | null>(null);
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(noteId));

  useDocumentTitle(note ? `${note.title} · CampusOne` : "Note · CampusOne");

  useEffect(() => {
    if (!noteId) return;

    const controller = new AbortController();
    let active = true;

    void Promise.all([
      getNoteById(noteId, controller.signal),
      getNoteManagementStatus(controller.signal).catch(() => ({
        canManage: false,
      })),
    ])
      .then(([noteResponse, managementStatus]) => {
        if (!active) return;
        setNote(noteResponse);
        setCanManage(managementStatus.canManage);
      })
      .catch((requestError: unknown) => {
        if (active) setError(actionErrorMessage(requestError));
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [noteId]);

  const toggleBookmark = async () => {
    if (!note) return;
    setBusyAction("bookmark");
    setActionError(null);
    try {
      const response = note.bookmarked
        ? await unbookmarkNote(note.id)
        : await bookmarkNote(note.id);
      setNote((current) =>
        current
          ? { ...current, bookmarked: response.bookmarked }
          : current,
      );
      showToast({
        title: response.bookmarked ? "Note bookmarked" : "Bookmark removed",
        message: note.title,
        variant: "success",
      });
    } catch (requestError) {
      setActionError(actionErrorMessage(requestError));
    } finally {
      setBusyAction(null);
    }
  };

  const submitRating = async (rating: number) => {
    if (!note) return;
    setBusyAction("rating");
    setActionError(null);
    try {
      const response = await rateNote(note.id, rating);
      setNote((current) =>
        current
          ? {
              ...current,
              averageRating: response.averageRating,
              currentUserRating: response.rating,
              ratingCount: response.ratingCount,
            }
          : current,
      );
      showToast({
        title: "Rating saved",
        message: `You rated this note ${rating} out of 5.`,
        variant: "success",
      });
    } catch (requestError) {
      setActionError(actionErrorMessage(requestError));
    } finally {
      setBusyAction(null);
    }
  };

  const recordDownload = async () => {
    if (!note) return;
    setBusyAction("download");
    setActionError(null);
    try {
      const response = await recordNoteDownload(note.id);
      setNote((current) =>
        current
          ? { ...current, downloadCount: response.downloadCount }
          : current,
      );
      showToast({
        title: "PDF ready",
        message: "Opening the uploaded study resource.",
        variant: "success",
      });
      window.location.assign(response.downloadUrl);
    } catch (requestError) {
      setActionError(actionErrorMessage(requestError));
    } finally {
      setBusyAction(null);
    }
  };

  const removeNote = async () => {
    if (!note) return;
    const confirmed = window.confirm(
      `Delete “${note.title}”? This action removes it from CampusOne.`,
    );
    if (!confirmed) return;

    setBusyAction("delete");
    setActionError(null);
    try {
      await deleteNote(note.id);
      showToast({
        title: "Note deleted",
        message: "The note was removed successfully.",
        variant: "success",
      });
      navigate(paths.notes, { replace: true });
    } catch (requestError) {
      setActionError(actionErrorMessage(requestError));
      setBusyAction(null);
    }
  };

  if (isLoading) {
    return (
      <div className="grid min-h-[60vh] place-items-center">
        <LoadingSpinner label="Loading note details" />
      </div>
    );
  }

  if (error || !note) {
    return (
      <div className="grid gap-4">
        <ErrorMessage message={error ?? "The note could not be found."} />
        <Link
          className="inline-flex h-10 w-fit items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          to={paths.notes}
        >
          <ArrowLeft className="size-4" />
          Back to notes
        </Link>
      </div>
    );
  }

  return (
    <div className="grid gap-6 pb-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link
          className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600 transition hover:text-brand-700"
          to={paths.notes}
        >
          <ArrowLeft className="size-4" />
          Back to notes
        </Link>
        {canManage ? (
          <div className="flex flex-wrap gap-2">
            <Link
              className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50"
              to={paths.noteEdit(note.id)}
            >
              <Edit3 className="size-4" />
              Edit note
            </Link>
            <Button
              loading={busyAction === "delete"}
              onClick={() => void removeNote()}
              variant="danger"
            >
              <Trash2 className="size-4" />
              Delete
            </Button>
          </div>
        ) : null}
      </div>

      {actionError ? <ErrorMessage message={actionError} /> : null}

      <section className="overflow-hidden rounded-3xl bg-slate-950 p-6 text-white shadow-xl sm:p-8">
        <div className="flex flex-wrap gap-2">
          <Badge className="bg-white/10 text-white ring-white/15">
            {note.fileType}
          </Badge>
          <Badge className="bg-white/10 text-white ring-white/15">
            {visibilityLabel(note.visibility)}
          </Badge>
          <Badge
            className={
              note.moderationStatus === "APPROVED"
                ? "bg-emerald-400/15 text-emerald-200 ring-emerald-300/20"
                : "bg-amber-400/15 text-amber-200 ring-amber-300/20"
            }
          >
            {moderationLabel(note.moderationStatus)}
          </Badge>
        </div>
        <h1 className="mt-5 max-w-4xl text-3xl font-bold tracking-tight sm:text-4xl">
          {note.title}
        </h1>
        <p className="mt-4 max-w-3xl leading-7 text-slate-300">
          {note.description}
        </p>
        <div className="mt-6 flex flex-wrap gap-x-6 gap-y-3 text-sm text-slate-300">
          <span className="flex items-center gap-2">
            <GraduationCap className="size-4 text-brand-300" />
            {note.course.courseCode} · {note.course.title}
          </span>
          <span className="flex items-center gap-2">
            <UserRound className="size-4 text-brand-300" />
            {note.uploader.fullName}
          </span>
          <span className="flex items-center gap-2">
            <CalendarDays className="size-4 text-brand-300" />
            {formatNoteDate(note.createdAt)}
          </span>
        </div>
      </section>

      {note.moderationReason ? (
        <Card className="border-amber-200 bg-amber-50">
          <CardContent className="p-5 text-sm text-amber-800">
            <strong>Moderation note:</strong> {note.moderationReason}
          </CardContent>
        </Card>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[1fr_22rem]">
        <div className="grid gap-6">
          <Card>
            <CardContent className="grid gap-5 p-5 sm:p-6">
              <h2 className="text-lg font-semibold text-slate-950">
                Academic details
              </h2>
              <dl className="grid gap-4 sm:grid-cols-2">
                <Detail label="Course" value={note.course.title} />
                <Detail label="Course code" value={note.course.courseCode} />
                <Detail label="Teacher" value={note.teacherName} />
                <Detail label="Semester" value={String(note.semester)} />
                <Detail
                  label="University"
                  value={note.uploader.university}
                />
                <Detail
                  label="Content version"
                  value={String(note.contentVersion)}
                />
              </dl>
              {note.tags.length > 0 ? (
                <div className="flex flex-wrap gap-2 border-t border-slate-100 pt-5">
                  {note.tags.map((tag) => (
                    <Badge key={tag.id}>#{tag.name}</Badge>
                  ))}
                </div>
              ) : null}
            </CardContent>
          </Card>

          <Card>
            <CardContent className="grid gap-5 p-5 sm:p-6">
              <div className="flex items-start gap-3">
                <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600">
                  <FileText className="size-5" />
                </span>
                <div>
                  <h2 className="font-semibold text-slate-950">
                    {note.file.originalFilename}
                  </h2>
                  <p className="mt-1 text-sm text-slate-500">
                    {note.file.mimeType} ·{" "}
                    {formatFileSize(note.file.sizeBytes)}
                  </p>
                </div>
                <Badge className="ml-auto">{note.file.status}</Badge>
              </div>
              <p className="text-sm leading-6 text-slate-500">
                {note.file.status === "READY"
                  ? "This PDF is stored securely in CampusOne object storage."
                  : "This legacy file record is not available for download."}
              </p>
              <Button
                className="w-fit"
                disabled={note.file.status !== "READY"}
                loading={busyAction === "download"}
                onClick={() => void recordDownload()}
              >
                <ArrowDownToLine className="size-4" />
                View or download PDF
              </Button>
            </CardContent>
          </Card>
        </div>

        <aside className="grid content-start gap-4">
          <Card>
            <CardContent className="grid gap-5 p-5">
              <div>
                <p className="text-sm font-semibold text-slate-700">
                  Community rating
                </p>
                <div className="mt-2 flex items-end gap-2">
                  <span className="text-3xl font-bold text-slate-950">
                    {note.averageRating.toFixed(1)}
                  </span>
                  <span className="mb-1 text-sm text-slate-500">
                    / 5 from {note.ratingCount} ratings
                  </span>
                </div>
              </div>
              <NoteRating
                disabled={busyAction === "rating"}
                onRate={(rating) => void submitRating(rating)}
                value={note.currentUserRating}
              />
              <div className="flex items-center gap-2 text-sm text-slate-500">
                <Star className="size-4 fill-amber-400 text-amber-400" />
                Your rating replaces any previous rating.
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="grid gap-3 p-5">
              <Button
                loading={busyAction === "bookmark"}
                onClick={() => void toggleBookmark()}
                variant={note.bookmarked ? "secondary" : "outline"}
              >
                <Bookmark
                  className={
                    note.bookmarked
                      ? "size-4 fill-brand-600 text-brand-600"
                      : "size-4"
                  }
                />
                {note.bookmarked ? "Bookmarked" : "Bookmark note"}
              </Button>
              <p className="text-center text-xs text-slate-400">
                <ShieldCheck className="mr-1 inline size-3.5" />
                {note.downloadCount.toLocaleString()} recorded downloads
              </p>
            </CardContent>
          </Card>
        </aside>
      </div>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">
        {label}
      </dt>
      <dd className="mt-1 text-sm font-medium text-slate-800">{value}</dd>
    </div>
  );
}
