import { CornerDownLeft } from "lucide-react";

export function SearchSuggestions({
  onSelect,
  suggestions,
}: {
  onSelect: (suggestion: string) => void;
  suggestions: string[];
}) {
  if (suggestions.length === 0) return null;
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-2 shadow-lg">
      <p className="px-3 py-2 text-xs font-semibold uppercase tracking-wide text-slate-400">
        Suggestions
      </p>
      {suggestions.map((suggestion) => (
        <button
          className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm text-slate-700 hover:bg-slate-50"
          key={suggestion}
          onClick={() => onSelect(suggestion)}
          type="button"
        >
          <span className="flex-1">{suggestion}</span>
          <CornerDownLeft className="size-3.5 text-slate-300" />
        </button>
      ))}
    </div>
  );
}

