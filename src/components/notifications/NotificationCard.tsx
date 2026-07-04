import { ArrowUpRight, Mail, MailOpen, Trash2 } from "lucide-react";

import { Badge, Button, Card, CardContent } from "@/components/common";
import {
  formatNotificationDate,
  notificationTypeLabel,
} from "@/components/notifications/notificationFormatting";
import type { NotificationItem } from "@/types/notifications";
import { cn } from "@/utils/cn";

export function NotificationCard({
  busyAction,
  notification,
  onDelete,
  onOpen,
  onToggleRead,
}: {
  busyAction: string | null;
  notification: NotificationItem;
  onDelete: (notification: NotificationItem) => void;
  onOpen: (notification: NotificationItem) => void;
  onToggleRead: (notification: NotificationItem) => void;
}) {
  return (
    <Card
      className={cn(
        !notification.isRead &&
          "border-brand-200 bg-brand-50/30 shadow-brand-100",
      )}
    >
      <CardContent className="flex flex-col gap-4 p-5 sm:flex-row sm:items-start">
        <span
          className={cn(
            "grid size-11 shrink-0 place-items-center rounded-xl",
            notification.isRead
              ? "bg-slate-100 text-slate-500"
              : "bg-brand-100 text-brand-700",
          )}
        >
          {notification.isRead ? (
            <MailOpen className="size-5" />
          ) : (
            <Mail className="size-5" />
          )}
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="font-semibold text-slate-950">
              {notification.title}
            </h2>
            {!notification.isRead ? (
              <span className="size-2 rounded-full bg-brand-500" title="Unread" />
            ) : null}
            <Badge className="sm:ml-auto">
              {notificationTypeLabel(notification.type)}
            </Badge>
          </div>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            {notification.message}
          </p>
          <p className="mt-2 text-xs text-slate-400">
            {formatNotificationDate(notification.createdAt)}
          </p>
          <div className="mt-4 flex flex-wrap gap-2">
            {notification.actionUrl ? (
              <Button
                disabled={Boolean(busyAction)}
                onClick={() => onOpen(notification)}
                size="sm"
              >
                Open
                <ArrowUpRight className="size-3.5" />
              </Button>
            ) : null}
            <Button
              loading={busyAction === `read:${notification.id}`}
              onClick={() => onToggleRead(notification)}
              size="sm"
              variant="outline"
            >
              {notification.isRead ? "Mark unread" : "Mark read"}
            </Button>
            <Button
              disabled={Boolean(busyAction)}
              onClick={() => onDelete(notification)}
              size="sm"
              variant="ghost"
            >
              <Trash2 className="size-3.5 text-red-600" />
              Delete
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

