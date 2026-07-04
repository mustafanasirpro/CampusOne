import { useEffect, useState } from "react";

import { ApiError } from "@/api/apiClient";
import {
  getMyReportById,
  listMyReports,
} from "@/api/moderationApi";
import {
  Dropdown,
  ErrorMessage,
  LoadingSpinner,
} from "@/components/common";
import { CreateReportForm } from "@/components/moderation/CreateReportForm";
import { ModerationPagination } from "@/components/moderation/ModerationPagination";
import { ReportDetailModal } from "@/components/moderation/ReportDetailModal";
import { ReportList } from "@/components/moderation/ReportList";
import {
  moderationTargetOptions,
  reportStatusOptions,
} from "@/components/moderation/moderationFormatting";
import type {
  ContentReportDetail,
  ContentReportPage,
  ModerationSort,
  ModerationTargetType,
  ReportStatus,
} from "@/types/moderation";

const pageSize = 12;

export function MyReportsPanel() {
  const [status, setStatus] = useState<ReportStatus | "">("");
  const [targetType, setTargetType] =
    useState<ModerationTargetType | "">("");
  const [sort, setSort] = useState<ModerationSort>("NEWEST");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<ContentReportPage | null>(null);
  const [selected, setSelected] = useState<ContentReportDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isOpening, setIsOpening] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void listMyReports({
      page,
      signal: controller.signal,
      size: pageSize,
      sort,
      status: status || undefined,
      targetType: targetType || undefined,
    })
      .then((response) => {
        if (!active) return;
        setResult(response);
        setError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setError(
          requestError instanceof ApiError
            ? requestError.message
            : "Your reports could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [page, refreshKey, sort, status, targetType]);

  const openReport = async (reportId: string) => {
    setIsOpening(true);
    setError(null);
    try {
      setSelected(await getMyReportById(reportId));
    } catch (requestError) {
      setError(
        requestError instanceof ApiError && requestError.status === 404
          ? "This report could not be loaded."
          : requestError instanceof ApiError
            ? requestError.message
            : "This report could not be loaded.",
      );
    } finally {
      setIsOpening(false);
    }
  };

  const resetPage = () => {
    setPage(0);
    setIsLoading(true);
  };

  return (
    <div className="grid gap-6">
      <CreateReportForm
        onCreated={(report) => {
          setSelected(report);
          setPage(0);
          setIsLoading(true);
          setRefreshKey((value) => value + 1);
        }}
      />

      <section className="grid gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-950">My reports</h2>
          <p className="mt-1 text-sm text-slate-500">
            Track reports you submitted and read moderator decisions.
          </p>
        </div>
        <div className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-card sm:grid-cols-3">
          <Dropdown
            label="Status"
            onChange={(event) => {
              setStatus(event.target.value as ReportStatus | "");
              resetPage();
            }}
            options={[
              { label: "All statuses", value: "" },
              ...reportStatusOptions,
            ]}
            value={status}
          />
          <Dropdown
            label="Target type"
            onChange={(event) => {
              setTargetType(event.target.value as ModerationTargetType | "");
              resetPage();
            }}
            options={[
              { label: "All target types", value: "" },
              ...moderationTargetOptions,
            ]}
            value={targetType}
          />
          <Dropdown
            label="Sort"
            onChange={(event) => {
              setSort(event.target.value as ModerationSort);
              resetPage();
            }}
            options={[
              { label: "Newest", value: "NEWEST" },
              { label: "Oldest", value: "OLDEST" },
            ]}
            value={sort}
          />
        </div>

        {error ? <ErrorMessage message={error} /> : null}
        {isLoading ? (
          <div className="grid min-h-64 place-items-center rounded-2xl border border-slate-200 bg-white">
            <LoadingSpinner label="Loading your reports" />
          </div>
        ) : result ? (
          <>
            <ReportList
              emptyDescription="Reports you submit will appear here."
              onOpen={(reportId) => void openReport(reportId)}
              reports={result.content}
            />
            <ModerationPagination
              first={result.first}
              last={result.last}
              onPageChange={(nextPage) => {
                setPage(nextPage);
                setIsLoading(true);
              }}
              page={result.page}
              totalPages={result.totalPages}
            />
          </>
        ) : null}
      </section>

      <ReportDetailModal
        isLoading={isOpening}
        onClose={() => {
          setSelected(null);
          setIsOpening(false);
        }}
        report={selected}
      />
    </div>
  );
}
