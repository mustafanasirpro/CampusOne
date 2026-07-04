import { Dropdown } from "@/components/common";
import type {
  GlobalSearchSort,
  GlobalSearchType,
  SearchTypeMetadata,
} from "@/types/search";
import { cn } from "@/utils/cn";

export function SearchFilters({
  availableTypes,
  onSortChange,
  onToggleType,
  selectedTypes,
  sort,
}: {
  availableTypes: SearchTypeMetadata[];
  onSortChange: (value: GlobalSearchSort) => void;
  onToggleType: (value: GlobalSearchType) => void;
  selectedTypes: GlobalSearchType[];
  sort: GlobalSearchSort;
}) {
  return (
    <div className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-card lg:flex-row lg:items-end">
      <fieldset className="min-w-0 flex-1">
        <legend className="mb-2 text-sm font-semibold text-slate-700">
          Search types
        </legend>
        <div className="flex flex-wrap gap-2">
          {availableTypes.map((item) => {
            const selected = selectedTypes.includes(item.type);
            return (
              <button
                aria-pressed={selected}
                className={cn(
                  "rounded-xl px-3 py-2 text-xs font-semibold transition",
                  selected
                    ? "bg-brand-600 text-white"
                    : "bg-slate-100 text-slate-600 hover:bg-brand-50 hover:text-brand-700",
                )}
                key={item.type}
                onClick={() => onToggleType(item.type)}
                title={item.description}
                type="button"
              >
                {item.displayName}
              </button>
            );
          })}
        </div>
        <p className="mt-2 text-xs text-slate-400">
          No selection searches every supported content type.
        </p>
      </fieldset>
      <Dropdown
        className="min-w-44"
        label="Sort results"
        onChange={(event) =>
          onSortChange(event.target.value as GlobalSearchSort)
        }
        options={[
          { label: "Relevance", value: "RELEVANCE" },
          { label: "Newest", value: "NEWEST" },
          { label: "Oldest", value: "OLDEST" },
        ]}
        value={sort}
      />
    </div>
  );
}

