import { ArrowLeft, ArrowRight, Search, SearchX } from "lucide-react";
import { useEffect, useState, type FormEvent } from "react";
import { useSearchParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  getSearchSuggestions,
  getSearchTypes,
  globalSearch,
} from "@/api/searchApi";
import {
  Button,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
} from "@/components/common";
import {
  SearchFilters,
  SearchResultCard,
  SearchSuggestions,
} from "@/components/search";
import type {
  GlobalSearchResponse,
  GlobalSearchSort,
  GlobalSearchType,
  SearchTypeMetadata,
} from "@/types/search";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

const pageSize = 10;

function normalizedSearchQuery(value: string | null) {
  const normalized = value?.trim() ?? "";
  return normalized.length >= 2 && normalized.length <= 100
    ? normalized
    : "";
}

function isAbortError(error: unknown) {
  return error instanceof DOMException && error.name === "AbortError";
}

export function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const query = normalizedSearchQuery(searchParams.get("q"));
  const [draftState, setDraftState] = useState({
    sourceQuery: query,
    value: query,
  });
  const draft =
    draftState.sourceQuery === query ? draftState.value : query;
  const [pageState, setPageState] = useState({
    page: 0,
    query,
  });
  const page = pageState.query === query ? pageState.page : 0;
  const [sort, setSort] = useState<GlobalSearchSort>("RELEVANCE");
  const [selectedTypes, setSelectedTypes] = useState<GlobalSearchType[]>([]);
  const [availableTypes, setAvailableTypes] = useState<SearchTypeMetadata[]>([]);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [result, setResult] = useState<GlobalSearchResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filterError, setFilterError] = useState<string | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(query));
  const [searchKey, setSearchKey] = useState(0);
  const searchPending =
    isLoading ||
    (Boolean(query) && !error && result?.query !== query);

  useDocumentTitle("Global Search · CampusOne");

  const setPage = (nextPage: number | ((current: number) => number)) => {
    setPageState((current) => {
      const currentPage = current.query === query ? current.page : 0;
      return {
        page:
          typeof nextPage === "function"
            ? nextPage(currentPage)
            : nextPage,
        query,
      };
    });
  };

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void getSearchTypes(controller.signal)
      .then((response) => {
        if (!active) return;
        setAvailableTypes(response);
        setFilterError(null);
      })
      .catch((requestError: unknown) => {
        if (!active || isAbortError(requestError)) return;
        setFilterError(
          requestError instanceof ApiError
            ? requestError.message
            : "Search filters could not be loaded.",
        );
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  useEffect(() => {
    if (draft.trim().length < 2 || draft.trim() === query) return;
    const controller = new AbortController();
    const timeout = window.setTimeout(() => {
      void getSearchSuggestions(draft.trim(), 5, controller.signal)
        .then((response) => setSuggestions(response.suggestions))
        .catch((requestError: unknown) => {
          if (!isAbortError(requestError)) setSuggestions([]);
        });
    }, 250);
    return () => {
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [draft, query]);

  useEffect(() => {
    if (!query) return;
    const controller = new AbortController();
    let active = true;
    void globalSearch({
      page,
      q: query,
      signal: controller.signal,
      size: pageSize,
      sort,
      types: selectedTypes.length > 0 ? selectedTypes : undefined,
    })
      .then((response) => {
        if (!active) return;
        setResult(response);
        setError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setResult(null);
        setError(
          requestError instanceof ApiError
            ? requestError.message
            : "Search could not be completed.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [page, query, searchKey, selectedTypes, sort]);

  const runSearch = (value: string) => {
    const normalized = value.trim();
    if (normalized.length < 2) {
      setValidationError("Enter at least 2 characters to search.");
      return;
    }
    if (normalized.length > 100) {
      setValidationError("Search queries cannot exceed 100 characters.");
      return;
    }
    setDraftState({ sourceQuery: normalized, value: normalized });
    setPage(0);
    setSuggestions([]);
    setValidationError(null);
    setError(null);
    setIsLoading(true);
    setSearchKey((value) => value + 1);
    setSearchParams({ q: normalized });
  };

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    runSearch(draft);
  };

  const toggleType = (type: GlobalSearchType) => {
    setSelectedTypes((current) =>
      current.includes(type)
        ? current.filter((item) => item !== type)
        : [...current, type],
    );
    setPage(0);
    if (query) setIsLoading(true);
  };

  return (
    <div className="grid gap-6 pb-8">
      <PageHeader
        description="Find public notes, listings, discussions, events, and internships from one place."
        eyebrow="CampusOne discovery"
        title="Global Search"
      />
      <form
        className="flex flex-col gap-2 sm:flex-row"
        onSubmit={submit}
        role="search"
      >
        <label className="sr-only" htmlFor="global-search-query">
          Search CampusOne
        </label>
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-slate-400" />
          <input
            className="h-12 w-full rounded-xl border border-slate-200 bg-white pl-12 pr-4 text-sm outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
            id="global-search-query"
            maxLength={100}
            onChange={(event) => {
              setDraftState({
                sourceQuery: query,
                value: event.target.value,
              });
              setValidationError(null);
              if (event.target.value.trim().length < 2) setSuggestions([]);
            }}
            placeholder="Search across CampusOne..."
            value={draft}
          />
        </div>
        <Button size="lg" type="submit">
          <Search className="size-4" />
          Search
        </Button>
      </form>
      {validationError ? <ErrorMessage message={validationError} /> : null}
      {filterError ? <ErrorMessage message={filterError} /> : null}
      <SearchSuggestions
        onSelect={(suggestion) => runSearch(suggestion)}
        suggestions={suggestions}
      />
      <SearchFilters
        availableTypes={availableTypes}
        onSortChange={(value) => {
          setSort(value);
          setPage(0);
          if (query) setIsLoading(true);
        }}
        onToggleType={toggleType}
        selectedTypes={selectedTypes}
        sort={sort}
      />
      {error ? <ErrorMessage message={error} /> : null}
      {searchPending ? (
        <div className="grid min-h-72 place-items-center rounded-2xl border border-slate-200 bg-white">
          <LoadingSpinner label="Searching CampusOne" />
        </div>
      ) : null}
      {!query && !searchPending ? (
        <EmptyState
          description="Enter a topic, course, company, event, or item to begin."
          icon={<Search className="size-6" />}
          title="What are you looking for?"
        />
      ) : null}
      {query && !searchPending && !error && result?.results.length === 0 ? (
        <EmptyState
          action={
            selectedTypes.length > 0 ? (
              <Button
                onClick={() => {
                  setSelectedTypes([]);
                  setPage(0);
                  setIsLoading(true);
                }}
                variant="outline"
              >
                Search all types
              </Button>
            ) : undefined
          }
          description="Try a broader phrase or search across every content type."
          icon={<SearchX className="size-6" />}
          title="No results found"
        />
      ) : null}
      {!searchPending && !error && result && result.results.length > 0 ? (
        <>
          <p className="text-sm text-slate-500">
            {result.totalElements.toLocaleString()} results for “{result.query}”
          </p>
          <div className="grid gap-4">
            {result.results.map((item) => (
              <SearchResultCard key={`${item.type}-${item.id}`} result={item} />
            ))}
          </div>
          <nav
            aria-label="Search pagination"
            className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white p-3"
          >
            <Button
              disabled={result.page === 0}
              onClick={() => {
                setPage((value) => Math.max(0, value - 1));
                setIsLoading(true);
              }}
              variant="outline"
            >
              <ArrowLeft className="size-4" />
              Previous
            </Button>
            <span className="text-sm font-semibold">
              {result.page + 1} / {Math.max(1, result.totalPages)}
            </span>
            <Button
              disabled={!result.hasNext}
              onClick={() => {
                setPage((value) => value + 1);
                setIsLoading(true);
              }}
              variant="outline"
            >
              Next
              <ArrowRight className="size-4" />
            </Button>
          </nav>
        </>
      ) : null}
    </div>
  );
}
