import { ChevronDown } from "lucide-react";
import { useId, type SelectHTMLAttributes } from "react";

import type { DropdownOption } from "@/components/common";
import { cn } from "@/utils/cn";

export interface SelectFieldProps
  extends SelectHTMLAttributes<HTMLSelectElement> {
  error?: string;
  label: string;
  options: DropdownOption[];
}

export function SelectField({
  className,
  error,
  id,
  label,
  options,
  required,
  ...props
}: SelectFieldProps) {
  const generatedId = useId();
  const selectId = id ?? generatedId;
  const messageId = `${selectId}-message`;

  return (
    <div className="grid gap-1.5">
      <label className="text-sm font-semibold text-slate-700" htmlFor={selectId}>
        {label}
        {required ? (
          <span aria-hidden="true" className="ml-1 text-red-500">
            *
          </span>
        ) : null}
      </label>
      <div className="relative">
        <select
          aria-describedby={error ? messageId : undefined}
          aria-invalid={Boolean(error)}
          className={cn(
            "h-11 w-full appearance-none rounded-xl border bg-white py-2 pl-3.5 pr-10 text-sm text-slate-950 outline-none transition hover:border-slate-300 focus:ring-4",
            error
              ? "border-red-300 focus:border-red-400 focus:ring-red-100"
              : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
            className,
          )}
          id={selectId}
          required={required}
          {...props}
        >
          {options.map((option) => (
            <option
              disabled={option.disabled}
              key={option.value}
              value={option.value}
            >
              {option.label}
            </option>
          ))}
        </select>
        <ChevronDown className="pointer-events-none absolute right-3.5 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
      </div>
      {error ? (
        <p className="text-xs font-medium text-red-600" id={messageId}>
          {error}
        </p>
      ) : null}
    </div>
  );
}

