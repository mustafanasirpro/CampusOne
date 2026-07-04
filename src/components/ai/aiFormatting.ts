import type {
  AiGeneratedItemType,
  AiSessionMode,
  AiUsageFeature,
} from "@/types/ai";
import { formatDateTime } from "@/utils/format";

export const aiSessionModeOptions: Array<{
  label: string;
  value: AiSessionMode;
}> = [
  { label: "General chat", value: "GENERAL_CHAT" },
  { label: "Explain concepts", value: "EXPLAIN_CONCEPT" },
  { label: "Summarize", value: "SUMMARIZE" },
  { label: "Flashcards", value: "FLASHCARDS" },
  { label: "Quiz", value: "QUIZ" },
  { label: "Study plan", value: "STUDY_PLAN" },
];

export function aiModeLabel(mode: AiSessionMode) {
  return aiSessionModeOptions.find((option) => option.value === mode)?.label ?? mode;
}

export function aiItemTypeLabel(type: AiGeneratedItemType) {
  return {
    FLASHCARDS: "Flashcards",
    QUIZ: "Quiz",
    STUDY_PLAN: "Study plan",
    SUMMARY: "Summary",
  }[type];
}

export function aiFeatureLabel(feature: AiUsageFeature) {
  return {
    CHAT: "Chat",
    EXPLAIN_CONCEPT: "Explain concept",
    FLASHCARDS: "Flashcards",
    QUIZ: "Quiz",
    STUDY_PLAN: "Study plan",
    SUMMARIZE: "Summary",
  }[feature];
}

export function formatAiDate(value: string) {
  return formatDateTime(value);
}
