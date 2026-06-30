import { Avatar } from "@/components/common/Avatar";
import type { ChatMessage } from "@/types/ai";

interface ChatBubbleProps {
  message: ChatMessage;
}

export function ChatBubble({ message }: ChatBubbleProps) {
  return (
    <div className="flex justify-end gap-3">
      <div className="max-w-[86%] sm:max-w-[76%]">
        <div className="rounded-2xl rounded-tr-md bg-gradient-to-br from-brand-600 to-brand-700 px-4 py-3 text-sm leading-6 text-white shadow-lg shadow-brand-600/15 sm:px-5">
          {message.text}
        </div>
        <p className="mt-1.5 text-right text-[11px] font-medium text-slate-400">
          You · {message.timestamp}
        </p>
      </div>
      <Avatar
        className="mt-0.5 bg-slate-900 text-white"
        name="Ali Khan"
        size="sm"
      />
    </div>
  );
}
