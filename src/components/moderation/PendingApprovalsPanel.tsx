import { CheckCircle2, Eye, RotateCcw, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  approvePendingContent,
  listPendingApprovals,
  rejectPendingContent,
} from "@/api/moderationApi";
import {
  Button,
  Dropdown,
  ErrorMessage,
  LoadingSpinner,
  useToast,
} from "@/components/common";
import { ModerationPagination } from "@/components/moderation/ModerationPagination";
import {
  formatModerationDate,
  moderationTargetOptions,
  targetTypeLabel,
} from "@/components/moderation/moderationFormatting";
import type {
  ModerationTargetType,
  PendingApprovalItem,
  PendingApprovalPage,
} from "@/types/moderation";

const pageSize = 12;

const approvalTargetOptions = moderationTargetOptions.filter((option) =>
  [
    "NOTE",
    "MARKETPLACE_LISTING",
    "EVENT",
    "DISCUSSION_QUESTION",
    "INTERNSHIP",
    "LOST_FOUND_ITEM",
  ].includes(option.value),
);

export function PendingApprovalsPanel() {
  const { showToast } = useToast();
  const [targetType, setTargetType] =
    useState<ModerationTargetType | "">("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<PendingApprovalPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [actingId, setActingId] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void listPendingApprovals({
      page,
      signal: controller.signal,
      size: pageSize,
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
            : "Pending approvals could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [page, refreshKey, targetType]);

  const approve = async (item: PendingApprovalItem) => {
    setActingId(item.id);
    try {
      await approvePendingContent(item.targetType, item.id);
      showToast({
        title: "Content approved",
        message: `${item.title} is now visible publicly.`,
        variant: "success",
      });
      window.dispatchEvent(new Event("campusone:notifications-refresh"));
      setRefreshKey((value) => value + 1);
    } catch (requestError) {
      setError(toActionError(requestError, "This item could not be approved."));
    } finally {
      setActingId(null);
    }
  };

  const reject = async (item: PendingApprovalItem) => {
    const reason = window.prompt(
      "Add a short rejection reason for the submission owner/admin record.",
    );
    if (reason === null) return;
    if (!reason.trim()) {
      setError("A rejection reason is required.");
      return;
    }
    setActingId(item.id);
    try {
      await rejectPendingContent(item.targetType, item.id, reason.trim());
      showToast({
        title: "Content rejected",
        message: `${item.title} will remain hidden from public pages.`,
        variant: "success",
      });
      window.dispatchEvent(new Event("campusone:notifications-refresh"));
      setRefreshKey((value) => value + 1);
    } catch (requestError) {
      setError(toActionError(requestError, "This item could not be rejected."));
    } finally {
      setActingId(null);
    }
  };

  const resetFilters = () => {
    setTargetType("");
    setPage(0);
    setIsLoading(true);
  };

  return (
    <div className="grid gap-5">
      <div>
        <h2 className="text-xl font-bold text-slate-950">
          Pending approvals
        </h2>
        <p className="mt-1 text-sm text-slate-500">
          Review user-submitted content before it appears on public pages.
        </p>
      </div>

      <div className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-card sm:grid-cols-[1fr_auto]">
        <Dropdown
          label="Content type"
          onChange={(event) => {
            setTargetType(event.target.value as ModerationTargetType | "");
            setPage(0);
            setIsLoading(true);
          }}
          options={[
            { label: "All content types", value: "" },
            ...approvalTargetOptions,
          ]}
          value={targetType}
        />
        <div className="flex items-end">
          <Button
            aria-label="Reset approval filters"
            onClick={resetFilters}
            variant="outline"
          >
            <RotateCcw className="size-4" />
            Reset
          </Button>
        </div>
      </div>

      {error ? <ErrorMessage message={error} /> : null}
      {isLoading ? (
        <div className="grid min-h-64 place-items-center rounded-2xl border border-slate-200 bg-white">
          <LoadingSpinner label="Loading pending approvals" />
        </div>
      ) : result && result.content.length > 0 ? (
        <>
          <div className="grid gap-4">
            {result.content.map((item) => (
              <article
                className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-card md:grid-cols-[96px_1fr] md:items-start"
                key={`${item.targetType}-${item.id}`}
              >
                <div className="grid size-24 place-items-center overflow-hidden rounded-xl bg-slate-100 text-xs font-semibold uppercase text-slate-400">
                  {item.previewUrl ? (
                    <img
                      alt=""
                      className="size-full object-cover"
                      src={item.previewUrl}
                    />
                  ) : (
                    targetTypeLabel(item.targetType).slice(0, 3)
                  )}
                </div>
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-full bg-amber-100 px-2.5 py-1 text-xs font-semibold text-amber-800">
                      Pending review
                    </span>
                    <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
                      {targetTypeLabel(item.targetType)}
                    </span>
                  </div>
                  <h3 className="mt-3 text-lg font-bold text-slate-950">
                    {item.title}
                  </h3>
                  <p className="mt-2 line-clamp-3 text-sm text-slate-600">
                    {item.description}
                  </p>
                  <dl className="mt-3 grid gap-1 text-sm text-slate-500 sm:grid-cols-2">
                    <div>
                      <dt className="font-semibold text-slate-700">
                        Submitted by
                      </dt>
                      <dd>
                        {item.submittedBy.fullName ||
                          `User ${item.submittedBy.userId.slice(0, 8)}`}
                      </dd>
                    </div>
                    <div>
                      <dt className="font-semibold text-slate-700">
                        Submitted
                      </dt>
                      <dd>{formatModerationDate(item.submittedAt)}</dd>
                    </div>
                  </dl>
                  <div className="mt-4 flex flex-wrap gap-2">
                    {item.detailUrl ? (
                      <Link
                        className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:bg-slate-50"
                        to={item.detailUrl}
                      >
                        <Eye className="size-4" />
                        View details
                      </Link>
                    ) : null}
                    <Button
                      loading={actingId === item.id}
                      onClick={() => void approve(item)}
                      variant="primary"
                    >
                      <CheckCircle2 className="size-4" />
                      Approve
                    </Button>
                    <Button
                      disabled={actingId === item.id}
                      onClick={() => void reject(item)}
                      variant="danger"
                    >
                      <XCircle className="size-4" />
                      Reject
                    </Button>
                  </div>
                </div>
              </article>
            ))}
          </div>
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
      ) : (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center">
          <h3 className="text-lg font-bold text-slate-950">
            No pending approvals
          </h3>
          <p className="mt-2 text-sm text-slate-500">
            New user submissions will appear here before they go public.
          </p>
        </div>
      )}
    </div>
  );
}

function toActionError(requestError: unknown, fallback: string) {
  if (requestError instanceof ApiError) {
    return requestError.message;
  }
  return fallback;
}
