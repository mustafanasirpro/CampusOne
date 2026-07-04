import {
  ArrowLeft,
  ArrowRight,
  Bell,
  CheckCheck,
  RefreshCw,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  deleteNotification,
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  markNotificationUnread,
} from "@/api/notificationsApi";
import {
  Badge,
  Button,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  useToast,
} from "@/components/common";
import {
  NotificationCard,
  NotificationFilterBar,
} from "@/components/notifications";
import type {
  NotificationItem,
  NotificationPage,
  NotificationSort,
  NotificationType,
} from "@/types/notifications";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

const pageSize = 12;

export function NotificationsPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [page, setPage] = useState(0);
  const [type, setType] = useState<NotificationType | "">("");
  const [sort, setSort] = useState<NotificationSort>("NEWEST");
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [result, setResult] = useState<NotificationPage | null>(null);
  const [unreadCount, setUnreadCount] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  useDocumentTitle("Notifications · CampusOne");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void Promise.all([
      listNotifications({
        page,
        signal: controller.signal,
        size: pageSize,
        sort,
        type: type || undefined,
        unreadOnly: unreadOnly || undefined,
      }),
      getUnreadCount(controller.signal),
    ])
      .then(([notifications, count]) => {
        if (!active) return;
        setResult(notifications);
        setUnreadCount(count.unreadCount);
        setError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setResult(null);
        setError(
          requestError instanceof ApiError
            ? requestError.message
            : "Notifications could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [page, refreshKey, sort, type, unreadOnly]);

  const refresh = () => {
    setIsLoading(true);
    setError(null);
    setRefreshKey((value) => value + 1);
  };

  const updateNotification = (updated: NotificationItem) => {
    setResult((current) => {
      if (!current) return current;
      if (unreadOnly && updated.isRead) {
        return {
          ...current,
          content: current.content.filter((item) => item.id !== updated.id),
          totalElements: Math.max(0, current.totalElements - 1),
        };
      }
      return {
        ...current,
        content: current.content.map((item) =>
          item.id === updated.id ? updated : item,
        ),
      };
    });
  };

  const toggleRead = async (notification: NotificationItem) => {
    setBusyAction(`read:${notification.id}`);
    setActionError(null);
    try {
      const updated = notification.isRead
        ? await markNotificationUnread(notification.id)
        : await markNotificationRead(notification.id);
      updateNotification(updated);
      setUnreadCount((value) =>
        Math.max(0, value + (updated.isRead ? -1 : 1)),
      );
    } catch (requestError) {
      setActionError(
        requestError instanceof ApiError
          ? requestError.message
          : "The notification state could not be updated.",
      );
    } finally {
      setBusyAction(null);
    }
  };

  const openNotification = async (notification: NotificationItem) => {
    if (!notification.actionUrl) return;
    if (!notification.isRead) {
      setBusyAction(`read:${notification.id}`);
      try {
        const updated = await markNotificationRead(notification.id);
        updateNotification(updated);
        setUnreadCount((value) => Math.max(0, value - 1));
      } catch {
        // Opening the destination remains useful if marking read fails.
      } finally {
        setBusyAction(null);
      }
    }
    if (/^https?:\/\//i.test(notification.actionUrl)) {
      window.open(notification.actionUrl, "_blank", "noopener,noreferrer");
    } else {
      navigate(
        notification.actionUrl.startsWith("/")
          ? notification.actionUrl
          : `/${notification.actionUrl}`,
      );
    }
  };

  const remove = async (notification: NotificationItem) => {
    if (!window.confirm(`Delete "${notification.title}"?`)) return;
    setBusyAction(`delete:${notification.id}`);
    setActionError(null);
    try {
      await deleteNotification(notification.id);
      setResult((current) =>
        current
          ? {
              ...current,
              content: current.content.filter(
                (item) => item.id !== notification.id,
              ),
              totalElements: Math.max(0, current.totalElements - 1),
            }
          : current,
      );
      if (!notification.isRead) {
        setUnreadCount((value) => Math.max(0, value - 1));
      }
      showToast({
        title: "Notification deleted",
        message: notification.title,
        variant: "success",
      });
    } catch (requestError) {
      setActionError(
        requestError instanceof ApiError
          ? requestError.message
          : "The notification could not be deleted.",
      );
    } finally {
      setBusyAction(null);
    }
  };

  const markAllRead = async () => {
    setBusyAction("read-all");
    setActionError(null);
    try {
      const response = await markAllNotificationsRead();
      setUnreadCount(0);
      setIsLoading(true);
      setRefreshKey((value) => value + 1);
      showToast({
        title: "Notifications updated",
        message: `${response.updatedCount} notifications marked as read.`,
        variant: "success",
      });
    } catch (requestError) {
      setActionError(
        requestError instanceof ApiError
          ? requestError.message
          : "Notifications could not be marked as read.",
      );
    } finally {
      setBusyAction(null);
    }
  };

  return (
    <div className="grid gap-6 pb-8">
      <PageHeader
        actions={
          <Button
            disabled={unreadCount === 0}
            loading={busyAction === "read-all"}
            onClick={() => void markAllRead()}
            variant="outline"
          >
            <CheckCheck className="size-4" />
            Mark all read
          </Button>
        }
        description="Keep up with replies, reminders, event changes, and CampusOne activity."
        eyebrow="Activity inbox"
        title="Notifications"
      />

      <div className="flex items-center gap-3">
        <Badge variant={unreadCount > 0 ? "brand" : "neutral"}>
          {unreadCount.toLocaleString()} unread
        </Badge>
        <span className="text-sm text-slate-500">
          Your notification inbox is private to your account.
        </span>
      </div>

      <NotificationFilterBar
        onSortChange={(value) => {
          setSort(value);
          setPage(0);
          setIsLoading(true);
        }}
        onTypeChange={(value) => {
          setType(value);
          setPage(0);
          setIsLoading(true);
        }}
        onUnreadOnlyChange={(value) => {
          setUnreadOnly(value);
          setPage(0);
          setIsLoading(true);
        }}
        sort={sort}
        type={type}
        unreadOnly={unreadOnly}
      />

      {actionError ? <ErrorMessage message={actionError} /> : null}
      {error ? (
        <div className="grid gap-3">
          <ErrorMessage message={error} />
          <Button className="w-fit" onClick={refresh} variant="outline">
            <RefreshCw className="size-4" />
            Try again
          </Button>
        </div>
      ) : null}
      {isLoading ? (
        <div className="grid min-h-72 place-items-center rounded-2xl border border-slate-200 bg-white">
          <LoadingSpinner label="Loading notifications" />
        </div>
      ) : null}
      {!isLoading && !error && result?.content.length === 0 ? (
        <EmptyState
          icon={<Bell className="size-6" />}
          description={
            unreadOnly
              ? "You have no unread notifications matching this filter."
              : "New CampusOne activity will appear here."
          }
          title="All caught up"
        />
      ) : null}
      {!isLoading && !error && result && result.content.length > 0 ? (
        <>
          <div className="grid gap-3">
            {result.content.map((notification) => (
              <NotificationCard
                busyAction={busyAction}
                key={notification.id}
                notification={notification}
                onDelete={(item) => void remove(item)}
                onOpen={(item) => void openNotification(item)}
                onToggleRead={(item) => void toggleRead(item)}
              />
            ))}
          </div>
          <nav
            aria-label="Notifications pagination"
            className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white p-3"
          >
            <Button
              disabled={result.first}
              onClick={() => {
                setPage((value) => Math.max(0, value - 1));
                setIsLoading(true);
              }}
              variant="outline"
            >
              <ArrowLeft className="size-4" />
              Previous
            </Button>
            <span className="text-sm font-semibold">
              {result.page + 1} / {Math.max(1, result.totalPages)}
            </span>
            <Button
              disabled={result.last}
              onClick={() => {
                setPage((value) => value + 1);
                setIsLoading(true);
              }}
              variant="outline"
            >
              Next
              <ArrowRight className="size-4" />
            </Button>
          </nav>
        </>
      ) : null}
    </div>
  );
}
