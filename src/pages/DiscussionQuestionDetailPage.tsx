import {
  ArrowLeft,
  ArrowRight,
  CalendarDays,
  CheckCircle2,
  Edit3,
  Eye,
  MessageCircle,
  Trash2,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  acceptAnswer,
  createAnswer,
  deleteAnswer,
  deleteQuestion,
  getQuestionById,
  listAnswers,
  removeAcceptedAnswer,
  removeAnswerVote,
  removeQuestionVote,
  updateAnswer,
  voteAnswer,
  voteQuestion,
} from "@/api/discussionApi";
import {
  Avatar,
  Badge,
  Button,
  Card,
  CardContent,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  useToast,
} from "@/components/common";
import {
  AnswerCard,
  AnswerForm,
  discussionCategoryLabel,
  discussionStatusLabel,
  formatDiscussionDate,
  VoteControls,
} from "@/components/discussion";
import { paths } from "@/routes/paths";
import type {
  DiscussionAnswer,
  DiscussionAnswerPage,
  DiscussionQuestionDetail,
  DiscussionVoteValue,
} from "@/types/discussion";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

const answerPageSize = 20;

function requestErrorMessage(error: unknown) {
  return error instanceof ApiError
    ? error.message
    : "The discussion action could not be completed.";
}

function voteErrorMessage(error: unknown) {
  return error instanceof ApiError && error.status === 403
    ? "You cannot vote on your own post."
    : requestErrorMessage(error);
}

export function DiscussionQuestionDetailPage() {
  const { questionId } = useParams<{ questionId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [question, setQuestion] =
    useState<DiscussionQuestionDetail | null>(null);
  const [answerPage, setAnswerPage] =
    useState<DiscussionAnswerPage | null>(null);
  const [error, setError] = useState<string | null>(() =>
    questionId ? null : "The question ID is missing.",
  );
  const [actionError, setActionError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(questionId));
  const [isAnswersLoading, setIsAnswersLoading] = useState(false);
  const [isSubmittingAnswer, setIsSubmittingAnswer] = useState(false);
  const [busyAction, setBusyAction] = useState<string | null>(null);

  useDocumentTitle(
    question
      ? `${question.title} · CampusOne`
      : "Discussion · CampusOne",
  );

  useEffect(() => {
    if (!questionId) return;
    const controller = new AbortController();
    let active = true;

    void getQuestionById(questionId, controller.signal)
      .then((response) => {
        if (!active) return;
        setQuestion(response);
        setAnswerPage(response.answers);
      })
      .catch((requestError: unknown) => {
        if (active) setError(requestErrorMessage(requestError));
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [questionId]);

  const loadAnswerPage = async (page: number) => {
    if (!questionId) return;
    setIsAnswersLoading(true);
    setActionError(null);
    try {
      const response = await listAnswers(questionId, {
        page,
        size: answerPageSize,
      });
      setAnswerPage(response);
    } catch (requestError) {
      setActionError(requestErrorMessage(requestError));
    } finally {
      setIsAnswersLoading(false);
    }
  };

  const submitQuestionVote = async (value: DiscussionVoteValue) => {
    if (!question) return;
    setBusyAction("question-vote");
    setActionError(null);
    try {
      const response = await voteQuestion(question.id, value);
      setQuestion((current) =>
        current
          ? {
              ...current,
              currentUserVote: response.currentUserVote,
              voteScore: response.voteScore,
            }
          : current,
      );
    } catch (requestError) {
      setActionError(voteErrorMessage(requestError));
    } finally {
      setBusyAction(null);
    }
  };

  const clearQuestionVote = async () => {
    if (!question) return;
    setBusyAction("question-vote");
    setActionError(null);
    try {
      const response = await removeQuestionVote(question.id);
      setQuestion((current) =>
        current
          ? {
              ...current,
              currentUserVote: response.currentUserVote,
              voteScore: response.voteScore,
            }
          : current,
      );
    } catch (requestError) {
      setActionError(voteErrorMessage(requestError));
    } finally {
      setBusyAction(null);
    }
  };

  const submitAnswer = async (body: string) => {
    if (!question || !answerPage) return;
    setIsSubmittingAnswer(true);
    setActionError(null);
    try {
      await createAnswer(question.id, { body });
      const nextTotal = answerPage.totalElements + 1;
      const lastPage = Math.max(
        0,
        Math.ceil(nextTotal / answerPageSize) - 1,
      );
      setQuestion((current) =>
        current
          ? { ...current, answerCount: current.answerCount + 1 }
          : current,
      );
      await loadAnswerPage(lastPage);
      showToast({
        title: "Answer posted",
        message: "Your answer is now part of the discussion.",
        variant: "success",
      });
    } catch (requestError) {
      setActionError(requestErrorMessage(requestError));
      throw requestError;
    } finally {
      setIsSubmittingAnswer(false);
    }
  };

  const submitAnswerVote = async (
    answerId: string,
    value: DiscussionVoteValue,
  ) => {
    setBusyAction(`answer:${answerId}`);
    setActionError(null);
    try {
      const response = await voteAnswer(answerId, value);
      setAnswerPage((current) =>
        current
          ? {
              ...current,
              content: current.content.map((answer) =>
                answer.id === answerId
                  ? {
                      ...answer,
                      currentUserVote: response.currentUserVote,
                      voteScore: response.voteScore,
                    }
                  : answer,
              ),
            }
          : current,
      );
    } catch (requestError) {
      setActionError(voteErrorMessage(requestError));
    } finally {
      setBusyAction(null);
    }
  };

  const clearAnswerVote = async (answerId: string) => {
    setBusyAction(`answer:${answerId}`);
    setActionError(null);
    try {
      const response = await removeAnswerVote(answerId);
      setAnswerPage((current) =>
        current
          ? {
              ...current,
              content: current.content.map((answer) =>
                answer.id === answerId
                  ? {
                      ...answer,
                      currentUserVote: response.currentUserVote,
                      voteScore: response.voteScore,
                    }
                  : answer,
              ),
            }
          : current,
      );
    } catch (requestError) {
      setActionError(voteErrorMessage(requestError));
    } finally {
      setBusyAction(null);
    }
  };

  const editAnswer = async (answerId: string, body: string) => {
    setBusyAction(`answer:${answerId}`);
    setActionError(null);
    try {
      const response = await updateAnswer(answerId, { body });
      setAnswerPage((current) =>
        current
          ? {
              ...current,
              content: current.content.map((answer) =>
                answer.id === answerId ? response : answer,
              ),
            }
          : current,
      );
      showToast({
        title: "Answer updated",
        message: "Your changes were saved.",
        variant: "success",
      });
    } catch (requestError) {
      setActionError(requestErrorMessage(requestError));
      throw requestError;
    } finally {
      setBusyAction(null);
    }
  };

  const removeAnswer = async (answer: DiscussionAnswer) => {
    if (!answerPage) return;
    const confirmed = window.confirm(
      "Delete this answer? It will be removed from the discussion.",
    );
    if (!confirmed) return;

    setBusyAction(`answer:${answer.id}`);
    setActionError(null);
    try {
      await deleteAnswer(answer.id);
      const nextTotal = Math.max(0, answerPage.totalElements - 1);
      const nextPage = Math.min(
        answerPage.page,
        Math.max(0, Math.ceil(nextTotal / answerPageSize) - 1),
      );
      setQuestion((current) =>
        current
          ? {
              ...current,
              acceptedAnswerId: answer.accepted
                ? null
                : current.acceptedAnswerId,
              answerCount: Math.max(0, current.answerCount - 1),
              status: answer.accepted ? "OPEN" : current.status,
            }
          : current,
      );
      await loadAnswerPage(nextPage);
      showToast({
        title: "Answer deleted",
        message: "Your answer was removed.",
        variant: "success",
      });
    } catch (requestError) {
      setActionError(requestErrorMessage(requestError));
    } finally {
      setBusyAction(null);
    }
  };

  const selectAcceptedAnswer = async (answerId: string) => {
    if (!question) return;
    setBusyAction(`answer:${answerId}`);
    setActionError(null);
    try {
      const response = await acceptAnswer(question.id, answerId);
      setQuestion(response);
      setAnswerPage(response.answers);
      showToast({
        title: "Answer accepted",
        message: "The answer is now highlighted as the solution.",
        variant: "success",
      });
    } catch (requestError) {
      setActionError(requestErrorMessage(requestError));
    } finally {
      setBusyAction(null);
    }
  };

  const clearAcceptedAnswer = async () => {
    if (!question) return;
    setBusyAction(`answer:${question.acceptedAnswerId ?? "accepted"}`);
    setActionError(null);
    try {
      const response = await removeAcceptedAnswer(question.id);
      setQuestion(response);
      setAnswerPage(response.answers);
      showToast({
        title: "Acceptance removed",
        message: "The question is open again.",
        variant: "success",
      });
    } catch (requestError) {
      setActionError(requestErrorMessage(requestError));
    } finally {
      setBusyAction(null);
    }
  };

  const removeQuestion = async () => {
    if (!question) return;
    const confirmed = window.confirm(
      `Delete "${question.title}"? The question will be removed from discussions.`,
    );
    if (!confirmed) return;

    setBusyAction("delete-question");
    setActionError(null);
    try {
      await deleteQuestion(question.id);
      showToast({
        title: "Question deleted",
        message: "The discussion question was removed.",
        variant: "success",
      });
      navigate(paths.discussions, { replace: true });
    } catch (requestError) {
      setActionError(requestErrorMessage(requestError));
      setBusyAction(null);
    }
  };

  if (isLoading) {
    return (
      <div className="grid min-h-[60vh] place-items-center">
        <LoadingSpinner label="Loading discussion question" />
      </div>
    );
  }

  if (error || !question || !answerPage) {
    return (
      <div className="grid gap-4">
        <ErrorMessage
          message={error ?? "The discussion question could not be found."}
        />
        <Link
          className="inline-flex h-10 w-fit items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          to={paths.discussions}
        >
          <ArrowLeft className="size-4" />
          Back to discussions
        </Link>
      </div>
    );
  }

  return (
    <div className="grid gap-6 pb-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link
          className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600 transition hover:text-brand-700"
          to={paths.discussions}
        >
          <ArrowLeft className="size-4" />
          Back to discussions
        </Link>
        {question.ownedByCurrentUser ? (
          <div className="flex flex-wrap gap-2">
            <Link
              className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50"
              to={paths.discussionQuestionEdit(question.id)}
            >
              <Edit3 className="size-4" />
              Edit question
            </Link>
            <Button
              loading={busyAction === "delete-question"}
              onClick={() => void removeQuestion()}
              variant="danger"
            >
              <Trash2 className="size-4" />
              Delete
            </Button>
          </div>
        ) : null}
      </div>

      {actionError ? <ErrorMessage message={actionError} /> : null}

      <Card>
        <CardContent className="grid gap-5 p-5 sm:p-7">
          <div className="flex flex-wrap gap-2">
            <Badge variant="brand">
              {discussionCategoryLabel(question.category)}
            </Badge>
            <Badge
              variant={
                question.status === "RESOLVED"
                  ? "success"
                  : question.status === "OPEN"
                    ? "warning"
                    : "neutral"
              }
            >
              {question.status === "RESOLVED" ? (
                <CheckCircle2 className="mr-1 size-3.5" />
              ) : null}
              {discussionStatusLabel(question.status)}
            </Badge>
          </div>

          <h1 className="text-2xl font-bold leading-tight tracking-tight text-slate-950 sm:text-3xl">
            {question.title}
          </h1>

          <div className="flex flex-wrap items-center gap-3">
            <Avatar
              name={question.author.fullName}
              size="sm"
              src={question.author.avatarUrl ?? undefined}
            />
            <div>
              <p className="text-sm font-semibold text-slate-900">
                {question.author.fullName}
              </p>
              <p className="text-xs text-slate-400">
                {question.author.university}
              </p>
            </div>
            <span className="ml-auto flex items-center gap-1.5 text-xs text-slate-400">
              <CalendarDays className="size-3.5" />
              {formatDiscussionDate(question.createdAt)}
            </span>
          </div>

          <div className="whitespace-pre-wrap border-y border-slate-100 py-6 text-sm leading-7 text-slate-600 sm:text-base">
            {question.body}
          </div>

          <div className="flex flex-wrap items-center gap-4">
            <VoteControls
              currentUserVote={question.currentUserVote}
              disabled={question.ownedByCurrentUser}
              isBusy={busyAction === "question-vote"}
              label="question"
              onRemoveVote={() => void clearQuestionVote()}
              onVote={(value) => void submitQuestionVote(value)}
              score={question.voteScore}
            />
            <span className="flex items-center gap-1.5 text-sm text-slate-500">
              <MessageCircle className="size-4" />
              {question.answerCount} answers
            </span>
            <span className="flex items-center gap-1.5 text-sm text-slate-500">
              <Eye className="size-4" />
              {question.viewCount} views
            </span>
            {question.ownedByCurrentUser ? (
              <span className="text-xs text-slate-400">
                You cannot vote on your own question.
              </span>
            ) : null}
          </div>
        </CardContent>
      </Card>

      <section className="grid gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-950">
            Community answers
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            Accepted answers appear first, followed by the remaining answers.
          </p>
        </div>

        {isAnswersLoading ? (
          <div className="grid min-h-36 place-items-center rounded-2xl border border-slate-200 bg-white">
            <LoadingSpinner label="Loading answers" />
          </div>
        ) : answerPage.content.length === 0 ? (
          <EmptyState
            description="Share a clear solution or helpful direction below."
            icon={<MessageCircle className="size-6" />}
            title="No answers yet"
          />
        ) : (
          <div className="grid gap-4">
            {answerPage.content.map((answer) => (
              <AnswerCard
                answer={answer}
                canAccept={question.ownedByCurrentUser}
                isBusy={busyAction === `answer:${answer.id}`}
                key={answer.id}
                onAccept={selectAcceptedAnswer}
                onDelete={removeAnswer}
                onRemoveVote={clearAnswerVote}
                onUnaccept={clearAcceptedAnswer}
                onUpdate={editAnswer}
                onVote={submitAnswerVote}
              />
            ))}
          </div>
        )}

        {answerPage.totalPages > 1 ? (
          <nav
            aria-label="Answer pagination"
            className="flex items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-3"
          >
            <Button
              disabled={answerPage.first || isAnswersLoading}
              onClick={() =>
                void loadAnswerPage(Math.max(0, answerPage.page - 1))
              }
              variant="outline"
            >
              <ArrowLeft className="size-4" />
              Previous
            </Button>
            <span className="text-sm font-semibold text-slate-700">
              {answerPage.page + 1} / {answerPage.totalPages}
            </span>
            <Button
              disabled={answerPage.last || isAnswersLoading}
              onClick={() => void loadAnswerPage(answerPage.page + 1)}
              variant="outline"
            >
              Next
              <ArrowRight className="size-4" />
            </Button>
          </nav>
        ) : null}
      </section>

      <Card>
        <CardContent className="grid gap-4 p-5 sm:p-6">
          <div>
            <h2 className="text-lg font-semibold text-slate-950">
              Add your answer
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              Be constructive, explain your reasoning, and keep the response
              relevant to the question.
            </p>
          </div>
          <AnswerForm
            isSubmitting={isSubmittingAnswer}
            onSubmit={submitAnswer}
          />
        </CardContent>
      </Card>
    </div>
  );
}
