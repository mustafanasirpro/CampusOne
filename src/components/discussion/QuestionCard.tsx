import {
  CalendarDays,
  CheckCircle2,
  Edit3,
  Eye,
  MessageCircle,
  ThumbsUp,
} from "lucide-react";
import { Link } from "react-router-dom";

import { Avatar, Badge, Card, CardContent } from "@/components/common";
import {
  discussionCategoryLabel,
  discussionStatusLabel,
  formatDiscussionDate,
} from "@/components/discussion/discussionFormatting";
import { paths } from "@/routes/paths";
import type { DiscussionQuestionSummary } from "@/types/discussion";

export function QuestionCard({
  owned = false,
  question,
}: {
  owned?: boolean;
  question: DiscussionQuestionSummary;
}) {
  return (
    <Card className="group hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-xl">
      <CardContent className="grid gap-4 p-5 sm:p-6">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="brand">
            {discussionCategoryLabel(question.category)}
          </Badge>
          <Badge
            variant={
              question.status === "RESOLVED"
                ? "success"
                : question.status === "CLOSED"
                  ? "neutral"
                  : "warning"
            }
          >
            {question.status === "RESOLVED" ? (
              <CheckCircle2 className="mr-1 size-3.5" />
            ) : null}
            {discussionStatusLabel(question.status)}
          </Badge>
          <span className="ml-auto flex items-center gap-1.5 text-xs text-slate-400">
            <CalendarDays className="size-3.5" />
            {formatDiscussionDate(question.createdAt)}
          </span>
        </div>

        <div>
          <Link
            className="text-lg font-semibold leading-7 text-slate-950 transition group-hover:text-brand-700"
            to={paths.discussionQuestion(question.id)}
          >
            {question.title}
          </Link>
          <p className="mt-2 line-clamp-3 text-sm leading-6 text-slate-500">
            {question.bodyPreview}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Avatar
            name={question.author.fullName}
            size="sm"
            src={question.author.avatarUrl ?? undefined}
          />
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-slate-800">
              {question.author.fullName}
            </p>
            <p className="truncate text-xs text-slate-400">
              {question.author.university}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-4 border-t border-slate-100 pt-4 text-xs font-medium text-slate-500">
          <span className="flex items-center gap-1.5">
            <ThumbsUp className="size-3.5" />
            {question.voteScore} votes
          </span>
          <span className="flex items-center gap-1.5">
            <MessageCircle className="size-3.5" />
            {question.answerCount} answers
          </span>
          <span className="flex items-center gap-1.5">
            <Eye className="size-3.5" />
            {question.viewCount} views
          </span>
          <div className="ml-auto flex gap-2">
            {owned ? (
              <Link
                className="inline-flex h-9 items-center gap-1.5 rounded-lg border border-slate-200 px-3 font-semibold text-slate-600 hover:bg-slate-50"
                to={paths.discussionQuestionEdit(question.id)}
              >
                <Edit3 className="size-3.5" />
                Edit
              </Link>
            ) : null}
            <Link
              className="inline-flex h-9 items-center rounded-lg bg-brand-600 px-3 font-semibold text-white hover:bg-brand-700"
              to={paths.discussionQuestion(question.id)}
            >
              View question
            </Link>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
