import {
  ArrowLeft,
  ArrowRight,
  Plus,
  RefreshCw,
  Store,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { getMyListings, listListings } from "@/api/marketplaceApi";
import {
  Button,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  Tabs,
} from "@/components/common";
import {
  MarketplaceFilterBar,
  MarketplaceListingCard,
} from "@/components/marketplace";
import { paths } from "@/routes/paths";
import type {
  MarketplaceCategory,
  MarketplaceListingPage,
} from "@/types/marketplace";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

type MarketplaceView = "browse" | "mine";

const pageSize = 12;

export function MarketplacePage() {
  const [view, setView] = useState<MarketplaceView>("browse");
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState<MarketplaceCategory | "">("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [appliedCategory, setAppliedCategory] = useState<
    MarketplaceCategory | ""
  >("");
  const [result, setResult] = useState<MarketplaceListingPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);

  useDocumentTitle("Marketplace · CampusOne");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    const request =
      view === "mine"
        ? getMyListings({
            page,
            signal: controller.signal,
            size: pageSize,
          })
        : listListings({
            category: appliedCategory || undefined,
            page,
            search: appliedSearch || undefined,
            signal: controller.signal,
            size: pageSize,
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
            : "Marketplace listings could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [appliedCategory, appliedSearch, page, refreshKey, view]);

  const applyFilters = () => {
    setIsLoading(true);
    setPage(0);
    setAppliedSearch(search.trim());
    setAppliedCategory(category);
    setRefreshKey((current) => current + 1);
  };

  const clearFilters = () => {
    setSearch("");
    setCategory("");
    setAppliedSearch("");
    setAppliedCategory("");
    setPage(0);
    setIsLoading(true);
    setRefreshKey((current) => current + 1);
  };

  const changeView = (nextView: MarketplaceView) => {
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
            to={paths.marketplaceNew}
          >
            <Plus className="size-4" />
            Create listing
          </Link>
        }
        description="Find useful campus items or give something you no longer need a new home."
        eyebrow="Student marketplace"
        title="Marketplace"
      />

      <Tabs
        activeTab={view}
        onChange={changeView}
        tabs={[
          { label: "Browse listings", value: "browse" },
          { label: "My listings", value: "mine" },
        ]}
      />

      <MarketplaceFilterBar
        category={category}
        disabled={view === "mine"}
        onApply={applyFilters}
        onCategoryChange={setCategory}
        onClear={clearFilters}
        onSearchChange={setSearch}
        search={search}
      />

      {view === "mine" ? (
        <p className="rounded-xl border border-brand-100 bg-brand-50 px-4 py-3 text-sm text-brand-800">
          My Listings includes your active and sold listings. Search and
          category filters are available only in the public marketplace.
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
          <LoadingSpinner label="Loading marketplace listings" />
        </div>
      ) : null}

      {!isLoading && !error && result?.content.length === 0 ? (
        <EmptyState
          action={
            view === "browse" && (appliedSearch || appliedCategory) ? (
              <Button onClick={clearFilters} variant="outline">
                Clear filters
              </Button>
            ) : (
              <Link
                className="inline-flex h-10 items-center justify-center rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700"
                to={paths.marketplaceNew}
              >
                Create your first listing
              </Link>
            )
          }
          description={
            view === "mine"
              ? "You have not created any marketplace listings yet."
              : "No active listings match your current search and category."
          }
          icon={<Store className="size-6" />}
          title="No listings found"
        />
      ) : null}

      {!isLoading && !error && result && result.content.length > 0 ? (
        <>
          <div className="flex items-center justify-between gap-3">
            <p className="text-sm text-slate-500">
              Showing {result.content.length} of{" "}
              {result.totalElements.toLocaleString()} listings
            </p>
            <p className="text-xs font-medium text-slate-400">
              Page {result.page + 1} of {Math.max(result.totalPages, 1)}
            </p>
          </div>

          <div className="grid gap-5 sm:grid-cols-2 2xl:grid-cols-3">
            {result.content.map((listing) => (
              <MarketplaceListingCard
                key={listing.id}
                listing={listing}
                owned={view === "mine"}
              />
            ))}
          </div>

          <nav
            aria-label="Marketplace pagination"
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
