import { useId, type ChangeEvent, type ReactNode } from "react";

import { cn } from "@/utils/cn";

export interface CheckboxFieldProps {
  checked: boolean;
  className?: string;
  error?: string;
  label: ReactNode;
  name?: string;
  onChange: (event: ChangeEvent<HTMLInputElement>) => void;
}

export function CheckboxField({
  checked,
  className,
  error,
  label,
  name,
  onChange,
}: CheckboxFieldProps) {
  const id = useId();
  const errorId = `${id}-error`;

  return (
    <div className={cn("grid gap-1.5", className)}>
      <label className="flex items-start gap-2.5 text-sm text-slate-600" htmlFor={id}>
        <input
          aria-describedby={error ? errorId : undefined}
          aria-invalid={Boolean(error)}
          checked={checked}
          className="mt-0.5 size-4 shrink-0 rounded border-slate-300 accent-brand-600"
          id={id}
          name={name}
          onChange={onChange}
          type="checkbox"
        />
        <span className="leading-5">{label}</span>
      </label>
      {error ? (
        <p className="text-xs font-medium text-red-600" id={errorId}>
          {error}
        </p>
      ) : null}
    </div>
  );
}

