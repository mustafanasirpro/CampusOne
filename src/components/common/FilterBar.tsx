import { SlidersHorizontal } from "lucide-react";
import type { ReactNode } from "react";

import { Button } from "@/components/common/Button";
import { cn } from "@/utils/cn";

export interface FilterBarProps {
  children: ReactNode;
  className?: string;
  onClear?: () => void;
  showClear?: boolean;
}

export function FilterBar({
  children,
  className,
  onClear,
  showClear = false,
}: FilterBarProps) {
  return (
    <div
      className={cn(
        "flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-white p-3",
        className,
      )}
    >
      <div className="flex items-center gap-2 px-1 text-sm font-semibold text-slate-700">
        <SlidersHorizontal className="size-4" />
        <span>Filters</span>
      </div>
      {children}
      {showClear ? (
        <Button className="ml-auto" onClick={onClear} size="sm" variant="ghost">
          Clear all
        </Button>
      ) : null}
    </div>
  );
}

