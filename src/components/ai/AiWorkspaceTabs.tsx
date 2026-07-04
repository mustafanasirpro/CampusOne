import {
  BookOpenText,
  Bot,
  BrainCircuit,
  CalendarRange,
  FileStack,
  Gauge,
  ListChecks,
  MessageSquareText,
} from "lucide-react";

import { cn } from "@/utils/cn";

export type AiWorkspaceTab =
  | "chat"
  | "explain"
  | "summary"
  | "flashcards"
  | "quiz"
  | "study-plan"
  | "generated"
  | "usage";

const tabs = [
  { icon: MessageSquareText, label: "Chat", value: "chat" },
  { icon: BrainCircuit, label: "Explain", value: "explain" },
  { icon: BookOpenText, label: "Summary", value: "summary" },
  { icon: FileStack, label: "Flashcards", value: "flashcards" },
  { icon: ListChecks, label: "Quiz", value: "quiz" },
  { icon: CalendarRange, label: "Study Plan", value: "study-plan" },
  { icon: Bot, label: "Generated", value: "generated" },
  { icon: Gauge, label: "Usage", value: "usage" },
] satisfies Array<{
  icon: typeof Bot;
  label: string;
  value: AiWorkspaceTab;
}>;

export function AiWorkspaceTabs({
  activeTab,
  onChange,
}: {
  activeTab: AiWorkspaceTab;
  onChange: (value: AiWorkspaceTab) => void;
}) {
  return (
    <nav
      aria-label="AI assistant tools"
      className="flex gap-2 overflow-x-auto rounded-2xl border border-slate-200 bg-white p-2 shadow-card"
    >
      {tabs.map((tab) => {
        const Icon = tab.icon;
        const active = tab.value === activeTab;
        return (
          <button
            aria-current={active ? "page" : undefined}
            className={cn(
              "inline-flex h-10 shrink-0 items-center gap-2 rounded-xl px-3 text-sm font-semibold transition",
              active
                ? "bg-brand-600 text-white"
                : "text-slate-600 hover:bg-slate-100",
            )}
            key={tab.value}
            onClick={() => onChange(tab.value)}
            type="button"
          >
            <Icon className="size-4" />
            {tab.label}
          </button>
        );
      })}
    </nav>
  );
}

