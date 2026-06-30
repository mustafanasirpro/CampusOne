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

