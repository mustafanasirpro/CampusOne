import type { LucideIcon } from "lucide-react";

export type AIContentBlock =
  | { type: "heading"; text: string }
  | { type: "paragraph"; text: string }
  | { type: "list"; items: string[]; ordered?: boolean }
  | { type: "code"; code: string; language: string }
  | { type: "table"; headers: string[]; rows: string[][] }
  | { type: "math"; expression: string; explanation?: string };

export interface ChatMessage {
  blocks?: AIContentBlock[];
  id: string;
  role: "user" | "assistant";
  text?: string;
  timestamp: string;
}

export interface AIConversation {
  category: string;
  id: string;
  isFavorite: boolean;
  messages: ChatMessage[];
  title: string;
  updatedAt: string;
}

export interface PromptSuggestion {
  description: string;
  icon: LucideIcon;
  id: string;
  prompt: string;
  response: AIContentBlock[];
  title: string;
}

export interface StudyTool {
  actionLabel: string;
  description: string;
  icon: LucideIcon;
  id: string;
  inputLabel: string;
  inputPlaceholder: string;
  sampleResult: string[];
  title: string;
}

export interface AIFeature {
  description: string;
  icon: LucideIcon;
  title: string;
  tone: "brand" | "emerald" | "amber" | "sky";
}

export interface StudyHistoryItem {
  detail: string;
  id: string;
  title: string;
}

export type AiSessionMode =
  | "GENERAL_CHAT"
  | "EXPLAIN_CONCEPT"
  | "SUMMARIZE"
  | "FLASHCARDS"
  | "QUIZ"
  | "STUDY_PLAN";

export type AiMessageRole = "USER" | "ASSISTANT" | "SYSTEM";
export type AiGeneratedItemType =
  | "SUMMARY"
  | "FLASHCARDS"
  | "QUIZ"
  | "STUDY_PLAN";
export type AiUsageFeature =
  | "CHAT"
  | "EXPLAIN_CONCEPT"
  | "SUMMARIZE"
  | "FLASHCARDS"
  | "QUIZ"
  | "STUDY_PLAN";
export type AiSort = "NEWEST" | "OLDEST";

export interface AiSession {
  createdAt: string;
  id: string;
  mode: AiSessionMode;
  title: string;
  updatedAt: string;
}

export interface AiMessage {
  content: string;
  createdAt: string;
  id: string;
  role: AiMessageRole;
  tokenEstimate: number;
}

export interface AiMessagePage {
  content: AiMessage[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AiSessionDetail extends AiSession {
  messages: AiMessage[];
}

export interface AiSessionPage {
  content: AiSession[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AiChatResponse {
  assistantMessage: AiMessage;
  provider: string;
  sessionId: string;
  userMessage: AiMessage;
}

export interface AiExplanationResponse {
  concept: string;
  explanation: string;
  provider: string;
}

export interface AiGeneratedItem {
  createdAt: string;
  generatedContent: unknown;
  id: string;
  itemType: AiGeneratedItemType;
  sourceText: string | null;
  title: string;
  updatedAt: string;
}

export interface AiGeneratedItemPage {
  content: AiGeneratedItem[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AiUsageRecord {
  createdAt: string;
  feature: AiUsageFeature;
  id: string;
  inputTokenEstimate: number;
  outputTokenEstimate: number;
  provider: string;
}

export interface AiUsagePage {
  content: AiUsageRecord[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateAiSessionRequest {
  mode: AiSessionMode;
  title: string;
}

export interface ExplainConceptRequest {
  concept: string;
  context?: string | null;
}

export interface GenerateTextRequest {
  text: string;
  title?: string | null;
}

export interface GenerateCountedTextRequest extends GenerateTextRequest {
  count?: number;
}

export interface GenerateStudyPlanRequest {
  context?: string | null;
  dailyMinutes: number;
  days: number;
  goal: string;
}
