export function LevelProgressBar({
  level,
  totalXp,
}: {
  level: number;
  totalXp: number;
}) {
  const progress = totalXp % 100;
  return (
    <div className="grid gap-2">
      <div className="flex justify-between text-xs font-medium text-slate-400">
        <span>Level {level}</span>
        <span>{100 - progress} XP to next level</span>
      </div>
      <div
        aria-label={`Level ${level} progress`}
        aria-valuemax={100}
        aria-valuemin={0}
        aria-valuenow={progress}
        className="h-2 overflow-hidden rounded-full bg-white/10"
        role="progressbar"
      >
        <div
          className="h-full rounded-full bg-gradient-to-r from-brand-400 to-emerald-400"
          style={{ width: `${progress}%` }}
        />
      </div>
    </div>
  );
}

