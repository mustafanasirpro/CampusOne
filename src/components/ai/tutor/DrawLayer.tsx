import { Eraser, Pencil, Trash2, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { cn } from "@/utils/cn";

const COLORS = ["#4f46e5", "#ef4444", "#16a34a", "#f59e0b", "#0f172a"];

/**
 * A transparent canvas layered over the whiteboard so the student can draw,
 * circle, and annotate. When off, it lets clicks/scroll pass through; when on,
 * it captures pointer input (and blocks scroll) so you can draw freely.
 */
export function DrawLayer() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [active, setActive] = useState(false);
  const [color, setColor] = useState(COLORS[0]);
  const [erasing, setErasing] = useState(false);
  const drawing = useRef(false);
  const last = useRef<{ x: number; y: number } | null>(null);

  // Keep the canvas backing store matched to its displayed size (crisp lines).
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const resize = () => {
      const rect = canvas.getBoundingClientRect();
      const dpr = window.devicePixelRatio || 1;
      // Preserve existing drawing across resizes.
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
        if (prev.width && prev.height) {
          ctx.drawImage(prev, 0, 0, rect.width, rect.height);
        }
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
      ctx.globalCompositeOperation = erasing ? "destination-out" : "source-over";
      ctx.strokeStyle = color;
      ctx.lineWidth = erasing ? 22 : 3;
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
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext("2d");
    if (canvas && ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
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
          "absolute inset-0 z-10 h-full w-full rounded-2xl",
          active ? "cursor-crosshair touch-none" : "pointer-events-none",
        )}
      />

      {/* Toolbar */}
      <div className="absolute right-2 top-2 z-20 flex items-center gap-1 rounded-xl border border-slate-200 bg-white/95 p-1 shadow-sm backdrop-blur">
        {active ? (
          <>
            {COLORS.map((c) => (
              <button
                key={c}
                type="button"
                aria-label={`Pen color ${c}`}
                onClick={() => {
                  setColor(c);
                  setErasing(false);
                }}
                className={cn(
                  "size-5 rounded-full border transition",
                  color === c && !erasing
                    ? "scale-110 border-slate-900"
                    : "border-white",
                )}
                style={{ backgroundColor: c }}
              />
            ))}
            <button
              type="button"
              title="Eraser"
              onClick={() => setErasing((e) => !e)}
              className={cn(
                "grid size-7 place-items-center rounded-lg transition",
                erasing ? "bg-brand-100 text-brand-700" : "text-slate-500 hover:bg-slate-100",
              )}
            >
              <Eraser className="size-4" />
            </button>
            <button
              type="button"
              title="Clear drawing"
              onClick={clear}
              className="grid size-7 place-items-center rounded-lg text-slate-500 hover:bg-slate-100"
            >
              <Trash2 className="size-4" />
            </button>
            <button
              type="button"
              title="Done drawing"
              onClick={() => setActive(false)}
              className="grid size-7 place-items-center rounded-lg text-slate-500 hover:bg-slate-100"
            >
              <X className="size-4" />
            </button>
          </>
        ) : (
          <button
            type="button"
            title="Draw on the board"
            onClick={() => setActive(true)}
            className="grid size-7 place-items-center rounded-lg text-slate-500 hover:bg-slate-100"
          >
            <Pencil className="size-4" />
          </button>
        )}
      </div>
    </>
  );
}
