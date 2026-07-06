import { ArrowLeft, LockKeyhole } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  createNote,
  getNoteManagementStatus,
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
import type { CreateNoteRequest } from "@/types/notes";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function CreateNotePage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<
    Record<string, string[]>
  >({});
  const [canManage, setCanManage] = useState<boolean | null>(null);
  const [accessError, setAccessError] = useState<string | null>(null);

  useDocumentTitle("Create note · CampusOne");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    void getNoteManagementStatus(controller.signal)
      .then((status) => {
        if (active) setCanManage(status.canManage);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setAccessError(
          requestError instanceof ApiError
            ? requestError.message
            : "Admin access could not be checked. Please try again.",
        );
        setCanManage(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  const handleSubmit = async (request: CreateNoteRequest, file: File) => {
    if (!canManage || isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      const note = await createNote(request, file);
      showToast({
        title: "Note submitted",
        message: "Your note is pending moderation review.",
        variant: "success",
      });
      navigate(paths.noteDetail(note.id), { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        setError(requestError.message);
        setFieldErrors(requestError.fieldErrors);
      } else {
        setError("The note could not be created. Please try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (canManage === null) {
    return (
      <div className="grid min-h-[60vh] place-items-center">
        <LoadingSpinner label="Checking admin access" />
      </div>
    );
  }

  if (!canManage) {
    return (
      <div className="grid gap-4">
        {accessError ? <ErrorMessage message={accessError} /> : null}
        <EmptyState
          action={
            <Link
              className="inline-flex h-10 items-center rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700"
              to={paths.notes}
            >
              Back to notes
            </Link>
          }
          description="Only admins can upload notes and past papers."
          icon={<LockKeyhole className="size-6" />}
          title="Admin access required"
        />
      </div>
    );
  }

  return (
    <div className="grid gap-6 pb-8">
      <Link
        className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand-700"
        to={paths.notes}
      >
        <ArrowLeft className="size-4" />
        Back to notes
      </Link>

      <PageHeader
        description="Upload a PDF study resource and submit its academic details for moderation."
        eyebrow="Notes"
        title="Create a note"
      />

      {error ? <ErrorMessage message={error} /> : null}

      <NoteForm
        backendFieldErrors={fieldErrors}
        isSubmitting={isSubmitting}
        mode="create"
        onSubmit={handleSubmit}
        submitLabel="Submit note"
      />
    </div>
  );
}
