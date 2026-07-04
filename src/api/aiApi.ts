import { apiRequest } from "@/api/apiClient";
import type {
  AiChatResponse,
  AiExplanationResponse,
  AiGeneratedItem,
  AiGeneratedItemPage,
  AiGeneratedItemType,
  AiMessagePage,
  AiSession,
  AiSessionDetail,
  AiSessionMode,
  AiSessionPage,
  AiSort,
  AiUsageFeature,
  AiUsagePage,
  CreateAiSessionRequest,
  ExplainConceptRequest,
  GenerateCountedTextRequest,
  GenerateStudyPlanRequest,
  GenerateTextRequest,
} from "@/types/ai";

function queryString(parameters: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(parameters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  const value = query.toString();
  return value ? `?${value}` : "";
}

const aiPath = "/ai";

export function createAiSession(request: CreateAiSessionRequest) {
  return apiRequest<AiSession>(`${aiPath}/sessions`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function listAiSessions({
  mode,
  page = 0,
  signal,
  size = 20,
  sort = "NEWEST",
}: {
  mode?: AiSessionMode;
  page?: number;
  signal?: AbortSignal;
  size?: number;
  sort?: AiSort;
} = {}) {
  return apiRequest<AiSessionPage>(
    `${aiPath}/sessions${queryString({ mode, page, size, sort })}`,
    { signal },
  );
}

export function getAiSession(sessionId: string, signal?: AbortSignal) {
  return apiRequest<AiSessionDetail>(`${aiPath}/sessions/${sessionId}`, {
    signal,
  });
}

export function deleteAiSession(sessionId: string) {
  return apiRequest<void>(`${aiPath}/sessions/${sessionId}`, {
    method: "DELETE",
  });
}

export function sendAiMessage(sessionId: string, content: string) {
  return apiRequest<AiChatResponse>(
    `${aiPath}/sessions/${sessionId}/messages`,
    {
      body: JSON.stringify({ content }),
      method: "POST",
    },
  );
}

export function listSessionMessages(
  sessionId: string,
  page = 0,
  size = 20,
  signal?: AbortSignal,
) {
  return apiRequest<AiMessagePage>(
    `${aiPath}/sessions/${sessionId}/messages${queryString({ page, size })}`,
    { signal },
  );
}

export function explainConcept(request: ExplainConceptRequest) {
  return apiRequest<AiExplanationResponse>(`${aiPath}/explain`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function generateSummary(request: GenerateTextRequest) {
  return apiRequest<AiGeneratedItem>(`${aiPath}/summarize`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function generateFlashcards(request: GenerateCountedTextRequest) {
  return apiRequest<AiGeneratedItem>(`${aiPath}/flashcards`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function generateQuiz(request: GenerateCountedTextRequest) {
  return apiRequest<AiGeneratedItem>(`${aiPath}/quiz`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function generateStudyPlan(request: GenerateStudyPlanRequest) {
  return apiRequest<AiGeneratedItem>(`${aiPath}/study-plan`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function listGeneratedItems({
  itemType,
  page = 0,
  signal,
  size = 12,
}: {
  itemType?: AiGeneratedItemType;
  page?: number;
  signal?: AbortSignal;
  size?: number;
} = {}) {
  return apiRequest<AiGeneratedItemPage>(
    `${aiPath}/generated-items${queryString({ itemType, page, size })}`,
    { signal },
  );
}

export function getGeneratedItem(itemId: string, signal?: AbortSignal) {
  return apiRequest<AiGeneratedItem>(
    `${aiPath}/generated-items/${itemId}`,
    { signal },
  );
}

export function deleteGeneratedItem(itemId: string) {
  return apiRequest<void>(`${aiPath}/generated-items/${itemId}`, {
    method: "DELETE",
  });
}

export function listAiUsage({
  feature,
  page = 0,
  signal,
  size = 12,
}: {
  feature?: AiUsageFeature;
  page?: number;
  signal?: AbortSignal;
  size?: number;
} = {}) {
  return apiRequest<AiUsagePage>(
    `${aiPath}/usage${queryString({ feature, page, size })}`,
    { signal },
  );
}
