import { Award, Trophy } from "lucide-react";

import { Avatar, Card, CardContent } from "@/components/common";
import type { LeaderboardEntry } from "@/types/content";
import { cn } from "@/utils/cn";

export interface LeaderboardCardProps {
  entry: LeaderboardEntry;
}

export function LeaderboardCard({ entry }: LeaderboardCardProps) {
  return (
    <Card>
      <CardContent className="flex items-center gap-4">
        <span
          className={cn(
            "grid size-10 shrink-0 place-items-center rounded-xl text-sm font-bold",
            entry.rank === 1
              ? "bg-amber-100 text-amber-700"
              : "bg-slate-100 text-slate-600",
          )}
        >
          {entry.rank === 1 ? <Trophy className="size-5" /> : entry.rank}
        </span>
        <Avatar name={entry.name} />
        <div className="min-w-0 flex-1">
          <h3 className="truncate font-semibold text-slate-950">
            {entry.name}
          </h3>
          <p className="truncate text-xs text-slate-500">{entry.university}</p>
        </div>
        <div className="text-right">
          <p className="font-bold text-brand-700">
            {entry.xp.toLocaleString()} XP
          </p>
          <p className="mt-1 flex items-center justify-end gap-1 text-xs text-slate-500">
            <Award className="size-3.5" />
            {entry.badges} badges
          </p>
        </div>
      </CardContent>
    </Card>
  );
}

