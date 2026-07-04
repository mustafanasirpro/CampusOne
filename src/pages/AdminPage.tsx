import { ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";

import { ApiError } from "@/api/apiClient";
import { getModeratorStatus } from "@/api/moderationApi";
import {
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  Tabs,
} from "@/components/common";
import {
  AdminReportsPanel,
  ModerationActionsPanel,
  ModeratorStatusCard,
  MyReportsPanel,
} from "@/components/moderation";
import type { ModeratorStatus } from "@/types/moderation";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

type AdminTab = "actions" | "my-reports" | "reports";

export function AdminPage() {
  const [status, setStatus] = useState<ModeratorStatus | null>(null);
  const [activeTab, setActiveTab] = useState<AdminTab>("reports");
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
        setActiveTab(response.activeModerator ? "reports" : "my-reports");
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
              <Tabs
                activeTab={activeTab}
                onChange={setActiveTab}
                tabs={[
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
