import { Award, CheckCircle2, LockKeyhole } from "lucide-react";

import { Badge, Card, CardContent } from "@/components/common";
import type { GamificationBadge } from "@/types/gamification";

export function BadgeCard({
  badge,
  earned,
}: {
  badge: GamificationBadge;
  earned: boolean;
}) {
  return (
    <Card className={earned ? "border-emerald-200" : "bg-slate-50/70"}>
      <CardContent className="flex h-full flex-col p-5">
        <div className="flex items-start justify-between gap-3">
          <span className="grid size-12 place-items-center rounded-2xl bg-brand-100 text-brand-700">
            <Award className="size-5" />
          </span>
          <Badge variant={earned ? "success" : "neutral"}>
            {earned ? (
              <CheckCircle2 className="mr-1 size-3.5" />
            ) : (
              <LockKeyhole className="mr-1 size-3.5" />
            )}
            {earned ? "Earned" : "Locked"}
          </Badge>
        </div>
        <h3 className="mt-4 font-semibold text-slate-950">{badge.name}</h3>
        <p className="mt-2 flex-1 text-sm leading-6 text-slate-500">
          {badge.description}
        </p>
        <p className="mt-4 text-xs font-semibold text-brand-700">
          {badge.xpRequired.toLocaleString()} XP required
        </p>
      </CardContent>
    </Card>
  );
}

