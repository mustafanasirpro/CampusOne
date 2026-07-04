import { ClipboardCheck, UserRound } from "lucide-react";

import {
  Badge,
  Card,
  CardContent,
  LoadingSpinner,
  Modal,
} from "@/components/common";
import {
  actionTypeLabel,
  formatModerationDate,
  targetTypeLabel,
} from "@/components/moderation/moderationFormatting";
import type { ModerationAction } from "@/types/moderation";

export function ModerationActionDetailModal({
  action,
  isLoading,
  onClose,
}: {
  action: ModerationAction | null;
  isLoading: boolean;
  onClose: () => void;
}) {
  return (
    <Modal
      isOpen={isLoading || Boolean(action)}
      onClose={onClose}
      size="lg"
      title="Moderation action"
    >
      {isLoading ? (
        <div className="grid min-h-64 place-items-center">
          <LoadingSpinner label="Loading moderation action" />
        </div>
      ) : action ? (
        <div className="grid gap-5">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="brand">{actionTypeLabel(action.actionType)}</Badge>
            <span className="font-mono text-xs text-slate-400">
              {action.id}
            </span>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Detail
              label="Target type"
              value={targetTypeLabel(action.targetType)}
            />
            <Detail label="Created" value={formatModerationDate(action.createdAt)} />
            <Detail label="Target ID" mono value={action.targetId} />
            <Detail
              label="Report ID"
              mono
              value={action.reportId ?? "No linked report"}
            />
          </div>
          <Card className="bg-slate-50 shadow-none">
            <CardContent className="p-4">
              <h3 className="flex items-center gap-2 text-sm font-semibold text-slate-800">
                <UserRound className="size-4 text-slate-400" />
                Moderator
              </h3>
              <p className="mt-2 text-sm text-slate-600">
                {action.moderator.fullName}
              </p>
              <p className="mt-1 font-mono text-xs text-slate-400">
                {action.moderator.userId}
              </p>
            </CardContent>
          </Card>
          <div>
            <h3 className="flex items-center gap-2 text-sm font-semibold text-slate-800">
              <ClipboardCheck className="size-4 text-slate-400" />
              Reason
            </h3>
            <p className="mt-2 whitespace-pre-wrap rounded-xl bg-slate-50 p-4 text-sm leading-6 text-slate-600">
              {action.reason || "No additional reason was recorded."}
            </p>
          </div>
          {action.metadata !== null ? (
            <div>
              <h3 className="text-sm font-semibold text-slate-800">Metadata</h3>
              <pre className="mt-2 max-h-72 overflow-auto rounded-xl bg-slate-950 p-4 text-xs leading-6 text-slate-200">
                {JSON.stringify(action.metadata, null, 2)}
              </pre>
            </div>
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
