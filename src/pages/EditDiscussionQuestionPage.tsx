import { ArrowLeft, LockKeyhole } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  getQuestionById,
  updateQuestion,
} from "@/api/discussionApi";
import {
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  useToast,
} from "@/components/common";
import { QuestionForm } from "@/components/discussion";
import { paths } from "@/routes/paths";
import type {
  DiscussionQuestionDetail,
  UpdateDiscussionQuestionRequest,
} from "@/types/discussion";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function EditDiscussionQuestionPage() {
  const { questionId } = useParams<{ questionId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [question, setQuestion] =
    useState<DiscussionQuestionDetail | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(questionId));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(() =>
    questionId ? null : "The question ID is missing.",
  );
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<
    Record<string, string[]>
  >({});

  useDocumentTitle(
    question
      ? `Edit ${question.title} · CampusOne`
      : "Edit question · CampusOne",
  );

  useEffect(() => {
    if (!questionId) return;
    const controller = new AbortController();
    let active = true;

    void getQuestionById(questionId, controller.signal)
      .then((response) => {
        if (active) setQuestion(response);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setLoadError(
          requestError instanceof ApiError
            ? requestError.message
            : "The question could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [questionId]);

  const handleSubmit = async (
    request: UpdateDiscussionQuestionRequest,
  ) => {
    if (!questionId) return;
    setIsSubmitting(true);
    setSubmitError(null);
    setFieldErrors({});
    try {
      const updated = await updateQuestion(questionId, request);
      showToast({
        title: "Question updated",
        message: "Your discussion question changes were saved.",
        variant: "success",
      });
      navigate(paths.discussionQuestion(updated.id), { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        setSubmitError(requestError.message);
        setFieldErrors(requestError.fieldErrors);
      } else {
        setSubmitError("The question could not be updated. Please try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="grid min-h-[60vh] place-items-center">
        <LoadingSpinner label="Loading your question" />
      </div>
    );
  }

  if (!question || loadError) {
    return (
      <div className="grid gap-4">
        <ErrorMessage
          message={loadError ?? "The question could not be found."}
        />
        <Link
          className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand-700"
          to={paths.discussions}
        >
          <ArrowLeft className="size-4" />
          Back to discussions
        </Link>
      </div>
    );
  }

  if (!question.ownedByCurrentUser) {
    return (
      <EmptyState
        action={
          <Link
            className="inline-flex h-10 items-center rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700"
            to={paths.discussionQuestion(question.id)}
          >
            View question
          </Link>
        }
        description="Only the student who asked this question can edit it."
        icon={<LockKeyhole className="size-6" />}
        title="Owner access required"
      />
    );
  }

  return (
    <div className="grid gap-6 pb-8">
      <Link
        className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand-700"
        to={paths.discussionQuestion(question.id)}
      >
        <ArrowLeft className="size-4" />
        Back to question
      </Link>

      <PageHeader
        description="Clarify your question, change its category, or update whether it is open."
        eyebrow="Your questions"
        title="Edit question"
      />

      {submitError ? <ErrorMessage message={submitError} /> : null}

      <QuestionForm
        backendFieldErrors={fieldErrors}
        initialQuestion={question}
        isSubmitting={isSubmitting}
        mode="edit"
        onSubmit={handleSubmit}
        submitLabel="Save changes"
      />
    </div>
  );
}

