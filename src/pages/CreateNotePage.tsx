import { ArrowLeft } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  createNote,
  getNoteManagementStatus,
} from "@/api/notesApi";
import {
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
            : "We could not confirm your note permissions. Please try again.",
        );
        setCanManage(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  const handleSubmit = async (request: CreateNoteRequest, file: File) => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      const note = await createNote(request, file);
      if (canManage) {
        showToast({
          title: "Note uploaded",
          message: "Note uploaded successfully.",
          variant: "success",
        });
        navigate(paths.noteDetail(note.id), { replace: true });
      } else {
        showToast({
          title: "Submitted for review",
          message:
            "Submitted for review. It will appear after admin approval.",
          variant: "success",
        });
        navigate(paths.notes, { replace: true });
      }
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
        <LoadingSpinner label="Checking your note tools" />
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
        description={
          canManage
            ? "Upload an approved PDF study resource with clear academic details."
            : "Submit a PDF study resource for review. Approved notes become visible in the public library."
        }
        eyebrow="Notes"
        title={canManage ? "Upload a note" : "Submit a note"}
      />

      {error ? <ErrorMessage message={error} /> : null}
      {!canManage && accessError ? (
        <ErrorMessage message={accessError} />
      ) : null}

      <NoteForm
        backendFieldErrors={fieldErrors}
        isSubmitting={isSubmitting}
        mode="create"
        onSubmit={handleSubmit}
        reviewNotice={
          canManage
            ? "Admin uploads are published directly to the approved notes library."
            : "Student submissions are reviewed before they appear publicly."
        }
        submitLabel={canManage ? "Upload note" : "Submit note"}
      />
    </div>
  );
}
