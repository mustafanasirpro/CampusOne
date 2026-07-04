import {
  CheckCircle2,
  Edit3,
  Save,
  Trash2,
  X,
} from "lucide-react";
import { useState, type FormEvent } from "react";

import {
  Avatar,
  Badge,
  Button,
  Card,
  CardContent,
} from "@/components/common";
import { VoteControls } from "@/components/discussion/VoteControls";
import { formatDiscussionDate } from "@/components/discussion/discussionFormatting";
import type {
  DiscussionAnswer,
  DiscussionVoteValue,
} from "@/types/discussion";
import { cn } from "@/utils/cn";

interface AnswerCardProps {
  answer: DiscussionAnswer;
  canAccept: boolean;
  isBusy: boolean;
  onAccept: (answerId: string) => Promise<void>;
  onDelete: (answer: DiscussionAnswer) => Promise<void>;
  onRemoveVote: (answerId: string) => Promise<void>;
  onUnaccept: () => Promise<void>;
  onUpdate: (answerId: string, body: string) => Promise<void>;
  onVote: (
    answerId: string,
    value: DiscussionVoteValue,
  ) => Promise<void>;
}

export function AnswerCard({
  answer,
  canAccept,
  isBusy,
  onAccept,
  onDelete,
  onRemoveVote,
  onUnaccept,
  onUpdate,
  onVote,
}: AnswerCardProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [body, setBody] = useState(answer.body);
  const [editError, setEditError] = useState<string | null>(null);

  const submitEdit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const normalizedBody = body.trim();
    if (normalizedBody.length < 10) {
      setEditError("Your answer must contain at least 10 characters.");
      return;
    }
    setEditError(null);
    try {
      await onUpdate(answer.id, normalizedBody);
      setIsEditing(false);
    } catch {
      // The page reports the API error and the editor remains open.
    }
  };

  return (
    <Card
      className={cn(
        answer.accepted &&
          "border-emerald-300 bg-emerald-50/30 shadow-emerald-100",
      )}
    >
      <CardContent className="grid gap-4 p-5 sm:p-6">
        <div className="flex flex-wrap items-start gap-3">
          <Avatar
            name={answer.author.fullName}
            size="sm"
            src={answer.author.avatarUrl ?? undefined}
          />
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-slate-900">
              {answer.author.fullName}
            </p>
            <p className="truncate text-xs text-slate-400">
              {answer.author.university} ·{" "}
              {formatDiscussionDate(answer.createdAt)}
            </p>
          </div>
          {answer.accepted ? (
            <Badge className="ml-auto" variant="success">
              <CheckCircle2 className="mr-1 size-3.5" />
              Accepted answer
            </Badge>
          ) : null}
        </div>

        {isEditing ? (
          <form className="grid gap-3" noValidate onSubmit={submitEdit}>
            <textarea
              aria-label="Edit answer"
              aria-invalid={Boolean(editError)}
              className={cn(
                "min-h-32 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm leading-6 text-slate-950 outline-none focus:ring-4",
                editError
                  ? "border-red-300 focus:ring-red-100"
                  : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
              )}
              maxLength={5000}
              onChange={(event) => {
                setBody(event.target.value);
                setEditError(null);
              }}
              value={body}
            />
            {editError ? (
              <p className="text-xs font-medium text-red-600">{editError}</p>
            ) : null}
            <div className="flex justify-end gap-2">
              <Button
                onClick={() => {
                  setBody(answer.body);
                  setEditError(null);
                  setIsEditing(false);
                }}
                size="sm"
                variant="ghost"
              >
                <X className="size-3.5" />
                Cancel
              </Button>
              <Button loading={isBusy} size="sm" type="submit">
                <Save className="size-3.5" />
                Save answer
              </Button>
            </div>
          </form>
        ) : (
          <p className="whitespace-pre-wrap text-sm leading-7 text-slate-600">
            {answer.body}
          </p>
        )}

        <div className="flex flex-wrap items-center gap-2 border-t border-slate-100 pt-4">
          <VoteControls
            currentUserVote={answer.currentUserVote}
            disabled={answer.ownedByCurrentUser}
            isBusy={isBusy}
            label="answer"
            onRemoveVote={() => void onRemoveVote(answer.id)}
            onVote={(value) => void onVote(answer.id, value)}
            score={answer.voteScore}
          />

          <div className="ml-auto flex flex-wrap justify-end gap-2">
            {canAccept ? (
              <Button
                loading={isBusy}
                onClick={() =>
                  void (answer.accepted
                    ? onUnaccept()
                    : onAccept(answer.id))
                }
                size="sm"
                variant={answer.accepted ? "secondary" : "outline"}
              >
                <CheckCircle2 className="size-3.5" />
                {answer.accepted ? "Remove acceptance" : "Accept answer"}
              </Button>
            ) : null}
            {answer.ownedByCurrentUser && !isEditing ? (
              <>
                <Button
                  disabled={isBusy}
                  onClick={() => setIsEditing(true)}
                  size="sm"
                  variant="outline"
                >
                  <Edit3 className="size-3.5" />
                  Edit
                </Button>
                <Button
                  disabled={isBusy}
                  onClick={() => void onDelete(answer)}
                  size="sm"
                  variant="ghost"
                >
                  <Trash2 className="size-3.5 text-red-600" />
                  Delete
                </Button>
              </>
            ) : null}
          </div>
        </div>

        {answer.ownedByCurrentUser ? (
          <p className="text-xs text-slate-400">
            You cannot vote on your own answer.
          </p>
        ) : null}
      </CardContent>
    </Card>
  );
}

