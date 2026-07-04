import { apiRequest } from "@/api/apiClient";
import type {
  CreateDiscussionQuestionRequest,
  DiscussionAnswer,
  DiscussionAnswerPage,
  DiscussionAnswerRequest,
  DiscussionQuestionDetail,
  DiscussionQuestionListParameters,
  DiscussionQuestionPage,
  DiscussionVoteResponse,
  DiscussionVoteValue,
  UpdateDiscussionQuestionRequest,
} from "@/types/discussion";

function queryString(parameters: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(parameters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      query.set(key, String(value));
    }
  });
  const value = query.toString();
  return value ? `?${value}` : "";
}

const discussionsPath = "/discussions";

export function listQuestions({
  category,
  page = 0,
  search,
  signal,
  size = 12,
  sort = "NEWEST",
}: DiscussionQuestionListParameters = {}) {
  return apiRequest<DiscussionQuestionPage>(
    `${discussionsPath}/questions${queryString({
      category,
      page,
      search,
      size,
      sort,
    })}`,
    { signal },
  );
}

export function getMyQuestions({
  page = 0,
  signal,
  size = 12,
  sort = "NEWEST",
}: Pick<
  DiscussionQuestionListParameters,
  "page" | "signal" | "size" | "sort"
> = {}) {
  return apiRequest<DiscussionQuestionPage>(
    `${discussionsPath}/questions/my${queryString({ page, size, sort })}`,
    { signal },
  );
}

export function getQuestionById(
  questionId: string,
  signal?: AbortSignal,
) {
  return apiRequest<DiscussionQuestionDetail>(
    `${discussionsPath}/questions/${questionId}`,
    { signal },
  );
}

export function createQuestion(
  request: CreateDiscussionQuestionRequest,
) {
  return apiRequest<DiscussionQuestionDetail>(
    `${discussionsPath}/questions`,
    {
      body: JSON.stringify(request),
      method: "POST",
    },
  );
}

export function updateQuestion(
  questionId: string,
  request: UpdateDiscussionQuestionRequest,
) {
  return apiRequest<DiscussionQuestionDetail>(
    `${discussionsPath}/questions/${questionId}`,
    {
      body: JSON.stringify(request),
      method: "PATCH",
    },
  );
}

export function deleteQuestion(questionId: string) {
  return apiRequest<void>(
    `${discussionsPath}/questions/${questionId}`,
    { method: "DELETE" },
  );
}

export function listAnswers(
  questionId: string,
  {
    page = 0,
    signal,
    size = 10,
  }: {
    page?: number;
    signal?: AbortSignal;
    size?: number;
  } = {},
) {
  return apiRequest<DiscussionAnswerPage>(
    `${discussionsPath}/questions/${questionId}/answers${queryString({
      page,
      size,
    })}`,
    { signal },
  );
}

export function createAnswer(
  questionId: string,
  request: DiscussionAnswerRequest,
) {
  return apiRequest<DiscussionAnswer>(
    `${discussionsPath}/questions/${questionId}/answers`,
    {
      body: JSON.stringify(request),
      method: "POST",
    },
  );
}

export function updateAnswer(
  answerId: string,
  request: DiscussionAnswerRequest,
) {
  return apiRequest<DiscussionAnswer>(
    `${discussionsPath}/answers/${answerId}`,
    {
      body: JSON.stringify(request),
      method: "PATCH",
    },
  );
}

export function deleteAnswer(answerId: string) {
  return apiRequest<void>(`${discussionsPath}/answers/${answerId}`, {
    method: "DELETE",
  });
}

export function voteQuestion(
  questionId: string,
  voteValue: DiscussionVoteValue,
) {
  return apiRequest<DiscussionVoteResponse>(
    `${discussionsPath}/questions/${questionId}/vote`,
    {
      body: JSON.stringify({ voteValue }),
      method: "PUT",
    },
  );
}

export function removeQuestionVote(questionId: string) {
  return apiRequest<DiscussionVoteResponse>(
    `${discussionsPath}/questions/${questionId}/vote`,
    { method: "DELETE" },
  );
}

export function voteAnswer(
  answerId: string,
  voteValue: DiscussionVoteValue,
) {
  return apiRequest<DiscussionVoteResponse>(
    `${discussionsPath}/answers/${answerId}/vote`,
    {
      body: JSON.stringify({ voteValue }),
      method: "PUT",
    },
  );
}

export function removeAnswerVote(answerId: string) {
  return apiRequest<DiscussionVoteResponse>(
    `${discussionsPath}/answers/${answerId}/vote`,
    { method: "DELETE" },
  );
}

export function acceptAnswer(questionId: string, answerId: string) {
  return apiRequest<DiscussionQuestionDetail>(
    `${discussionsPath}/questions/${questionId}/accepted-answer/${answerId}`,
    { method: "PUT" },
  );
}

export function removeAcceptedAnswer(questionId: string) {
  return apiRequest<DiscussionQuestionDetail>(
    `${discussionsPath}/questions/${questionId}/accepted-answer`,
    { method: "DELETE" },
  );
}

