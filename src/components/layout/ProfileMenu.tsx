import {
  ChevronDown,
  LogOut,
  Settings,
  UserRound,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";

import { Avatar, useToast } from "@/components/common";
import { paths } from "@/routes/paths";
import { cn } from "@/utils/cn";

export function ProfileMenu({ compact = false }: { compact?: boolean }) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const { showToast } = useToast();

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

  return (
    <div className="relative" ref={containerRef}>
      <button
        aria-expanded={isOpen}
        aria-haspopup="menu"
        aria-label="Open profile menu"
        className={cn(
          "flex items-center rounded-xl transition hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500",
          compact ? "p-1" : "gap-2 p-1 pr-2",
        )}
        onClick={() => setIsOpen((current) => !current)}
        type="button"
      >
        <Avatar name="Ali Khan" size={compact ? "sm" : "md"} />
        {!compact ? (
          <>
            <span className="hidden text-left xl:block">
              <span className="block text-xs font-semibold text-slate-800">
                Ali Khan
              </span>
              <span className="block text-[10px] text-slate-400">
                COMSATS Islamabad
              </span>
            </span>
            <ChevronDown
              className={cn(
                "size-3.5 text-slate-400 transition-transform",
                isOpen && "rotate-180",
              )}
            />
          </>
        ) : null}
      </button>

      {isOpen ? (
        <div
          className="animate-dropdown-in absolute right-0 top-[calc(100%+0.5rem)] z-50 w-64 overflow-hidden rounded-2xl border border-slate-200 bg-white p-2 shadow-2xl shadow-slate-950/10"
          role="menu"
        >
          <div className="border-b border-slate-100 px-3 py-2.5">
            <p className="text-sm font-semibold text-slate-950">Ali Khan</p>
            <p className="mt-0.5 truncate text-xs text-slate-500">
              ali.khan@student.comsats.edu.pk
            </p>
          </div>
          <div className="py-1.5">
            <Link
              className="flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-600 transition hover:bg-slate-50 hover:text-slate-950"
              onClick={() => setIsOpen(false)}
              role="menuitem"
              to={paths.profile}
            >
              <UserRound className="size-4 text-slate-400" />
              View profile
            </Link>
            <Link
              className="flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-600 transition hover:bg-slate-50 hover:text-slate-950"
              onClick={() => setIsOpen(false)}
              role="menuitem"
              to={paths.settings}
            >
              <Settings className="size-4 text-slate-400" />
              Settings
            </Link>
          </div>
          <div className="border-t border-slate-100 pt-1.5">
            <button
              className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-medium text-red-600 transition hover:bg-red-50"
              onClick={() => {
                setIsOpen(false);
                showToast({
                  title: "Demo session",
                  message: "Logout will be connected with authentication.",
                });
              }}
              role="menuitem"
              type="button"
            >
              <LogOut className="size-4" />
              Log out
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
