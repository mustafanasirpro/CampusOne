import { Bot } from "lucide-react";

export function TypingIndicator() {
  return (
    <div aria-label="CampusOne AI is typing" className="flex gap-3" role="status">
      <span className="grid size-8 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-brand-600 to-violet-600 text-white shadow-sm">
        <Bot className="size-4" />
      </span>
      <div className="rounded-2xl rounded-tl-md border border-slate-200 bg-white px-4 py-3 shadow-sm">
        <div className="flex h-5 items-center gap-1.5">
          <span className="size-1.5 animate-bounce rounded-full bg-brand-500 [animation-delay:-0.3s]" />
          <span className="size-1.5 animate-bounce rounded-full bg-brand-500 [animation-delay:-0.15s]" />
          <span className="size-1.5 animate-bounce rounded-full bg-brand-500" />
        </div>
      </div>
    </div>
  );
}
