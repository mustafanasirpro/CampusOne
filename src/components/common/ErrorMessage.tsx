import { CircleAlert } from "lucide-react";

import { cn } from "@/utils/cn";

export function ErrorMessage({
  className,
  message,
}: {
  className?: string;
  message: string;
}) {
  return (
    <div
      className={cn(
        "min-w-0 break-words flex items-start gap-2.5 rounded-xl border border-red-200 bg-red-50 px-3.5 py-3 text-sm text-red-700",
        className,
      )}
      role="alert"
    >
      <CircleAlert className="mt-0.5 size-4 shrink-0" />
      <span>{message}</span>
    </div>
  );
}
