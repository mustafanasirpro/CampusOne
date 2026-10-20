import { useId } from "react";
import { Link } from "react-router-dom";

import { paths } from "@/routes/paths";
import { cn } from "@/utils/cn";

export interface CampusOneLogoProps {
  className?: string;
  compact?: boolean;
  to?: string;
}

/**
 * The CampusOne mark: a maroon crescent "C" cradling a slender campus tower with
 * a star finial, standing on a quiet wave — rendered as crisp, themeable SVG.
 */
export function CampusOneMark({ className }: { className?: string }) {
  const uid = useId().replace(/[:]/g, "");
  const tower = `co-tower-${uid}`;
  const crescent = `co-crescent-${uid}`;
  return (
    <svg
      viewBox="0 0 44 44"
      className={className}
      role="img"
      aria-label="CampusOne"
    >
      <defs>
        <linearGradient id={tower} x1="22" y1="12" x2="22" y2="33" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#9d9c75" />
          <stop offset="1" stopColor="#343723" />
        </linearGradient>
        <linearGradient id={crescent} x1="8" y1="8" x2="36" y2="36" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#8f2f2b" />
          <stop offset="1" stopColor="#6a2120" />
        </linearGradient>
      </defs>

      {/* crescent C */}
      <path
        d="M31.4 9 A 16 16 0 1 0 31.4 35"
        fill="none"
        stroke={`url(#${crescent})`}
        strokeWidth="4.4"
        strokeLinecap="round"
      />

      {/* star finial */}
      <path
        d="M22 8.2 L22.9 11 L25.7 11.9 L22.9 12.8 L22 15.6 L21.1 12.8 L18.3 11.9 L21.1 11 Z"
        fill="#b8975a"
      />

      {/* campus towers */}
      <g fill={`url(#${tower})`}>
        <path d="M20.4 17.4 L22 15.3 L23.6 17.4 L23.6 31 L20.4 31 Z" />
        <rect x="16.1" y="22" width="2.6" height="9" rx="0.6" />
        <rect x="25.3" y="22" width="2.6" height="9" rx="0.6" />
      </g>

      {/* wave */}
      <path
        d="M14 32.4 C 17.5 30.6, 20 34, 22 33 C 24 32, 26.5 30.6, 30 32.4"
        fill="none"
        stroke="#4f5033"
        strokeWidth="2.1"
        strokeLinecap="round"
      />
    </svg>
  );
}

export function CampusOneLogo({
  className,
  compact = false,
  to = paths.landing,
}: CampusOneLogoProps) {
  return (
    <Link
      aria-label="CampusOne home"
      className={cn("inline-flex items-center gap-2.5", className)}
      to={to}
    >
      <CampusOneMark className="size-9 shrink-0" />
      {!compact ? (
        <span className="font-display text-[1.6rem] font-semibold leading-none tracking-[0.01em] text-slate-900">
          Campus<span className="text-brand-700">One</span>
        </span>
      ) : null}
    </Link>
  );
}
