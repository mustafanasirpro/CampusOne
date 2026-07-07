export type DiscussionCategory =
  | "GENERAL"
  | "ACADEMIC"
  | "PROGRAMMING"
  | "EXAMS"
  | "CAREER"
  | "CAMPUS"
  | "OTHER";

export type DiscussionQuestionStatus =
  | "PENDING_REVIEW"
  | "OPEN"
  | "RESOLVED"
  | "CLOSED"
  | "HIDDEN"
  | "REJECTED";

export type DiscussionQuestionUpdateStatus = "OPEN" | "CLOSED";

export type DiscussionQuestionSort =
  | "NEWEST"
  | "OLDEST"
  | "MOST_VOTED"
  | "MOST_ANSWERED";

export type DiscussionVoteValue = -1 | 1;

export interface DiscussionAuthor {
  avatarUrl: string | null;
  fullName: string;
  university: string;
  userId: string;
}

export interface DiscussionQuestionSummary {
  acceptedAnswerId: string | null;
  answerCount: number;
  author: DiscussionAuthor;
  bodyPreview: string;
  category: DiscussionCategory;
  createdAt: string;
  id: string;
  status: DiscussionQuestionStatus;
  title: string;
  updatedAt: string;
  viewCount: number;
  voteScore: number;
}

export interface DiscussionQuestionPage {
  content: DiscussionQuestionSummary[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DiscussionAnswer {
  accepted: boolean;
  author: DiscussionAuthor;
  body: string;
  createdAt: string;
  currentUserVote: DiscussionVoteValue | null;
  id: string;
  ownedByCurrentUser: boolean;
  questionId: string;
  updatedAt: string;
  voteScore: number;
}

export interface DiscussionAnswerPage {
  content: DiscussionAnswer[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DiscussionQuestionDetail {
  acceptedAnswerId: string | null;
  answerCount: number;
  answers: DiscussionAnswerPage;
  author: DiscussionAuthor;
  body: string;
  category: DiscussionCategory;
  createdAt: string;
  currentUserVote: DiscussionVoteValue | null;
  id: string;
  ownedByCurrentUser: boolean;
  status: DiscussionQuestionStatus;
  title: string;
  updatedAt: string;
  viewCount: number;
  voteScore: number;
}

export interface DiscussionVoteResponse {
  currentUserVote: DiscussionVoteValue | null;
  targetId: string;
  voteScore: number;
}

export interface CreateDiscussionQuestionRequest {
  body: string;
  category: DiscussionCategory;
  title: string;
}

export interface UpdateDiscussionQuestionRequest {
  body?: string;
  category?: DiscussionCategory;
  status?: DiscussionQuestionUpdateStatus;
  title?: string;
}

export interface DiscussionAnswerRequest {
  body: string;
}

export interface DiscussionQuestionListParameters {
  category?: DiscussionCategory;
  page?: number;
  search?: string;
  signal?: AbortSignal;
  size?: number;
  sort?: DiscussionQuestionSort;
}
