import {
  ArrowRight,
  BookOpen,
  CircleStop,
  Download,
  FastForward,
  FileUp,
  Gauge,
  GraduationCap,
  Hand,
  Headphones,
  KeyRound,
  Lightbulb,
  ListChecks,
  Mic,
  Play,
  RotateCcw,
  ScanSearch,
  Send,
  Sparkles,
  Telescope,
  TriangleAlert,
  Volume2,
  VolumeX,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { captureElement, VISION_ENABLED } from "./visionClient";

import { hasApiKey } from "@/api/apiKeys";
import { TUTOR_MODEL, TUTOR_PROVIDER } from "@/api/tutorClient";
import { Button } from "@/components/common";
import type { LessonDepth } from "@/types/tutor";
import { cn } from "@/utils/cn";

import { ApiKeySetup } from "./ApiKeySetup";
import { DrawLayer } from "./DrawLayer";
import { LearningJourney } from "./LearningJourney";
import { buildLessonNotes, downloadLessonNotes } from "./lessonNotes";
import { SlidePanel } from "./SlidePanel";
import { Whiteboard } from "./Whiteboard";
import { useTutor } from "./useTutor";
import { useVoice } from "./useVoice";

const SPEED_OPTIONS = [
  { value: 0.75, label: "0.75×" },
  { value: 1, label: "1×" },
  { value: 1.5, label: "1.5×" },
];

const SUGGESTIONS = [
  "Binary search, step by step",
  "How does photosynthesis work?",
  "Explain Big-O notation with examples",
  "What is a derivative in calculus?",
];

export function TutorConsole() {
  const tutor = useTutor();
  const {
    phase,
    topic,
    boardItems,
    transcript,
    error,
    status,
    preparedCount,
    speed,
    setSpeed,
    startLesson,
    mode,
    docSlides,
    docTitle,
    currentSlide,
    focusLine,
    startDocument,
    goToSlide,
    nextSlide,
    interrupt,
    askAboutBoard,
    resume,
    followUp,
    skipPacing,
    stop,
    reset,
    refreshStatus,
    setVoiceControl,
  } = tutor;

  const voice = useVoice();
  const [topicInput, setTopicInput] = useState("");
  const [question, setQuestion] = useState("");
  const [depth, setDepth] = useState<LessonDepth>("standard");
  const [pdfBusy, setPdfBusy] = useState(false);
  // BYOK gate: the hosted tutor (Groq) needs the user's own free key first.
  const needsKey = TUTOR_PROVIDER === "groq";
  const [setupOpen, setSetupOpen] = useState(() => needsKey && !hasApiKey("groq"));
  const boardRef = useRef<HTMLDivElement>(null);

  const boardText = () =>
    boardItems
      .map((b) => b.text || (b.nodes ?? b.steps ?? []).join(" → ") || b.kind)
      .filter(Boolean)
      .join("\n");

  const handleAskDrawing = async () => {
    if (!boardRef.current) return;
    try {
      const image = await captureElement(boardRef.current);
      askAboutBoard(question, image, boardText());
      setQuestion("");
    } catch {
      window.alert("Couldn't capture the board. Please try again.");
    }
  };

  const handleDocument = async (file: File) => {
    setPdfBusy(true);
    try {
      const isPptx = /\.pptx$/i.test(file.name);
      const slides = isPptx
        ? await (await import("./pptSlides")).loadPptxSlides(file)
        : await (await import("./pdfSlides")).loadPdfSlides(file);
      if (slides.length === 0 || slides.every((s) => s.lines.length === 0)) {
        window.alert(
          isPptx
            ? "Couldn't read text from that PowerPoint. Please try another file."
            : "That PDF had no readable pages (it may be scanned images).",
        );
        return;
      }
      const title = file.name
        .replace(/\.(pdf|pptx)$/i, "")
        .replace(/[_-]+/g, " ")
        .trim();
      startDocument(slides, title || "Your document", depth);
    } catch {
      window.alert("Sorry, that document could not be opened. Please try another.");
    } finally {
      setPdfBusy(false);
    }
  };

  useEffect(() => {
    refreshStatus();
  }, [refreshStatus]);

  // Pace playback by speech when voice is on; fall back to the timer when off.
  useEffect(() => {
    setVoiceControl(
      voice.voiceOn
        ? { speak: voice.speak, cancel: voice.cancelSpeech }
        : null,
    );
  }, [voice.voiceOn, voice.speak, voice.cancelSpeech, setVoiceControl]);

  const handleMic = () => {
    if (voice.listening) {
      voice.stopListening();
      return;
    }
    voice.startListening((transcript) => {
      setQuestion("");
      interrupt(transcript);
    });
  };

  const handleSpeed = (value: number) => {
    setSpeed(value);
    voice.setRate(value); // keep spoken pace in sync with the UI speed
  };

  const handleSaveNotes = () => {
    downloadLessonNotes(topic, buildLessonNotes(topic, boardItems, transcript));
  };

  const busy = phase === "teaching" || phase === "answering";
  const started = phase !== "idle";
  const docMode = mode === "document" && docSlides.length > 0;

  // Keyboard shortcuts: Space = pause/resume, → = reveal the rest, Esc = stop.
  // Ignored while typing in a field so the raise-hand box keeps working normally.
  useEffect(() => {
    if (!started) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.metaKey || e.ctrlKey || e.altKey) return;
      const el = e.target as HTMLElement | null;
      if (
        el &&
        (el.tagName === "INPUT" ||
          el.tagName === "TEXTAREA" ||
          el.isContentEditable)
      )
        return;
      if (e.code === "Space") {
        e.preventDefault();
        if (busy) stop();
        else if (phase === "paused") resume();
      } else if (e.key === "ArrowRight") {
        if (busy) {
          e.preventDefault();
          skipPacing();
        }
      } else if (e.key === "Escape") {
        if (busy) stop();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [started, busy, phase, stop, resume, skipPacing]);
  const modelReady = !status.checked || (status.running && status.modelInstalled);
  // "Preparing" = generating but nothing revealed on the board yet for this turn.
  const lastEntry = transcript[transcript.length - 1];
  const preparing =
    busy &&
    boardItems.length === 0 &&
    (!lastEntry || lastEntry.role === "student");
  // The teaching lives in speech; surface the latest spoken line prominently.
  const currentNarration = [...transcript]
    .reverse()
    .find((entry) => entry.role === "tutor");

  // --- Live voice conversation (hands-free) ---
  const {
    conversing: live,
    speaking: voiceSpeaking,
    voiceOn: voiceIsOn,
    toggleVoice,
    startConversation,
    stopConversation,
    setListeningEnabled,
  } = voice;

  // Route each spoken utterance to the tutor: a topic when idle starts a lesson,
  // otherwise it's an interruption/answer. Kept in a ref so the recogniser always
  // calls the latest version (current phase + depth) without re-subscribing.
  const liveUtterRef = useRef<(t: string) => void>(() => {});
  useEffect(() => {
    liveUtterRef.current = (text: string) => {
      const t = text.trim();
      if (!t) return;
      if (phase === "idle") startLesson(t, depth);
      else interrupt(t);
    };
  }, [phase, depth, startLesson, interrupt]);

  const startLive = () => {
    if (!voiceIsOn) toggleVoice(); // the tutor must speak in live mode
    startConversation((t) => liveUtterRef.current(t));
  };
  const endLive = () => stopConversation();

  // Arm the mic only when it's the student's turn: live session on, and the tutor
  // is neither speaking nor thinking. This is the echo guard — the mic is off
  // whenever the tutor's voice is playing, so it never hears itself.
  useEffect(() => {
    setListeningEnabled(live && !busy && !preparing && !voiceSpeaking);
  }, [live, busy, preparing, voiceSpeaking, setListeningEnabled]);

  // End the live session cleanly if the component unmounts.
  useEffect(() => () => stopConversation(), [stopConversation]);

  if (setupOpen) {
    return (
      <ApiKeySetup
        onDone={() => {
          setSetupOpen(false);
          refreshStatus();
        }}
      />
    );
  }

  return (
    <div className="grid gap-4">
      {!modelReady ? (
        <ModelSetupBanner
          status={status}
          onRetry={refreshStatus}
          onManageKeys={() => setSetupOpen(true)}
        />
      ) : null}

      {live ? (
        <LiveVoiceBar
          speaking={voiceSpeaking}
          listening={voice.listening}
          preparing={preparing}
          busy={busy}
          idle={phase === "idle"}
          onEnd={endLive}
        />
      ) : null}

      {/* Teaching view: (slides +) whiteboard + the live caption are on screen
          together; the full history sits in the side rail. */}
      <div
        className={cn(
          "grid gap-3 lg:h-[64vh] lg:min-h-[26rem]",
          docMode
            ? "xl:grid-cols-[16rem_minmax(0,1fr)_15rem] lg:grid-cols-[14rem_minmax(0,1fr)]"
            : "lg:grid-cols-[1fr_19rem]",
        )}
      >
        {docMode ? (
          <SlidePanel
            slides={docSlides}
            current={currentSlide}
            title={docTitle}
            focusLine={focusLine}
            onGoTo={goToSlide}
            busy={busy}
          />
        ) : null}
        <div className="flex h-[56vh] min-h-[20rem] flex-col gap-2 lg:h-auto">
          <div ref={boardRef} className="relative min-h-0 flex-1">
            <Whiteboard
              items={boardItems}
              writing={busy && !preparing}
              preparing={preparing}
              preparedCount={preparedCount}
            />
            <DrawLayer />
          </div>
          {started && !preparing && currentNarration ? (
            <NarrationCaption
              text={currentNarration.text}
              answer={currentNarration.answer}
            />
          ) : null}
        </div>

        {/* Side rail: status + full conversation */}
        <aside className="flex h-[38vh] min-h-[14rem] flex-col gap-2 lg:h-auto">
          <StatusPill phase={preparing ? "preparing" : phase} topic={topic} />
          <Transcript transcript={transcript} />
        </aside>
      </div>

      {error ? (
        <div className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          <TriangleAlert className="mt-0.5 size-4 shrink-0" />
          <span>{error}</span>
        </div>
      ) : null}

      {/* Controls */}
      {!started ? (
        <div className="grid gap-4">
          {voice.sttSupported && !live ? (
            <button
              type="button"
              disabled={!modelReady}
              onClick={startLive}
              className="flex items-center justify-center gap-2 rounded-2xl border border-brand-300 bg-gradient-to-r from-brand-50 to-white px-5 py-3 text-sm font-semibold text-brand-700 shadow-sm transition hover:border-brand-400 hover:to-brand-50 disabled:opacity-50"
            >
              <Headphones className="size-5" />
              Start a live voice session — just talk, hands-free
            </button>
          ) : null}
          <StartPanel
            value={topicInput}
            onChange={setTopicInput}
            depth={depth}
            onDepthChange={setDepth}
            onStart={() => {
              startLesson(topicInput, depth);
              setTopicInput("");
            }}
            disabled={!modelReady}
            ttsSupported={voice.ttsSupported}
            voiceOn={voice.voiceOn}
            onToggleVoice={voice.toggleVoice}
            onUploadDocument={handleDocument}
            docBusy={pdfBusy}
            showManageKeys={needsKey}
            onManageKeys={() => setSetupOpen(true)}
          />
          <LearningJourney
            disabled={!modelReady}
            onPick={(pick) => startLesson(pick, depth)}
          />
        </div>
      ) : (
        <div className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <RaiseHandInput
            value={question}
            onChange={setQuestion}
            onAsk={() => {
              interrupt(question);
              setQuestion("");
            }}
            busy={busy}
            phase={phase}
            micSupported={voice.sttSupported}
            listening={voice.listening}
            onMic={handleMic}
          />
          <div className="flex flex-wrap items-center gap-2">
            <SpeedControl value={speed} onChange={handleSpeed} />
            {voice.sttSupported ? (
              <Button
                variant={live ? "primary" : "outline"}
                size="sm"
                onClick={live ? endLive : startLive}
                title={
                  live
                    ? "End the live voice session"
                    : "Talk to the tutor hands-free"
                }
              >
                <Headphones className="size-4" />
                {live ? "Live voice on" : "Live voice"}
              </Button>
            ) : null}
            {busy ? (
              <>
                <Button variant="outline" size="sm" onClick={stop}>
                  <CircleStop className="size-4" />
                  Stop
                </Button>
                <Button variant="ghost" size="sm" onClick={skipPacing}>
                  <FastForward className="size-4" />
                  Reveal rest
                </Button>
              </>
            ) : null}
            {VISION_ENABLED && !busy ? (
              <Button
                variant="outline"
                size="sm"
                onClick={handleAskDrawing}
                title="Draw/circle on the board, then ask about it"
              >
                <ScanSearch className="size-4" />
                Ask about my drawing
              </Button>
            ) : null}
            {phase === "paused" ? (
              <Button variant="primary" size="sm" onClick={resume}>
                <Play className="size-4" />
                Resume
              </Button>
            ) : null}
            {docMode &&
            (phase === "done" || phase === "paused") &&
            !busy &&
            currentSlide < docSlides.length - 1 ? (
              <Button variant="primary" size="sm" onClick={nextSlide}>
                Next slide
                <ArrowRight className="size-4" />
              </Button>
            ) : null}
            {docMode && phase === "done" && currentSlide === docSlides.length - 1 ? (
              <span className="text-sm font-medium text-campus-600">
                🎉 You've finished the document!
              </span>
            ) : null}
            {phase === "error" ? (
              <Button
                variant="primary"
                size="sm"
                onClick={() =>
                  docMode
                    ? goToSlide(currentSlide)
                    : topic && startLesson(topic)
                }
              >
                <RotateCcw className="size-4" />
                Try again
              </Button>
            ) : null}
            {(phase === "done" || phase === "paused") &&
            !busy &&
            boardItems.length > 0 ? (
              <Button
                variant="ghost"
                size="sm"
                className="ml-auto"
                onClick={handleSaveNotes}
                title="Download this lesson as a study sheet (Markdown)"
              >
                <Download className="size-4" />
                Save notes
              </Button>
            ) : null}
            {voice.ttsSupported ? (
              <Button
                variant="ghost"
                size="sm"
                className={
                  (phase === "done" || phase === "paused") &&
                  !busy &&
                  boardItems.length > 0
                    ? ""
                    : "ml-auto"
                }
                onClick={voice.toggleVoice}
                title={voice.voiceOn ? "Turn voice off" : "Turn voice on"}
              >
                {voice.voiceOn ? (
                  <Volume2 className="size-4 text-brand-600" />
                ) : (
                  <VolumeX className="size-4" />
                )}
                {voice.voiceOn ? "Voice on" : "Voice off"}
              </Button>
            ) : null}
            <Button
              variant="ghost"
              size="sm"
              className={voice.ttsSupported ? "" : "ml-auto"}
              onClick={() => {
                reset();
                setQuestion("");
              }}
            >
              <RotateCcw className="size-4" />
              {docMode ? "Exit document" : "New topic"}
            </Button>
          </div>

          {(phase === "done" || phase === "paused") && !busy ? (
            <FollowUpChips onPick={followUp} />
          ) : null}

          <ShortcutsHint />
        </div>
      )}
    </div>
  );
}

const DEPTH_OPTIONS: { value: LessonDepth; label: string; hint: string }[] = [
  { value: "quick", label: "Quick", hint: "~4-6 beats · fastest" },
  { value: "standard", label: "Standard", hint: "~6-10 beats" },
  { value: "deep", label: "Deep", hint: "~10-15 beats · slowest" },
];

function StartPanel({
  value,
  onChange,
  depth,
  onDepthChange,
  onStart,
  disabled,
  ttsSupported,
  voiceOn,
  onToggleVoice,
  onUploadDocument,
  docBusy,
  showManageKeys,
  onManageKeys,
}: {
  value: string;
  onChange: (v: string) => void;
  depth: LessonDepth;
  onDepthChange: (d: LessonDepth) => void;
  onStart: () => void;
  disabled: boolean;
  ttsSupported: boolean;
  voiceOn: boolean;
  onToggleVoice: () => void;
  onUploadDocument: (file: File) => void;
  docBusy: boolean;
  showManageKeys: boolean;
  onManageKeys: () => void;
}) {
  const fileRef = useRef<HTMLInputElement>(null);
  const [startMode, setStartMode] = useState<"topic" | "document">("topic");

  return (
    <div className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      {/* Mode selector */}
      <div className="flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={() => setStartMode("topic")}
          className={cn(
            "inline-flex items-center gap-1.5 rounded-xl border px-3 py-2 text-sm font-medium transition",
            startMode === "topic"
              ? "border-brand-500 bg-brand-50 text-brand-700"
              : "border-slate-200 text-slate-600 hover:border-brand-300",
          )}
        >
          <GraduationCap className="size-4" />
          Learn any topic
        </button>
        <button
          type="button"
          onClick={() => setStartMode("document")}
          className={cn(
            "inline-flex items-center gap-1.5 rounded-xl border px-3 py-2 text-sm font-medium transition",
            startMode === "document"
              ? "border-brand-500 bg-brand-50 text-brand-700"
              : "border-slate-200 text-slate-600 hover:border-brand-300",
          )}
        >
          <BookOpen className="size-4" />
          Master a document
        </button>
        <div className="ml-auto flex items-center gap-2">
          {showManageKeys ? (
            <button
              type="button"
              onClick={onManageKeys}
              title="View or change your API keys"
              className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-500 transition hover:border-brand-300 hover:text-brand-700"
            >
              <KeyRound className="size-3.5" />
              API keys
            </button>
          ) : null}
          {ttsSupported ? (
            <button
              type="button"
              onClick={onToggleVoice}
              title={voiceOn ? "Voice on — the tutor speaks" : "Voice off"}
              className={cn(
                "inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium transition",
                voiceOn
                  ? "border-brand-300 bg-brand-50 text-brand-700"
                  : "border-slate-200 bg-slate-50 text-slate-500 hover:border-brand-300",
              )}
            >
              {voiceOn ? <Volume2 className="size-3.5" /> : <VolumeX className="size-3.5" />}
              {voiceOn ? "Voice on" : "Voice off"}
            </button>
          ) : null}
        </div>
      </div>

      {startMode === "topic" ? (
        <>
          <form
            className="flex flex-col gap-2 sm:flex-row"
            onSubmit={(event) => {
              event.preventDefault();
              if (!disabled) onStart();
            }}
          >
            <input
              autoFocus
              className="h-11 flex-1 rounded-xl border border-slate-300 px-4 text-sm outline-none focus-visible:border-brand-500 focus-visible:ring-2 focus-visible:ring-brand-200"
              placeholder="e.g. Teach me how binary search works"
              value={value}
              onChange={(event) => onChange(event.target.value)}
            />
            <Button type="submit" disabled={disabled || !value.trim()}>
              <Sparkles className="size-4" />
              Start lesson
            </Button>
          </form>
          <div className="flex flex-wrap gap-2">
            {SUGGESTIONS.map((suggestion) => (
              <button
                key={suggestion}
                type="button"
                disabled={disabled}
                onClick={() => onChange(suggestion)}
                className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs text-slate-600 transition hover:border-brand-300 hover:text-brand-700 disabled:opacity-50"
              >
                {suggestion}
              </button>
            ))}
          </div>
        </>
      ) : (
        <button
          type="button"
          disabled={disabled || docBusy}
          onClick={() => fileRef.current?.click()}
          className="flex flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center transition hover:border-brand-400 hover:bg-brand-50/40 disabled:opacity-60"
        >
          {docBusy ? (
            <>
              <span className="inline-block size-6 animate-spin rounded-full border-2 border-brand-200 border-t-brand-600" />
              <span className="text-sm font-medium text-slate-600">
                Opening your document…
              </span>
            </>
          ) : (
            <>
              <FileUp className="size-6 text-brand-600" />
              <span className="text-sm font-medium text-slate-700">
                Upload a PDF or PowerPoint to master
              </span>
              <span className="max-w-xs text-xs text-slate-400">
                The tutor opens it beside the whiteboard and teaches every slide,
                point by point, with its own explanations and visuals.
              </span>
            </>
          )}
          <input
            ref={fileRef}
            type="file"
            accept="application/pdf,.pdf,.pptx,application/vnd.openxmlformats-officedocument.presentationml.presentation"
            className="hidden"
            onChange={(event) => {
              const file = event.target.files?.[0];
              if (file) onUploadDocument(file);
              event.target.value = "";
            }}
          />
        </button>
      )}

      <div className="flex flex-wrap items-center gap-2">
        <span className="text-xs font-medium text-slate-400">Depth:</span>
        {DEPTH_OPTIONS.map((option) => (
          <button
            key={option.value}
            type="button"
            title={option.hint}
            onClick={() => onDepthChange(option.value)}
            className={cn(
              "rounded-full border px-3 py-1 text-xs font-medium transition",
              depth === option.value
                ? "border-brand-500 bg-brand-600 text-white"
                : "border-slate-200 bg-slate-50 text-slate-600 hover:border-brand-300",
            )}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  );
}

function LiveVoiceBar({
  speaking,
  listening,
  preparing,
  busy,
  idle,
  onEnd,
}: {
  speaking: boolean;
  listening: boolean;
  preparing: boolean;
  busy: boolean;
  idle: boolean;
  onEnd: () => void;
}) {
  const state = speaking
    ? "speaking"
    : preparing || busy
      ? "thinking"
      : listening
        ? "listening"
        : "waiting";

  const copy: Record<string, { label: string; hint: string }> = {
    speaking: { label: "Tutor is speaking", hint: "Listen — you can reply when it pauses" },
    thinking: { label: "Thinking…", hint: "Preparing your answer" },
    listening: {
      label: "Listening — just talk",
      hint: idle ? "Say a topic, e.g. “Teach me binary search”" : "Ask or answer out loud",
    },
    waiting: { label: "Live voice on", hint: "Starting the mic…" },
  };
  const info = copy[state];

  return (
    <div
      className={cn(
        "flex items-center gap-3 rounded-2xl border px-4 py-3 shadow-sm transition-colors",
        state === "listening"
          ? "border-red-300 bg-red-50"
          : state === "speaking"
            ? "border-brand-300 bg-brand-50"
            : "border-slate-200 bg-white",
      )}
    >
      <span
        className={cn(
          "grid size-11 shrink-0 place-items-center rounded-full text-white",
          state === "listening"
            ? "animate-pulse bg-red-500"
            : state === "speaking"
              ? "bg-brand-600"
              : "bg-slate-400",
        )}
      >
        {state === "thinking" ? (
          <span className="inline-block size-5 animate-spin rounded-full border-2 border-white/40 border-t-white" />
        ) : state === "speaking" ? (
          <Volume2 className="size-5" />
        ) : (
          <Mic className="size-5" />
        )}
      </span>
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <span className="text-[10px] font-bold uppercase tracking-wider text-brand-600">
            ● Live voice
          </span>
        </div>
        <p className="text-sm font-semibold text-slate-800">{info.label}</p>
        <p className="truncate text-xs text-slate-500">{info.hint}</p>
      </div>
      <Button variant="outline" size="sm" onClick={onEnd}>
        <CircleStop className="size-4" />
        End
      </Button>
    </div>
  );
}

function SpeedControl({
  value,
  onChange,
}: {
  value: number;
  onChange: (v: number) => void;
}) {
  return (
    <div
      className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-slate-50 p-0.5"
      title="Teaching speed"
    >
      <Gauge className="ml-1.5 size-3.5 text-slate-400" />
      {SPEED_OPTIONS.map((option) => (
        <button
          key={option.value}
          type="button"
          onClick={() => onChange(option.value)}
          className={cn(
            "rounded-full px-2 py-0.5 text-xs font-medium transition",
            value === option.value
              ? "bg-brand-600 text-white"
              : "text-slate-500 hover:text-brand-700",
          )}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}

function ShortcutsHint() {
  return (
    <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[11px] text-slate-400">
      <span>
        <kbd className="rounded border border-slate-200 bg-slate-50 px-1 font-sans">
          Space
        </kbd>{" "}
        pause / resume
      </span>
      <span>
        <kbd className="rounded border border-slate-200 bg-slate-50 px-1 font-sans">
          →
        </kbd>{" "}
        reveal the rest
      </span>
      <span>
        <kbd className="rounded border border-slate-200 bg-slate-50 px-1 font-sans">
          Esc
        </kbd>{" "}
        stop
      </span>
    </p>
  );
}

function RaiseHandInput({
  value,
  onChange,
  onAsk,
  busy,
  phase,
  micSupported,
  listening,
  onMic,
}: {
  value: string;
  onChange: (v: string) => void;
  onAsk: () => void;
  busy: boolean;
  phase: string;
  micSupported: boolean;
  listening: boolean;
  onMic: () => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  return (
    <form
      className="flex flex-col gap-2 sm:flex-row sm:items-center"
      onSubmit={(event) => {
        event.preventDefault();
        onAsk();
      }}
    >
      {micSupported ? (
        <button
          type="button"
          onClick={onMic}
          title={listening ? "Stop listening" : "Speak your question or answer"}
          className={cn(
            "grid size-11 shrink-0 place-items-center rounded-xl border transition",
            listening
              ? "animate-pulse border-red-400 bg-red-500 text-white"
              : "border-slate-300 bg-white text-slate-600 hover:border-brand-400 hover:text-brand-600",
          )}
        >
          <Mic className="size-5" />
        </button>
      ) : null}
      <div className="flex flex-1 items-center gap-2 rounded-xl border border-amber-300 bg-amber-50/60 px-3">
        <Hand className="size-4 shrink-0 text-amber-500" />
        <input
          ref={inputRef}
          className="h-11 flex-1 bg-transparent text-sm outline-none placeholder:text-amber-700/50"
          placeholder={
            listening
              ? "Listening… speak now"
              : busy
                ? "Confused? Type to interrupt and ask…"
                : "Answer the tutor, or ask a question…"
          }
          value={value}
          onChange={(event) => onChange(event.target.value)}
        />
      </div>
      <Button type="submit" variant="secondary" disabled={!value.trim()}>
        <Send className="size-4" />
        {phase === "teaching" ? "Interrupt" : "Send"}
      </Button>
    </form>
  );
}

const FOLLOW_UPS = [
  { icon: Telescope, label: "Go deeper", instruction: "Go deeper on this topic with more detail." },
  { icon: Lightbulb, label: "Give an example", instruction: "Show a concrete worked example of this." },
  { icon: ListChecks, label: "Quiz me", instruction: "Quiz me with a couple of short questions about what you just taught." },
];

function FollowUpChips({ onPick }: { onPick: (instruction: string) => void }) {
  return (
    <div className="flex flex-wrap items-center gap-2 border-t border-slate-100 pt-3">
      <span className="text-xs font-medium text-slate-400">Keep going:</span>
      {FOLLOW_UPS.map(({ icon: Icon, label, instruction }) => (
        <button
          key={label}
          type="button"
          onClick={() => onPick(instruction)}
          className="inline-flex items-center gap-1.5 rounded-full border border-brand-200 bg-brand-50 px-3 py-1 text-xs font-medium text-brand-700 transition hover:bg-brand-100"
        >
          <Icon className="size-3.5" />
          {label}
        </button>
      ))}
    </div>
  );
}

function NarrationCaption({
  text,
  answer,
}: {
  text: string;
  answer?: boolean;
}) {
  return (
    <div
      className={cn(
        "flex shrink-0 items-start gap-3 rounded-2xl border px-4 py-3 shadow-sm sm:px-5",
        answer
          ? "border-amber-300 bg-amber-50"
          : "border-brand-200 bg-gradient-to-r from-brand-50 to-white",
      )}
    >
      <span className="grid size-9 shrink-0 place-items-center rounded-full bg-brand-600 text-white">
        <GraduationCap className="size-5" />
      </span>
      <div className="min-w-0">
        <p
          className={cn(
            "text-[10px] font-semibold uppercase tracking-wide",
            answer ? "text-amber-700" : "text-brand-600",
          )}
        >
          {answer ? "Tutor is answering" : "Tutor is saying"}
        </p>
        <p
          key={text}
          className="tutor-fade-in mt-0.5 max-h-24 overflow-y-auto text-base leading-relaxed text-slate-800 sm:text-lg"
        >
          {text}
        </p>
      </div>
    </div>
  );
}

function StatusPill({ phase, topic }: { phase: string; topic: string }) {
  const map: Record<string, { label: string; className: string }> = {
    idle: { label: "Ready", className: "bg-slate-100 text-slate-600" },
    preparing: {
      label: "Preparing lesson…",
      className: "bg-brand-100 text-brand-700",
    },
    teaching: {
      label: "Teaching…",
      className: "bg-brand-100 text-brand-700",
    },
    answering: {
      label: "Answering your question…",
      className: "bg-amber-100 text-amber-800",
    },
    paused: { label: "Paused — resume when ready", className: "bg-amber-100 text-amber-800" },
    done: { label: "Lesson complete", className: "bg-campus-50 text-campus-600" },
    error: { label: "Something went wrong", className: "bg-red-100 text-red-700" },
    listening: { label: "Listening…", className: "bg-amber-100 text-amber-800" },
  };
  const info = map[phase] ?? map.idle;
  return (
    <div className="flex items-center justify-between rounded-xl border border-slate-200 bg-white px-3 py-2 shadow-sm">
      <span
        className={cn(
          "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium",
          info.className,
        )}
      >
        <span className="inline-block size-1.5 rounded-full bg-current" />
        {info.label}
      </span>
      {topic ? (
        <span className="max-w-[9rem] truncate text-xs text-slate-400" title={topic}>
          {topic}
        </span>
      ) : null}
    </div>
  );
}

function Transcript({
  transcript,
}: {
  transcript: { id: string; role: string; text: string; answer?: boolean }[];
}) {
  const endRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [transcript.length]);

  return (
    <div className="flex min-h-0 flex-1 flex-col rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 px-3 py-2 text-xs font-semibold uppercase tracking-wide text-slate-400">
        Conversation
      </div>
      <div className="flex-1 space-y-2 overflow-y-auto p-3">
        {transcript.length === 0 ? (
          <p className="text-xs text-slate-400">
            The tutor's narration and your questions appear here.
          </p>
        ) : (
          transcript.map((entry) => (
            <div
              key={entry.id}
              className={cn(
                "rounded-lg px-2.5 py-1.5 text-sm",
                entry.role === "student"
                  ? "ml-4 bg-brand-600 text-white"
                  : entry.answer
                    ? "mr-2 bg-amber-50 text-amber-900 ring-1 ring-amber-200"
                    : "mr-2 bg-slate-50 text-slate-700",
              )}
            >
              {entry.role === "tutor" ? (
                <span className="mb-0.5 block text-[10px] font-semibold uppercase tracking-wide text-slate-400">
                  Tutor
                </span>
              ) : null}
              {entry.text}
            </div>
          ))
        )}
        <div ref={endRef} />
      </div>
    </div>
  );
}

function ModelSetupBanner({
  status,
  onRetry,
  onManageKeys,
}: {
  status: { running: boolean; modelInstalled: boolean };
  onRetry: () => void;
  onManageKeys: () => void;
}) {
  const isGroq = TUTOR_PROVIDER === "groq";
  return (
    <div className="flex flex-col gap-2 rounded-xl border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900">
      <div className="flex items-center gap-2 font-semibold">
        <TriangleAlert className="size-4" />
        Tutor model not reachable
      </div>
      {isGroq ? (
        <p>
          The tutor can't reach Groq. Your Groq key may be missing or incorrect —
          open <strong>API keys</strong> and paste a valid free key (it starts
          with <code className="rounded bg-amber-100 px-1">gsk_</code>), or check
          your internet connection.
        </p>
      ) : !status.running ? (
        <p>
          Ollama isn't responding. Start it, then retry. The whiteboard tutor
          runs fully offline on your machine — no account or internet needed.
        </p>
      ) : (
        <p>
          Ollama is running but the model{" "}
          <code className="rounded bg-amber-100 px-1">{TUTOR_MODEL}</code> isn't
          installed. Run{" "}
          <code className="rounded bg-amber-100 px-1">
            ollama pull {TUTOR_MODEL}
          </code>
          .
        </p>
      )}
      <div className="flex gap-2">
        {isGroq ? (
          <Button variant="primary" size="sm" onClick={onManageKeys}>
            <KeyRound className="size-4" />
            API keys
          </Button>
        ) : null}
        <Button variant="outline" size="sm" onClick={onRetry}>
          <RotateCcw className="size-4" />
          Retry
        </Button>
      </div>
    </div>
  );
}
