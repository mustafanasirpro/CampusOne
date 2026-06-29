import { ArrowDownRight, ArrowUpRight, type LucideIcon } from "lucide-react";

import { Card, CardContent } from "@/components/common";
import { cn } from "@/utils/cn";

export interface StatCardProps {
  change?: number;
  description?: string;
  icon: LucideIcon;
  label: string;
  value: string | number;
}

export function StatCard({
  change,
  description,
  icon: Icon,
  label,
  value,
}: StatCardProps) {
  const hasChange = typeof change === "number";
  const isPositive = hasChange && change >= 0;

  return (
    <Card>
      <CardContent className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-slate-500">{label}</p>
          <p className="mt-2 text-2xl font-bold tracking-tight text-slate-950">
            {value}
          </p>
          {hasChange ? (
            <p
              className={cn(
                "mt-2 flex items-center gap-1 text-xs font-semibold",
                isPositive ? "text-emerald-600" : "text-red-600",
              )}
            >
              {isPositive ? (
                <ArrowUpRight className="size-3.5" />
              ) : (
                <ArrowDownRight className="size-3.5" />
              )}
              {Math.abs(change)}%
            </p>
          ) : description ? (
            <p className="mt-2 text-xs text-slate-500">{description}</p>
          ) : null}
        </div>
        <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600">
          <Icon className="size-5" />
        </span>
      </CardContent>
    </Card>
  );
}

