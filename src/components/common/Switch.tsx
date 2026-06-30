import { cn } from "@/utils/cn";

export interface SwitchProps {
  checked: boolean;
  className?: string;
  description?: string;
  disabled?: boolean;
  label: string;
  onCheckedChange: (checked: boolean) => void;
}

export function Switch({
  checked,
  className,
  description,
  disabled = false,
  label,
  onCheckedChange,
}: SwitchProps) {
  return (
    <div
      className={cn(
        "flex items-center justify-between gap-4 rounded-xl py-2",
        className,
      )}
    >
      <div className="min-w-0">
        <p className="text-sm font-semibold text-slate-800">{label}</p>
        {description ? (
          <p className="mt-0.5 text-xs leading-5 text-slate-500">
            {description}
          </p>
        ) : null}
      </div>
      <button
        aria-checked={checked}
        aria-label={label}
        className={cn(
          "relative h-6 w-11 shrink-0 rounded-full transition-colors duration-200",
          "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500",
          checked ? "bg-brand-600" : "bg-slate-300",
          disabled && "cursor-not-allowed opacity-50",
        )}
        disabled={disabled}
        onClick={() => onCheckedChange(!checked)}
        role="switch"
        type="button"
      >
        <span
          className={cn(
            "absolute left-0.5 top-0.5 size-5 rounded-full bg-white shadow-sm transition-transform duration-200",
            checked && "translate-x-5",
          )}
        />
      </button>
    </div>
  );
}
