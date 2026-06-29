import type { HTMLAttributes } from "react";

import { cn } from "@/utils/cn";

type BadgeVariant = "brand" | "neutral" | "success" | "warning" | "danger";

const variantClasses: Record<BadgeVariant, string> = {
  brand: "bg-brand-50 text-brand-700 ring-brand-600/10",
  neutral: "bg-slate-100 text-slate-700 ring-slate-600/10",
  success: "bg-emerald-50 text-emerald-700 ring-emerald-600/10",
  warning: "bg-amber-50 text-amber-700 ring-amber-600/10",
  danger: "bg-red-50 text-red-700 ring-red-600/10",
};

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
}

export function Badge({
  className,
  variant = "neutral",
  ...props
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex w-fit items-center rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset",
        variantClasses[variant],
        className,
      )}
      {...props}
    />
  );
}

