import { Flame, Sparkles, Trophy } from "lucide-react";
import type { ReactNode } from "react";

import { Avatar, Card, CardContent } from "@/components/common";
import { LevelProgressBar } from "@/components/gamification/LevelProgressBar";
import type { GamificationProfile } from "@/types/gamification";

export function GamificationProfileCard({
  profile,
}: {
  profile: GamificationProfile;
}) {
  return (
    <Card className="overflow-hidden border-0 bg-slate-950 text-white shadow-xl">
      <CardContent className="grid gap-6 p-6 sm:p-8 lg:grid-cols-[1fr_auto] lg:items-center">
        <div>
          <div className="flex items-center gap-4">
            <Avatar name={profile.fullName} size="xl" />
            <div>
              <p className="text-sm text-slate-400">Your CampusOne progress</p>
              <h2 className="mt-1 text-2xl font-bold">{profile.fullName}</h2>
              <p className="mt-1 text-sm font-semibold text-brand-300">
                {profile.totalXp.toLocaleString()} XP · Level {profile.level}
              </p>
            </div>
          </div>
          <div className="mt-6 max-w-xl">
            <LevelProgressBar level={profile.level} totalXp={profile.totalXp} />
          </div>
        </div>
        <div className="grid grid-cols-3 gap-3">
          <Stat icon={<Flame className="size-5" />} label="Current streak" value={profile.currentStreak} />
          <Stat icon={<Trophy className="size-5" />} label="Longest streak" value={profile.longestStreak} />
          <Stat icon={<Sparkles className="size-5" />} label="Badges" value={profile.badges.length} />
        </div>
      </CardContent>
    </Card>
  );
}

function Stat({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: number;
}) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/[0.06] p-4 text-center">
      <span className="mx-auto grid size-9 place-items-center rounded-xl bg-brand-500/15 text-brand-200">
        {icon}
      </span>
      <p className="mt-2 text-xl font-bold">{value}</p>
      <p className="mt-1 text-[11px] text-slate-400">{label}</p>
    </div>
  );
}
