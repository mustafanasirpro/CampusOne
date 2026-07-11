import {
  ArrowLeft,
  ArrowRight,
  FilePlus2,
  FileSearch,
  RefreshCw,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  getMyNotes,
  getNoteManagementStatus,
  listNotes,
} from "@/api/notesApi";
import {
  Button,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  Tabs,
} from "@/components/common";
import { NoteCard, NotesFilterBar } from "@/components/notes";
import { paths } from "@/routes/paths";
import type { NotePage, NoteSort } from "@/types/notes";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

type NotesView = "library" | "mine";

const pageSize = 12;

export function NotesPage() {
  const [view, setView] = useState<NotesView>("library");
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<NoteSort>("RELEVANCE");
  const [query, setQuery] = useState("");
  const [course, setCourse] = useState("");
  const [tag, setTag] = useState("");
  const [appliedQuery, setAppliedQuery] = useState("");
  const [appliedCourse, setAppliedCourse] = useState("");
  const [appliedTag, setAppliedTag] = useState("");
  const [result, setResult] = useState<NotePage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);
  const [canManage, setCanManage] = useState(false);

  useDocumentTitle("Notes · CampusOne");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    void getNoteManagementStatus(controller.signal)
      .then((status) => {
        if (active) setCanManage(status.canManage);
      })
      .catch(() => {
        if (active) setCanManage(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    const request =
      view === "mine"
        ? getMyNotes({
            page,
            signal: controller.signal,
            size: pageSize,
            sort,
          })
        : listNotes({
            course: appliedCourse || undefined,
            page,
            q: appliedQuery || undefined,
            signal: controller.signal,
            size: pageSize,
            sort,
            tag: appliedTag || undefined,
          });

    void request
      .then((response) => {
        if (active) {
          setError(null);
          setResult(response);
        }
      })
      .catch((requestError: unknown) => {
        if (active) {
          setError(
            requestError instanceof ApiError
              ? requestError.message
              : "Notes could not be loaded. Please try again.",
          );
        }
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [appliedCourse, appliedQuery, appliedTag, page, refreshKey, sort, view]);

  const changeView = (nextView: NotesView) => {
    setIsLoading(true);
    setError(null);
    setView(nextView);
    setPage(0);
    setRefreshKey((current) => current + 1);
  };

  const applyFilters = () => {
    const normalizedQuery = query.trim();
    const normalizedCourse = course.trim();
    const normalizedTag = tag.trim();
    if (normalizedQuery && normalizedQuery.length < 2) {
      setError("Search must contain at least 2 characters.");
      setIsLoading(false);
      return;
    }
    if (normalizedQuery.length > 100) {
      setError("Search must contain 100 characters or fewer.");
      setIsLoading(false);
      return;
    }
    if (normalizedCourse && normalizedCourse.length < 2) {
      setError("Course filter must contain at least 2 characters.");
      setIsLoading(false);
      return;
    }
    if (normalizedTag && normalizedTag.length < 2) {
      setError("Tag filter must contain at least 2 characters.");
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);
    setAppliedQuery(normalizedQuery);
    setAppliedCourse(normalizedCourse);
    setAppliedTag(normalizedTag);
    setPage(0);
    setRefreshKey((current) => current + 1);
  };

  const clearFilters = () => {
    setIsLoading(true);
    setError(null);
    setQuery("");
    setCourse("");
    setTag("");
    setAppliedQuery("");
    setAppliedCourse("");
    setAppliedTag("");
    setPage(0);
    setRefreshKey((current) => current + 1);
  };

  return (
    <div className="grid gap-6 pb-8">
      <PageHeader
        actions={
          <Link
            className="inline-flex h-10 items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-brand-700"
            to={paths.noteNew}
          >
            <FilePlus2 className="size-4" />
            {canManage ? "Upload note" : "Submit note"}
          </Link>
        }
        description="Share study notes and past papers. Submissions are reviewed before they appear publicly."
        eyebrow="Study resources"
        title="Notes"
      />

      <Tabs
        activeTab={view}
        onChange={changeView}
        tabs={
          [
            {
              count:
                view === "library"
                  ? result?.totalElements
                  : undefined,
              label: "Notes library",
              value: "library",
            },
            {
              count: view === "mine" ? result?.totalElements : undefined,
              label: canManage ? "Admin uploads" : "My submissions",
              value: "mine",
            },
          ]
        }
      />

      <NotesFilterBar
        course={course}
        disabled={view === "mine"}
        onApply={applyFilters}
        onClear={clearFilters}
        onCourseChange={setCourse}
        onQueryChange={setQuery}
        onSortChange={(value) => {
          setIsLoading(true);
          setError(null);
          setSort(value);
          setPage(0);
          setRefreshKey((current) => current + 1);
        }}
        onTagChange={setTag}
        query={query}
        sort={sort}
        tag={tag}
      />

      {error ? (
        <div className="grid gap-3">
          <ErrorMessage message={error} />
          <Button
            className="w-fit"
            onClick={() => {
              setError(null);
              setIsLoading(true);
              setRefreshKey((current) => current + 1);
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
          <LoadingSpinner label="Loading notes" />
        </div>
      ) : null}

      {!isLoading && !error && result?.content.length === 0 ? (
        <EmptyState
          action={
            view === "library" &&
            (appliedQuery || appliedCourse || appliedTag) ? (
              <Button onClick={clearFilters} variant="outline">
                Clear filters
              </Button>
            ) : (
              <Link
                className="inline-flex h-10 items-center justify-center rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700"
                to={paths.noteNew}
              >
                {canManage ? "Upload a note" : "Submit a note"}
              </Link>
            )
          }
          description={
            view === "mine"
              ? canManage
                ? "No notes have been uploaded by this admin account."
                : "No notes have been submitted by your account yet."
              : appliedQuery
                ? `No approved public notes matched "${appliedQuery}". Try one word, a teacher name, course code, tag, or filename.`
                : "No approved public notes match the selected course and tag."
          }
          icon={<FileSearch className="size-6" />}
          title="No notes found"
        />
      ) : null}

      {!isLoading && !error && result && result.content.length > 0 ? (
        <>
          <div className="flex items-center justify-between gap-3">
            <p className="text-sm text-slate-500">
              Showing {result.content.length} of{" "}
              {result.totalElements.toLocaleString()} notes
            </p>
            <p className="text-xs font-medium text-slate-400">
              Page {result.page + 1} of {Math.max(result.totalPages, 1)}
            </p>
          </div>

          <div className="grid gap-4 md:grid-cols-2 2xl:grid-cols-3">
            {result.content.map((note) => (
              <NoteCard
                key={note.id}
                note={note}
                owned={canManage && view === "mine"}
                query={appliedQuery}
              />
            ))}
          </div>

          <nav
            aria-label="Notes pagination"
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
