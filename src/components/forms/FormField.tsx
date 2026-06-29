import { useId, type InputHTMLAttributes, type ReactNode } from "react";

import { cn } from "@/utils/cn";

export interface FormFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, "size"> {
  error?: string;
  hint?: string;
  icon?: ReactNode;
  inputClassName?: string;
  label: string;
  trailing?: ReactNode;
}

export function FormField({
  className,
  error,
  hint,
  icon,
  id,
  inputClassName,
  label,
  required,
  trailing,
  ...props
}: FormFieldProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const messageId = `${inputId}-message`;

  return (
    <div className={cn("grid gap-1.5", className)}>
      <label className="text-sm font-semibold text-slate-700" htmlFor={inputId}>
        {label}
        {required ? (
          <span aria-hidden="true" className="ml-1 text-red-500">
            *
          </span>
        ) : null}
      </label>
      <div className="relative">
        {icon ? (
          <span className="pointer-events-none absolute left-3.5 top-1/2 flex -translate-y-1/2 text-slate-400">
            {icon}
          </span>
        ) : null}
        <input
          aria-describedby={error || hint ? messageId : undefined}
          aria-invalid={Boolean(error)}
          className={cn(
            "h-11 w-full rounded-xl border bg-white px-3.5 text-sm text-slate-950 outline-none transition",
            "placeholder:text-slate-400 hover:border-slate-300 focus:ring-4",
            icon && "pl-10",
            trailing && "pr-11",
            error
              ? "border-red-300 focus:border-red-400 focus:ring-red-100"
              : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
            inputClassName,
          )}
          id={inputId}
          required={required}
          {...props}
        />
        {trailing ? (
          <span className="absolute right-2 top-1/2 flex -translate-y-1/2">
            {trailing}
          </span>
        ) : null}
      </div>
      {error ? (
        <p className="text-xs font-medium text-red-600" id={messageId}>
          {error}
        </p>
      ) : hint ? (
        <p className="text-xs text-slate-500" id={messageId}>
          {hint}
        </p>
      ) : null}
    </div>
  );
}

