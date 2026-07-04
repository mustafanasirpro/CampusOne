import { ArrowLeft, ArrowRight, CalendarPlus, RefreshCw } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { getJoinedEvents, getMyEvents, listEvents } from "@/api/eventsApi";
import {
  Button,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  Tabs,
} from "@/components/common";
import { EventCard, EventFilterBar } from "@/components/events";
import { paths } from "@/routes/paths";
import type { EventPage, EventSort, EventStatus } from "@/types/events";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

type EventsView = "browse" | "joined" | "mine";
const pageSize = 12;

export function EventsPage() {
  const [view, setView] = useState<EventsView>("browse");
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<EventSort>("UPCOMING");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<EventStatus | "">("");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [appliedStatus, setAppliedStatus] = useState<EventStatus | "">("");
  const [result, setResult] = useState<EventPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);

  useDocumentTitle("Campus Events · CampusOne");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    const common = { page, signal: controller.signal, size: pageSize, sort };
    const request =
      view === "mine"
        ? getMyEvents(common)
        : view === "joined"
          ? getJoinedEvents(common)
          : listEvents({
              ...common,
              search: appliedSearch || undefined,
              status: appliedStatus || undefined,
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
            : "Events could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [appliedSearch, appliedStatus, page, refreshKey, sort, view]);

  const applyFilters = () => {
    setPage(0);
    setAppliedSearch(search.trim());
    setAppliedStatus(status);
    setIsLoading(true);
    setRefreshKey((value) => value + 1);
  };
  const reset = () => {
    setSearch("");
    setStatus("");
    setAppliedSearch("");
    setAppliedStatus("");
    setSort("UPCOMING");
    setPage(0);
    setIsLoading(true);
    setRefreshKey((value) => value + 1);
  };
  const changeView = (next: EventsView) => {
    if (next === view) return;
    setView(next);
    setPage(0);
    setResult(null);
    setError(null);
    setIsLoading(true);
  };

  return (
    <div className="grid gap-6 pb-8">
      <PageHeader
        actions={
          <Link
            className="inline-flex h-10 items-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700"
            to={paths.eventNew}
          >
            <CalendarPlus className="size-4" />
            Create event
          </Link>
        }
        description="Discover and organize workshops, meetups, and campus experiences."
        eyebrow="Campus calendar"
        title="Events"
      />
      <Tabs
        activeTab={view}
        onChange={changeView}
        tabs={[
          { label: "Browse events", value: "browse" },
          { label: "My events", value: "mine" },
          { label: "Joined events", value: "joined" },
        ]}
      />
      <EventFilterBar
        filtersDisabled={view !== "browse"}
        onApply={applyFilters}
        onClear={reset}
        onSearchChange={setSearch}
        onSortChange={(value) => {
          setSort(value);
          setPage(0);
          setIsLoading(true);
        }}
        onStatusChange={setStatus}
        search={search}
        sort={sort}
        status={status}
      />
      {error ? (
        <div className="grid gap-3">
          <ErrorMessage message={error} />
          <Button
            className="w-fit"
            onClick={() => {
              setError(null);
              setIsLoading(true);
              setRefreshKey((value) => value + 1);
            }}
            variant="outline"
          >
            <RefreshCw className="size-4" />
            Try again
          </Button>
        </div>
      ) : null}
      {isLoading ? (
        <div className="grid min-h-72 place-items-center rounded-2xl border border-slate-200 bg-white">
          <LoadingSpinner label="Loading events" />
        </div>
      ) : null}
      {!isLoading && !error && result?.content.length === 0 ? (
        <EmptyState
          action={
            <Link
              className="inline-flex h-10 items-center rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white"
              to={paths.eventNew}
            >
              Create an event
            </Link>
          }
          description={
            view === "joined"
              ? "You have not joined any events yet."
              : view === "mine"
                ? "You have not organized any events yet."
                : "No public events match the current filters."
          }
          icon={<CalendarPlus className="size-6" />}
          title="No events found"
        />
      ) : null}
      {!isLoading && !error && result && result.content.length > 0 ? (
        <>
          <p className="text-sm text-slate-500">
            Showing {result.content.length} of {result.totalElements} events
          </p>
          <div className="grid gap-5 md:grid-cols-2 2xl:grid-cols-3">
            {result.content.map((event) => (
              <EventCard event={event} key={event.id} />
            ))}
          </div>
          <nav
            aria-label="Events pagination"
            className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white p-3"
          >
            <Button
              disabled={result.first}
              onClick={() => {
                setPage((value) => Math.max(0, value - 1));
                setIsLoading(true);
              }}
              variant="outline"
            >
              <ArrowLeft className="size-4" /> Previous
            </Button>
            <span className="text-sm font-semibold">
              {result.page + 1} / {Math.max(1, result.totalPages)}
            </span>
            <Button
              disabled={result.last}
              onClick={() => {
                setPage((value) => value + 1);
                setIsLoading(true);
              }}
              variant="outline"
            >
              Next <ArrowRight className="size-4" />
            </Button>
          </nav>
        </>
      ) : null}
    </div>
  );
}
