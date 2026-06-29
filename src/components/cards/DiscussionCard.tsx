import { MessageCircle, ThumbsUp } from "lucide-react";

import { Avatar, Badge, Card, CardContent } from "@/components/common";
import type { DiscussionSummary } from "@/types/content";

export interface DiscussionCardProps {
  discussion: DiscussionSummary;
  onOpen?: () => void;
  onUpvote?: () => void;
}

export function DiscussionCard({
  discussion,
  onOpen,
  onUpvote,
}: DiscussionCardProps) {
  return (
    <Card>
      <CardContent>
        <div className="flex items-center gap-3">
          <Avatar name={discussion.author} size="sm" />
          <div className="min-w-0">
            <p className="truncate text-sm font-medium text-slate-800">
              {discussion.author}
            </p>
            <p className="text-xs text-slate-400">{discussion.time}</p>
          </div>
          <Badge className="ml-auto">{discussion.category}</Badge>
        </div>
        <button
          className="mt-4 block text-left text-base font-semibold leading-6 text-slate-950 hover:text-brand-700"
          onClick={onOpen}
          type="button"
        >
          {discussion.title}
        </button>
        <div className="mt-3 flex flex-wrap gap-2">
          {discussion.tags.map((tag) => (
            <Badge key={tag} variant="brand">
              #{tag}
            </Badge>
          ))}
        </div>
        <div className="mt-4 flex items-center gap-5 text-sm text-slate-500">
          <button
            className="flex items-center gap-1.5 hover:text-brand-700"
            onClick={onUpvote}
            type="button"
          >
            <ThumbsUp className="size-4" />
            {discussion.upvotes}
          </button>
          <span className="flex items-center gap-1.5">
            <MessageCircle className="size-4" />
            {discussion.comments}
          </span>
        </div>
      </CardContent>
    </Card>
  );
}

