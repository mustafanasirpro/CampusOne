import {
  Bell,
  BriefcaseBusiness,
  CalendarDays,
  CheckCheck,
  FileText,
  MessageSquareText,
  X,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";

import { Button, useToast } from "@/components/common";
import { dashboardNotifications } from "@/data/dashboard";
import { cn } from "@/utils/cn";

const notificationIcons = {
  note: FileText,
  discussion: MessageSquareText,
  event: CalendarDays,
  internship: BriefcaseBusiness,
};

const notificationTones = {
  note: "bg-brand-50 text-brand-600",
  discussion: "bg-emerald-50 text-emerald-600",
  event: "bg-amber-50 text-amber-600",
  internship: "bg-sky-50 text-sky-600",
};

export function NotificationMenu() {
  const [isOpen, setIsOpen] = useState(false);
  const [readIds, setReadIds] = useState(
    () =>
      new Set(
        dashboardNotifications
          .filter((notification) => notification.read)
          .map((notification) => notification.id),
      ),
  );
  const containerRef = useRef<HTMLDivElement>(null);
  const { showToast } = useToast();
  const unreadCount = useMemo(
    () =>
      dashboardNotifications.filter(
        (notification) => !readIds.has(notification.id),
      ).length,
    [readIds],
  );

  useEffect(() => {
    if (!isOpen) return;

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
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  const markAllRead = () => {
    setReadIds(
      new Set(
        dashboardNotifications.map((notification) => notification.id),
      ),
    );
  };

  return (
    <div className="relative" ref={containerRef}>
      <Button
        aria-expanded={isOpen}
        aria-haspopup="menu"
        aria-label={`Notifications, ${unreadCount} unread`}
        className="relative"
        onClick={() => setIsOpen((current) => !current)}
        size="icon"
        variant="ghost"
      >
        <Bell className="size-5" />
        {unreadCount > 0 ? (
          <span className="absolute right-1.5 top-1.5 grid min-w-4 place-items-center rounded-full bg-red-500 px-1 text-[9px] font-bold leading-4 text-white ring-2 ring-white">
            {unreadCount}
          </span>
        ) : null}
      </Button>

      {isOpen ? (
        <div
          aria-label="Notifications"
          className="fixed left-4 right-4 top-16 z-50 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl shadow-slate-950/10 sm:absolute sm:left-auto sm:right-0 sm:top-12 sm:w-96"
          role="menu"
        >
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3.5">
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
                  className="inline-flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-xs font-semibold text-brand-700 transition hover:bg-brand-50"
                  onClick={markAllRead}
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

          <div className="max-h-[min(28rem,calc(100vh-9rem))] overflow-y-auto p-2">
            {dashboardNotifications.map((notification) => {
              const Icon = notificationIcons[notification.kind];
              const isRead = readIds.has(notification.id);

              return (
                <button
                  className={cn(
                    "flex w-full gap-3 rounded-xl p-3 text-left transition hover:bg-slate-50",
                    !isRead && "bg-brand-50/50",
                  )}
                  key={notification.id}
                  onClick={() => {
                    setReadIds(
                      (current) => new Set([...current, notification.id]),
                    );
                    showToast({
                      title: notification.title,
                      message: notification.message,
                    });
                  }}
                  role="menuitem"
                  type="button"
                >
                  <span
                    className={cn(
                      "grid size-10 shrink-0 place-items-center rounded-xl",
                      notificationTones[notification.kind],
                    )}
                  >
                    <Icon className="size-4.5" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="flex items-start gap-2">
                      <span
                        className={cn(
                          "flex-1 text-sm text-slate-800",
                          !isRead && "font-semibold text-slate-950",
                        )}
                      >
                        {notification.title}
                      </span>
                      {!isRead ? (
                        <span className="mt-1.5 size-2 shrink-0 rounded-full bg-brand-500" />
                      ) : null}
                    </span>
                    <span className="mt-0.5 block text-xs leading-5 text-slate-500">
                      {notification.message}
                    </span>
                    <span className="mt-1 block text-[11px] font-medium text-slate-400">
                      {notification.time}
                    </span>
                  </span>
                </button>
              );
            })}
          </div>

          <div className="border-t border-slate-100 p-2">
            <button
              className="w-full rounded-xl px-3 py-2 text-sm font-semibold text-brand-700 transition hover:bg-brand-50"
              onClick={() =>
                showToast({
                  title: "Notifications",
                  message: "You are viewing all current demo notifications.",
                })
              }
              type="button"
            >
              View all notifications
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}

