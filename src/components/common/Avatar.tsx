import type { ImgHTMLAttributes } from "react";

import { cn } from "@/utils/cn";

type AvatarSize = "sm" | "md" | "lg" | "xl";

const sizeClasses: Record<AvatarSize, string> = {
  sm: "size-8 text-xs",
  md: "size-10 text-sm",
  lg: "size-12 text-base",
  xl: "size-20 text-xl",
};

export interface AvatarProps
  extends Omit<ImgHTMLAttributes<HTMLImageElement>, "src"> {
  name: string;
  src?: string;
  size?: AvatarSize;
}

function getInitials(name: string) {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
}

export function Avatar({
  alt,
  className,
  name,
  size = "md",
  src,
  ...props
}: AvatarProps) {
  const baseClasses = cn(
    "inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full bg-brand-100 font-semibold text-brand-700 ring-2 ring-white",
    sizeClasses[size],
    className,
  );

  if (src) {
    return (
      <img
        alt={alt ?? name}
        className={cn(baseClasses, "object-cover")}
        src={src}
        {...props}
      />
    );
  }

  return (
    <span aria-label={name} className={baseClasses} role="img">
      {getInitials(name)}
    </span>
  );
}

