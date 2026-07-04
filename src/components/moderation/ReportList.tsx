import { Eye, Flag } from "lucide-react";

import {
  Button,
  Card,
  CardContent,
  EmptyState,
} from "@/components/common";
import { ReportStatusBadge } from "@/components/moderation/ReportStatusBadge";
import {
  formatModerationDate,
  reportReasonLabel,
  shortModerationId,
  targetTypeLabel,
} from "@/components/moderation/moderationFormatting";
import type { ContentReportSummary } from "@/types/moderation";

export function ReportList({
  emptyDescription,
  onOpen,
  reports,
}: {
  emptyDescription: string;
  onOpen: (reportId: string) => void;
  reports: ContentReportSummary[];
}) {
  if (reports.length === 0) {
    return (
      <EmptyState
        description={emptyDescription}
        icon={<Flag className="size-6" />}
        title="No reports found"
      />
    );
  }

  return (
    <div className="grid gap-3">
      {reports.map((report) => (
        <Card key={report.id}>
          <CardContent className="grid gap-4 p-4 lg:grid-cols-[minmax(0,1.35fr)_minmax(0,1fr)_auto] lg:items-center">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <ReportStatusBadge status={report.status} />
                <span className="text-xs font-medium text-slate-400">
                  Report {shortModerationId(report.id)}
                </span>
              </div>
              <h3 className="mt-3 font-semibold text-slate-950">
                {reportReasonLabel(report.reason)}
              </h3>
              <p className="mt-1 text-sm text-slate-500">
                {targetTypeLabel(report.targetType)} ·{" "}
                <span className="font-mono text-xs">
                  {shortModerationId(report.targetId)}
                </span>
              </p>
            </div>
            <div className="text-sm text-slate-500">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                Submitted
              </p>
              <p className="mt-1">{formatModerationDate(report.createdAt)}</p>
            </div>
            <Button onClick={() => onOpen(report.id)} variant="outline">
              <Eye className="size-4" />
              View details
            </Button>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
