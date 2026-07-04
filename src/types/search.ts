export type GlobalSearchType =
  | "NOTE"
  | "MARKETPLACE"
  | "DISCUSSION"
  | "EVENT"
  | "INTERNSHIP";

export type GlobalSearchSort = "RELEVANCE" | "NEWEST" | "OLDEST";

export interface GlobalSearchResult {
  createdAt: string;
  id: string;
  metadata: Record<string, unknown>;
  ownerOrAuthorName: string | null;
  snippet: string;
  targetUrl: string;
  title: string;
  type: GlobalSearchType;
  updatedAt: string | null;
}

export interface GlobalSearchResponse {
  hasNext: boolean;
  page: number;
  query: string;
  results: GlobalSearchResult[];
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SearchSuggestionResponse {
  query: string;
  suggestions: string[];
}

export interface SearchTypeMetadata {
  description: string;
  displayName: string;
  type: GlobalSearchType;
}

export interface GlobalSearchParameters {
  page?: number;
  q: string;
  signal?: AbortSignal;
  size?: number;
  sort?: GlobalSearchSort;
  types?: GlobalSearchType[];
}

