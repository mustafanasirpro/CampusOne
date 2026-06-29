import { Search, X } from "lucide-react";
import type { FormEvent, InputHTMLAttributes } from "react";

import { cn } from "@/utils/cn";

export interface SearchBarProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, "onChange" | "type"> {
  onSearch?: (value: string) => void;
  onValueChange?: (value: string) => void;
  value?: string;
}

export function SearchBar({
  className,
  onSearch,
  onValueChange,
  placeholder = "Search CampusOne",
  value = "",
  ...props
}: SearchBarProps) {
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onSearch?.(value.trim());
  };

  return (
    <form
      className={cn("relative w-full", className)}
      onSubmit={handleSubmit}
      role="search"
    >
      <Search
        aria-hidden="true"
        className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-slate-400"
      />
      <input
        className="h-10 w-full rounded-xl border border-slate-200 bg-slate-50 py-2 pl-10 pr-9 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:border-brand-400 focus:bg-white focus:ring-4 focus:ring-brand-100"
        onChange={(event) => onValueChange?.(event.target.value)}
        placeholder={placeholder}
        type="search"
        value={value}
        {...props}
      />
      {value ? (
        <button
          aria-label="Clear search"
          className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-md p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600"
          onClick={() => onValueChange?.("")}
          type="button"
        >
          <X className="size-3.5" />
        </button>
      ) : null}
    </form>
  );
}

