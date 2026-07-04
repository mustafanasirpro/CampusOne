import { ShieldCheck } from "lucide-react";

import { ModulePlaceholderPage } from "@/pages/ModulePlaceholderPage";

export function AdminPage() {
  return (
    <ModulePlaceholderPage
      description="Moderation queues, reports, and administrative tools will live in this protected workspace."
      icon={ShieldCheck}
      title="Admin & Moderation"
    />
  );
}
