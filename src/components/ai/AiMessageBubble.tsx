import { Bot, UserRound } from "lucide-react";

import type { AiMessage } from "@/types/ai";
import { cn } from "@/utils/cn";
import { formatAiDate } from "@/components/ai/aiFormatting";

export function AiMessageBubble({ message }: { message: AiMessage }) {
  const user = message.role === "USER";
  return (
    <article
      className={cn(
        "flex max-w-[92%] gap-3",
        user ? "ml-auto flex-row-reverse" : "mr-auto",
      )}
    >
      <span
        className={cn(
          "grid size-9 shrink-0 place-items-center rounded-xl",
          user
            ? "bg-brand-600 text-white"
            : "bg-violet-100 text-violet-700",
        )}
      >
        {user ? <UserRound className="size-4" /> : <Bot className="size-4" />}
      </span>
      <div
        className={cn(
          "rounded-2xl px-4 py-3 shadow-sm",
          user
            ? "rounded-tr-sm bg-brand-600 text-white"
            : "rounded-tl-sm border border-slate-200 bg-white text-slate-700",
        )}
      >
        <p className="whitespace-pre-wrap text-sm leading-6">{message.content}</p>
        <p
          className={cn(
            "mt-2 text-[10px]",
            user ? "text-brand-100" : "text-slate-400",
          )}
        >
          {formatAiDate(message.createdAt)}
        </p>
      </div>
    </article>
  );
}
