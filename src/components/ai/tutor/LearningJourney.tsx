import { FileText, Flame, RotateCw, Sparkles, Trash2, Wand2 } from "lucide-react";
import { useMemo, useState } from "react";

import { suggestNextTopics } from "@/api/tutorClient";
import { cn } from "@/utils/cn";

import {
  clearJourney,
  loadJourney,
  relativeDay,
  studyStreak,
  type JourneyEntry,
} from "./journey";

// The futuristic piece: the tutor REMEMBERS you. Everything you've learned becomes
// a living constellation — a map of your knowledge that grows each session — and
// the model suggests what to explore next from where you've been. All local & free.

// Deterministic 0..1 hash so each topic always lands in the same spot on the map.
function hash01(s: string): number {
  let h = 2166136261;
  for (let i = 0; i < s.length; i += 1) {
    h ^= s.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return ((h >>> 0) % 10000) / 10000;
}

interface Placed extends JourneyEntry {
  x: number;
  y: number;
  r: number;
  recent: number; // 0 (old) .. 1 (newest)
}

function placeNodes(entries: JourneyEntry[]): Placed[] {
  // Oldest → newest, left to right, on a gentle wave so it reads as a journey.
  const ordered = [...entries]
    .sort((a, b) => Date.parse(a.lastLearned) - Date.parse(b.lastLearned))
    .slice(-14);
  const n = ordered.length;
  const now = Date.now();
  const spans = ordered.map((e) => now - Date.parse(e.lastLearned));
  const maxSpan = Math.max(1, ...spans);
  return ordered.map((e, i) => {
    const t = n === 1 ? 0.5 : i / (n - 1);
    const jitter = (hash01(e.topic) - 0.5) * 34;
    return {
      ...e,
      x: 40 + t * 620,
      y: 90 + Math.sin(t * Math.PI * 1.6) * 44 + jitter,
      r: 7 + Math.min(6, (e.timesLearned - 1) * 2.5),
      recent: 1 - (now - Date.parse(e.lastLearned)) / maxSpan,
    };
  });
}

export function LearningJourney({
  onPick,
  disabled,
}: {
  onPick: (topic: string) => void;
  disabled: boolean;
}) {
  const [entries, setEntries] = useState<JourneyEntry[]>(() => loadJourney());
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [loadingNext, setLoadingNext] = useState(false);
  const [nextError, setNextError] = useState(false);

  const nodes = useMemo(() => placeNodes(entries), [entries]);
  const streak = useMemo(() => studyStreak(entries), [entries]);
  const recent = useMemo(
    () =>
      [...entries]
        .sort((a, b) => Date.parse(b.lastLearned) - Date.parse(a.lastLearned))
        .slice(0, 5),
    [entries],
  );

  if (entries.length === 0) return null; // nothing to show for a brand-new student

  const handleSuggest = async () => {
    setLoadingNext(true);
    setNextError(false);
    try {
      const next = await suggestNextTopics(entries.map((e) => e.topic));
      setSuggestions(next);
      if (next.length === 0) setNextError(true);
    } catch {
      setNextError(true);
    } finally {
      setLoadingNext(false);
    }
  };

  const handleClear = () => {
    if (!window.confirm("Clear your whole learning journey? This can't be undone."))
      return;
    clearJourney();
    setEntries([]);
    setSuggestions([]);
  };

  return (
    <div className="grid gap-3 rounded-2xl border border-slate-200 bg-gradient-to-br from-slate-50 to-white p-5 shadow-sm">
      <div className="flex flex-wrap items-center gap-2">
        <span className="grid size-8 place-items-center rounded-xl bg-brand-600 text-white">
          <Sparkles className="size-4" />
        </span>
        <div className="mr-auto">
          <h3 className="text-sm font-semibold text-slate-800">
            Your learning journey
          </h3>
          <p className="text-xs text-slate-500">
            {entries.length} {entries.length === 1 ? "topic" : "topics"} mastered
            {streak >= 2 ? (
              <span className="ml-1 inline-flex items-center gap-0.5 font-medium text-amber-600">
                · <Flame className="size-3" />
                {streak}-day streak
              </span>
            ) : null}
          </p>
        </div>
        <button
          type="button"
          onClick={handleClear}
          title="Clear your journey"
          className="inline-flex items-center gap-1 rounded-lg border border-slate-200 px-2 py-1 text-xs text-slate-400 transition hover:border-red-300 hover:text-red-500"
        >
          <Trash2 className="size-3.5" />
          Clear
        </button>
      </div>

      {/* The constellation of everything learned. */}
      <div className="overflow-hidden rounded-xl border border-slate-100 bg-slate-950/[0.03]">
        <svg
          viewBox="0 0 700 190"
          className="h-auto w-full"
          role="img"
          aria-label="A map of the topics you've learned"
        >
          {/* connecting path (the route you've travelled) */}
          <polyline
            points={nodes.map((n) => `${n.x},${n.y}`).join(" ")}
            fill="none"
            stroke="rgb(99 102 241 / 0.25)"
            strokeWidth={1.5}
            strokeDasharray="4 4"
          />
          {nodes.map((n) => {
            const brand = n.kind === "document";
            const fill = brand
              ? `rgb(16 185 129 / ${0.35 + n.recent * 0.6})`
              : `rgb(79 70 229 / ${0.35 + n.recent * 0.6})`;
            return (
              <g
                key={n.topic}
                className={cn(
                  "cursor-pointer",
                  disabled && "pointer-events-none opacity-60",
                )}
                onClick={() => !disabled && onPick(n.topic)}
              >
                <title>
                  {n.topic} · {relativeDay(n.lastLearned)}
                  {n.timesLearned > 1 ? ` · ${n.timesLearned}×` : ""}
                </title>
                {n.recent > 0.75 ? (
                  <circle cx={n.x} cy={n.y} r={n.r + 6} fill={fill} opacity={0.25} />
                ) : null}
                <circle
                  cx={n.x}
                  cy={n.y}
                  r={n.r}
                  fill={fill}
                  stroke="white"
                  strokeWidth={1.5}
                />
              </g>
            );
          })}
        </svg>
      </div>

      {/* Recent, as clear clickable chips (click = learn it again). */}
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-xs font-medium text-slate-400">Recent:</span>
        {recent.map((e) => (
          <button
            key={e.topic}
            type="button"
            disabled={disabled}
            onClick={() => onPick(e.topic)}
            title={`Learn again · ${relativeDay(e.lastLearned)}`}
            className="group inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-white px-3 py-1 text-xs text-slate-600 transition hover:border-brand-300 hover:text-brand-700 disabled:opacity-50"
          >
            {e.kind === "document" ? (
              <FileText className="size-3 text-campus-500" />
            ) : (
              <RotateCw className="size-3 text-slate-300 group-hover:text-brand-500" />
            )}
            <span className="max-w-[12rem] truncate">{e.topic}</span>
          </button>
        ))}
      </div>

      {/* AI picks what to learn next, based on the journey. */}
      <div className="flex flex-wrap items-center gap-2 border-t border-slate-100 pt-3">
        <button
          type="button"
          disabled={disabled || loadingNext}
          onClick={handleSuggest}
          className="inline-flex items-center gap-1.5 rounded-full border border-brand-300 bg-brand-50 px-3 py-1 text-xs font-medium text-brand-700 transition hover:bg-brand-100 disabled:opacity-60"
        >
          {loadingNext ? (
            <span className="inline-block size-3 animate-spin rounded-full border-2 border-brand-300 border-t-brand-600" />
          ) : (
            <Wand2 className="size-3.5" />
          )}
          {loadingNext ? "Thinking…" : "Suggest what to learn next"}
        </button>
        {suggestions.map((s) => (
          <button
            key={s}
            type="button"
            disabled={disabled}
            onClick={() => onPick(s)}
            className="tutor-fade-in inline-flex items-center gap-1 rounded-full border border-campus-200 bg-campus-50 px-3 py-1 text-xs font-medium text-campus-700 transition hover:bg-campus-100 disabled:opacity-50"
          >
            <Sparkles className="size-3" />
            {s}
          </button>
        ))}
        {nextError ? (
          <span className="text-xs text-slate-400">
            Couldn't fetch ideas — try again in a moment.
          </span>
        ) : null}
      </div>
    </div>
  );
}
