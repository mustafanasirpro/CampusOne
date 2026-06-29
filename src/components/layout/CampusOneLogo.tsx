import { Sparkles } from "lucide-react";
import { Link } from "react-router-dom";

import { paths } from "@/routes/paths";
import { cn } from "@/utils/cn";

export interface CampusOneLogoProps {
  className?: string;
  compact?: boolean;
  to?: string;
}

export function CampusOneLogo({
  className,
  compact = false,
  to = paths.landing,
}: CampusOneLogoProps) {
  return (
    <Link
      aria-label="CampusOne home"
      className={cn("inline-flex items-center gap-3", className)}
      to={to}
    >
      <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-600 text-white shadow-lg shadow-brand-600/20">
        <Sparkles className="size-5" />
      </span>
      {!compact ? (
        <span className="text-lg font-bold tracking-tight text-slate-950">
          Campus<span className="text-brand-600">One</span>
        </span>
      ) : null}
    </Link>
  );
}

