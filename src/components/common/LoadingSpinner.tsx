import { LoaderCircle } from "lucide-react";

import { cn } from "@/utils/cn";

export function LoadingSpinner({
  className,
  label = "Loading",
}: {
  className?: string;
  label?: string;
}) {
  return (
    <div
      aria-live="polite"
      className={cn(
        "inline-flex items-center gap-2 text-sm font-medium text-slate-600",
        className,
      )}
      role="status"
    >
      <LoaderCircle className="size-5 animate-spin text-brand-600" />
      <span>{label}</span>
    </div>
  );
}
