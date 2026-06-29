import { cn } from "@/utils/cn";

export interface TabItem<T extends string> {
  count?: number;
  label: string;
  value: T;
}

export interface TabsProps<T extends string> {
  activeTab: T;
  className?: string;
  onChange: (value: T) => void;
  tabs: Array<TabItem<T>>;
}

export function Tabs<T extends string>({
  activeTab,
  className,
  onChange,
  tabs,
}: TabsProps<T>) {
  return (
    <div
      aria-label="Tabs"
      className={cn(
        "inline-flex max-w-full gap-1 overflow-x-auto rounded-xl bg-slate-100 p-1",
        className,
      )}
      role="tablist"
    >
      {tabs.map((tab) => {
        const isActive = tab.value === activeTab;

        return (
          <button
            aria-selected={isActive}
            className={cn(
              "inline-flex h-9 shrink-0 items-center gap-2 rounded-lg px-3 text-sm font-medium transition",
              isActive
                ? "bg-white text-slate-950 shadow-sm"
                : "text-slate-500 hover:text-slate-800",
            )}
            key={tab.value}
            onClick={() => onChange(tab.value)}
            role="tab"
            type="button"
          >
            {tab.label}
            {typeof tab.count === "number" ? (
              <span
                className={cn(
                  "rounded-full px-1.5 py-0.5 text-[10px]",
                  isActive
                    ? "bg-brand-50 text-brand-700"
                    : "bg-slate-200 text-slate-600",
                )}
              >
                {tab.count}
              </span>
            ) : null}
          </button>
        );
      })}
    </div>
  );
}

