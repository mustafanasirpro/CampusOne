import { cn } from "@/utils/cn";

export function Skeleton({
  className,
}: {
  className?: string;
}) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        "block animate-pulse rounded-lg bg-slate-200/80",
        className,
      )}
    />
  );
}

export function PageLoadingState({ routePath }: { routePath: string }) {
  const isListRoute = [
    "/notes",
    "/discussions",
    "/marketplace",
    "/internships",
    "/events",
    "/leaderboard",
  ].includes(routePath);

  return (
    <div
      aria-busy="true"
      aria-label="Loading page"
      className="grid animate-content-in gap-8 pb-8"
      role="status"
    >
      <span className="sr-only">Loading CampusOne content…</span>
      <div className="flex items-end justify-between gap-4">
        <div className="grid flex-1 gap-3">
          <Skeleton className="h-3 w-28" />
          <Skeleton className="h-8 w-56 max-w-full" />
          <Skeleton className="h-4 w-[32rem] max-w-full" />
        </div>
        <Skeleton className="hidden h-10 w-32 sm:block" />
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }, (_, index) => (
          <div
            className="rounded-2xl border border-slate-200 bg-white p-5 shadow-card"
            key={index}
          >
            <div className="flex items-start justify-between">
              <div className="grid gap-3">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-7 w-16" />
                <Skeleton className="h-3 w-20" />
              </div>
              <Skeleton className="size-11 rounded-xl" />
            </div>
          </div>
        ))}
      </div>

      <div
        className={cn(
          "grid gap-4",
          isListRoute ? "md:grid-cols-2 xl:grid-cols-3" : "xl:grid-cols-2",
        )}
      >
        {Array.from({ length: isListRoute ? 6 : 4 }, (_, index) => (
          <div
            className="rounded-2xl border border-slate-200 bg-white p-5 shadow-card"
            key={index}
          >
            <div className="flex gap-3">
              <Skeleton className="size-11 shrink-0 rounded-xl" />
              <div className="grid flex-1 gap-2">
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-1/2" />
              </div>
            </div>
            <Skeleton className="mt-5 h-3 w-full" />
            <Skeleton className="mt-2 h-3 w-5/6" />
            <Skeleton className="mt-5 h-9 w-full" />
          </div>
        ))}
      </div>
    </div>
  );
}
