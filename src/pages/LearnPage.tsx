import {
  BookOpen,
  CheckCircle2,
  ClipboardList,
  Code2,
  Play,
  RotateCcw,
  Terminal,
  XCircle,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";

import { Badge, Button, Card, CardContent, PageHeader } from "@/components/common";
// Core logic lives in plain JavaScript modules (see src/features/learn/*.js).
import { tracks } from "@/features/learn/curriculum";
import { handleEditorKeydown } from "@/features/learn/editor";
import { feedbackFor, gradeQuiz } from "@/features/learn/quiz";
import { getQuiz } from "@/features/learn/quizzes";
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
interface QuizQuestion {
  q: string;
  options: string[];
  answer: number;
  explain: string;
}
interface Quiz {
  title: string;
  questions: QuizQuestion[];
}

const TRACKS = tracks as Track[];
type Pane = "html" | "css" | "js";
type Mode = "lesson" | "quiz";
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
  const [mode, setMode] = useState<Mode>("lesson");
  const iframeRef = useRef<HTMLIFrameElement>(null);

  const quiz = getQuiz(trackId) as Quiz | null;

  // Switching lesson/track loads that example into the editor (event-driven so
  // we never reset state from inside an effect).
  const openLesson = (nextLesson: Lesson) => {
    setMode("lesson");
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
                mode === "lesson" && l.id === lessonId
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
          {quiz ? (
            <button
              className={cn(
                "mt-1 flex items-center gap-2 rounded-xl border-t border-slate-100 px-3 py-2 text-left text-sm font-semibold transition",
                mode === "quiz"
                  ? "bg-brand-50 text-brand-700"
                  : "text-slate-600 hover:bg-slate-50",
              )}
              onClick={() => setMode("quiz")}
              type="button"
            >
              <ClipboardList className="size-4 shrink-0" />
              Take the quiz
            </button>
          ) : null}
        </aside>

        {/* Lesson content + editor, or the quiz */}
        <div className="grid gap-5">
          {mode === "quiz" && quiz ? (
            <QuizPanel key={trackId} quiz={quiz} />
          ) : (
          <>
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
          </>
          )}
        </div>
      </div>
    </div>
  );
}

function QuizPanel({ quiz }: { quiz: Quiz }) {
  const [answers, setAnswers] = useState<Array<number | null>>(
    () => quiz.questions.map(() => null),
  );
  const [submitted, setSubmitted] = useState(false);

  const result = useMemo(
    () => (submitted ? gradeQuiz(quiz.questions, answers) : null),
    [submitted, quiz.questions, answers],
  );
  const answeredCount = answers.filter((a) => a !== null).length;

  const choose = (questionIndex: number, optionIndex: number) => {
    if (submitted) return;
    setAnswers((prev) => prev.map((value, i) => (i === questionIndex ? optionIndex : value)));
  };
  const retake = () => {
    setAnswers(quiz.questions.map(() => null));
    setSubmitted(false);
  };

  return (
    <div className="grid gap-4">
      <div>
        <p className="text-sm font-medium text-brand-600">Check your understanding</p>
        <h2 className="font-display text-2xl font-semibold text-slate-900">{quiz.title}</h2>
      </div>

      {result ? (
        <Card className="border-brand-200 bg-brand-50/50">
          <CardContent className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-display text-3xl font-semibold text-brand-700">
                {result.score} / {result.total}
              </p>
              <p className="text-sm text-slate-600">
                {result.percent}% · {feedbackFor(result.percent)}
              </p>
            </div>
            <Button onClick={retake} variant="outline">
              <RotateCcw className="size-4" /> Retake quiz
            </Button>
          </CardContent>
        </Card>
      ) : null}

      <div className="grid gap-3">
        {quiz.questions.map((question, qIndex) => {
          const chosen = answers[qIndex];
          const correctIndex = question.answer;
          return (
            <Card key={qIndex}>
              <CardContent className="grid gap-3">
                <p className="font-semibold text-slate-900">
                  {qIndex + 1}. {question.q}
                </p>
                <div className="grid gap-2">
                  {question.options.map((option, oIndex) => {
                    const isChosen = chosen === oIndex;
                    const showCorrect = submitted && oIndex === correctIndex;
                    const showWrong = submitted && isChosen && oIndex !== correctIndex;
                    return (
                      <button
                        className={cn(
                          "flex items-center gap-2 rounded-xl border px-3 py-2 text-left text-sm transition",
                          showCorrect
                            ? "border-emerald-300 bg-emerald-50 text-emerald-800"
                            : showWrong
                              ? "border-rose-300 bg-rose-50 text-rose-700"
                              : isChosen
                                ? "border-brand-300 bg-brand-50 text-brand-700"
                                : "border-slate-200 text-slate-600 hover:bg-slate-50",
                        )}
                        disabled={submitted}
                        key={oIndex}
                        onClick={() => choose(qIndex, oIndex)}
                        type="button"
                      >
                        {showCorrect ? (
                          <CheckCircle2 className="size-4 shrink-0" />
                        ) : showWrong ? (
                          <XCircle className="size-4 shrink-0" />
                        ) : (
                          <span
                            className={cn(
                              "grid size-4 shrink-0 place-items-center rounded-full border text-[10px]",
                              isChosen ? "border-brand-500 bg-brand-500 text-white" : "border-slate-300",
                            )}
                          >
                            {String.fromCharCode(65 + oIndex)}
                          </span>
                        )}
                        {option}
                      </button>
                    );
                  })}
                </div>
                {submitted ? (
                  <p className="rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-600">
                    {question.explain}
                  </p>
                ) : null}
              </CardContent>
            </Card>
          );
        })}
      </div>

      {!submitted ? (
        <div className="flex items-center gap-3">
          <Button disabled={answeredCount < quiz.questions.length} onClick={() => setSubmitted(true)}>
            Submit answers
          </Button>
          <span className="text-sm text-slate-500">
            {answeredCount} / {quiz.questions.length} answered
          </span>
        </div>
      ) : null}
    </div>
  );
}
