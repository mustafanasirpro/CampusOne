import { BookOpen, Code2, Play, RotateCcw, Terminal } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";

import { Badge, Button, Card, CardContent, PageHeader } from "@/components/common";
// Core logic lives in plain JavaScript modules (see src/features/learn/*.js).
import { tracks } from "@/features/learn/curriculum";
import { handleEditorKeydown } from "@/features/learn/editor";
import { listenForConsole, runInIframe } from "@/features/learn/sandboxRunner";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

interface LessonBlock {
  kind: "text" | "tip" | "code";
  text?: string;
  code?: string;
  lang?: string;
}
interface Example {
  html: string;
  css: string;
  js: string;
}
interface Lesson {
  id: string;
  title: string;
  blocks: LessonBlock[];
  example: Example;
}
interface Track {
  id: string;
  label: string;
  tagline: string;
  lessons: Lesson[];
}

const TRACKS = tracks as Track[];
type Pane = "html" | "css" | "js";
interface ConsoleEntry {
  level: string;
  text: string;
}

export function LearnPage() {
  useDocumentTitle("Learn");

  const [trackId, setTrackId] = useState(TRACKS[0].id);
  const track = useMemo(() => TRACKS.find((t) => t.id === trackId) ?? TRACKS[0], [trackId]);
  const [lessonId, setLessonId] = useState(track.lessons[0].id);
  const lesson = useMemo(
    () => track.lessons.find((l) => l.id === lessonId) ?? track.lessons[0],
    [track, lessonId],
  );

  const [code, setCode] = useState<Example>(lesson.example);
  const [pane, setPane] = useState<Pane>("html");
  const [consoleEntries, setConsoleEntries] = useState<ConsoleEntry[]>([]);
  const iframeRef = useRef<HTMLIFrameElement>(null);

  // Switching lesson/track loads that example into the editor (event-driven so
  // we never reset state from inside an effect).
  const openLesson = (nextLesson: Lesson) => {
    setLessonId(nextLesson.id);
    setCode(nextLesson.example);
    setPane("html");
    setConsoleEntries([]);
  };
  const selectTrack = (nextTrackId: string) => {
    const nextTrack = TRACKS.find((t) => t.id === nextTrackId) ?? TRACKS[0];
    setTrackId(nextTrackId);
    openLesson(nextTrack.lessons[0]);
  };

  // Listen for console output streamed back from the sandbox iframe.
  useEffect(() => listenForConsole((entry: ConsoleEntry) => {
    setConsoleEntries((prev) => [...prev, entry].slice(-50));
  }), []);

  // Run the current code (re-runs automatically when the loaded example changes).
  const run = () => {
    setConsoleEntries([]);
    runInIframe(iframeRef.current, code);
  };
  useEffect(() => {
    const id = window.setTimeout(run, 60);
    return () => window.clearTimeout(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [code]);

  const updatePane = (value: string) => setCode((prev) => ({ ...prev, [pane]: value }));

  return (
    <div className="grid gap-6 pb-10">
      <PageHeader
        description="Interactive HTML, CSS and JavaScript tutorials — read a lesson, then edit the code live and run it, right here."
        eyebrow="CampusOne Learn"
        title="Learn to build"
      />

      {/* Track tabs */}
      <div className="flex flex-wrap gap-2">
        {TRACKS.map((t) => (
          <button
            className={cn(
              "inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold transition",
              t.id === trackId
                ? "bg-brand-600 text-white"
                : "bg-slate-100 text-slate-600 hover:bg-slate-200",
            )}
            key={t.id}
            onClick={() => selectTrack(t.id)}
            type="button"
          >
            <Code2 className="size-4" />
            {t.label}
          </button>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-[220px_1fr]">
        {/* Lesson sidebar */}
        <aside className="grid h-fit gap-1 rounded-2xl border border-slate-200 bg-white p-2">
          <p className="px-3 py-2 text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">
            {track.label} tutorial
          </p>
          {track.lessons.map((l) => (
            <button
              className={cn(
                "flex items-center gap-2 rounded-xl px-3 py-2 text-left text-sm font-medium transition",
                l.id === lessonId
                  ? "bg-brand-50 text-brand-700"
                  : "text-slate-600 hover:bg-slate-50",
              )}
              key={l.id}
              onClick={() => openLesson(l)}
              type="button"
            >
              <BookOpen className="size-4 shrink-0" />
              {l.title}
            </button>
          ))}
        </aside>

        {/* Lesson content + editor */}
        <div className="grid gap-5">
          <div>
            <p className="text-sm font-medium text-brand-600">{track.tagline}</p>
            <h2 className="font-display text-2xl font-semibold text-slate-900">{lesson.title}</h2>
          </div>

          <div className="grid gap-3">
            {lesson.blocks.map((block, index) => {
              if (block.kind === "code") {
                return (
                  <pre
                    className="overflow-x-auto rounded-xl bg-slate-900 p-4 text-sm text-slate-100"
                    key={index}
                  >
                    <code>{block.code}</code>
                  </pre>
                );
              }
              if (block.kind === "tip") {
                return (
                  <div
                    className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
                    key={index}
                  >
                    💡 {block.text}
                  </div>
                );
              }
              return (
                <p className="text-[15px] leading-7 text-slate-600" key={index}>
                  {block.text}
                </p>
              );
            })}
          </div>

          {/* Try it Yourself */}
          <Card>
            <CardContent className="grid gap-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2 font-semibold text-slate-900">
                  <Play className="size-4 text-emerald-600" />
                  Try it Yourself
                </div>
                <div className="flex items-center gap-2">
                  <Button onClick={() => setCode(lesson.example)} variant="ghost">
                    <RotateCcw className="size-4" /> Reset
                  </Button>
                  <Button onClick={run}>
                    <Play className="size-4" /> Run
                  </Button>
                </div>
              </div>

              <div className="grid gap-4 lg:grid-cols-2">
                {/* Editor */}
                <div className="overflow-hidden rounded-xl border border-slate-200">
                  <div className="flex border-b border-slate-200 bg-slate-50">
                    {(["html", "css", "js"] as Pane[]).map((key) => (
                      <button
                        className={cn(
                          "px-4 py-2 text-xs font-semibold uppercase tracking-wide transition",
                          pane === key
                            ? "bg-white text-brand-700"
                            : "text-slate-500 hover:text-slate-800",
                        )}
                        key={key}
                        onClick={() => setPane(key)}
                        type="button"
                      >
                        {key}
                      </button>
                    ))}
                  </div>
                  <textarea
                    aria-label={`${pane} code`}
                    className="h-64 w-full resize-none bg-slate-900 p-4 font-mono text-sm text-slate-100 outline-none"
                    onChange={(event) => updatePane(event.target.value)}
                    onKeyDown={(event) => {
                      const next = handleEditorKeydown(event);
                      if (typeof next === "string") updatePane(next);
                    }}
                    spellCheck={false}
                    value={code[pane]}
                  />
                </div>

                {/* Result + console */}
                <div className="grid grid-rows-[1fr_auto] gap-3">
                  <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
                    <div className="border-b border-slate-200 bg-slate-50 px-4 py-2 text-xs font-semibold uppercase tracking-wide text-slate-500">
                      Result
                    </div>
                    <iframe
                      className="h-52 w-full"
                      ref={iframeRef}
                      sandbox="allow-scripts allow-modals"
                      title="Live result"
                    />
                  </div>
                  <div className="rounded-xl border border-slate-800 bg-slate-950 p-3 font-mono text-xs">
                    <div className="mb-1.5 flex items-center gap-1.5 text-slate-400">
                      <Terminal className="size-3.5" /> Console
                    </div>
                    {consoleEntries.length ? (
                      <div className="grid gap-0.5">
                        {consoleEntries.map((entry, index) => (
                          <div
                            className={cn(
                              "whitespace-pre-wrap break-words",
                              entry.level === "error"
                                ? "text-rose-400"
                                : entry.level === "warn"
                                  ? "text-amber-300"
                                  : "text-slate-200",
                            )}
                            key={index}
                          >
                            {entry.text}
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="text-slate-600">Console output appears here.</p>
                    )}
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          <div className="flex items-center gap-2 text-xs text-slate-400">
            <Badge variant="neutral">Powered by JavaScript</Badge>
            <span>The editor engine lives in src/features/learn/*.js</span>
          </div>
        </div>
      </div>
    </div>
  );
}
