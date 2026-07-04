import { Dropdown, Switch } from "@/components/common";
import { notificationTypeOptions } from "@/components/notifications/notificationFormatting";
import type {
  NotificationSort,
  NotificationType,
} from "@/types/notifications";

export function NotificationFilterBar({
  onSortChange,
  onTypeChange,
  onUnreadOnlyChange,
  sort,
  type,
  unreadOnly,
}: {
  onSortChange: (value: NotificationSort) => void;
  onTypeChange: (value: NotificationType | "") => void;
  onUnreadOnlyChange: (value: boolean) => void;
  sort: NotificationSort;
  type: NotificationType | "";
  unreadOnly: boolean;
}) {
  return (
    <div className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-card sm:flex-row sm:items-end">
      <Dropdown
        className="min-w-52"
        label="Notification type"
        onChange={(event) =>
          onTypeChange(event.target.value as NotificationType | "")
        }
        options={[
          { label: "All notification types", value: "" },
          ...notificationTypeOptions,
        ]}
        value={type}
      />
      <Dropdown
        className="min-w-40"
        label="Sort"
        onChange={(event) =>
          onSortChange(event.target.value as NotificationSort)
        }
        options={[
          { label: "Newest", value: "NEWEST" },
          { label: "Oldest", value: "OLDEST" },
        ]}
        value={sort}
      />
      <div className="sm:ml-auto sm:pb-1">
        <Switch
          checked={unreadOnly}
          label="Unread only"
          onCheckedChange={onUnreadOnlyChange}
        />
      </div>
    </div>
  );
}
