import { useEffect, useRef, useState } from "react";

import { cn } from "@/utils/cn";
import type { BoardItem } from "@/types/tutor";

import { collectImageCandidates, type TopicImage } from "./topicImage";

interface WhiteboardProps {
  items: BoardItem[];
  writing: boolean;
  preparing?: boolean;
  preparedCount?: number;
}

/**
 * The live whiteboard. Board items flow top-to-bottom and animate in as the tutor
 * "writes" them. A "clear" item visually starts a fresh section (handled by the
 * console, which drops prior items), so here we just render the current items.
 */
export function Whiteboard({
  items,
  writing,
  preparing = false,
  preparedCount = 0,
}: WhiteboardProps) {
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [items.length]);

  return (
    <div className="relative h-full overflow-hidden rounded-2xl border border-slate-200 bg-[#fbfdff] shadow-inner">
      {/* subtle grid, like a real whiteboard */}
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 opacity-[0.5]"
        style={{
          backgroundImage:
            "linear-gradient(#e6eef7 1px, transparent 1px), linear-gradient(90deg, #e6eef7 1px, transparent 1px)",
          backgroundSize: "28px 28px",
        }}
      />
      <div className="relative h-full overflow-y-auto px-6 py-6 sm:px-10 sm:py-8">
        {items.length === 0 && preparing ? (
          <div className="flex h-full min-h-56 flex-col items-center justify-center gap-3 text-center">
            <span className="inline-block size-8 animate-spin rounded-full border-2 border-brand-200 border-t-brand-600" />
            <p className="text-sm font-medium text-slate-500">
              Preparing your lesson…
            </p>
            <p className="max-w-xs text-xs text-slate-400">
              The tutor plans the whole lesson first, then teaches it start to
              finish without stopping.
            </p>
            {preparedCount > 0 ? (
              <p className="text-xs font-medium text-brand-600">
                {preparedCount} point{preparedCount === 1 ? "" : "s"} ready…
              </p>
            ) : null}
          </div>
        ) : items.length === 0 ? (
          <div className="flex h-full min-h-56 items-center justify-center text-center">
            <p className="max-w-sm text-sm text-slate-400">
              The whiteboard is clear. Choose a topic and the lesson will appear
              here, written out step by step.
            </p>
          </div>
        ) : (
          <div className="mx-auto flex max-w-2xl flex-col gap-3">
            {items.map((item) => (
              <BoardBlock key={item.id} item={item} />
            ))}
            {writing || preparing ? (
              <WritingCursor
                label={
                  preparing
                    ? "Thinking about your question…"
                    : "Writing on the board…"
                }
              />
            ) : null}
            <div ref={endRef} />
          </div>
        )}
      </div>
    </div>
  );
}

function WritingCursor({ label = "Writing on the board…" }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 pt-1 text-slate-400">
      <span className="inline-block h-5 w-[2px] animate-pulse bg-brand-500" />
      <span className="text-xs">{label}</span>
    </div>
  );
}

function BoardBlock({ item }: { item: BoardItem }) {
  const answerRing = item.answer
    ? "ring-1 ring-amber-300/70 bg-amber-50/60"
    : "";

  switch (item.kind) {
    case "title":
      return (
        <h3 className="tutor-fade-in mt-2 border-b-2 border-brand-200 pb-1 font-handwritten text-2xl font-bold text-brand-800">
          {item.text}
        </h3>
      );

    case "text":
      return (
        <p
          className={cn(
            "tutor-fade-in rounded-lg px-1 font-handwritten text-lg leading-relaxed text-slate-800",
            answerRing && `${answerRing} px-3 py-1.5`,
          )}
        >
          {item.text}
        </p>
      );

    case "bullet":
      return (
        <div
          className={cn(
            "tutor-fade-in flex gap-2.5 rounded-lg px-1 font-handwritten text-lg leading-relaxed text-slate-800",
            answerRing && `${answerRing} px-3 py-1.5`,
          )}
        >
          <span className="mt-1 select-none text-brand-500">•</span>
          <span>{item.text}</span>
        </div>
      );

    case "equation":
      return (
        <div className="tutor-fade-in my-1 rounded-xl border border-slate-200 bg-white px-4 py-3 text-center font-mono text-lg text-slate-900 shadow-sm">
          {item.text}
        </div>
      );

    case "code":
      return (
        <pre className="tutor-fade-in overflow-x-auto rounded-xl border border-slate-800 bg-slate-950 px-4 py-3 text-sm leading-relaxed text-slate-100 shadow-sm">
          <code>{item.text}</code>
        </pre>
      );

    case "note":
      return (
        <div className="tutor-fade-in flex gap-2 rounded-xl border border-amber-300 bg-amber-50 px-4 py-2.5 font-handwritten text-base text-amber-900">
          <span aria-hidden>📌</span>
          <span>{item.text}</span>
        </div>
      );

    case "diagram":
      return <DiagramRow nodes={item.nodes ?? []} arrows={item.arrows ?? []} />;

    case "array":
      return (
        <ArrayVisual cells={item.cells ?? []} pointers={item.pointers ?? []} />
      );

    case "flow":
      return <FlowChart steps={item.steps ?? []} />;

    case "graph":
      return (
        <GraphPlot
          shape={item.shape ?? "line"}
          points={item.points ?? []}
          xLabel={item.xLabel}
          yLabel={item.yLabel}
        />
      );

    case "tree":
      return <TreeVisual nodes={item.nodes ?? []} />;

    case "image":
      return (
        <BoardImage
          query={item.query ?? item.text ?? ""}
          caption={item.text}
          generate={item.generate ?? false}
        />
      );

    case "clear":
      return null;

    default:
      return null;
  }
}

/** A row of array cells with index numbers and labeled pointers (low/mid/high). */
function ArrayVisual({
  cells,
  pointers,
}: {
  cells: (string | number)[];
  pointers: { label: string; at: number }[];
}) {
  if (cells.length === 0) return null;
  const n = cells.length;
  const cell = 46;
  const gap = 6;
  const padX = 12;
  const width = padX * 2 + n * cell + (n - 1) * gap;
  const topBand = pointers.length > 0 ? 34 : 8;
  const cellY = topBand;
  const height = cellY + cell + 20;
  const cellX = (i: number) => padX + i * (cell + gap);

  return (
    <div className="tutor-fade-in my-1 overflow-x-auto">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        width="100%"
        style={{ maxWidth: width, height: "auto" }}
        role="img"
      >
        {cells.map((value, i) => (
          <g key={i}>
            <rect
              x={cellX(i)}
              y={cellY}
              width={cell}
              height={cell}
              rx={6}
              fill="#fff"
              stroke="#94a3b8"
              strokeWidth={1.5}
            />
            <text
              x={cellX(i) + cell / 2}
              y={cellY + cell / 2 + 5}
              textAnchor="middle"
              fontSize={16}
              fontFamily="ui-monospace, monospace"
              fill="#0f172a"
            >
              {String(value)}
            </text>
            <text
              x={cellX(i) + cell / 2}
              y={cellY + cell + 14}
              textAnchor="middle"
              fontSize={11}
              fill="#94a3b8"
            >
              {i}
            </text>
          </g>
        ))}
        {pointers
          .filter((p) => p.at >= 0 && p.at < n)
          .map((p, k) => {
            const cx = cellX(p.at) + cell / 2;
            return (
              <g key={k} fill="#4f46e5">
                <text
                  x={cx}
                  y={12}
                  textAnchor="middle"
                  fontSize={12}
                  fontWeight={700}
                >
                  {p.label}
                </text>
                <path
                  d={`M ${cx} ${16} L ${cx} ${cellY - 2}`}
                  stroke="#4f46e5"
                  strokeWidth={1.5}
                  markerEnd="url(#arrow-down)"
                />
              </g>
            );
          })}
        <defs>
          <marker
            id="arrow-down"
            markerWidth={8}
            markerHeight={8}
            refX={4}
            refY={6}
            orient="auto"
          >
            <path d="M1,1 L4,6 L7,1" fill="none" stroke="#4f46e5" strokeWidth={1.5} />
          </marker>
        </defs>
      </svg>
    </div>
  );
}

/** Shows a teaching image — searched or generated — trying candidates in order. */
function BoardImage({
  query,
  caption,
  generate,
}: {
  query: string;
  caption?: string;
  generate: boolean;
}) {
  // null = resolving; [] = none found.
  const [candidates, setCandidates] = useState<TopicImage[] | null>(
    query.trim() ? null : [],
  );
  const [index, setIndex] = useState(0);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    if (!query.trim()) return;
    const controller = new AbortController();
    collectImageCandidates(query, generate, controller.signal)
      .then((list) => {
        setCandidates(list);
        setIndex(0);
        setLoaded(false);
      })
      .catch((error) => {
        // Ignore aborts (unmount / StrictMode remount) — only a real failure
        // means "no images found".
        if (error instanceof DOMException && error.name === "AbortError") return;
        setCandidates([]);
      });
    return () => controller.abort();
  }, [query, generate]);

  const current = candidates?.[index];

  // Per-candidate timeout: a source that hangs (slow/broken) advances to the next.
  // Shorter for searched images (prefetch usually has them cached already); longer
  // for a generated image, which legitimately takes several seconds to render.
  useEffect(() => {
    if (!current || loaded) return;
    const ms = current.generated ? 15000 : 6000;
    const timer = setTimeout(() => setIndex((i) => i + 1), ms);
    return () => clearTimeout(timer);
  }, [current, loaded]);

  // Resolving.
  if (candidates === null) {
    return (
      <figure className="tutor-fade-in my-1 flex flex-col items-center gap-1.5">
        <div className="flex h-40 w-full max-w-sm items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white text-sm text-slate-400 shadow-sm">
          <span className="inline-block size-5 animate-spin rounded-full border-2 border-brand-200 border-t-brand-600" />
          {generate ? "Creating an image…" : "Finding a picture…"}
        </div>
      </figure>
    );
  }

  // Exhausted all candidates — labeled placeholder so the beat still lands.
  if (!current) {
    return (
      <div className="tutor-fade-in my-1 flex items-center gap-2 rounded-xl border border-dashed border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-500">
        <span aria-hidden>🖼️</span>
        <span>{caption || query}</span>
      </div>
    );
  }

  return (
    <figure className="tutor-fade-in my-1 flex flex-col items-center gap-1.5">
      <div className="relative w-full max-w-sm overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        {!loaded ? (
          <div className="flex h-40 items-center justify-center gap-2 text-sm text-slate-400">
            <span className="inline-block size-5 animate-spin rounded-full border-2 border-brand-200 border-t-brand-600" />
            {current.generated ? "Creating an image…" : "Finding a picture…"}
          </div>
        ) : null}
        <img
          key={current.url}
          src={current.url}
          alt={caption || query}
          className={cn(
            "mx-auto max-h-64 w-auto object-contain",
            !loaded && "hidden",
          )}
          onLoad={() => setLoaded(true)}
          onError={() => setIndex((i) => i + 1)}
        />
      </div>
      {caption ? (
        <figcaption className="text-center font-handwritten text-sm text-slate-600">
          {caption}
        </figcaption>
      ) : null}
    </figure>
  );
}

/** A binary tree drawn from a level-order node list (children at 2i+1, 2i+2). */
function TreeVisual({ nodes }: { nodes: string[] }) {
  const isEmpty = (v: string | undefined) =>
    v === undefined || v === "" || v === "null" || v === "-" || v === "_";
  const present = nodes.map((v) => !isEmpty(v));
  if (!present.some(Boolean)) return null;

  const lastIndex = present.lastIndexOf(true);
  const maxLevel = Math.floor(Math.log2(lastIndex + 1)); // 0-based deepest level
  const r = 18;
  const levelH = 64;
  const bottomSlots = 2 ** maxLevel;
  const width = Math.max(220, bottomSlots * 52);
  const height = (maxLevel + 1) * levelH + r;

  const pos = (i: number) => {
    const level = Math.floor(Math.log2(i + 1));
    const slots = 2 ** level;
    const posInLevel = i - (slots - 1);
    const x = ((posInLevel + 0.5) / slots) * width;
    const y = level * levelH + r + 6;
    return { x, y };
  };

  return (
    <div className="tutor-fade-in my-1 flex justify-center overflow-x-auto">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        width="100%"
        style={{ maxWidth: width, height: "auto" }}
        role="img"
      >
        {/* edges first, so circles sit on top */}
        {nodes.map((_, i) => {
          if (!present[i] || i === 0) return null;
          const parent = Math.floor((i - 1) / 2);
          if (!present[parent]) return null;
          const a = pos(parent);
          const b = pos(i);
          return (
            <line
              key={`e${i}`}
              x1={a.x}
              y1={a.y}
              x2={b.x}
              y2={b.y}
              stroke="#94a3b8"
              strokeWidth={1.5}
            />
          );
        })}
        {nodes.map((value, i) => {
          if (!present[i]) return null;
          const { x, y } = pos(i);
          return (
            <g key={`n${i}`}>
              <circle cx={x} cy={y} r={r} fill="#eef2ff" stroke="#6366f1" strokeWidth={2} />
              <text
                x={x}
                y={y + 5}
                textAnchor="middle"
                fontSize={14}
                fontWeight={600}
                fill="#3730a3"
              >
                {value}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}

/** A vertical flowchart; a step ending in "?" renders as a decision diamond. */
function FlowChart({ steps }: { steps: string[] }) {
  const clean = steps.filter((s) => s && s.trim());
  if (clean.length === 0) return null;
  const w = 260;
  const boxH = 42;
  const gapY = 26;
  const rowH = boxH + gapY;
  const height = clean.length * rowH - gapY + 8;
  const cx = w / 2;

  return (
    <div className="tutor-fade-in my-1 flex justify-center overflow-x-auto">
      <svg
        viewBox={`0 0 ${w} ${height}`}
        width="100%"
        style={{ maxWidth: w, height: "auto" }}
        role="img"
      >
        <defs>
          <marker
            id="flow-arrow"
            markerWidth={10}
            markerHeight={10}
            refX={5}
            refY={8}
            orient="auto"
          >
            <path d="M1,1 L5,8 L9,1" fill="none" stroke="#64748b" strokeWidth={1.5} />
          </marker>
        </defs>
        {clean.map((step, i) => {
          const y = i * rowH;
          const decision = step.trim().endsWith("?");
          return (
            <g key={i}>
              {i > 0 ? (
                <path
                  d={`M ${cx} ${y - gapY + 2} L ${cx} ${y - 2}`}
                  stroke="#64748b"
                  strokeWidth={1.5}
                  markerEnd="url(#flow-arrow)"
                />
              ) : null}
              {decision ? (
                <polygon
                  points={`${cx},${y} ${cx + 120},${y + boxH / 2} ${cx},${y + boxH} ${cx - 120},${y + boxH / 2}`}
                  fill="#fef9c3"
                  stroke="#eab308"
                  strokeWidth={1.5}
                />
              ) : (
                <rect
                  x={cx - 115}
                  y={y}
                  width={230}
                  height={boxH}
                  rx={8}
                  fill="#eef2ff"
                  stroke="#818cf8"
                  strokeWidth={1.5}
                />
              )}
              <text
                x={cx}
                y={y + boxH / 2 + 4}
                textAnchor="middle"
                fontSize={12}
                fill="#1e293b"
              >
                {step.length > 34 ? `${step.slice(0, 33)}…` : step}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}

/** A simple coordinate plot: axes + a shaped line + optional labeled points. */
function GraphPlot({
  shape,
  points,
  xLabel,
  yLabel,
}: {
  shape: "line" | "up" | "down" | "parabola" | "wave";
  points: { x: number; y: number; label?: string }[];
  xLabel?: string;
  yLabel?: string;
}) {
  const w = 300;
  const h = 220;
  const m = 34; // margin for axes
  // Map data coords (0..10) to svg pixels.
  const px = (x: number) => m + (x / 10) * (w - m - 12);
  const py = (y: number) => h - m - (y / 10) * (h - m - 12);
  const curve = (() => {
    const pts: string[] = [];
    for (let i = 0; i <= 20; i += 1) {
      const x = (i / 20) * 10;
      let y: number;
      switch (shape) {
        case "up":
          y = x; // linear rising
          break;
        case "down":
          y = 10 - x;
          break;
        case "parabola":
          y = (Math.pow(x - 5, 2) / 25) * 10 * 0.9;
          break;
        case "wave":
          y = 5 + 3.5 * Math.sin((x / 10) * Math.PI * 2);
          break;
        default:
          y = x * 0.9 + 0.5; // line
      }
      pts.push(`${px(x)},${py(Math.max(0, Math.min(10, y)))}`);
    }
    return pts.join(" ");
  })();

  return (
    <div className="tutor-fade-in my-1 flex justify-center overflow-x-auto">
      <svg
        viewBox={`0 0 ${w} ${h}`}
        width="100%"
        style={{ maxWidth: w, height: "auto" }}
        role="img"
      >
        <defs>
          <marker id="axis-arrow" markerWidth={8} markerHeight={8} refX={6} refY={4} orient="auto">
            <path d="M1,1 L6,4 L1,7" fill="none" stroke="#475569" strokeWidth={1.5} />
          </marker>
        </defs>
        {/* axes */}
        <path d={`M ${m} ${h - m} L ${w - 6} ${h - m}`} stroke="#475569" strokeWidth={1.5} markerEnd="url(#axis-arrow)" />
        <path d={`M ${m} ${h - m} L ${m} ${6}`} stroke="#475569" strokeWidth={1.5} markerEnd="url(#axis-arrow)" />
        {xLabel ? (
          <text x={w - 10} y={h - m + 18} textAnchor="end" fontSize={11} fill="#64748b">
            {xLabel}
          </text>
        ) : null}
        {yLabel ? (
          <text x={m - 6} y={14} textAnchor="start" fontSize={11} fill="#64748b">
            {yLabel}
          </text>
        ) : null}
        <polyline points={curve} fill="none" stroke="#4f46e5" strokeWidth={2.5} />
        {points.map((p, i) => (
          <g key={i}>
            <circle cx={px(p.x)} cy={py(p.y)} r={4} fill="#4f46e5" />
            {p.label ? (
              <text x={px(p.x) + 6} y={py(p.y) - 6} fontSize={11} fill="#4f46e5">
                {p.label}
              </text>
            ) : null}
          </g>
        ))}
      </svg>
    </div>
  );
}

function DiagramRow({
  nodes,
  arrows,
}: {
  nodes: string[];
  arrows: [number, number][];
}) {
  if (nodes.length === 0) return null;
  // Adjacent connections render as inline arrows; any connection is at least shown
  // as a small marker so the relationship is visible without absolute positioning.
  const adjacent = new Set(
    arrows.filter(([a, b]) => Math.abs(a - b) === 1).map(([a, b]) => Math.min(a, b)),
  );
  return (
    <div className="tutor-fade-in my-1 flex flex-wrap items-center justify-center gap-1.5">
      {nodes.map((node, index) => (
        <div key={index} className="flex items-center gap-1.5">
          <div className="grid min-w-14 place-items-center rounded-lg border-2 border-brand-300 bg-white px-3 py-2 font-mono text-sm font-semibold text-brand-800 shadow-sm">
            {node}
          </div>
          {index < nodes.length - 1 ? (
            <span
              className={cn(
                "select-none text-lg",
                adjacent.has(index) ? "text-brand-500" : "text-slate-300",
              )}
            >
              →
            </span>
          ) : null}
        </div>
      ))}
    </div>
  );
}
