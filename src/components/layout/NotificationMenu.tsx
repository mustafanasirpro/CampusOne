import {
  Bell,
  CheckCheck,
  Mail,
  MailOpen,
  RefreshCw,
  X,
} from "lucide-react";
import {
  useEffect,
  useRef,
  useState,
} from "react";
import { Link, useNavigate } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from "@/api/notificationsApi";
import {
  Button,
  LoadingSpinner,
  useToast,
} from "@/components/common";
import {
  formatNotificationDate,
  notificationTypeLabel,
} from "@/components/notifications/notificationFormatting";
import { paths } from "@/routes/paths";
import type { NotificationItem } from "@/types/notifications";
import { cn } from "@/utils/cn";

const previewSize = 6;

export function NotificationMenu() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void getUnreadCount(controller.signal)
      .then((response) => {
        if (active) setUnreadCount(response.unreadCount);
      })
      .catch((requestError: unknown) => {
        if (
          active &&
          !(requestError instanceof DOMException &&
            requestError.name === "AbortError")
        ) {
          setError(
            requestError instanceof ApiError
              ? requestError.message
              : "The unread notification count could not be loaded.",
          );
        }
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  useEffect(() => {
    let active = true;
    const refreshUnreadCount = () => {
      void getUnreadCount()
        .then((response) => {
          if (active) setUnreadCount(response.unreadCount);
        })
        .catch(() => {
          if (active) {
            setError("The unread notification count could not be refreshed.");
          }
        });
    };
    window.addEventListener(
      "campusone:notifications-refresh",
      refreshUnreadCount,
    );
    window.addEventListener("focus", refreshUnreadCount);
    const intervalId = window.setInterval(refreshUnreadCount, 60_000);
    return () => {
      active = false;
      window.clearInterval(intervalId);
      window.removeEventListener(
        "campusone:notifications-refresh",
        refreshUnreadCount,
      );
      window.removeEventListener("focus", refreshUnreadCount);
    };
  }, []);

  useEffect(() => {
    if (!isOpen) return;

    const controller = new AbortController();
    let active = true;
    void Promise.all([
      listNotifications({
        page: 0,
        signal: controller.signal,
        size: previewSize,
        sort: "NEWEST",
      }),
      getUnreadCount(controller.signal),
    ])
      .then(([page, count]) => {
        if (!active) return;
        setNotifications(page.content);
        setUnreadCount(count.unreadCount);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setError(
          requestError instanceof ApiError
            ? requestError.message
            : "Notifications could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    const handlePointerDown = (event: PointerEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setIsOpen(false);
    };
    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      active = false;
      controller.abort();
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen, refreshKey]);

  const openNotification = async (notification: NotificationItem) => {
    if (!notification.isRead) {
      setIsUpdating(true);
      try {
        const updated = await markNotificationRead(notification.id);
        setNotifications((current) =>
          current.map((item) => (item.id === updated.id ? updated : item)),
        );
        setUnreadCount((current) => Math.max(0, current - 1));
      } catch {
        showToast({
          title: "Notification opened",
          message: "Its read state could not be updated. Please try again.",
          variant: "error",
        });
      } finally {
        setIsUpdating(false);
      }
    }

    setIsOpen(false);
    const destination = notification.actionUrl;
    if (destination?.startsWith("/") && !destination.startsWith("//")) {
      navigate(destination);
    } else {
      navigate(paths.notifications);
    }
  };

  const markAllRead = async () => {
    setIsUpdating(true);
    setError(null);
    try {
      const response = await markAllNotificationsRead();
      setNotifications((current) =>
        current.map((notification) => ({
          ...notification,
          isRead: true,
        })),
      );
      setUnreadCount(0);
      showToast({
        title: "All caught up",
        message: `${response.updatedCount} notifications marked as read.`,
        variant: "success",
      });
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "Notifications could not be marked as read.",
      );
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <div className="relative" ref={containerRef}>
      <Button
        aria-expanded={isOpen}
        aria-haspopup="dialog"
        aria-label={`Notifications, ${unreadCount} unread`}
        className="relative"
        onClick={() => {
          if (!isOpen) {
            setIsLoading(true);
            setError(null);
          }
          setIsOpen((current) => !current);
        }}
        size="icon"
        variant="ghost"
      >
        <Bell className="size-5" />
        {unreadCount > 0 ? (
          <span className="absolute right-1 top-1 grid min-w-4 place-items-center rounded-full bg-red-500 px-1 text-[9px] font-bold leading-4 text-white ring-2 ring-white">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        ) : null}
      </Button>

      {isOpen ? (
        <div
          aria-label="Notifications"
          className="animate-dropdown-in fixed left-3 right-3 top-16 z-50 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl shadow-slate-950/10 sm:absolute sm:left-auto sm:right-0 sm:top-12 sm:w-[26rem]"
          role="dialog"
        >
          <div className="flex items-center justify-between gap-3 border-b border-slate-100 px-4 py-3.5">
            <div>
              <h2 className="font-semibold text-slate-950">Notifications</h2>
              <p className="text-xs text-slate-500">
                {unreadCount > 0
                  ? `${unreadCount} unread updates`
                  : "You are all caught up"}
              </p>
            </div>
            <div className="flex items-center gap-1">
              {unreadCount > 0 ? (
                <button
                  className="inline-flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-xs font-semibold text-brand-700 transition hover:bg-brand-50 disabled:cursor-not-allowed disabled:opacity-50"
                  disabled={isUpdating}
                  onClick={() => void markAllRead()}
                  type="button"
                >
                  <CheckCheck className="size-3.5" />
                  Mark all read
                </button>
              ) : null}
              <button
                aria-label="Close notifications"
                className="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 sm:hidden"
                onClick={() => setIsOpen(false)}
                type="button"
              >
                <X className="size-4" />
              </button>
            </div>
          </div>

          <div className="max-h-[min(30rem,calc(100vh-9rem))] overflow-y-auto p-2">
            {isLoading ? (
              <div className="grid min-h-44 place-items-center">
                <LoadingSpinner label="Loading notifications" />
              </div>
            ) : error ? (
              <div className="p-5 text-center">
                <p className="text-sm font-semibold text-red-700">
                  Something went wrong
                </p>
                <p className="mt-1 text-xs leading-5 text-red-600">{error}</p>
                <Button
                  className="mt-3"
                  onClick={() => {
                    setIsLoading(true);
                    setError(null);
                    setRefreshKey((value) => value + 1);
                  }}
                  size="sm"
                  variant="outline"
                >
                  <RefreshCw className="size-3.5" />
                  Try again
                </Button>
              </div>
            ) : notifications.length === 0 ? (
              <div className="grid min-h-44 place-items-center px-6 py-8 text-center">
                <div>
                  <span className="mx-auto grid size-11 place-items-center rounded-2xl bg-slate-100 text-slate-400">
                    <Bell className="size-5" />
                  </span>
                  <p className="mt-3 text-sm font-semibold text-slate-800">
                    No notifications yet
                  </p>
                  <p className="mt-1 text-xs leading-5 text-slate-500">
                    New CampusOne activity will appear here.
                  </p>
                </div>
              </div>
            ) : (
              notifications.map((notification) => (
                <button
                  className={cn(
                    "flex w-full gap-3 rounded-xl p-3 text-left transition hover:bg-slate-50 disabled:cursor-wait",
                    !notification.isRead && "bg-brand-50/50",
                  )}
                  disabled={isUpdating}
                  key={notification.id}
                  onClick={() => void openNotification(notification)}
                  type="button"
                >
                  <span
                    className={cn(
                      "grid size-10 shrink-0 place-items-center rounded-xl",
                      notification.isRead
                        ? "bg-slate-100 text-slate-500"
                        : "bg-brand-100 text-brand-700",
                    )}
                  >
                    {notification.isRead ? (
                      <MailOpen className="size-4.5" />
                    ) : (
                      <Mail className="size-4.5" />
                    )}
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="flex items-start gap-2">
                      <span
                        className={cn(
                          "flex-1 text-sm text-slate-800",
                          !notification.isRead &&
                            "font-semibold text-slate-950",
                        )}
                      >
                        {notification.title}
                      </span>
                      {!notification.isRead ? (
                        <span
                          aria-label="Unread"
                          className="mt-1.5 size-2 shrink-0 rounded-full bg-brand-500"
                        />
                      ) : null}
                    </span>
                    <span className="mt-0.5 line-clamp-2 block text-xs leading-5 text-slate-500">
                      {notification.message}
                    </span>
                    <span className="mt-1 flex flex-wrap gap-x-2 text-[11px] font-medium text-slate-400">
                      <span>{notificationTypeLabel(notification.type)}</span>
                      <span aria-hidden="true">·</span>
                      <span>
                        {formatNotificationDate(notification.createdAt)}
                      </span>
                    </span>
                  </span>
                </button>
              ))
            )}
          </div>

          <div className="border-t border-slate-100 p-2">
            <Link
              className="flex h-10 items-center justify-center rounded-xl text-sm font-semibold text-brand-700 transition hover:bg-brand-50"
              onClick={() => setIsOpen(false)}
              to={paths.notifications}
            >
              View all notifications
            </Link>
          </div>
        </div>
      ) : null}
    </div>
  );
}
