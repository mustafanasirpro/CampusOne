import {
  ShieldCheck,
  ShieldX,
} from "lucide-react";

import {
  Badge,
  Card,
  CardContent,
} from "@/components/common";
import { formatModerationDate } from "@/components/moderation/moderationFormatting";
import type { ModeratorStatus } from "@/types/moderation";

export function ModeratorStatusCard({
  status,
}: {
  status: ModeratorStatus;
}) {
  if (!status.activeModerator) {
    return (
      <Card className="border-amber-200 bg-amber-50/60">
        <CardContent className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
          <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-amber-100 text-amber-700">
            <ShieldX className="size-6" />
          </span>
          <div>
            <h2 className="font-semibold text-amber-950">
              You do not have moderator access.
            </h2>
            <p className="mt-1 text-sm leading-6 text-amber-800">
              Admin report queues and audit history are hidden. You can still
              submit content reports and track your own reports below.
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="overflow-hidden border-0 bg-gradient-to-r from-slate-950 via-slate-900 to-brand-950 text-white shadow-xl">
      <CardContent className="flex flex-col gap-5 p-6 sm:flex-row sm:items-center">
        <span className="grid size-14 shrink-0 place-items-center rounded-2xl bg-white/10 text-brand-200 ring-1 ring-white/10">
          <ShieldCheck className="size-7" />
        </span>
        <div className="flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-lg font-semibold">Moderator access verified</h2>
            <Badge variant="success">{status.role}</Badge>
          </div>
          <p className="mt-2 text-sm leading-6 text-slate-300">
            Review reports and audit moderation decisions. Content references
            remain unchanged by this moderation phase.
          </p>
          {status.assignedAt ? (
            <p className="mt-2 text-xs text-slate-400">
              Assigned {formatModerationDate(status.assignedAt)}
            </p>
          ) : null}
        </div>
      </CardContent>
    </Card>
  );
}
