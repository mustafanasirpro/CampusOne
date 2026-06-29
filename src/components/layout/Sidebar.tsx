import { LogOut } from "lucide-react";
import { NavLink } from "react-router-dom";

import { Button, useToast } from "@/components/common";
import { CampusOneLogo } from "@/components/layout/CampusOneLogo";
import {
  primaryNavigation,
  secondaryNavigation,
} from "@/data/navigation";
import { paths } from "@/routes/paths";
import { cn } from "@/utils/cn";

export interface SidebarProps {
  className?: string;
  onNavigate?: () => void;
}

export function Sidebar({ className, onNavigate }: SidebarProps) {
  const { showToast } = useToast();

  const handleLogout = () => {
    onNavigate?.();
    showToast({
      message: "Authentication will be connected in a later phase.",
      title: "Demo session",
    });
  };

  return (
    <aside
      className={cn(
        "flex h-full w-72 flex-col border-r border-slate-200 bg-white",
        className,
      )}
    >
      <div className="flex h-20 shrink-0 items-center px-6">
        <CampusOneLogo to={paths.dashboard} />
      </div>

      <nav
        aria-label="Main navigation"
        className="flex-1 overflow-y-auto px-4 py-3"
      >
        <p className="mb-2 px-3 text-[11px] font-bold uppercase tracking-[0.16em] text-slate-400">
          Campus
        </p>
        <div className="grid gap-1">
          {primaryNavigation.map((item) => (
            <NavLink
              className={({ isActive }) =>
                cn(
                  "flex h-11 items-center gap-3 rounded-xl px-3 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-brand-50 text-brand-700"
                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-950",
                )
              }
              key={item.path}
              onClick={onNavigate}
              to={item.path}
            >
              {({ isActive }) => (
                <>
                  <item.icon
                    className={cn(
                      "size-[19px]",
                      isActive ? "text-brand-600" : "text-slate-400",
                    )}
                  />
                  <span>{item.label}</span>
                </>
              )}
            </NavLink>
          ))}
        </div>

        <div className="my-4 border-t border-slate-100" />

        <div className="grid gap-1">
          {secondaryNavigation.map((item) => (
            <NavLink
              className={({ isActive }) =>
                cn(
                  "flex h-11 items-center gap-3 rounded-xl px-3 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-brand-50 text-brand-700"
                    : "text-slate-600 hover:bg-slate-50 hover:text-slate-950",
                )
              }
              key={item.path}
              onClick={onNavigate}
              to={item.path}
            >
              <item.icon className="size-[19px]" />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </div>
      </nav>

      <div className="border-t border-slate-100 p-4">
        <Button
          className="w-full justify-start"
          onClick={handleLogout}
          variant="ghost"
        >
          <LogOut className="size-[19px]" />
          Log out
        </Button>
      </div>
    </aside>
  );
}
