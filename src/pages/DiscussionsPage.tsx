import {
  ArrowLeft,
  ArrowRight,
  MessageSquarePlus,
  MessagesSquare,
  RefreshCw,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { getMyQuestions, listQuestions } from "@/api/discussionApi";
import {
  Button,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  Tabs,
} from "@/components/common";
import {
  QuestionCard,
  QuestionFilterBar,
} from "@/components/discussion";
import { paths } from "@/routes/paths";
import type {
  DiscussionCategory,
  DiscussionQuestionPage,
  DiscussionQuestionSort,
} from "@/types/discussion";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

type DiscussionView = "community" | "mine";

const pageSize = 12;

export function DiscussionsPage() {
  const [view, setView] = useState<DiscussionView>("community");
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<DiscussionQuestionSort>("NEWEST");
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState<DiscussionCategory | "">("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [appliedCategory, setAppliedCategory] = useState<
    DiscussionCategory | ""
  >("");
  const [result, setResult] = useState<DiscussionQuestionPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);

  useDocumentTitle("Discussions · CampusOne");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    const request =
      view === "mine"
        ? getMyQuestions({
            page,
            signal: controller.signal,
            size: pageSize,
            sort,
          })
        : listQuestions({
            category: appliedCategory || undefined,
            page,
            search: appliedSearch || undefined,
            signal: controller.signal,
            size: pageSize,
            sort,
          });

    void request
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
            : "Discussion questions could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [appliedCategory, appliedSearch, page, refreshKey, sort, view]);

  const applyFilters = () => {
    setPage(0);
    setAppliedSearch(search.trim());
    setAppliedCategory(category);
    setIsLoading(true);
    setRefreshKey((current) => current + 1);
  };

  const clearFilters = () => {
    setSearch("");
    setCategory("");
    setAppliedSearch("");
    setAppliedCategory("");
    setSort("NEWEST");
    setPage(0);
    setIsLoading(true);
    setRefreshKey((current) => current + 1);
  };

  const changeView = (nextView: DiscussionView) => {
    if (nextView === view) return;
    setView(nextView);
    setPage(0);
    setResult(null);
    setError(null);
    setIsLoading(true);
  };

  const changeSort = (nextSort: DiscussionQuestionSort) => {
    setSort(nextSort);
    setPage(0);
    setIsLoading(true);
  };

  const retry = () => {
    setError(null);
    setIsLoading(true);
    setRefreshKey((current) => current + 1);
  };

  return (
    <div className="grid gap-6 pb-8">
      <PageHeader
        actions={
          <Link
            className="inline-flex h-10 items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white transition hover:bg-brand-700"
            to={paths.discussionQuestionNew}
          >
            <MessageSquarePlus className="size-4" />
            Ask a question
          </Link>
        }
        description="Ask focused questions, share practical answers, and learn with your campus community."
        eyebrow="Community Q&A"
        title="Discussions"
      />

      <Tabs
        activeTab={view}
        onChange={changeView}
        tabs={[
          { label: "Community questions", value: "community" },
          { label: "My questions", value: "mine" },
        ]}
      />

      <QuestionFilterBar
        category={category}
        filtersDisabled={view === "mine"}
        onApply={applyFilters}
        onCategoryChange={setCategory}
        onClear={clearFilters}
        onSearchChange={setSearch}
        onSortChange={changeSort}
        search={search}
        sort={sort}
      />

      {view === "mine" ? (
        <p className="rounded-xl border border-brand-100 bg-brand-50 px-4 py-3 text-sm text-brand-800">
          My Questions includes your visible questions. Search and category
          filters are available on the community feed; sorting works in both
          views.
        </p>
      ) : null}

      {error ? (
        <div className="grid gap-3">
          <ErrorMessage message={error} />
          <Button className="w-fit" onClick={retry} variant="outline">
            <RefreshCw className="size-4" />
            Try again
          </Button>
        </div>
      ) : null}

      {isLoading ? (
        <div className="grid min-h-72 place-items-center rounded-2xl border border-slate-200 bg-white">
          <LoadingSpinner label="Loading discussion questions" />
        </div>
      ) : null}

      {!isLoading && !error && result?.content.length === 0 ? (
        <EmptyState
          action={
            view === "community" &&
            (appliedSearch || appliedCategory) ? (
              <Button onClick={clearFilters} variant="outline">
                Clear filters
              </Button>
            ) : (
              <Link
                className="inline-flex h-10 items-center justify-center rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700"
                to={paths.discussionQuestionNew}
              >
                Ask the first question
              </Link>
            )
          }
          description={
            view === "mine"
              ? "You have not asked any discussion questions yet."
              : "No visible questions match your current search and category."
          }
          icon={<MessagesSquare className="size-6" />}
          title="No questions found"
        />
      ) : null}

      {!isLoading && !error && result && result.content.length > 0 ? (
        <>
          <div className="flex items-center justify-between gap-3">
            <p className="text-sm text-slate-500">
              Showing {result.content.length} of{" "}
              {result.totalElements.toLocaleString()} questions
            </p>
            <p className="text-xs font-medium text-slate-400">
              Page {result.page + 1} of {Math.max(result.totalPages, 1)}
            </p>
          </div>

          <div className="grid gap-4">
            {result.content.map((question) => (
              <QuestionCard
                key={question.id}
                owned={view === "mine"}
                question={question}
              />
            ))}
          </div>

          <nav
            aria-label="Discussion pagination"
            className="flex items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-3"
          >
            <Button
              disabled={result.first}
              onClick={() => {
                setIsLoading(true);
                setPage((current) => Math.max(0, current - 1));
              }}
              variant="outline"
            >
              <ArrowLeft className="size-4" />
              Previous
            </Button>
            <span className="text-sm font-semibold text-slate-700">
              {result.page + 1} / {Math.max(result.totalPages, 1)}
            </span>
            <Button
              disabled={result.last}
              onClick={() => {
                setIsLoading(true);
                setPage((current) => current + 1);
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
