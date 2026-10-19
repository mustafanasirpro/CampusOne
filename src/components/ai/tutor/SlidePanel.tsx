import {
  ChevronLeft,
  ChevronRight,
  GraduationCap,
  Highlighter,
  Trash2,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { Button } from "@/components/common";
import { cn } from "@/utils/cn";

import type { DocSlide } from "./pdfSlides";

/** The document panel beside the whiteboard: the current slide + its points. */
export function SlidePanel({
  slides,
  current,
  title,
  focusLine,
  onView,
  onTeach,
  busy,
  className,
}: {
  slides: DocSlide[];
  current: number;
  title: string;
  focusLine: number | null;
  /** Just show a slide (free navigation). */
  onView: (index: number) => void;
  /** Start teaching a slide. */
  onTeach: (index: number) => void;
  busy: boolean;
  className?: string;
}) {
  const activeRef = useRef<HTMLLIElement>(null);
  const [jump, setJump] = useState("");

  useEffect(() => {
    activeRef.current?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }, [focusLine]);

  const slide = slides[current];
  if (!slide) return null;

  const go = (i: number) => {
    if (i >= 0 && i < slides.length && !busy) onView(i);
  };
  const submitJump = () => {
    const n = Number(jump);
    if (Number.isFinite(n)) go(Math.round(n) - 1);
    setJump("");
  };

  return (
    <aside
      className={cn(
        "flex h-[46vh] min-h-[18rem] flex-col gap-2 rounded-2xl border border-slate-200 bg-white p-2 shadow-sm lg:h-auto",
        className,
      )}
    >
      <div className="flex items-center justify-between gap-2 px-1">
        <span className="truncate text-xs font-semibold text-slate-600" title={title}>
          {title}
        </span>
        <span className="shrink-0 text-[11px] font-medium text-slate-400">
          Slide {current + 1} / {slides.length}
        </span>
      </div>

      {/* Current slide — shown in FULL (a rendered PDF page, or a slide-styled
          preview for PPTX). You can highlight directly on it. */}
      <div className="relative min-h-0 flex-1 overflow-hidden rounded-lg border border-slate-200 bg-slate-100">
        <div className="absolute inset-0 overflow-auto">
          {slide.image ? (
            <img
              src={slide.image}
              alt={`Slide ${current + 1}`}
              className="mx-auto h-auto w-full object-contain"
            />
          ) : (
            <SlideTextPreview lines={slide.lines} focusLine={focusLine} />
          )}
        </div>
        <SlideHighlighter key={current} />
      </div>

      {/* Nav — free navigation (just shows the slide; it doesn't start teaching). */}
      <div className="flex items-center justify-between gap-2">
        <button
          type="button"
          disabled={busy || current === 0}
          onClick={() => go(current - 1)}
          className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2 py-1 text-xs text-slate-600 transition hover:border-brand-300 disabled:opacity-40"
        >
          <ChevronLeft className="size-3.5" />
          Prev
        </button>
        <form
          className="flex items-center gap-1"
          onSubmit={(e) => {
            e.preventDefault();
            submitJump();
          }}
        >
          <input
            value={jump}
            onChange={(e) => setJump(e.target.value.replace(/[^0-9]/g, ""))}
            placeholder="Go to #"
            aria-label="Jump to slide number"
            className="h-7 w-16 rounded-md border border-slate-200 px-2 text-center text-xs outline-none focus-visible:border-brand-400"
          />
        </form>
        <button
          type="button"
          disabled={busy || current === slides.length - 1}
          onClick={() => go(current + 1)}
          className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2 py-1 text-xs text-slate-600 transition hover:border-brand-300 disabled:opacity-40"
        >
          Next
          <ChevronRight className="size-3.5" />
        </button>
      </div>

      {/* Teach this slide — the student controls when teaching starts. */}
      <Button
        variant="primary"
        size="sm"
        disabled={busy}
        onClick={() => onTeach(current)}
        className="w-full justify-center"
      >
        <GraduationCap className="size-4" />
        Teach this slide
      </Button>

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

/**
 * A slide-styled thumbnail built from a .pptx slide's extracted text (the first
 * line as the title, the rest as bullets), since PowerPoint can't be rendered to
 * an image in the browser. The line currently being taught is highlighted so the
 * preview tracks the lesson, just like the points list below it.
 */
function SlideTextPreview({
  lines,
  focusLine,
}: {
  lines: string[];
  focusLine: number | null;
}) {
  if (lines.length === 0) {
    return (
      <div className="grid h-32 place-items-center px-3 text-center text-xs text-slate-400">
        This slide is mostly a diagram or image.
      </div>
    );
  }
  const [title, ...bullets] = lines;
  const titleActive = focusLine === 1;
  return (
    <div className="aspect-[16/10] w-full bg-gradient-to-br from-white to-slate-100 p-3">
      <div className="flex h-full flex-col overflow-hidden rounded-md bg-white/70 p-3 shadow-inner ring-1 ring-slate-200">
        <p
          className={cn(
            "mb-1.5 line-clamp-2 border-b border-slate-200 pb-1 text-[13px] font-bold leading-tight",
            titleActive ? "text-amber-700" : "text-slate-800",
          )}
        >
          {title}
        </p>
        <ul className="min-h-0 flex-1 space-y-1 overflow-hidden">
          {bullets.slice(0, 6).map((b, i) => {
            const active = focusLine === i + 2; // line 1 is the title
            return (
              <li
                key={i}
                className={cn(
                  "flex gap-1.5 text-[11px] leading-snug",
                  active ? "font-semibold text-amber-700" : "text-slate-600",
                )}
              >
                <span className={active ? "text-amber-500" : "text-brand-400"}>•</span>
                <span className="line-clamp-1">{b}</span>
              </li>
            );
          })}
          {bullets.length > 6 ? (
            <li className="pl-3 text-[10px] text-slate-400">
              +{bullets.length - 6} more…
            </li>
          ) : null}
        </ul>
      </div>
    </div>
  );
}

/** A translucent highlighter the student can draw on the slide to mark things. */
function SlideHighlighter() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [active, setActive] = useState(false);
  const drawing = useRef(false);
  const last = useRef<{ x: number; y: number } | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const resize = () => {
      const rect = canvas.getBoundingClientRect();
      const dpr = window.devicePixelRatio || 1;
      const prev = document.createElement("canvas");
      prev.width = canvas.width;
      prev.height = canvas.height;
      prev.getContext("2d")?.drawImage(canvas, 0, 0);
      canvas.width = Math.max(1, Math.round(rect.width * dpr));
      canvas.height = Math.max(1, Math.round(rect.height * dpr));
      const ctx = canvas.getContext("2d");
      if (ctx) {
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.lineCap = "round";
        ctx.lineJoin = "round";
        if (prev.width && prev.height) ctx.drawImage(prev, 0, 0, rect.width, rect.height);
      }
    };
    resize();
    const observer = new ResizeObserver(resize);
    observer.observe(canvas);
    return () => observer.disconnect();
  }, []);

  const point = (event: React.PointerEvent) => {
    const rect = canvasRef.current!.getBoundingClientRect();
    return { x: event.clientX - rect.left, y: event.clientY - rect.top };
  };
  const onDown = (event: React.PointerEvent) => {
    if (!active) return;
    canvasRef.current?.setPointerCapture(event.pointerId);
    drawing.current = true;
    last.current = point(event);
  };
  const onMove = (event: React.PointerEvent) => {
    if (!active || !drawing.current) return;
    const ctx = canvasRef.current?.getContext("2d");
    const p = point(event);
    if (ctx && last.current) {
      ctx.globalCompositeOperation = "source-over";
      ctx.strokeStyle = "rgba(250, 204, 21, 0.45)"; // soft yellow highlighter
      ctx.lineWidth = 14;
      ctx.beginPath();
      ctx.moveTo(last.current.x, last.current.y);
      ctx.lineTo(p.x, p.y);
      ctx.stroke();
    }
    last.current = p;
  };
  const onUp = () => {
    drawing.current = false;
    last.current = null;
  };
  const clear = () => {
    const c = canvasRef.current;
    const ctx = c?.getContext("2d");
    if (c && ctx) ctx.clearRect(0, 0, c.width, c.height);
  };

  return (
    <>
      <canvas
        ref={canvasRef}
        onPointerDown={onDown}
        onPointerMove={onMove}
        onPointerUp={onUp}
        onPointerLeave={onUp}
        className={cn(
          "absolute inset-0 z-10 h-full w-full",
          active ? "cursor-crosshair touch-none" : "pointer-events-none",
        )}
      />
      <div className="absolute right-1.5 top-1.5 z-20 flex items-center gap-1 rounded-lg border border-slate-200 bg-white/95 p-0.5 shadow-sm backdrop-blur">
        <button
          type="button"
          title="Highlight on the slide"
          onClick={() => setActive((a) => !a)}
          className={cn(
            "grid size-6 place-items-center rounded-md transition",
            active ? "bg-amber-100 text-amber-700" : "text-slate-500 hover:bg-slate-100",
          )}
        >
          <Highlighter className="size-3.5" />
        </button>
        {active ? (
          <button
            type="button"
            title="Clear highlights"
            onClick={clear}
            className="grid size-6 place-items-center rounded-md text-slate-500 hover:bg-slate-100"
          >
            <Trash2 className="size-3.5" />
          </button>
        ) : null}
      </div>
    </>
  );
}
