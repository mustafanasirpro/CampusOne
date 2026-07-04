import { ChevronDown, ChevronUp } from "lucide-react";

import { cn } from "@/utils/cn";
import type {
  DiscussionVoteValue,
} from "@/types/discussion";

interface VoteControlsProps {
  currentUserVote: DiscussionVoteValue | null;
  disabled?: boolean;
  isBusy?: boolean;
  label: string;
  onRemoveVote: () => void;
  onVote: (value: DiscussionVoteValue) => void;
  score: number;
}

export function VoteControls({
  currentUserVote,
  disabled = false,
  isBusy = false,
  label,
  onRemoveVote,
  onVote,
  score,
}: VoteControlsProps) {
  const submitVote = (value: DiscussionVoteValue) => {
    if (currentUserVote === value) {
      onRemoveVote();
    } else {
      onVote(value);
    }
  };

  return (
    <div
      aria-label={`${label} voting`}
      className="inline-flex items-center rounded-xl border border-slate-200 bg-white p-1 shadow-sm"
    >
      <button
        aria-label={
          currentUserVote === 1
            ? `Remove upvote from ${label}`
            : `Upvote ${label}`
        }
        aria-pressed={currentUserVote === 1}
        className={cn(
          "grid size-9 place-items-center rounded-lg transition focus-visible:outline-2 focus-visible:outline-brand-500",
          currentUserVote === 1
            ? "bg-brand-50 text-brand-700"
            : "text-slate-500 hover:bg-slate-50 hover:text-brand-700",
        )}
        disabled={disabled || isBusy}
        onClick={() => submitVote(1)}
        title={disabled ? "You cannot vote on your own post." : undefined}
        type="button"
      >
        <ChevronUp className="size-5" />
      </button>
      <span
        aria-label={`${score} votes`}
        className="min-w-10 text-center text-sm font-bold text-slate-800"
      >
        {score}
      </span>
      <button
        aria-label={
          currentUserVote === -1
            ? `Remove downvote from ${label}`
            : `Downvote ${label}`
        }
        aria-pressed={currentUserVote === -1}
        className={cn(
          "grid size-9 place-items-center rounded-lg transition focus-visible:outline-2 focus-visible:outline-brand-500",
          currentUserVote === -1
            ? "bg-red-50 text-red-700"
            : "text-slate-500 hover:bg-slate-50 hover:text-red-600",
        )}
        disabled={disabled || isBusy}
        onClick={() => submitVote(-1)}
        title={disabled ? "You cannot vote on your own post." : undefined}
        type="button"
      >
        <ChevronDown className="size-5" />
      </button>
    </div>
  );
}

