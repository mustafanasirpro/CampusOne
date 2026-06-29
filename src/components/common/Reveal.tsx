import { useEffect, useRef, type HTMLAttributes } from "react";

import { cn } from "@/utils/cn";

export interface RevealProps extends HTMLAttributes<HTMLDivElement> {
  delay?: number;
}

export function Reveal({
  children,
  className,
  delay = 0,
  style,
  ...props
}: RevealProps) {
  const elementRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const element = elementRef.current;
    if (!element) return;

    if (
      window.matchMedia("(prefers-reduced-motion: reduce)").matches ||
      !("IntersectionObserver" in window)
    ) {
      return;
    }

    const bounds = element.getBoundingClientRect();
    const isInitiallyVisible =
      bounds.bottom > 0 && bounds.top < window.innerHeight * 0.92;

    if (isInitiallyVisible) return;

    element.classList.add("reveal-pending");

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("reveal-visible");
          observer.unobserve(entry.target);
        }
      },
      { rootMargin: "0px 0px -8% 0px", threshold: 0.12 },
    );

    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  return (
    <div
      className={cn("reveal", className)}
      ref={elementRef}
      style={{ ...style, transitionDelay: `${delay}ms` }}
      {...props}
    >
      {children}
    </div>
  );
}
