import { Bell } from "lucide-react";

import { ModulePlaceholderPage } from "@/pages/ModulePlaceholderPage";

export function NotificationsPage() {
  return (
    <ModulePlaceholderPage
      description="Review campus updates, replies, reminders, and account activity in one place."
      icon={Bell}
      title="Notifications"
    />
  );
}
