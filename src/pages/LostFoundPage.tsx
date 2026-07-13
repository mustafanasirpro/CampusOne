import {
  ArrowLeft,
  ArrowRight,
  Plus,
  RefreshCw,
  SearchX,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  listLostFoundItems,
  listMyLostFoundItems,
} from "@/api/lostFoundApi";
import {
  Button,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  Tabs,
} from "@/components/common";
import {
  LostFoundFilterBar,
  LostFoundItemCard,
} from "@/components/lost-found";
import { paths } from "@/routes/paths";
import type {
  LostFoundCategory,
  LostFoundItemPage,
  LostFoundItemType,
} from "@/types/lostFound";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

type LostFoundView = "browse" | "mine";

const pageSize = 12;

export function LostFoundPage() {
  const [view, setView] = useState<LostFoundView>("browse");
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [type, setType] = useState<LostFoundItemType | "">("");
  const [category, setCategory] = useState<LostFoundCategory | "">("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [appliedType, setAppliedType] = useState<LostFoundItemType | "">("");
  const [appliedCategory, setAppliedCategory] = useState<
    LostFoundCategory | ""
  >("");
  const [result, setResult] = useState<LostFoundItemPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);

  useDocumentTitle("Lost & Found · CampusOne");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    const request =
      view === "mine"
        ? listMyLostFoundItems({
            page,
            signal: controller.signal,
            size: pageSize,
          })
        : listLostFoundItems({
            category: appliedCategory || undefined,
            page,
            search: appliedSearch || undefined,
            signal: controller.signal,
            size: pageSize,
            type: appliedType || undefined,
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
            : "Lost & Found items could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [appliedCategory, appliedSearch, appliedType, page, refreshKey, view]);

  const applyFilters = () => {
    setPage(0);
    setIsLoading(true);
    setAppliedSearch(search.trim());
    setAppliedType(type);
    setAppliedCategory(category);
    setRefreshKey((current) => current + 1);
  };

  const clearFilters = () => {
    setSearch("");
    setType("");
    setCategory("");
    setAppliedSearch("");
    setAppliedType("");
    setAppliedCategory("");
    setPage(0);
    setIsLoading(true);
    setRefreshKey((current) => current + 1);
  };

  const changeView = (nextView: LostFoundView) => {
    if (nextView === view) return;
    setView(nextView);
    setPage(0);
    setError(null);
    setResult(null);
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
            to={paths.lostFoundNew}
          >
            <Plus className="size-4" />
            Submit item
          </Link>
        }
        description="Report lost or found items, review safe matches, and help students recover what matters."
        eyebrow="Campus safety"
        title="Lost & Found"
      />

      <Tabs
        activeTab={view}
        onChange={changeView}
        tabs={[
          { label: "Browse items", value: "browse" },
          { label: "My submissions", value: "mine" },
        ]}
      />

      {view === "browse" ? (
        <LostFoundFilterBar
          category={category}
          onApply={applyFilters}
          onCategoryChange={setCategory}
          onClear={clearFilters}
          onSearchChange={setSearch}
          onTypeChange={setType}
          search={search}
          type={type}
        />
      ) : (
        <p className="rounded-xl border border-brand-100 bg-brand-50 px-4 py-3 text-sm text-brand-800">
          My submissions includes items waiting for review, active posts, and resolved records.
        </p>
      )}

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
          <LoadingSpinner label="Loading Lost & Found items" />
        </div>
      ) : null}

      {!isLoading && !error && result?.content.length === 0 ? (
        <EmptyState
          action={
            view === "browse" && (appliedSearch || appliedType || appliedCategory) ? (
              <Button onClick={clearFilters} variant="outline">
                Clear filters
              </Button>
            ) : (
              <Link
                className="inline-flex h-10 items-center justify-center rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700"
                to={paths.lostFoundNew}
              >
                Submit an item
              </Link>
            )
          }
          description={
            view === "mine"
              ? "You have not submitted any Lost & Found items yet."
              : "No published items match your current search."
          }
          icon={<SearchX className="size-6" />}
          title="No items found"
        />
      ) : null}

      {!isLoading && !error && result && result.content.length > 0 ? (
        <>
          <div className="flex items-center justify-between gap-3">
            <p className="text-sm text-slate-500">
              Showing {result.content.length} of{" "}
              {result.totalElements.toLocaleString()} items
            </p>
            <p className="text-xs font-medium text-slate-400">
              Page {result.page + 1} of {Math.max(result.totalPages, 1)}
            </p>
          </div>

          <div className="grid gap-5 sm:grid-cols-2 2xl:grid-cols-3">
            {result.content.map((item) => (
              <LostFoundItemCard
                item={item}
                key={item.id}
                showStatus={view === "mine"}
              />
            ))}
          </div>

          <nav
            aria-label="Lost and Found pagination"
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
