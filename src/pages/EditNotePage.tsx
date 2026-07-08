import { ArrowLeft, LockKeyhole } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  getNoteById,
  getNoteManagementStatus,
  updateNote,
} from "@/api/notesApi";
import {
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  useToast,
} from "@/components/common";
import { NoteForm } from "@/components/notes";
import { paths } from "@/routes/paths";
import type { NoteDetail, UpdateNoteRequest } from "@/types/notes";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function EditNotePage() {
  const { noteId } = useParams<{ noteId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [note, setNote] = useState<NoteDetail | null>(null);
  const [canManage, setCanManage] = useState(false);
  const [isLoading, setIsLoading] = useState(Boolean(noteId));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(() =>
    noteId ? null : "The note ID is missing.",
  );
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<
    Record<string, string[]>
  >({});

  useDocumentTitle(note ? `Edit ${note.title} · CampusOne` : "Edit note · CampusOne");

  useEffect(() => {
    if (!noteId) return;

    const controller = new AbortController();
    let active = true;

    void Promise.all([
      getNoteById(noteId, controller.signal),
      getNoteManagementStatus(controller.signal),
    ])
      .then(([noteResponse, managementStatus]) => {
        if (!active) return;
        setNote(noteResponse);
        setCanManage(managementStatus.canManage);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setLoadError(
          requestError instanceof ApiError
            ? requestError.message
            : "The note could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [noteId]);

  const handleSubmit = async (request: UpdateNoteRequest) => {
    if (!noteId || !canManage || isSubmitting) return;
    setIsSubmitting(true);
    setSubmitError(null);
    setFieldErrors({});
    try {
      const updated = await updateNote(noteId, request);
      showToast({
        title: "Note updated",
        message: "Your changes were saved and the note remains approved.",
        variant: "success",
      });
      navigate(paths.noteDetail(updated.id), { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        setSubmitError(requestError.message);
        setFieldErrors(requestError.fieldErrors);
      } else {
        setSubmitError("The note could not be updated. Please try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="grid min-h-[60vh] place-items-center">
        <LoadingSpinner label="Loading your note" />
      </div>
    );
  }

  if (!note || loadError) {
    return (
      <div className="grid gap-4">
        <ErrorMessage
          message={loadError ?? "The note could not be found."}
        />
        <Link
          className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand-700"
          to={paths.notes}
        >
          <ArrowLeft className="size-4" />
          Back to notes
        </Link>
      </div>
    );
  }

  if (!canManage) {
    return (
      <EmptyState
        action={
          <Link
            className="inline-flex h-10 items-center rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700"
            to={paths.noteDetail(note.id)}
          >
            View note
          </Link>
        }
        description="Only admins can edit or delete notes after submission."
        icon={<LockKeyhole className="size-6" />}
        title="Admin access required"
      />
    );
  }

  return (
    <div className="grid gap-6 pb-8">
      <Link
        className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand-700"
        to={paths.noteDetail(note.id)}
      >
        <ArrowLeft className="size-4" />
        Back to note
      </Link>

      <PageHeader
        description="Update descriptive note fields while keeping the currently uploaded file."
        eyebrow="Admin notes"
        title="Edit note"
      />

      {submitError ? <ErrorMessage message={submitError} /> : null}

      <NoteForm
        backendFieldErrors={fieldErrors}
        initialNote={note}
        isSubmitting={isSubmitting}
        mode="edit"
        onSubmit={handleSubmit}
        submitLabel="Save changes"
      />
    </div>
  );
}
