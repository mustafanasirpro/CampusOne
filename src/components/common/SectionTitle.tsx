import type { ReactNode } from "react";

import { cn } from "@/utils/cn";

export interface SectionTitleProps {
  action?: ReactNode;
  className?: string;
  description?: string;
  title: string;
}

export function SectionTitle({
  action,
  className,
  description,
  title,
}: SectionTitleProps) {
  return (
    <div
      className={cn(
        "flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between",
        className,
      )}
    >
      <div>
        <h2 className="text-lg font-semibold text-slate-950">{title}</h2>
        {description ? (
          <p className="mt-1 text-sm text-slate-500">{description}</p>
        ) : null}
      </div>
      {action}
    </div>
  );
}

