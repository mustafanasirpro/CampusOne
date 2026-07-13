import { ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";

import { ApiError } from "@/api/apiClient";
import { getLostFoundStats } from "@/api/lostFoundApi";
import { getModeratorStatus } from "@/api/moderationApi";
import {
  Card,
  CardContent,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  Tabs,
} from "@/components/common";
import { lostFoundStatusLabel } from "@/components/lost-found";
import {
  AdminReportsPanel,
  ModerationActionsPanel,
  ModeratorStatusCard,
  MyReportsPanel,
  PendingApprovalsPanel,
} from "@/components/moderation";
import type { LostFoundStats } from "@/types/lostFound";
import type { ModeratorStatus } from "@/types/moderation";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

type AdminTab = "actions" | "approvals" | "my-reports" | "reports";

export function AdminPage() {
  const [status, setStatus] = useState<ModeratorStatus | null>(null);
  const [activeTab, setActiveTab] = useState<AdminTab>("approvals");
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useDocumentTitle("Admin & Moderation · CampusOne");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void getModeratorStatus(controller.signal)
      .then((response) => {
        if (!active) return;
        setStatus(response);
        setActiveTab(response.activeModerator ? "approvals" : "my-reports");
        setError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setError(
          requestError instanceof ApiError
            ? requestError.message
            : "Your moderation access status could not be checked.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  return (
    <div className="grid gap-6 pb-8">
      <PageHeader
        description="Submit reports, review moderation queues, and inspect the decision audit trail."
        eyebrow="Trust and safety"
        title="Admin & Moderation"
      />

      {isLoading ? (
        <div className="grid min-h-72 place-items-center rounded-2xl border border-slate-200 bg-white">
          <LoadingSpinner label="Checking moderation access" />
        </div>
      ) : error ? (
        <ErrorMessage message={error} />
      ) : status ? (
        <>
          <ModeratorStatusCard status={status} />
          {status.activeModerator ? (
            <>
              <LostFoundStatsStrip />
              <Tabs
                activeTab={activeTab}
                onChange={setActiveTab}
                tabs={[
                  {
                    label: "Approvals",
                    value: "approvals",
                  },
                  {
                    label: "Reports",
                    value: "reports",
                  },
                  {
                    label: "Actions",
                    value: "actions",
                  },
                  {
                    label: "My Reports",
                    value: "my-reports",
                  },
                ]}
              />
              <section aria-live="polite">
                {activeTab === "approvals" ? <PendingApprovalsPanel /> : null}
                {activeTab === "reports" ? <AdminReportsPanel /> : null}
                {activeTab === "actions" ? <ModerationActionsPanel /> : null}
                {activeTab === "my-reports" ? <MyReportsPanel /> : null}
              </section>
            </>
          ) : (
            <MyReportsPanel />
          )}
        </>
      ) : (
        <div className="grid min-h-72 place-items-center text-slate-500">
          <ShieldCheck className="size-8" />
        </div>
      )}
    </div>
  );
}

function LostFoundStatsStrip() {
  const [stats, setStats] = useState<LostFoundStats | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void getLostFoundStats(controller.signal)
      .then((response) => {
        if (!active) return;
        setStats(response);
        setError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setError(
          requestError instanceof ApiError
            ? requestError.message
            : "Lost & Found stats could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  const highlightedStatuses = [
    "PENDING_REVIEW",
    "PUBLISHED",
    "CLAIM_IN_PROGRESS",
    "RESOLVED",
    "ARCHIVED",
  ];

  return (
    <section aria-label="Lost & Found moderation statistics">
      <div className="mb-3">
        <h2 className="text-lg font-bold text-slate-950">
          Lost & Found overview
        </h2>
        <p className="mt-1 text-sm text-slate-500">
          Quick moderation health for active campus reports.
        </p>
      </div>
      {isLoading ? (
        <div className="grid min-h-32 place-items-center rounded-2xl border border-slate-200 bg-white">
          <LoadingSpinner label="Loading Lost & Found stats" />
        </div>
      ) : error ? (
        <ErrorMessage message={error} />
      ) : stats ? (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
          {highlightedStatuses.map((status) => (
            <Card key={status}>
              <CardContent className="p-4">
                <p className="text-sm font-medium text-slate-500">
                  {lostFoundStatusLabel(
                    status as Parameters<typeof lostFoundStatusLabel>[0],
                  )}
                </p>
                <p className="mt-2 text-2xl font-bold text-slate-950">
                  {stats.statusCounts[status] ?? 0}
                </p>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : null}
    </section>
  );
}
