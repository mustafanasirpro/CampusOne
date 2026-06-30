import { MessageSquareText, Star } from "lucide-react";

import type { AIConversation } from "@/types/ai";
import { cn } from "@/utils/cn";

interface ConversationListProps {
  activeId: string | null;
  conversations: AIConversation[];
  emptyMessage?: string;
  onSelect: (id: string) => void;
  onToggleFavorite: (conversation: AIConversation) => void;
  title: string;
}

export function ConversationList({
  activeId,
  conversations,
  emptyMessage = "No chats here yet.",
  onSelect,
  onToggleFavorite,
  title,
}: ConversationListProps) {
  return (
    <section>
      <h3 className="px-2 text-[11px] font-bold uppercase tracking-[0.14em] text-slate-400">
        {title}
      </h3>
      <div className="mt-2 grid gap-1">
        {conversations.length > 0 ? (
          conversations.map((conversation) => {
            const isActive = conversation.id === activeId;

            return (
              <div
                className={cn(
                  "group flex items-center rounded-xl border border-transparent transition-colors",
                  isActive
                    ? "border-brand-100 bg-brand-50"
                    : "hover:bg-slate-50",
                )}
                key={conversation.id}
              >
                <button
                  className="flex min-w-0 flex-1 items-center gap-2.5 px-2.5 py-2.5 text-left focus-visible:outline-2 focus-visible:outline-brand-500"
                  onClick={() => onSelect(conversation.id)}
                  type="button"
                >
                  <MessageSquareText
                    className={cn(
                      "size-4 shrink-0",
                      isActive ? "text-brand-600" : "text-slate-400",
                    )}
                  />
                  <span className="min-w-0 flex-1">
                    <span
                      className={cn(
                        "block truncate text-xs font-semibold",
                        isActive ? "text-brand-800" : "text-slate-700",
                      )}
                    >
                      {conversation.title}
                    </span>
                    <span className="mt-0.5 block truncate text-[10px] text-slate-400">
                      {conversation.category} · {conversation.updatedAt}
                    </span>
                  </span>
                </button>
                <button
                  aria-label={
                    conversation.isFavorite
                      ? `Remove ${conversation.title} from favorites`
                      : `Add ${conversation.title} to favorites`
                  }
                  className={cn(
                    "mr-1.5 rounded-lg p-1.5 transition focus-visible:outline-2 focus-visible:outline-brand-500",
                    conversation.isFavorite
                      ? "text-amber-500"
                      : "text-slate-300 opacity-0 hover:bg-white hover:text-amber-500 group-hover:opacity-100 focus:opacity-100",
                  )}
                  onClick={() => onToggleFavorite(conversation)}
                  type="button"
                >
                  <Star
                    className={cn(
                      "size-3.5",
                      conversation.isFavorite && "fill-current",
                    )}
                  />
                </button>
              </div>
            );
          })
        ) : (
          <p className="px-2 py-3 text-xs leading-5 text-slate-400">
            {emptyMessage}
          </p>
        )}
      </div>
    </section>
  );
}
