import { ArrowLeft } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { createQuestion } from "@/api/discussionApi";
import { ErrorMessage, PageHeader, useToast } from "@/components/common";
import { QuestionForm } from "@/components/discussion";
import { paths } from "@/routes/paths";
import type { CreateDiscussionQuestionRequest } from "@/types/discussion";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function CreateDiscussionQuestionPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<
    Record<string, string[]>
  >({});

  useDocumentTitle("Ask a question · CampusOne");

  const handleSubmit = async (
    request: CreateDiscussionQuestionRequest,
  ) => {
    setIsSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      const question = await createQuestion(request);
      showToast({
        title: "Question posted",
        message: "Your question is now visible to the CampusOne community.",
        variant: "success",
      });
      navigate(paths.discussionQuestion(question.id), { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        setError(requestError.message);
        setFieldErrors(requestError.fieldErrors);
      } else {
        setError("The question could not be created. Please try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="grid gap-6 pb-8">
      <Link
        className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand-700"
        to={paths.discussions}
      >
        <ArrowLeft className="size-4" />
        Back to discussions
      </Link>

      <PageHeader
        description="Share enough context for other students to give a useful, accurate answer."
        eyebrow="Community Q&A"
        title="Ask a question"
      />

      {error ? <ErrorMessage message={error} /> : null}

      <QuestionForm
        backendFieldErrors={fieldErrors}
        isSubmitting={isSubmitting}
        mode="create"
        onSubmit={handleSubmit}
        submitLabel="Post question"
      />
    </div>
  );
}

