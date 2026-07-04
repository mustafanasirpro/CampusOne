import { apiRequest } from "@/api/apiClient";
import type {
  GlobalSearchParameters,
  GlobalSearchResponse,
  SearchSuggestionResponse,
  SearchTypeMetadata,
} from "@/types/search";

function queryString(parameters: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(parameters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  return `?${query.toString()}`;
}

const searchPath = "/search";

export function globalSearch({
  page = 0,
  q,
  signal,
  size = 10,
  sort = "RELEVANCE",
  types,
}: GlobalSearchParameters) {
  return apiRequest<GlobalSearchResponse>(
    `${searchPath}${queryString({
      page,
      q,
      size,
      sort,
      types: types?.join(","),
    })}`,
    { signal },
  );
}

export function getSearchSuggestions(
  q: string,
  limit = 5,
  signal?: AbortSignal,
) {
  return apiRequest<SearchSuggestionResponse>(
    `${searchPath}/suggestions${queryString({ limit, q })}`,
    { signal },
  );
}

export function getSearchTypes(signal?: AbortSignal) {
  return apiRequest<SearchTypeMetadata[]>(`${searchPath}/types`, {
    signal,
  });
}

