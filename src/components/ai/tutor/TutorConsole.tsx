import {
  ArrowRight,
  BookOpen,
  CircleStop,
  Download,
  FastForward,
  FileText,
  FileUp,
  Gauge,
  GraduationCap,
  Hand,
  Headphones,
  KeyRound,
  Languages,
  Lightbulb,
  ListChecks,
  Maximize2,
  MessageSquare,
  Mic,
  MicOff,
  Minimize2,
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
import { useEffect, useRef, useState, type ReactNode } from "react";

import { captureElement, VISION_ENABLED } from "./visionClient";

import { hasApiKey } from "@/api/apiKeys";
import { TUTOR_MODEL, TUTOR_PROVIDER } from "@/api/tutorClient";
import { Button } from "@/components/common";
import type { LessonDepth, LessonLanguage } from "@/types/tutor";
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

const LANGUAGES: {
  value: LessonLanguage;
  label: string;
  hint: string;
  stt: string;
  tts: string;
}[] = [
  {
    value: "english",
    label: "English",
    hint: "Speaks English, writes English",
    stt: "en-US",
    tts: "en-US",
  },
  {
    value: "urdu",
    label: "Urdu",
    hint: "Speaks Urdu, writes Roman Urdu (needs an Urdu voice — e.g. Edge)",
    stt: "ur-PK",
    tts: "ur-PK",
  },
  {
    value: "roman-urdu",
    label: "Roman Urdu",
    hint: "Speaks Roman Urdu, writes English — works on every device",
    stt: "ur-PK",
    tts: "en-US",
  },
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
    language,
    setLanguage,
    startLesson,
    mode,
    docSlides,
    docTitle,
    currentSlide,
    focusLine,
    startDocument,
    goToSlide,
    viewSlide,
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
  // Layout: fullscreen "theater" mode + collapsible side panels (responsive).
  const [expanded, setExpanded] = useState(false);
  const [showTranscript, setShowTranscript] = useState(true);
  const [showSlides, setShowSlides] = useState(true);
  const boardRef = useRef<HTMLDivElement>(null);
  const shellRef = useRef<HTMLDivElement>(null);

  // Expand to a full-viewport "theater" view. We also try the native Fullscreen
  // API (so it fills the whole screen where allowed), but the CSS overlay is what
  // actually drives the layout — so it still works where fullscreen is blocked.
  const toggleExpanded = () => {
    const next = !expanded;
    setExpanded(next);
    try {
      if (next) shellRef.current?.requestFullscreen?.().catch(() => undefined);
      else if (document.fullscreenElement)
        document.exitFullscreen?.().catch(() => undefined);
    } catch {
      /* native fullscreen unavailable — the CSS overlay still works */
    }
  };
  // If the user leaves NATIVE fullscreen (Esc), drop our expanded layout too.
  useEffect(() => {
    const onFs = () => {
      if (!document.fullscreenElement) setExpanded(false);
    };
    document.addEventListener("fullscreenchange", onFs);
    return () => document.removeEventListener("fullscreenchange", onFs);
  }, []);

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

  const [urduVoiceMissing, setUrduVoiceMissing] = useState(false);
  const handleLanguage = (value: LessonLanguage) => {
    setLanguage(value);
    const opt = LANGUAGES.find((l) => l.value === value) ?? LANGUAGES[0];
    voice.setSpeechLang(opt.stt, opt.tts); // mic + spoken voice follow the language
    // "Urdu" (spoken Urdu) needs an Urdu TTS voice; warn if this device has none.
    setUrduVoiceMissing(value === "urdu" && !voice.hasVoiceFor("ur"));
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

  // Route each spoken utterance to the tutor, Jarvis-style. Simple spoken commands
  // control playback directly; anything else is a question/instruction that
  // interrupts the tutor immediately. Kept in a ref so the always-on recogniser
  // always calls the latest version (current phase + depth) without re-subscribing.
  const liveUtterRef = useRef<(t: string) => void>(() => {});
  useEffect(() => {
    liveUtterRef.current = (text: string) => {
      const t = text.trim();
      if (!t) return;
      const low = t.toLowerCase();
      if (phase === "idle") {
        startLesson(t, depth);
        return;
      }
      if (/^(stop|pause|wait|hold on|hang on|one sec|quiet|be quiet|shh+)\b/.test(low)) {
        stop();
        return;
      }
      if (/^(continue|resume|go on|carry on|keep going|go ahead)\b/.test(low)) {
        resume();
        return;
      }
      interrupt(t); // a real question/instruction — cuts in and gets answered
    };
  }, [phase, depth, startLesson, interrupt, stop, resume]);

  // Lets the student silence the mic (e.g. to talk to someone) without ending the
  // live session — the tutor keeps teaching, it just stops listening.
  const [micMuted, setMicMuted] = useState(false);

  const startLive = () => {
    if (!voiceIsOn) toggleVoice(); // the tutor must speak in live mode
    setMicMuted(false);
    startConversation((t) => liveUtterRef.current(t));
  };
  const endLive = () => {
    setMicMuted(false);
    stopConversation();
  };

  // Jarvis mode: the mic stays ON the whole live session, even while the tutor
  // talks, so the student can barge in anytime — unless they've muted it. The echo
  // filter in useVoice discards the tutor's own voice coming back through speakers.
  useEffect(() => {
    setListeningEnabled(live && !micMuted);
  }, [live, micMuted, setListeningEnabled]);

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
    <div
      ref={shellRef}
      className={cn(
        expanded
          ? "fixed inset-0 z-40 flex flex-col gap-2 overflow-auto bg-slate-100 p-2 sm:p-3"
          : "grid gap-4",
      )}
    >
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
          idle={phase === "idle"}
          muted={micMuted}
          onToggleMute={() => setMicMuted((m) => !m)}
          onEnd={endLive}
        />
      ) : null}

      {/* Board toolbar: status + panel toggles + fullscreen. */}
      {started ? (
        <div className="flex flex-wrap items-center gap-2">
          <StatusPill phase={preparing ? "preparing" : phase} topic={topic} />
          <div className="ml-auto flex items-center gap-1.5">
            {docMode ? (
              <PanelToggle
                on={showSlides}
                onClick={() => setShowSlides((s) => !s)}
                icon={<FileText className="size-4" />}
                label="Slides"
              />
            ) : null}
            <PanelToggle
              on={showTranscript}
              onClick={() => setShowTranscript((s) => !s)}
              icon={<MessageSquare className="size-4" />}
              label="Transcript"
            />
            <PanelToggle
              on={expanded}
              onClick={toggleExpanded}
              icon={
                expanded ? (
                  <Minimize2 className="size-4" />
                ) : (
                  <Maximize2 className="size-4" />
                )
              }
              label={expanded ? "Exit" : "Fullscreen"}
            />
          </div>
        </div>
      ) : null}

      {/* Teaching view: (slides +) whiteboard with a caption overlay + the
          collapsible transcript rail. Flex so panels can be toggled; responsive. */}
      <div
        className={cn(
          "flex flex-col gap-3 lg:flex-row",
          expanded
            ? "lg:min-h-0 lg:flex-1"
            : docMode
              ? "lg:h-[66vh] lg:min-h-[26rem]"
              : "lg:h-[64vh] lg:min-h-[26rem]",
        )}
      >
        {docMode && showSlides ? (
          <SlidePanel
            slides={docSlides}
            current={currentSlide}
            title={docTitle}
            focusLine={focusLine}
            onView={viewSlide}
            onTeach={goToSlide}
            busy={busy}
            className="w-full shrink-0 lg:w-72 xl:w-80"
          />
        ) : null}

        <div
          ref={boardRef}
          className="relative min-h-[46vh] flex-1 overflow-hidden rounded-2xl lg:min-h-0"
        >
          <Whiteboard
            items={boardItems}
            writing={busy && !preparing}
            preparing={preparing}
            preparedCount={preparedCount}
          />
          <DrawLayer />
          {started && !preparing && currentNarration ? (
            <CaptionOverlay
              text={currentNarration.text}
              answer={currentNarration.answer}
            />
          ) : null}
        </div>

        {/* Side rail: full conversation (collapsible). */}
        {showTranscript ? (
          <aside className="flex h-[38vh] w-full shrink-0 flex-col gap-2 lg:h-auto lg:w-72 xl:w-80">
            <Transcript transcript={transcript} />
          </aside>
        ) : null}
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
            language={language}
            onLanguageChange={handleLanguage}
            urduVoiceMissing={urduVoiceMissing}
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
            {phase === "paused" && boardItems.length > 0 ? (
              <Button variant="primary" size="sm" onClick={resume}>
                <Play className="size-4" />
                Resume
              </Button>
            ) : null}
            {docMode &&
            (phase === "done" || phase === "paused") &&
            !busy &&
            boardItems.length > 0 &&
            currentSlide < docSlides.length - 1 ? (
              <Button variant="primary" size="sm" onClick={nextSlide}>
                Teach next slide
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

          {(phase === "done" || phase === "paused") &&
          !busy &&
          boardItems.length > 0 ? (
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
  language,
  onLanguageChange,
  urduVoiceMissing,
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
  language: LessonLanguage;
  onLanguageChange: (l: LessonLanguage) => void;
  urduVoiceMissing: boolean;
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

      <div className="flex flex-wrap items-center gap-2">
        <span className="inline-flex items-center gap-1 text-xs font-medium text-slate-400">
          <Languages className="size-3.5" />
          Language:
        </span>
        {LANGUAGES.map((option) => (
          <button
            key={option.value}
            type="button"
            title={option.hint}
            onClick={() => onLanguageChange(option.value)}
            className={cn(
              "rounded-full border px-3 py-1 text-xs font-medium transition",
              language === option.value
                ? "border-brand-500 bg-brand-600 text-white"
                : "border-slate-200 bg-slate-50 text-slate-600 hover:border-brand-300",
            )}
          >
            {option.label}
          </button>
        ))}
      </div>

      {urduVoiceMissing ? (
        <p className="flex items-start gap-1.5 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
          <TriangleAlert className="mt-0.5 size-3.5 shrink-0" />
          <span>
            This browser has no Urdu voice, so <strong>Urdu</strong> mode won&apos;t
            speak aloud here. Open the tutor in <strong>Microsoft Edge</strong> (it
            has a free built-in Urdu voice on every device), or use{" "}
            <strong>Roman Urdu</strong> — it speaks on any device (and writes English
            on the board).
          </span>
        </p>
      ) : null}
    </div>
  );
}

function LiveVoiceBar({
  speaking,
  listening,
  preparing,
  idle,
  muted,
  onToggleMute,
  onEnd,
}: {
  speaking: boolean;
  listening: boolean;
  preparing: boolean;
  idle: boolean;
  muted: boolean;
  onToggleMute: () => void;
  onEnd: () => void;
}) {
  const state = muted
    ? "muted"
    : speaking
      ? "speaking"
      : preparing
        ? "thinking"
        : listening
          ? "listening"
          : "waiting";

  const copy: Record<string, { label: string; hint: string }> = {
    muted: {
      label: "Mic off — you’re muted",
      hint: "The tutor keeps teaching. Turn the mic on when you want to talk.",
    },
    speaking: {
      label: "Tutor is speaking",
      hint: "Just talk to jump in — say “stop”, or ask anything",
    },
    thinking: { label: "Thinking…", hint: "Say “stop” anytime to cut in" },
    listening: {
      label: "Listening — just talk",
      hint: idle ? "Say a topic, e.g. “Teach me binary search”" : "Ask, answer, or say “continue”",
    },
    waiting: { label: "Live voice on", hint: "Starting the mic…" },
  };
  const info = copy[state];

  return (
    <div
      className={cn(
        "flex items-center gap-3 rounded-2xl border px-4 py-3 shadow-sm transition-colors",
        state === "muted"
          ? "border-slate-300 bg-slate-100"
          : state === "listening"
            ? "border-red-300 bg-red-50"
            : state === "speaking"
              ? "border-brand-300 bg-brand-50"
              : "border-slate-200 bg-white",
      )}
    >
      <span
        className={cn(
          "grid size-11 shrink-0 place-items-center rounded-full text-white",
          state === "muted"
            ? "bg-slate-400"
            : state === "listening"
              ? "animate-pulse bg-red-500"
              : state === "speaking"
                ? "bg-brand-600"
                : "bg-slate-400",
        )}
      >
        {state === "muted" ? (
          <MicOff className="size-5" />
        ) : state === "thinking" ? (
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
      <Button
        variant={muted ? "primary" : "outline"}
        size="sm"
        onClick={onToggleMute}
        title={muted ? "Turn the mic on" : "Mute the mic"}
      >
        {muted ? <MicOff className="size-4" /> : <Mic className="size-4" />}
        {muted ? "Mic off" : "Mute"}
      </Button>
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

/** A small pill toggle for the board toolbar (slides / transcript / fullscreen). */
function PanelToggle({
  on,
  onClick,
  icon,
  label,
}: {
  on: boolean;
  onClick: () => void;
  icon: ReactNode;
  label: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      title={label}
      aria-pressed={on}
      className={cn(
        "inline-flex items-center gap-1.5 rounded-lg border px-2.5 py-1.5 text-xs font-medium transition",
        on
          ? "border-brand-300 bg-brand-50 text-brand-700"
          : "border-slate-200 bg-white text-slate-500 hover:border-brand-300",
      )}
    >
      {icon}
      <span className="hidden sm:inline">{label}</span>
    </button>
  );
}

/**
 * A YouTube-style caption: a floating bar pinned to the bottom of the board that
 * shows ONLY the line the tutor is currently saying, clamped to two lines.
 */
function CaptionOverlay({ text, answer }: { text: string; answer?: boolean }) {
  return (
    <div
      data-no-capture="true"
      className="pointer-events-none absolute inset-x-0 bottom-0 z-20 flex justify-center px-3 pb-3 sm:px-6 sm:pb-4"
    >
      <p
        key={text}
        className={cn(
          "tutor-fade-in line-clamp-2 max-w-3xl rounded-xl px-4 py-2 text-center text-sm font-medium leading-snug text-white shadow-lg sm:text-base",
          answer ? "bg-amber-900/85" : "bg-slate-900/85",
        )}
      >
        {text}
      </p>
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
