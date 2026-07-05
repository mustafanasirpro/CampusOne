import { ArrowLeft } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { createNote } from "@/api/notesApi";
import { ErrorMessage, PageHeader, useToast } from "@/components/common";
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

  useDocumentTitle("Create note · CampusOne");

  const handleSubmit = async (request: CreateNoteRequest, file: File) => {
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
