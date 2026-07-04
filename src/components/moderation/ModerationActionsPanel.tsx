import {
  ClipboardCheck,
  Eye,
  RotateCcw,
  Search,
} from "lucide-react";
import {
  useEffect,
  useState,
  type FormEvent,
} from "react";

import { ApiError } from "@/api/apiClient";
import {
  getModerationActionById,
  listModerationActions,
} from "@/api/moderationApi";
import {
  Badge,
  Button,
  Card,
  CardContent,
  Dropdown,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
} from "@/components/common";
import { FormField } from "@/components/forms";
import { ModerationActionDetailModal } from "@/components/moderation/ModerationActionDetailModal";
import { ModerationPagination } from "@/components/moderation/ModerationPagination";
import {
  actionTypeLabel,
  formatModerationDate,
  isUuid,
  moderationActionOptions,
  moderationTargetOptions,
  shortModerationId,
  targetTypeLabel,
} from "@/components/moderation/moderationFormatting";
import type {
  ModerationAction,
  ModerationActionPage,
  ModerationActionType,
  ModerationSort,
  ModerationTargetType,
} from "@/types/moderation";

const pageSize = 12;

export function ModerationActionsPanel() {
  const [actionType, setActionType] =
    useState<ModerationActionType | "">("");
  const [targetType, setTargetType] =
    useState<ModerationTargetType | "">("");
  const [sort, setSort] = useState<ModerationSort>("NEWEST");
  const [moderatorInput, setModeratorInput] = useState("");
  const [reportInput, setReportInput] = useState("");
  const [moderatorUserId, setModeratorUserId] = useState("");
  const [reportId, setReportId] = useState("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<ModerationActionPage | null>(null);
  const [selected, setSelected] = useState<ModerationAction | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isOpening, setIsOpening] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filterError, setFilterError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void listModerationActions({
      actionType: actionType || undefined,
      moderatorUserId: moderatorUserId || undefined,
      page,
      reportId: reportId || undefined,
      signal: controller.signal,
      size: pageSize,
      sort,
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
            : "Moderation action history could not be loaded.",
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
    actionType,
    moderatorUserId,
    page,
    reportId,
    sort,
    targetType,
  ]);

  const applyIdFilters = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const moderator = moderatorInput.trim();
    const report = reportInput.trim();
    if (moderator && !isUuid(moderator)) {
      setFilterError("Moderator user ID must be a valid UUID.");
      return;
    }
    if (report && !isUuid(report)) {
      setFilterError("Report ID must be a valid UUID.");
      return;
    }
    setFilterError(null);
    setModeratorUserId(moderator);
    setReportId(report);
    setPage(0);
    setIsLoading(true);
  };

  const openAction = async (actionId: string) => {
    setIsOpening(true);
    setError(null);
    try {
      setSelected(await getModerationActionById(actionId));
    } catch (requestError) {
      setError(
        requestError instanceof ApiError && requestError.status === 404
          ? "This moderation action could not be loaded."
          : requestError instanceof ApiError
            ? requestError.message
            : "This moderation action could not be loaded.",
      );
    } finally {
      setIsOpening(false);
    }
  };

  const resetFilters = () => {
    setActionType("");
    setTargetType("");
    setSort("NEWEST");
    setModeratorInput("");
    setReportInput("");
    setModeratorUserId("");
    setReportId("");
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
        <h2 className="text-xl font-bold text-slate-950">
          Moderation actions
        </h2>
        <p className="mt-1 text-sm text-slate-500">
          Read the immutable audit trail created by moderation decisions.
        </p>
      </div>

      <form
        className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-card lg:grid-cols-3"
        onSubmit={applyIdFilters}
      >
        <Dropdown
          label="Action type"
          onChange={(event) => {
            setActionType(event.target.value as ModerationActionType | "");
            changeFilter();
          }}
          options={[
            { label: "All action types", value: "" },
            ...moderationActionOptions,
          ]}
          value={actionType}
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
          error={filterError?.startsWith("Moderator") ? filterError : undefined}
          label="Moderator user UUID"
          onChange={(event) => setModeratorInput(event.target.value)}
          placeholder="Optional exact UUID"
          value={moderatorInput}
        />
        <FormField
          error={filterError?.startsWith("Report") ? filterError : undefined}
          label="Report UUID"
          onChange={(event) => setReportInput(event.target.value)}
          placeholder="Optional exact UUID"
          value={reportInput}
        />
        <div className="flex items-end gap-2">
          <Button className="flex-1" type="submit">
            <Search className="size-4" />
            Apply
          </Button>
          <Button
            aria-label="Reset action filters"
            onClick={resetFilters}
            size="icon"
            variant="outline"
          >
            <RotateCcw className="size-4" />
          </Button>
        </div>
      </form>

      {error ? <ErrorMessage message={error} /> : null}
      {isLoading ? (
        <div className="grid min-h-64 place-items-center rounded-2xl border border-slate-200 bg-white">
          <LoadingSpinner label="Loading moderation actions" />
        </div>
      ) : result?.content.length === 0 ? (
        <EmptyState
          description="No moderation actions match the selected filters."
          icon={<ClipboardCheck className="size-6" />}
          title="No actions found"
        />
      ) : result ? (
        <>
          <div className="grid gap-3">
            {result.content.map((action) => (
              <Card key={action.id}>
                <CardContent className="grid gap-4 p-4 lg:grid-cols-[minmax(0,1.3fr)_minmax(0,1fr)_auto] lg:items-center">
                  <div className="min-w-0">
                    <Badge variant="brand">
                      {actionTypeLabel(action.actionType)}
                    </Badge>
                    <h3 className="mt-3 font-semibold text-slate-950">
                      {targetTypeLabel(action.targetType)}
                    </h3>
                    <p className="mt-1 font-mono text-xs text-slate-400">
                      {shortModerationId(action.targetId)}
                    </p>
                  </div>
                  <div className="text-sm text-slate-500">
                    <p>{action.moderator.fullName}</p>
                    <p className="mt-1 text-xs text-slate-400">
                      {formatModerationDate(action.createdAt)}
                    </p>
                  </div>
                  <Button
                    onClick={() => void openAction(action.id)}
                    variant="outline"
                  >
                    <Eye className="size-4" />
                    View details
                  </Button>
                </CardContent>
              </Card>
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
      ) : null}

      <ModerationActionDetailModal
        action={selected}
        isLoading={isOpening}
        onClose={() => {
          setSelected(null);
          setIsOpening(false);
        }}
      />
    </div>
  );
}
