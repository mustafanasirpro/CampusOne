import { Send } from "lucide-react";
import { useState, type FormEvent } from "react";

import { Button } from "@/components/common";
import { cn } from "@/utils/cn";

export function AnswerForm({
  disabled = false,
  isSubmitting,
  onSubmit,
}: {
  disabled?: boolean;
  isSubmitting: boolean;
  onSubmit: (body: string) => Promise<void>;
}) {
  const [body, setBody] = useState("");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const normalizedBody = body.trim();
    if (normalizedBody.length < 10) {
      setError("Your answer must contain at least 10 characters.");
      return;
    }
    setError(null);
    try {
      await onSubmit(normalizedBody);
      setBody("");
    } catch {
      // The page displays the API error while preserving the draft.
    }
  };

  return (
    <form className="grid gap-3" noValidate onSubmit={handleSubmit}>
      <label className="grid gap-1.5">
        <span className="text-sm font-semibold text-slate-700">
          Your answer
        </span>
        <textarea
          aria-invalid={Boolean(error)}
          className={cn(
            "min-h-32 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm leading-6 text-slate-950 outline-none transition placeholder:text-slate-400 focus:ring-4",
            error
              ? "border-red-300 focus:border-red-400 focus:ring-red-100"
              : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
          )}
          disabled={disabled || isSubmitting}
          maxLength={5000}
          onChange={(event) => {
            setBody(event.target.value);
            setError(null);
          }}
          placeholder="Write a clear, constructive answer..."
          value={body}
        />
        <span className="flex justify-between gap-3 text-xs">
          <span className={error ? "font-medium text-red-600" : "text-slate-500"}>
            {error ?? "Answers should explain the solution, not just state it."}
          </span>
          <span className="shrink-0 text-slate-400">
            {body.length}/5000
          </span>
        </span>
      </label>
      <Button
        className="w-full sm:w-auto sm:justify-self-end"
        disabled={disabled}
        loading={isSubmitting}
        type="submit"
      >
        <Send className="size-4" />
        Post answer
      </Button>
    </form>
  );
}
