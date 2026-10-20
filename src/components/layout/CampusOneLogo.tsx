import { Link } from "react-router-dom";

import { paths } from "@/routes/paths";
import { cn } from "@/utils/cn";

export interface CampusOneLogoProps {
  className?: string;
  compact?: boolean;
  to?: string;
}

/** The CampusOne emblem (crescent C + campus tower) — the brand's actual mark. */
export function CampusOneMark({ className }: { className?: string }) {
  return (
    <img
      src="/brand/mark.png"
      alt=""
      aria-hidden
      className={cn("w-auto object-contain", className)}
      draggable={false}
    />
  );
}

/** The full CampusOne lockup: emblem + the "CampusOne" wordmark. */
export function CampusOneLogo({
  className,
  compact = false,
  to = paths.landing,
}: CampusOneLogoProps) {
  return (
    <Link
      aria-label="CampusOne home"
      className={cn("inline-flex items-center gap-2", className)}
      to={to}
    >
      <CampusOneMark className="h-9 shrink-0 sm:h-10" />
      {!compact ? (
        <img
          src="/brand/wordmark.png"
          alt=""
          aria-hidden
          className="h-[22px] w-auto object-contain sm:h-6"
          draggable={false}
        />
      ) : null}
    </Link>
  );
}
