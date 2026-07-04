import { Medal } from "lucide-react";

import { Avatar, Badge, Card, CardContent } from "@/components/common";
import type { LeaderboardEntry } from "@/types/gamification";
import { cn } from "@/utils/cn";

export function LeaderboardTable({
  currentUserId,
  entries,
}: {
  currentUserId: string | null;
  entries: LeaderboardEntry[];
}) {
  return (
    <div className="grid gap-3">
      {entries.map((entry) => {
        const currentUser =
          currentUserId !== null && entry.userId === currentUserId;
        return (
          <Card
            className={cn(
              currentUser && "border-brand-300 bg-brand-50/40",
            )}
            key={entry.userId}
          >
            <CardContent className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center">
              <span
                className={cn(
                  "grid size-11 shrink-0 place-items-center rounded-xl text-sm font-bold",
                  entry.rank <= 3
                    ? "bg-amber-100 text-amber-700"
                    : "bg-slate-100 text-slate-600",
                )}
              >
                {entry.rank <= 3 ? <Medal className="size-5" /> : entry.rank}
              </span>
              <Avatar name={entry.fullName} />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-semibold text-slate-900">
                    {entry.fullName}
                  </p>
                  {currentUser ? <Badge variant="brand">You</Badge> : null}
                </div>
                <p className="mt-1 text-xs text-slate-400">
                  Level {entry.level} · {entry.allTimeXp.toLocaleString()} all-time XP
                </p>
              </div>
              <div className="sm:text-right">
                <p className="text-lg font-bold text-brand-700">
                  {entry.totalXpForPeriod.toLocaleString()} XP
                </p>
                <p className="text-xs text-slate-400">Selected period</p>
              </div>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
