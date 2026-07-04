import { Badge } from "@/components/common";
import type { ReportStatus } from "@/types/moderation";

const statusDetails = {
  DISMISSED: { label: "Dismissed", variant: "neutral" },
  PENDING: { label: "Pending", variant: "warning" },
  RESOLVED: { label: "Resolved", variant: "success" },
  UNDER_REVIEW: { label: "Under review", variant: "brand" },
} as const;

export function ReportStatusBadge({ status }: { status: ReportStatus }) {
  const detail = statusDetails[status];
  return <Badge variant={detail.variant}>{detail.label}</Badge>;
}
