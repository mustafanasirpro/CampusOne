import { Star } from "lucide-react";

import { cn } from "@/utils/cn";

export function NoteRating({
  disabled = false,
  onRate,
  value,
}: {
  disabled?: boolean;
  onRate: (rating: number) => void;
  value: number | null;
}) {
  return (
    <fieldset disabled={disabled}>
      <legend className="mb-2 text-sm font-semibold text-slate-700">
        Your rating
      </legend>
      <div className="flex items-center gap-1" role="radiogroup">
        {Array.from({ length: 5 }, (_, index) => {
          const rating = index + 1;
          const selected = value !== null && rating <= value;
          return (
            <button
              aria-label={`Rate ${rating} out of 5`}
              aria-pressed={value === rating}
              className="rounded-lg p-1.5 transition hover:bg-amber-50 disabled:cursor-not-allowed disabled:opacity-50"
              key={rating}
              onClick={() => onRate(rating)}
              type="button"
            >
              <Star
                className={cn(
                  "size-5 text-amber-400",
                  selected && "fill-amber-400",
                )}
              />
            </button>
          );
        })}
      </div>
    </fieldset>
  );
}
