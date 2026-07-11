import { RotateCcw, Search } from "lucide-react";
import {
  useEffect,
  useState,
  type FormEvent,
} from "react";

import { ApiError } from "@/api/apiClient";
import {
  dismissReport,
  getAdminReportById,
  listAdminReports,
  markReportUnderReview,
  resolveReport,
} from "@/api/moderationApi";
import {
  Button,
  Dropdown,
  ErrorMessage,
  LoadingSpinner,
  useToast,
} from "@/components/common";
import { FormField } from "@/components/forms";
import { ModerationPagination } from "@/components/moderation/ModerationPagination";
import { ReportDetailModal } from "@/components/moderation/ReportDetailModal";
import { ReportList } from "@/components/moderation/ReportList";
import {
  moderationTargetOptions,
  isUuid,
  reportReasonOptions,
  reportStatusOptions,
} from "@/components/moderation/moderationFormatting";
import {
  ResolutionModal,
  type ResolutionMode,
} from "@/components/moderation/ResolutionModal";
import type {
  ContentReportDetail,
  ContentReportPage,
  ModerationSort,
  ModerationTargetType,
  ReportReason,
  ReportStatus,
} from "@/types/moderation";

const pageSize = 12;

export function AdminReportsPanel() {
  const { showToast } = useToast();
  const [status, setStatus] = useState<ReportStatus | "">("");
  const [reason, setReason] = useState<ReportReason | "">("");
  const [targetType, setTargetType] =
    useState<ModerationTargetType | "">("");
  const [sort, setSort] = useState<ModerationSort>("NEWEST");
  const [reporterInput, setReporterInput] = useState("");
  const [reporterUserId, setReporterUserId] = useState("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<ContentReportPage | null>(null);
  const [selected, setSelected] = useState<ContentReportDetail | null>(null);
  const [resolutionMode, setResolutionMode] =
    useState<ResolutionMode | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isOpening, setIsOpening] = useState(false);
  const [isActing, setIsActing] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [resolutionError, setResolutionError] = useState<string | null>(null);
  const [filterError, setFilterError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void listAdminReports({
      page,
      reason: reason || undefined,
      reporterUserId: reporterUserId || undefined,
      signal: controller.signal,
      size: pageSize,
      sort,
      status: status || undefined,
      targetType: targetType || undefined,
    })
      .then((response) => {
        if (!active) return;
        setResult(response);
        setListError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setListError(
          requestError instanceof ApiError
            ? requestError.message
            : "Moderation reports could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [
    page,
    reason,
    refreshKey,
    reporterUserId,
    sort,
    status,
    targetType,
  ]);

  const openReport = async (reportId: string) => {
    setIsOpening(true);
    setDetailError(null);
    try {
      setSelected(await getAdminReportById(reportId));
    } catch (requestError) {
      setListError(
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

  const review = async () => {
    if (!selected) return;
    setIsActing(true);
    setDetailError(null);
    try {
      const updated = await markReportUnderReview(selected.id);
      setSelected(updated);
      setRefreshKey((value) => value + 1);
      showToast({
        title: "Report under review",
        message: "The review assignment has been recorded.",
        variant: "success",
      });
    } catch (requestError) {
      setDetailError(toActionError(requestError));
    } finally {
      setIsActing(false);
    }
  };

  const completeTransition = async (resolutionNote: string) => {
    if (!selected || !resolutionMode) return;
    setIsActing(true);
    setResolutionError(null);
    try {
      const updated =
        resolutionMode === "resolve"
          ? await resolveReport(selected.id, resolutionNote)
          : await dismissReport(selected.id, resolutionNote);
      setSelected(updated);
      setDetailError(null);
      setResolutionMode(null);
      setRefreshKey((value) => value + 1);
      showToast({
        title:
          updated.status === "RESOLVED"
            ? "Report resolved"
            : "Report dismissed",
        message: "The moderation decision and audit action were recorded.",
        variant: "success",
      });
    } catch (requestError) {
      setResolutionError(toActionError(requestError));
    } finally {
      setIsActing(false);
    }
  };

  const applyReporterFilter = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const normalized = reporterInput.trim();
    if (normalized && !isUuid(normalized)) {
      setFilterError("Use a valid reporter ID.");
      return;
    }
    setFilterError(null);
    setReporterUserId(normalized);
    setPage(0);
    setIsLoading(true);
  };

  const resetFilters = () => {
    setStatus("");
    setReason("");
    setTargetType("");
    setSort("NEWEST");
    setReporterInput("");
    setReporterUserId("");
    setFilterError(null);
    setPage(0);
    setIsLoading(true);
  };

  const changeFilter = () => {
    setPage(0);
    setIsLoading(true);
  };

  return (
    <div className="grid gap-5">
      <div>
        <h2 className="text-xl font-bold text-slate-950">Report queue</h2>
        <p className="mt-1 text-sm text-slate-500">
          Review submitted reports without altering the referenced content.
        </p>
      </div>

      <form
        className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-card lg:grid-cols-4"
        onSubmit={applyReporterFilter}
      >
        <Dropdown
          label="Status"
          onChange={(event) => {
            setStatus(event.target.value as ReportStatus | "");
            changeFilter();
          }}
          options={[
            { label: "All statuses", value: "" },
            ...reportStatusOptions,
          ]}
          value={status}
        />
        <Dropdown
          label="Reason"
          onChange={(event) => {
            setReason(event.target.value as ReportReason | "");
            changeFilter();
          }}
          options={[
            { label: "All reasons", value: "" },
            ...reportReasonOptions,
          ]}
          value={reason}
        />
        <Dropdown
          label="Target type"
          onChange={(event) => {
            setTargetType(event.target.value as ModerationTargetType | "");
            changeFilter();
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
            changeFilter();
          }}
          options={[
            { label: "Newest", value: "NEWEST" },
            { label: "Oldest", value: "OLDEST" },
          ]}
          value={sort}
        />
        <FormField
          className="lg:col-span-3"
          error={filterError ?? undefined}
          label="Reporter ID"
          onChange={(event) => setReporterInput(event.target.value)}
          placeholder="Paste an ID if needed"
          value={reporterInput}
        />
        <div className="flex items-end gap-2">
          <Button className="flex-1" type="submit">
            <Search className="size-4" />
            Apply
          </Button>
          <Button
            aria-label="Reset report filters"
            onClick={resetFilters}
            size="icon"
            variant="outline"
          >
            <RotateCcw className="size-4" />
          </Button>
        </div>
      </form>

      {listError ? <ErrorMessage message={listError} /> : null}
      {isLoading ? (
        <div className="grid min-h-64 place-items-center rounded-2xl border border-slate-200 bg-white">
          <LoadingSpinner label="Loading moderation reports" />
        </div>
      ) : result ? (
        <>
          <ReportList
            emptyDescription="No reports match the selected filters."
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

      <ReportDetailModal
        actionLoading={isActing}
        adminMode
        error={detailError}
        isLoading={isOpening}
        onClose={() => {
          setSelected(null);
          setDetailError(null);
          setIsOpening(false);
        }}
        onDismiss={() => {
          setDetailError(null);
          setResolutionError(null);
          setResolutionMode("dismiss");
        }}
        onResolve={() => {
          setDetailError(null);
          setResolutionError(null);
          setResolutionMode("resolve");
        }}
        onReview={() => void review()}
        report={selected}
      />
      <ResolutionModal
        error={resolutionError}
        isLoading={isActing}
        key={resolutionMode ?? "closed"}
        mode={resolutionMode}
        onClose={() => {
          if (isActing) return;
          setResolutionMode(null);
          setResolutionError(null);
        }}
        onSubmit={completeTransition}
      />
    </div>
  );
}

function toActionError(requestError: unknown) {
  if (requestError instanceof ApiError) {
    return requestError.message;
  }
  return "This action is not allowed for the current report status.";
}
