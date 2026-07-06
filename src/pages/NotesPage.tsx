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
const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function NotesPage() {
  const [view, setView] = useState<NotesView>("library");
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<NoteSort>("NEWEST");
  const [courseId, setCourseId] = useState("");
  const [tag, setTag] = useState("");
  const [appliedCourseId, setAppliedCourseId] = useState("");
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
            courseId: appliedCourseId || undefined,
            page,
            signal: controller.signal,
            size: pageSize,
            sort,
            tag: appliedTag || undefined,
          });

    void request
      .then((response) => {
        if (active) setResult(response);
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
  }, [appliedCourseId, appliedTag, page, refreshKey, sort, view]);

  const changeView = (nextView: NotesView) => {
    setIsLoading(true);
    setError(null);
    setView(nextView);
    setPage(0);
    setRefreshKey((current) => current + 1);
  };

  const applyFilters = () => {
    const normalizedCourseId = courseId.trim();
    const normalizedTag = tag.trim();
    if (normalizedCourseId && !uuidPattern.test(normalizedCourseId)) {
      setError("Course ID must be a valid UUID.");
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
    setAppliedCourseId(normalizedCourseId);
    setAppliedTag(normalizedTag);
    setPage(0);
    setRefreshKey((current) => current + 1);
  };

  const clearFilters = () => {
    setIsLoading(true);
    setError(null);
    setCourseId("");
    setTag("");
    setAppliedCourseId("");
    setAppliedTag("");
    setPage(0);
    setRefreshKey((current) => current + 1);
  };

  return (
    <div className="grid gap-6 pb-8">
      <PageHeader
        actions={
          canManage ? (
            <Link
              className="inline-flex h-10 items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white shadow-sm transition hover:bg-brand-700"
              to={paths.noteNew}
            >
              <FilePlus2 className="size-4" />
              Upload note
            </Link>
          ) : null
        }
        description="Discover approved notes and past papers. Upload and management tools are available to admins."
        eyebrow="Study resources"
        title="Notes"
      />

      <Tabs
        activeTab={view}
        onChange={changeView}
        tabs={
          canManage
            ? [
                {
                  count:
                    view === "library"
                      ? result?.totalElements
                      : undefined,
                  label: "Notes library",
                  value: "library",
                },
                {
                  count:
                    view === "mine" ? result?.totalElements : undefined,
                  label: "Admin uploads",
                  value: "mine",
                },
              ]
            : [
                {
                  count: result?.totalElements,
                  label: "Notes library",
                  value: "library",
                },
              ]
        }
      />

      <NotesFilterBar
        courseId={courseId}
        disabled={view === "mine"}
        onApply={applyFilters}
        onClear={clearFilters}
        onCourseIdChange={setCourseId}
        onSortChange={(value) => {
          setIsLoading(true);
          setError(null);
          setSort(value);
          setPage(0);
          setRefreshKey((current) => current + 1);
        }}
        onTagChange={setTag}
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
            (appliedCourseId || appliedTag) ? (
              <Button onClick={clearFilters} variant="outline">
                Clear filters
              </Button>
            ) : canManage ? (
              <Link
                className="inline-flex h-10 items-center justify-center rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700"
                to={paths.noteNew}
              >
                Upload a note
              </Link>
            ) : undefined
          }
          description={
            view === "mine"
              ? "No notes have been uploaded by this admin account."
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
