import { ChevronLeft, ChevronRight } from "lucide-react";
import { useEffect, useRef } from "react";

import { cn } from "@/utils/cn";

import type { DocSlide } from "./pdfSlides";

/** The document panel beside the whiteboard: the current slide + its points. */
export function SlidePanel({
  slides,
  current,
  title,
  focusLine,
  onGoTo,
  busy,
}: {
  slides: DocSlide[];
  current: number;
  title: string;
  focusLine: number | null;
  onGoTo: (index: number) => void;
  busy: boolean;
}) {
  const activeRef = useRef<HTMLLIElement>(null);

  useEffect(() => {
    activeRef.current?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }, [focusLine]);

  const slide = slides[current];
  if (!slide) return null;

  return (
    <aside className="flex h-[40vh] min-h-[16rem] flex-col gap-2 rounded-2xl border border-slate-200 bg-white p-2 shadow-sm lg:h-auto">
      <div className="flex items-center justify-between px-1">
        <span className="truncate text-xs font-semibold text-slate-600" title={title}>
          {title}
        </span>
        <span className="shrink-0 text-[11px] font-medium text-slate-400">
          Slide {current + 1} / {slides.length}
        </span>
      </div>

      {/* Current slide image */}
      <div className="relative overflow-hidden rounded-lg border border-slate-200 bg-slate-50">
        {slide.image ? (
          <img
            src={slide.image}
            alt={`Slide ${current + 1}`}
            className="mx-auto max-h-48 w-auto object-contain"
          />
        ) : (
          <div className="grid h-32 place-items-center text-xs text-slate-400">
            (no preview)
          </div>
        )}
      </div>

      {/* Nav */}
      <div className="flex items-center justify-between gap-2">
        <button
          type="button"
          disabled={busy || current === 0}
          onClick={() => onGoTo(current - 1)}
          className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2 py-1 text-xs text-slate-600 transition hover:border-brand-300 disabled:opacity-40"
        >
          <ChevronLeft className="size-3.5" />
          Prev
        </button>
        <div className="flex flex-1 items-center justify-center gap-1">
          {slides.map((_, i) => (
            <button
              key={i}
              type="button"
              disabled={busy}
              aria-label={`Go to slide ${i + 1}`}
              onClick={() => onGoTo(i)}
              className={cn(
                "size-1.5 rounded-full transition disabled:cursor-not-allowed",
                i === current ? "bg-brand-600" : "bg-slate-300 hover:bg-slate-400",
              )}
            />
          ))}
        </div>
        <button
          type="button"
          disabled={busy || current === slides.length - 1}
          onClick={() => onGoTo(current + 1)}
          className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2 py-1 text-xs text-slate-600 transition hover:border-brand-300 disabled:opacity-40"
        >
          Next
          <ChevronRight className="size-3.5" />
        </button>
      </div>

      {/* The slide's points */}
      <div className="min-h-0 flex-1 overflow-y-auto rounded-lg bg-slate-50 p-2">
        <p className="mb-1 text-[10px] font-semibold uppercase tracking-wide text-slate-400">
          On this slide
        </p>
        {slide.lines.length ? (
          <ol className="space-y-0.5 text-xs text-slate-700">
            {slide.lines.map((line, i) => {
              const active = focusLine === i + 1;
              return (
                <li
                  key={i}
                  ref={active ? activeRef : undefined}
                  className={cn(
                    "flex gap-1.5 rounded-md px-1.5 py-1 transition-colors",
                    active
                      ? "bg-amber-100 font-medium text-amber-900 ring-1 ring-amber-300"
                      : "",
                  )}
                >
                  <span
                    className={cn(
                      "shrink-0 font-semibold",
                      active ? "text-amber-600" : "text-brand-500",
                    )}
                  >
                    {i + 1}.
                  </span>
                  <span>{line}</span>
                </li>
              );
            })}
          </ol>
        ) : (
          <p className="text-xs text-slate-400">
            This slide is mostly a diagram/image — the tutor will teach what it
            shows.
          </p>
        )}
      </div>
    </aside>
  );
}
