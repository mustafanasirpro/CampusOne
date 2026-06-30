import { Inbox } from "lucide-react";
import type { ReactNode } from "react";

import { cn } from "@/utils/cn";

export interface EmptyStateProps {
  action?: ReactNode;
  className?: string;
  description: string;
  icon?: ReactNode;
  title: string;
}

export function EmptyState({
  action,
  className,
  description,
  icon = <Inbox className="size-6" />,
  title,
}: EmptyStateProps) {
  return (
    <div
      role="status"
      className={cn(
        "flex min-h-64 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white/60 p-6 text-center sm:p-8",
        className,
      )}
    >
      <div className="mb-4 grid size-12 place-items-center rounded-2xl bg-brand-50 text-brand-600">
        {icon}
      </div>
      <h3 className="text-base font-semibold text-slate-900">{title}</h3>
      <p className="mt-1 max-w-md text-sm leading-6 text-slate-500">
        {description}
      </p>
      {action ? <div className="mt-5">{action}</div> : null}
    </div>
  );
}
