import { ArrowUpRight } from "lucide-react";

import type { PromptSuggestion } from "@/types/ai";
import { cn } from "@/utils/cn";

interface PromptCardProps {
  compact?: boolean;
  onClick: () => void;
  suggestion: PromptSuggestion;
}

export function PromptCard({
  compact = false,
  onClick,
  suggestion,
}: PromptCardProps) {
  const Icon = suggestion.icon;

  return (
    <button
      className={cn(
        "group flex w-full items-start gap-3 rounded-2xl border border-slate-200 bg-white text-left shadow-sm transition duration-200",
        "hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-lg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500",
        compact ? "p-3" : "p-4",
      )}
      onClick={onClick}
      type="button"
    >
      <span
        className={cn(
          "grid shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600 transition-colors group-hover:bg-brand-600 group-hover:text-white",
          compact ? "size-9" : "size-10",
        )}
      >
        <Icon className="size-4.5" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex items-center justify-between gap-2">
          <span className="text-sm font-semibold text-slate-900">
            {suggestion.title}
          </span>
          <ArrowUpRight className="size-3.5 shrink-0 text-slate-300 transition group-hover:text-brand-600" />
        </span>
        {!compact ? (
          <span className="mt-1 block text-xs leading-5 text-slate-500">
            {suggestion.description}
          </span>
        ) : null}
      </span>
    </button>
  );
}
