import {
  BookOpen,
  Check,
  CheckCircle2,
  ClipboardList,
  Code2,
  Database,
  Play,
  RotateCcw,
  Table,
  Target,
  Terminal,
  XCircle,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";

import { Badge, Button, Card, CardContent, PageHeader } from "@/components/common";
// Core logic lives in plain JavaScript modules (see src/features/learn/*.js).
import { getChallenges } from "@/features/learn/challenges";
import { runChallenge, listenForChallenge } from "@/features/learn/challengeRunner";
import { tracks } from "@/features/learn/curriculum";
import { handleEditorKeydown } from "@/features/learn/editor";
import {
  isChallengeDone,
  isLessonDone,
  isQuizDone,
  loadProgress,
  markChallenge,
  markLesson,
  markQuiz,
  trackStats,
} from "@/features/learn/progress";
import { feedbackFor, gradeQuiz } from "@/features/learn/quiz";
import { getQuiz } from "@/features/learn/quizzes";
import { listenForConsole, runInIframe } from "@/features/learn/sandboxRunner";
import { getSqlChallenges } from "@/features/learn/sqlChallenges";
import { tables as sqlTables } from "@/features/learn/sqlData";
import { runSql } from "@/features/learn/sqlEngine";
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
  example?: Example;
  sql?: string;
}
interface Track {
  id: string;
  label: string;
  tagline: string;
  kind?: string;
  lessons: Lesson[];
}
const EMPTY_EXAMPLE: Example = { html: "", css: "", js: "" };
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
interface Challenge {
  id: string;
  title: string;
  prompt: string;
  fn: string;
  starter: string;
  cases: Array<{ args: unknown[]; expected: unknown }>;
}
interface ChallengeResult {
  passed: boolean;
  hasFn: boolean;
  error?: string;
  results: Array<{ name: string; pass: boolean; expected: unknown; actual: unknown; errored: boolean }>;
}
interface SqlChallenge {
  id: string;
  title: string;
  prompt: string;
  starter: string;
  solution: string;
}

const TRACKS = tracks as Track[];
type Pane = "html" | "css" | "js";
type Mode = "lesson" | "quiz" | "challenge";
type Progress = ReturnType<typeof loadProgress>;
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

  const [code, setCode] = useState<Example>(lesson.example ?? EMPTY_EXAMPLE);
  const [pane, setPane] = useState<Pane>("html");
  const [consoleEntries, setConsoleEntries] = useState<ConsoleEntry[]>([]);
  const [mode, setMode] = useState<Mode>("lesson");
  const [progress, setProgress] = useState<Progress>(() => loadProgress());
  const iframeRef = useRef<HTMLIFrameElement>(null);

  const quiz = getQuiz(trackId) as Quiz | null;
  const isSql = track.kind === "sql";
  const trackChallenges = getChallenges(trackId) as Challenge[];
  const sqlChallenges = (isSql ? getSqlChallenges() : []) as SqlChallenge[];
  const challengeCount = isSql ? sqlChallenges.length : trackChallenges.length;
  const hasChallenges = challengeCount > 0;
  const stats = trackStats(progress, trackId, track.lessons.length, challengeCount);

  // Switching lesson/track loads that example into the editor (event-driven so
  // we never reset state from inside an effect).
  const openLesson = (nextLesson: Lesson) => {
    setMode("lesson");
    setLessonId(nextLesson.id);
    setCode(nextLesson.example ?? EMPTY_EXAMPLE);
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
    if (isSql) return undefined;
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
          <div className="px-3 pb-1 pt-2">
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">
              {track.label} tutorial
            </p>
            <div className="mt-2 flex items-center gap-2">
              <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-100">
                <div
                  className="h-full rounded-full bg-brand-500 transition-all"
                  style={{ width: `${stats.percent}%` }}
                />
              </div>
              <span className="text-[11px] font-medium text-slate-500">
                {stats.done}/{stats.total}
              </span>
            </div>
          </div>
          {track.lessons.map((l) => {
            const done = isLessonDone(progress, trackId, l.id);
            return (
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
                {done ? (
                  <CheckCircle2 className="size-4 shrink-0 text-emerald-600" />
                ) : (
                  <BookOpen className="size-4 shrink-0" />
                )}
                <span className="flex-1 truncate">{l.title}</span>
              </button>
            );
          })}
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
              {isQuizDone(progress, trackId) ? (
                <CheckCircle2 className="size-4 shrink-0 text-emerald-600" />
              ) : (
                <ClipboardList className="size-4 shrink-0" />
              )}
              Take the quiz
            </button>
          ) : null}
          {hasChallenges ? (
            <button
              className={cn(
                "flex items-center gap-2 rounded-xl px-3 py-2 text-left text-sm font-semibold transition",
                mode === "challenge"
                  ? "bg-brand-50 text-brand-700"
                  : "text-slate-600 hover:bg-slate-50",
              )}
              onClick={() => setMode("challenge")}
              type="button"
            >
              <Target className="size-4 shrink-0" />
              {isSql ? "Query challenges" : "Code challenges"}
            </button>
          ) : null}
        </aside>

        {/* Lesson content + editor, or the quiz */}
        <div className="grid gap-5">
          {mode === "quiz" && quiz ? (
            <QuizPanel
              key={trackId}
              onComplete={() => setProgress(markQuiz(trackId))}
              quiz={quiz}
            />
          ) : mode === "challenge" && isSql && sqlChallenges.length ? (
            <SqlChallengePanel
              challenges={sqlChallenges}
              isDone={(id) => isChallengeDone(progress, trackId, id)}
              key={trackId}
              onSolved={(id) => setProgress(markChallenge(trackId, id))}
            />
          ) : mode === "challenge" && trackChallenges.length ? (
            <ChallengePanel
              challenges={trackChallenges}
              isDone={(id) => isChallengeDone(progress, trackId, id)}
              key={trackId}
              onSolved={(id) => setProgress(markChallenge(trackId, id))}
            />
          ) : (
          <>
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-sm font-medium text-brand-600">{track.tagline}</p>
              <h2 className="font-display text-2xl font-semibold text-slate-900">{lesson.title}</h2>
            </div>
            <Button
              onClick={() => setProgress(markLesson(trackId, lesson.id, !isLessonDone(progress, trackId, lesson.id)))}
              variant={isLessonDone(progress, trackId, lesson.id) ? "ghost" : "outline"}
            >
              <Check className="size-4" />
              {isLessonDone(progress, trackId, lesson.id) ? "Completed" : "Mark as complete"}
            </Button>
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

          {/* Try it Yourself, or the SQL playground for the database track */}
          {isSql ? (
            <SqlRunner initialQuery={lesson.sql ?? ""} key={lesson.id} />
          ) : (
          <Card>
            <CardContent className="grid gap-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2 font-semibold text-slate-900">
                  <Play className="size-4 text-emerald-600" />
                  Try it Yourself
                </div>
                <div className="flex items-center gap-2">
                  <Button onClick={() => setCode(lesson.example ?? EMPTY_EXAMPLE)} variant="ghost">
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
          )}

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

function QuizPanel({ quiz, onComplete }: { quiz: Quiz; onComplete: () => void }) {
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
          <Button
            disabled={answeredCount < quiz.questions.length}
            onClick={() => {
              const graded = gradeQuiz(quiz.questions, answers);
              setSubmitted(true);
              if (graded.passed) onComplete();
            }}
          >
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

function ChallengePanel({
  challenges,
  isDone,
  onSolved,
}: {
  challenges: Challenge[];
  isDone: (id: string) => boolean;
  onSolved: (id: string) => void;
}) {
  return (
    <div className="grid gap-4">
      <div>
        <p className="text-sm font-medium text-brand-600">Write code to pass the tests</p>
        <h2 className="font-display text-2xl font-semibold text-slate-900">Code challenges</h2>
      </div>
      {challenges.map((challenge) => (
        <ChallengeCard
          challenge={challenge}
          done={isDone(challenge.id)}
          key={challenge.id}
          onSolved={() => onSolved(challenge.id)}
        />
      ))}
    </div>
  );
}

function ChallengeCard({
  challenge,
  done,
  onSolved,
}: {
  challenge: Challenge;
  done: boolean;
  onSolved: () => void;
}) {
  const [code, setCode] = useState(challenge.starter);
  const [result, setResult] = useState<ChallengeResult | null>(null);
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const onSolvedRef = useRef(onSolved);
  useEffect(() => {
    onSolvedRef.current = onSolved;
  }, [onSolved]);

  useEffect(() => {
    const iframe = iframeRef.current;
    if (!iframe) return undefined;
    return listenForChallenge(iframe, (res: ChallengeResult) => {
      setResult(res);
      if (res.passed) onSolvedRef.current();
    });
  }, []);

  const check = () => {
    setResult(null);
    runChallenge(iframeRef.current, code, challenge);
  };

  return (
    <Card>
      <CardContent className="grid gap-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="flex items-center gap-2 font-semibold text-slate-900">
            <Target className="size-4 text-brand-600" />
            {challenge.title}
            {done ? <Badge variant="success">Solved</Badge> : null}
          </p>
          <Button onClick={check}>
            <Play className="size-4" /> Run &amp; check
          </Button>
        </div>
        <p className="text-sm text-slate-600">{challenge.prompt}</p>
        <textarea
          aria-label={`${challenge.title} solution`}
          className="h-40 w-full resize-none rounded-xl bg-slate-900 p-4 font-mono text-sm text-slate-100 outline-none"
          onChange={(event) => setCode(event.target.value)}
          onKeyDown={(event) => {
            const next = handleEditorKeydown(event);
            if (typeof next === "string") setCode(next);
          }}
          spellCheck={false}
          value={code}
        />
        {result ? (
          <div
            className={cn(
              "grid gap-1.5 rounded-xl border p-3",
              result.passed ? "border-emerald-200 bg-emerald-50" : "border-slate-200 bg-slate-50",
            )}
          >
            {!result.hasFn ? (
              <p className="text-sm font-medium text-rose-600">
                Define a function named <code>{challenge.fn}(...)</code>.
              </p>
            ) : null}
            {result.error ? (
              <p className="text-sm font-medium text-rose-600">Error: {result.error}</p>
            ) : null}
            {result.results.map((r, index) => (
              <div className="flex items-center gap-2 font-mono text-xs" key={index}>
                {r.pass ? (
                  <CheckCircle2 className="size-3.5 shrink-0 text-emerald-600" />
                ) : (
                  <XCircle className="size-3.5 shrink-0 text-rose-500" />
                )}
                <span className={r.pass ? "text-slate-600" : "text-rose-600"}>
                  {r.name} → {JSON.stringify(r.actual)}
                  {r.pass ? "" : ` (expected ${JSON.stringify(r.expected)})`}
                </span>
              </div>
            ))}
            {result.passed ? (
              <p className="flex items-center gap-1.5 text-sm font-semibold text-emerald-700">
                <CheckCircle2 className="size-4" /> All tests passed — challenge solved!
              </p>
            ) : null}
          </div>
        ) : null}
        <iframe
          className="hidden"
          ref={iframeRef}
          sandbox="allow-scripts"
          title={`challenge-runner-${challenge.id}`}
        />
      </CardContent>
    </Card>
  );
}

function SqlRunner({ initialQuery }: { initialQuery: string }) {
  const [query, setQuery] = useState(initialQuery);
  const [state, setState] = useState<{ columns?: string[]; rows?: unknown[][]; error?: string }>(
    () => runSql(initialQuery, sqlTables),
  );
  const [showTables, setShowTables] = useState(false);

  return (
    <Card>
      <CardContent className="grid gap-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2 font-semibold text-slate-900">
            <Database className="size-4 text-brand-600" />
            SQL playground
          </div>
          <div className="flex items-center gap-2">
            <Button onClick={() => setShowTables((value) => !value)} variant="ghost">
              <Table className="size-4" /> {showTables ? "Hide tables" : "Tables"}
            </Button>
            <Button onClick={() => setState(runSql(query, sqlTables))}>
              <Play className="size-4" /> Run query
            </Button>
          </div>
        </div>

        <textarea
          aria-label="SQL query"
          className="h-28 w-full resize-none rounded-xl bg-slate-900 p-4 font-mono text-sm text-slate-100 outline-none"
          onChange={(event) => setQuery(event.target.value)}
          spellCheck={false}
          value={query}
        />

        {state.error ? (
          <div className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {state.error}
          </div>
        ) : (
          <ResultTable columns={state.columns ?? []} rows={state.rows ?? []} />
        )}

        {showTables ? (
          <div className="grid gap-3 rounded-xl border border-slate-200 bg-slate-50 p-3">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
              Sample database
            </p>
            {Object.entries(sqlTables).map(([name, t]) => (
              <div key={name}>
                <p className="mb-1 text-sm font-semibold text-slate-800">{name}</p>
                <ResultTable columns={t.columns} rows={t.rows} />
              </div>
            ))}
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}

function ResultTable({ columns, rows }: { columns: string[]; rows: unknown[][] }) {
  if (!rows.length) {
    return <p className="text-sm text-slate-500">No rows returned.</p>;
  }
  return (
    <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
      <table className="min-w-full text-sm">
        <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
          <tr>
            {columns.map((c) => (
              <th className="px-3 py-2 font-semibold" key={c}>
                {c}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rows.map((row, rowIndex) => (
            <tr key={rowIndex}>
              {row.map((cell, cellIndex) => (
                <td className="px-3 py-2 text-slate-700" key={cellIndex}>
                  {String(cell)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function sameResult(
  a: { columns?: string[]; rows?: unknown[][] },
  b: { columns?: string[]; rows?: unknown[][] },
) {
  const norm = (r: { rows?: unknown[][] }) =>
    JSON.stringify([...(r.rows ?? [])].map((row) => JSON.stringify(row)).sort());
  return (a.columns?.length ?? 0) === (b.columns?.length ?? 0) && norm(a) === norm(b);
}

function SqlChallengePanel({
  challenges,
  isDone,
  onSolved,
}: {
  challenges: SqlChallenge[];
  isDone: (id: string) => boolean;
  onSolved: (id: string) => void;
}) {
  return (
    <div className="grid gap-4">
      <div>
        <p className="text-sm font-medium text-brand-600">Write a query to get the data</p>
        <h2 className="font-display text-2xl font-semibold text-slate-900">Query challenges</h2>
      </div>
      {challenges.map((challenge) => (
        <SqlChallengeCard
          challenge={challenge}
          done={isDone(challenge.id)}
          key={challenge.id}
          onSolved={() => onSolved(challenge.id)}
        />
      ))}
    </div>
  );
}

function SqlChallengeCard({
  challenge,
  done,
  onSolved,
}: {
  challenge: SqlChallenge;
  done: boolean;
  onSolved: () => void;
}) {
  const [query, setQuery] = useState(challenge.starter);
  const [state, setState] = useState<{
    columns?: string[];
    rows?: unknown[][];
    error?: string;
    passed?: boolean;
  } | null>(null);

  const check = () => {
    const got = runSql(query, sqlTables);
    if (got.error) {
      setState({ error: got.error });
      return;
    }
    const expected = runSql(challenge.solution, sqlTables);
    const passed = sameResult(got, expected);
    setState({ columns: got.columns, rows: got.rows, passed });
    if (passed) onSolved();
  };

  return (
    <Card>
      <CardContent className="grid gap-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="flex items-center gap-2 font-semibold text-slate-900">
            <Target className="size-4 text-brand-600" />
            {challenge.title}
            {done ? <Badge variant="success">Solved</Badge> : null}
          </p>
          <Button onClick={check}>
            <Play className="size-4" /> Run &amp; check
          </Button>
        </div>
        <p className="text-sm text-slate-600">{challenge.prompt}</p>
        <textarea
          aria-label={`${challenge.title} query`}
          className="h-24 w-full resize-none rounded-xl bg-slate-900 p-4 font-mono text-sm text-slate-100 outline-none"
          onChange={(event) => setQuery(event.target.value)}
          spellCheck={false}
          value={query}
        />
        {state?.error ? (
          <div className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {state.error}
          </div>
        ) : state ? (
          <div className="grid gap-2">
            <ResultTable columns={state.columns ?? []} rows={state.rows ?? []} />
            {state.passed ? (
              <p className="flex items-center gap-1.5 text-sm font-semibold text-emerald-700">
                <CheckCircle2 className="size-4" /> Correct — challenge solved!
              </p>
            ) : (
              <p className="flex items-center gap-1.5 text-sm font-medium text-rose-600">
                <XCircle className="size-4" /> Not quite — that result doesn&apos;t match. Keep trying.
              </p>
            )}
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
