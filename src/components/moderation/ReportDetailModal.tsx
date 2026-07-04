import {
  CheckCircle2,
  Clock3,
  Flag,
  UserRound,
  XCircle,
} from "lucide-react";

import {
  Button,
  Card,
  CardContent,
  ErrorMessage,
  LoadingSpinner,
  Modal,
} from "@/components/common";
import { ReportStatusBadge } from "@/components/moderation/ReportStatusBadge";
import {
  formatModerationDate,
  reportReasonLabel,
  targetTypeLabel,
} from "@/components/moderation/moderationFormatting";
import type { ContentReportDetail } from "@/types/moderation";

export function ReportDetailModal({
  actionLoading,
  adminMode = false,
  error,
  isLoading,
  onClose,
  onDismiss,
  onResolve,
  onReview,
  report,
}: {
  actionLoading?: boolean;
  adminMode?: boolean;
  error?: string | null;
  isLoading: boolean;
  onClose: () => void;
  onDismiss?: () => void;
  onResolve?: () => void;
  onReview?: () => void;
  report: ContentReportDetail | null;
}) {
  const canAct =
    report?.status === "PENDING" || report?.status === "UNDER_REVIEW";
  const footer =
    adminMode && report && canAct ? (
      <>
        {report.status === "PENDING" ? (
          <Button
            loading={actionLoading}
            onClick={onReview}
            variant="outline"
          >
            <Clock3 className="size-4" />
            Mark under review
          </Button>
        ) : null}
        {canAct ? (
          <>
            <Button
              disabled={actionLoading}
              onClick={onDismiss}
              variant="danger"
            >
              <XCircle className="size-4" />
              Dismiss
            </Button>
            <Button
              disabled={actionLoading}
              onClick={onResolve}
            >
              <CheckCircle2 className="size-4" />
              Resolve
            </Button>
          </>
        ) : null}
      </>
    ) : undefined;

  return (
    <Modal
      footer={footer}
      isOpen={isLoading || Boolean(report)}
      onClose={onClose}
      size="lg"
      title={adminMode ? "Moderation report" : "My report"}
    >
      {isLoading ? (
        <div className="grid min-h-64 place-items-center">
          <LoadingSpinner label="Loading report" />
        </div>
      ) : report ? (
        <div className="grid gap-5">
          {error ? <ErrorMessage message={error} /> : null}
          <div className="flex flex-wrap items-center gap-3">
            <ReportStatusBadge status={report.status} />
            <span className="font-mono text-xs text-slate-400">
              {report.id}
            </span>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Detail label="Reason" value={reportReasonLabel(report.reason)} />
            <Detail
              label="Target type"
              value={targetTypeLabel(report.targetType)}
            />
            <Detail label="Target ID" mono value={report.targetId} />
            <Detail
              label="Submitted"
              value={formatModerationDate(report.createdAt)}
            />
          </div>

          <Card className="bg-slate-50 shadow-none">
            <CardContent className="p-4">
              <div className="flex items-center gap-2 text-sm font-semibold text-slate-800">
                <UserRound className="size-4 text-slate-400" />
                Reporter
              </div>
              <p className="mt-2 text-sm text-slate-600">
                {report.reporter.fullName}
              </p>
              <p className="mt-1 font-mono text-xs text-slate-400">
                {report.reporter.userId}
              </p>
            </CardContent>
          </Card>

          <DetailBlock
            fallback="No additional details were provided."
            label="Report details"
            value={report.details}
          />

          {report.reviewedBy || report.reviewedAt ? (
            <div className="grid gap-4 rounded-2xl border border-slate-200 p-4 sm:grid-cols-2">
              <Detail
                label="Reviewed by"
                value={report.reviewedBy?.fullName ?? "Not assigned"}
              />
              <Detail
                label="Reviewed at"
                value={formatModerationDate(report.reviewedAt)}
              />
            </div>
          ) : null}

          {report.resolutionNote ? (
            <DetailBlock
              label="Resolution note"
              value={report.resolutionNote}
            />
          ) : null}
        </div>
      ) : null}
    </Modal>
  );
}

function Detail({
  label,
  mono = false,
  value,
}: {
  label: string;
  mono?: boolean;
  value: string;
}) {
  return (
    <div className="min-w-0">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
        {label}
      </p>
      <p
        className={`mt-1 break-words text-sm text-slate-700 ${
          mono ? "font-mono text-xs" : ""
        }`}
      >
        {value}
      </p>
    </div>
  );
}

function DetailBlock({
  fallback,
  label,
  value,
}: {
  fallback?: string;
  label: string;
  value: string | null;
}) {
  return (
    <div>
      <h3 className="flex items-center gap-2 text-sm font-semibold text-slate-800">
        <Flag className="size-4 text-slate-400" />
        {label}
      </h3>
      <p className="mt-2 whitespace-pre-wrap rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-600">
        {value || fallback}
      </p>
    </div>
  );
}
