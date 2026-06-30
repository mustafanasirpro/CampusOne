import {
  Bot,
  Check,
  Copy,
  RotateCcw,
  Share2,
  Sigma,
  ThumbsDown,
  ThumbsUp,
} from "lucide-react";

import type { AIContentBlock, ChatMessage } from "@/types/ai";
import { cn } from "@/utils/cn";

type Reaction = "like" | "dislike" | null;

interface AIResponseCardProps {
  copied?: boolean;
  message: ChatMessage;
  onCopy: () => void;
  onDislike: () => void;
  onLike: () => void;
  onRegenerate: () => void;
  onShare: () => void;
  reaction: Reaction;
}

function ContentBlock({ block }: { block: AIContentBlock }) {
  if (block.type === "heading") {
    return (
      <h4 className="text-base font-semibold tracking-tight text-slate-950">
        {block.text}
      </h4>
    );
  }

  if (block.type === "paragraph") {
    return <p className="text-sm leading-6 text-slate-600">{block.text}</p>;
  }

  if (block.type === "list") {
    const List = block.ordered ? "ol" : "ul";

    return (
      <List
        className={cn(
          "grid gap-2 pl-5 text-sm leading-6 text-slate-600",
          block.ordered ? "list-decimal" : "list-disc marker:text-brand-500",
        )}
      >
        {block.items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </List>
    );
  }

  if (block.type === "code") {
    return (
      <div className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-950 shadow-lg shadow-slate-950/10">
        <div className="flex items-center justify-between border-b border-white/10 px-4 py-2">
          <span className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-[0.12em] text-slate-400">
            <span className="size-2 rounded-full bg-emerald-400" />
            {block.language}
          </span>
          <span className="text-[10px] text-slate-500">Example</span>
        </div>
        <pre className="overflow-x-auto p-4 text-xs leading-6 text-slate-200">
          <code>{block.code}</code>
        </pre>
      </div>
    );
  }

  if (block.type === "table") {
    return (
      <div className="overflow-x-auto rounded-2xl border border-slate-200">
        <table className="w-full min-w-[440px] border-collapse text-left text-xs">
          <thead className="bg-slate-50 text-slate-700">
            <tr>
              {block.headers.map((header) => (
                <th
                  className="border-b border-slate-200 px-4 py-3 font-semibold"
                  key={header}
                  scope="col"
                >
                  {header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {block.rows.map((row, rowIndex) => (
              <tr className="transition-colors hover:bg-slate-50/70" key={rowIndex}>
                {row.map((cell, cellIndex) => (
                  <td
                    className="px-4 py-3 leading-5 text-slate-600"
                    key={`${rowIndex}-${cellIndex}`}
                  >
                    {cell}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <div className="flex gap-3 rounded-2xl border border-violet-100 bg-violet-50/70 p-4">
      <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-violet-100 text-violet-700">
        <Sigma className="size-4" />
      </span>
      <div>
        <p className="font-mono text-sm font-semibold text-violet-950">
          {block.expression}
        </p>
        {block.explanation ? (
          <p className="mt-1 text-xs leading-5 text-violet-700">
            {block.explanation}
          </p>
        ) : null}
      </div>
    </div>
  );
}

const actionClasses =
  "inline-flex size-8 items-center justify-center rounded-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 focus-visible:outline-2 focus-visible:outline-brand-500";

export function AIResponseCard({
  copied = false,
  message,
  onCopy,
  onDislike,
  onLike,
  onRegenerate,
  onShare,
  reaction,
}: AIResponseCardProps) {
  return (
    <div className="flex gap-3">
      <span className="grid size-8 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-brand-600 to-violet-600 text-white shadow-md shadow-brand-600/15">
        <Bot className="size-4" />
      </span>
      <div className="min-w-0 max-w-[calc(100%-2.75rem)] flex-1">
        <div className="rounded-2xl rounded-tl-md border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
          <div className="grid gap-4">
            {message.blocks?.map((block, index) => (
              <ContentBlock block={block} key={`${message.id}-${index}`} />
            ))}
          </div>
        </div>
        <div className="mt-1.5 flex flex-wrap items-center gap-0.5">
          <span className="mr-2 text-[11px] font-medium text-slate-400">
            CampusOne AI · {message.timestamp}
          </span>
          <button
            aria-label="Copy response"
            className={actionClasses}
            onClick={onCopy}
            title="Copy"
            type="button"
          >
            {copied ? (
              <Check className="size-3.5 text-emerald-600" />
            ) : (
              <Copy className="size-3.5" />
            )}
          </button>
          <button
            aria-label="Like response"
            className={cn(
              actionClasses,
              reaction === "like" && "bg-brand-50 text-brand-600",
            )}
            onClick={onLike}
            title="Like"
            type="button"
          >
            <ThumbsUp
              className={cn(
                "size-3.5",
                reaction === "like" && "fill-current",
              )}
            />
          </button>
          <button
            aria-label="Dislike response"
            className={cn(
              actionClasses,
              reaction === "dislike" && "bg-rose-50 text-rose-600",
            )}
            onClick={onDislike}
            title="Dislike"
            type="button"
          >
            <ThumbsDown
              className={cn(
                "size-3.5",
                reaction === "dislike" && "fill-current",
              )}
            />
          </button>
          <button
            aria-label="Regenerate response"
            className={actionClasses}
            onClick={onRegenerate}
            title="Regenerate"
            type="button"
          >
            <RotateCcw className="size-3.5" />
          </button>
          <button
            aria-label="Share response"
            className={actionClasses}
            onClick={onShare}
            title="Share"
            type="button"
          >
            <Share2 className="size-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
}
