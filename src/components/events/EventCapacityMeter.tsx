export function EventCapacityMeter({
  capacity,
  participantCount,
}: {
  capacity: number;
  participantCount: number;
}) {
  const percentage = Math.min(
    100,
    Math.round((participantCount / capacity) * 100),
  );

  return (
    <div className="grid gap-2">
      <div className="flex justify-between gap-3 text-xs font-medium text-slate-500">
        <span>{participantCount.toLocaleString()} joined</span>
        <span>{capacity.toLocaleString()} capacity</span>
      </div>
      <div
        aria-label={`${percentage}% of event capacity filled`}
        aria-valuemax={100}
        aria-valuemin={0}
        aria-valuenow={percentage}
        className="h-2 overflow-hidden rounded-full bg-slate-100"
        role="progressbar"
      >
        <div
          className="h-full rounded-full bg-brand-500 transition-[width]"
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  );
}

