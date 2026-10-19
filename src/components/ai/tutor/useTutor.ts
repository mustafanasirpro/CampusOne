import { useCallback, useEffect, useRef, useState } from "react";

import {
  LessonStepParser,
  assistantTurnFromRaw,
  buildLessonFromSourceMessages,
  buildLessonOpeningMessages,
  buildSlideLessonMessages,
  checkTutorModel,
  followUpMessage,
  resumeMessage,
  streamChat,
  studentQuestionMessage,
  TutorModelError,
  warmUpModel,
} from "@/api/tutorClient";
import type {
  BoardItem,
  LessonDepth,
  LessonLanguage,
  LessonStep,
  TranscriptEntry,
  TutorChatMessage,
  TutorPhase,
} from "@/types/tutor";

import { recordLesson } from "./journey";
import type { DocSlide } from "./pdfSlides";
import { prefetchImage } from "./topicImage";
import { streamVision } from "./visionClient";

export type TutorMode = "topic" | "document";

const STORAGE_KEY = "campusone.tutor.lastLesson";

interface SavedLesson {
  topic: string;
  boardItems: BoardItem[];
  transcript: TranscriptEntry[];
  history: TutorChatMessage[];
}

function loadSaved(): SavedLesson | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as SavedLesson;
    if (!parsed.topic || !Array.isArray(parsed.boardItems)) return null;
    return parsed;
  } catch {
    return null;
  }
}

let idCounter = 0;
const nextId = (prefix: string) => `${prefix}-${(idCounter += 1)}`;

// Playback pacing: the WHOLE lesson is generated first, then played back from the
// buffer — so beats flow continuously at reading speed and never stall waiting on
// the model. These are comfortable, tutor-like reading times.
const MIN_DWELL_MS = 1100;
const PER_WORD_MS = 240;
const MAX_DWELL_MS = 4200;

function dwellFor(step: LessonStep): number {
  const words = step.say ? step.say.trim().split(/\s+/).length : 4;
  const base = Math.min(MAX_DWELL_MS, Math.max(MIN_DWELL_MS, words * PER_WORD_MS));
  // Board items with more to read get a touch longer.
  return step.board && step.board.kind !== "clear" ? base + 300 : base;
}

function normalizeWords(text: string): Set<string> {
  return new Set(
    text
      .toLowerCase()
      .replace(/[^a-z0-9\s]/g, " ")
      .split(/\s+/)
      .filter(Boolean),
  );
}

function overlap(a: Set<string>, b: Set<string>): number {
  if (a.size === 0 || b.size === 0) return 0;
  let inter = 0;
  for (const w of a) if (b.has(w)) inter += 1;
  return inter / Math.min(a.size, b.size);
}

/**
 * Cleans a fully-generated lesson before playback: drops near-duplicate beats
 * (Qwen3 often restates the same point in slightly different words) and cuts the
 * lesson at a restart boundary (a second title). Runs on the complete buffer, so
 * it's far more reliable than detecting duplicates mid-stream.
 */
export function cleanLesson(steps: LessonStep[]): LessonStep[] {
  const kept: LessonStep[] = [];
  const keptWords: Set<string>[] = [];
  const keptBoards = new Set<string>();
  let titles = 0;
  for (const step of steps) {
    if (step.board?.kind === "title") {
      titles += 1;
      if (titles >= 2) break; // second title → the model restarted; stop here
    }
    const boardKey = step.board?.text?.trim().toLowerCase() ?? "";
    if (boardKey && keptBoards.has(boardKey)) continue; // duplicate board line
    const words = normalizeWords(step.say);
    const isDup =
      words.size >= 3 && keptWords.some((prev) => overlap(words, prev) >= 0.7);
    if (isDup) continue;
    kept.push(step);
    keptWords.push(words);
    if (boardKey) keptBoards.add(boardKey);
  }
  return kept;
}

export interface TutorModelStatus {
  checked: boolean;
  running: boolean;
  modelInstalled: boolean;
}

export interface UseTutor {
  phase: TutorPhase;
  topic: string;
  boardItems: BoardItem[];
  transcript: TranscriptEntry[];
  error: string | null;
  status: TutorModelStatus;
  /** How many beats are ready while a lesson is still being prepared. */
  preparedCount: number;
  /** Playback speed multiplier (1 = normal). */
  speed: number;
  setSpeed: (value: number) => void;
  /** Teaching language (English / Roman Urdu / Urdu voice). */
  language: LessonLanguage;
  setLanguage: (value: LessonLanguage) => void;
  startLesson: (topic: string, depth?: LessonDepth) => void;
  startLessonFromSource: (
    title: string,
    sourceText: string,
    depth?: LessonDepth,
  ) => void;
  // Document ("master a document") mode.
  mode: TutorMode;
  docSlides: DocSlide[];
  docTitle: string;
  currentSlide: number;
  focusLine: number | null;
  startDocument: (slides: DocSlide[], title: string, depth?: LessonDepth) => void;
  /** Teach a specific slide now. */
  goToSlide: (index: number) => void;
  /** Just SHOW a slide without teaching it (free navigation / skip ahead). */
  viewSlide: (index: number) => void;
  nextSlide: () => void;
  prevSlide: () => void;
  interrupt: (question: string) => void;
  askAboutBoard: (
    question: string,
    imageDataUrl: string,
    boardText: string,
  ) => void;
  resume: () => void;
  followUp: (instruction: string) => void;
  skipPacing: () => void;
  stop: () => void;
  reset: () => void;
  refreshStatus: () => void;
  setVoiceControl: (
    control: { speak: (t: string) => Promise<void>; cancel: () => void } | null,
  ) => void;
}

export function useTutor(): UseTutor {
  // Restore the last lesson (if any) once, via lazy initializers, so a
  // slow-to-generate lesson survives a refresh. It returns as a finished lesson;
  // follow-ups still work because the model conversation is restored too.
  const [initial] = useState<SavedLesson | null>(() => loadSaved());

  const [phase, setPhase] = useState<TutorPhase>(initial ? "done" : "idle");
  const [topic, setTopic] = useState(initial?.topic ?? "");
  const [boardItems, setBoardItems] = useState<BoardItem[]>(
    initial?.boardItems ?? [],
  );
  const [transcript, setTranscript] = useState<TranscriptEntry[]>(
    initial?.transcript ?? [],
  );
  const [error, setError] = useState<string | null>(null);
  const [preparedCount, setPreparedCount] = useState(0);
  const [speed, setSpeedState] = useState(1);
  const [language, setLanguageState] = useState<LessonLanguage>("english");
  const languageRef = useRef<LessonLanguage>("english");
  const [status, setStatus] = useState<TutorModelStatus>({
    checked: false,
    running: false,
    modelInstalled: false,
  });

  // Document mode state (+ refs for synchronous next/prev).
  const [mode, setMode] = useState<TutorMode>("topic");
  const [docSlides, setDocSlides] = useState<DocSlide[]>([]);
  const [docTitle, setDocTitle] = useState("");
  const [currentSlide, setCurrentSlide] = useState(0);
  const [focusLine, setFocusLine] = useState<number | null>(null);
  const docSlidesRef = useRef<DocSlide[]>([]);
  const docTitleRef = useRef("");
  const docDepthRef = useRef<LessonDepth>("standard");
  const currentSlideRef = useRef(0);

  // Conversation + generation refs (read synchronously; state is async).
  const historyRef = useRef<TutorChatMessage[]>(initial?.history ?? []);
  // Guards the Learning Journey record so a lesson is logged once per session, not
  // on every teaching→done toggle (follow-ups, resume). Null until first recorded.
  const recordedKeyRef = useRef<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);
  const turnRef = useRef<Promise<unknown> | null>(null);
  const rawAccumRef = useRef("");

  // Playback refs: beats are generated into a queue and revealed on a timer.
  const pendingRef = useRef<LessonStep[]>([]);
  const genDoneRef = useRef(false);
  const pumpingRef = useRef(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const endPhaseRef = useRef<TutorPhase>("done");
  const fastRef = useRef(false); // "reveal the rest now" — drop pacing delays
  const speedRef = useRef(1); // playback speed multiplier (0.75 / 1 / 1.5)
  const revealedThisTurnRef = useRef(0); // to detect a turn that produced nothing
  // Voice: when set, playback is paced by speech (next beat advances when the
  // tutor finishes speaking) instead of a timer. playToken invalidates pending
  // speech-advances when the turn is interrupted/stopped/fast-forwarded.
  const voiceRef = useRef<{
    speak: (text: string) => Promise<void>;
    cancel: () => void;
  } | null>(null);
  const playTokenRef = useRef(0);

  useEffect(() => {
    // Clear any pending playback timer on unmount.
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
      abortRef.current?.abort();
    };
  }, []);

  // Persist whenever a lesson settles with content on the board.
  useEffect(() => {
    if ((phase === "done" || phase === "paused") && boardItems.length > 0) {
      try {
        const payload: SavedLesson = {
          topic,
          boardItems,
          transcript,
          history: historyRef.current,
        };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
      } catch {
        // Storage may be unavailable (private mode / quota) — non-fatal.
      }
    }
  }, [phase, boardItems, transcript, topic]);

  // Record a completed TOPIC lesson into the Learning Journey (once per session).
  // Documents are recorded when opened (see startDocument). Skips the restored
  // lesson on first mount — that's not a fresh completion.
  const restoredRef = useRef(initial != null);
  useEffect(() => {
    if (restoredRef.current) {
      restoredRef.current = false;
      return;
    }
    if (phase === "done" && mode === "topic" && topic.trim()) {
      const key = `topic:${topic.trim().toLowerCase()}`;
      if (recordedKeyRef.current !== key) {
        recordedKeyRef.current = key;
        recordLesson(topic, "topic");
      }
    }
  }, [phase, mode, topic]);

  const refreshStatus = useCallback(() => {
    void checkTutorModel().then((result) => {
      setStatus({ checked: true, ...result });
      // Pre-load the model so the first lesson doesn't pay the cold start.
      if (result.running && result.modelInstalled) void warmUpModel();
    });
  }, []);

  const revealStep = useCallback((step: LessonStep) => {
    if (step.say) {
      // Compute the id outside the updater: nextId() mutates a counter, and
      // React double-invokes updaters in StrictMode, which would shift ids.
      const sayId = nextId("t");
      setTranscript((prev) => [
        ...prev,
        { id: sayId, role: "tutor", text: step.say, answer: step.answer },
      ]);
    }
    if (step.focus) setFocusLine(step.focus);
    const board = step.board;
    if (!board) return;
    if (board.kind === "clear") {
      if (!step.answer) setBoardItems([]);
      return;
    }
    const boardId = nextId("b");
    setBoardItems((prev) => [
      ...prev,
      { ...board, id: boardId, answer: step.answer },
    ]);
  }, []);

  // Playback loop: reveal one beat, wait, repeat. Pauses itself when the queue
  // is empty and resumes when generation feeds more. Finalizes when generation
  // is done and nothing is left to show.
  const pumpRef = useRef<() => void>(() => {});
  const pump = useCallback(() => {
    const next = pendingRef.current.shift();
    if (!next) {
      pumpingRef.current = false;
      if (genDoneRef.current) {
        // A turn that produced no beats at all means the model misbehaved
        // (e.g. spent its whole budget reasoning). Surface a retry instead of
        // silently ending on an empty board.
        if (revealedThisTurnRef.current === 0) {
          setError(
            "The tutor didn't produce a lesson that time. Please try again.",
          );
          setPhase("error");
        } else {
          setPhase(endPhaseRef.current);
        }
      }
      return;
    }
    pumpingRef.current = true;
    revealedThisTurnRef.current += 1;
    revealStep(next);
    const voice = voiceRef.current;
    if (voice && next.say && !fastRef.current) {
      // Speech-paced: speak this beat, advance when the tutor finishes talking.
      const token = playTokenRef.current;
      void voice.speak(next.say).then(() => {
        if (playTokenRef.current === token) pumpRef.current();
      });
    } else {
      const delay = fastRef.current ? 0 : dwellFor(next) / speedRef.current;
      timerRef.current = setTimeout(() => pumpRef.current(), delay);
    }
  }, [revealStep]);
  useEffect(() => {
    pumpRef.current = pump;
  }, [pump]);

  const ensurePump = useCallback(() => {
    if (!pumpingRef.current && pendingRef.current.length > 0) {
      pump();
    } else if (
      !pumpingRef.current &&
      genDoneRef.current &&
      pendingRef.current.length === 0
    ) {
      setPhase(endPhaseRef.current);
    }
  }, [pump]);

  /**
   * Generates one whole turn into a buffer (nothing is shown yet — the UI shows
   * "preparing"), then cleans it and plays it back continuously so teaching never
   * stalls waiting on the model. Records the assistant text into history even if
   * aborted, so the follow-up (answer / resume) turn sees a consistent chat.
   *
   * An early-abort heuristic stops generation as soon as the model clearly
   * restarts the lesson (a second title), so we don't wait minutes for output we
   * would throw away. The final cleanLesson() pass removes paraphrased duplicates.
   */
  const runTurn = useCallback(
    async (
      messages: TutorChatMessage[],
      markAnswer: boolean,
      endPhase: TutorPhase,
      // Optional alternate generator (e.g. a vision model) with the same
      // onDelta/signal contract as streamChat; defaults to the text model.
      streamOverride?: (args: {
        signal: AbortSignal;
        onDelta: (text: string) => void;
      }) => Promise<string>,
      // Vision models answer in prose, not our JSON beats — if nothing parsed,
      // show the raw text as one spoken beat instead of erroring.
      plainTextFallback = false,
    ) => {
      const controller = new AbortController();
      abortRef.current = controller;
      const parser = new LessonStepParser(nextId("p"), markAnswer);
      rawAccumRef.current = "";
      genDoneRef.current = false;
      endPhaseRef.current = endPhase;
      revealedThisTurnRef.current = 0;
      setPreparedCount(0);

      // Buffer beats as they are generated; do NOT reveal them yet.
      const buffer: LessonStep[] = [];
      let seenTitle = false;
      let truncated = false; // early-abort because the model restarted
      const ingest = (steps: LessonStep[]) => {
        for (const step of steps) {
          if (truncated) break;
          if (step.board?.kind === "title") {
            if (seenTitle) {
              truncated = true;
              controller.abort();
              break;
            }
            seenTitle = true;
          }
          buffer.push(step);
        }
        setPreparedCount(buffer.length);
      };

      let userAbort = false;
      try {
        const onDelta = (text: string) => {
          rawAccumRef.current += text;
          ingest(parser.push(text));
        };
        const raw = streamOverride
          ? await streamOverride({ signal: controller.signal, onDelta })
          : await streamChat({ messages, signal: controller.signal, onDelta });
        ingest(parser.flush());
        historyRef.current = [...historyRef.current, assistantTurnFromRaw(raw)];
      } catch (caught) {
        const aborted =
          caught instanceof DOMException && caught.name === "AbortError";
        const partial = rawAccumRef.current.trim();
        if (partial) {
          historyRef.current = [
            ...historyRef.current,
            assistantTurnFromRaw(partial),
          ];
        }
        if (!aborted) {
          setError(
            caught instanceof TutorModelError
              ? caught.message
              : "The tutor ran into a problem. Please try again.",
          );
          setPhase("error");
          return;
        }
        // Aborted without a restart => a user action (interrupt/stop/new lesson)
        // took over; that handler drives what happens next, so don't finalize.
        userAbort = !truncated;
      } finally {
        if (abortRef.current === controller) abortRef.current = null;
      }
      if (userAbort) return;

      // No JSON beats but we have prose (a vision model) — show it as one beat.
      if (buffer.length === 0 && plainTextFallback) {
        const prose = rawAccumRef.current
          .replace(/<think>[\s\S]*?<\/think>/g, "")
          .replace(/```(?:json)?/gi, "")
          .trim();
        if (prose) {
          buffer.push({
            id: nextId("p"),
            say: prose.slice(0, 1200),
            board: null,
            answer: true,
          });
        }
      }

      // Lesson fully prepared — clean it, load it, and start continuous playback.
      pendingRef.current = cleanLesson(buffer);
      // Kick off image fetches NOW (while playback runs) so pictures are cached and
      // appear instantly when their beat is revealed — not after the lesson ends.
      for (const step of pendingRef.current) {
        if (step.board?.kind === "image" && step.board.query) {
          prefetchImage(step.board.query, !!step.board.generate);
        }
      }
      genDoneRef.current = true;
      ensurePump();
    },
    [ensurePump],
  );

  const clearPlayback = useCallback(() => {
    playTokenRef.current += 1; // invalidate any in-flight speech-paced advance
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    pumpingRef.current = false;
    pendingRef.current = [];
    genDoneRef.current = false;
    fastRef.current = false;
    voiceRef.current?.cancel(); // stop the tutor talking
  }, []);

  // Reveal any remaining (and still-incoming) beats without pacing delays.
  const skipPacing = useCallback(() => {
    fastRef.current = true;
    playTokenRef.current += 1; // drop any pending speech-advance; we re-drive below
    voiceRef.current?.cancel();
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    pump();
  }, [pump]);

  /** Set playback speed (affects timer pacing; speech rate is set in the UI). */
  const setSpeed = useCallback((value: number) => {
    speedRef.current = value;
    setSpeedState(value);
  }, []);

  /** Set the teaching language (applies to the next lesson/turn). */
  const setLanguage = useCallback((value: LessonLanguage) => {
    languageRef.current = value;
    setLanguageState(value);
  }, []);

  /** Set (or clear) the voice control used to pace playback by speech. */
  const setVoiceControl = useCallback(
    (control: { speak: (t: string) => Promise<void>; cancel: () => void } | null) => {
      voiceRef.current = control;
    },
    [],
  );

  const startLesson = useCallback(
    (nextTopic: string, depth: LessonDepth = "standard") => {
      const trimmed = nextTopic.trim();
      if (!trimmed) return;
      abortRef.current?.abort();
      clearPlayback();
      recordedKeyRef.current = null; // a fresh lesson should count again
      setTopic(trimmed);
      setBoardItems([]);
      setTranscript([]);
      setError(null);
      setPhase("teaching");
      const opening = buildLessonOpeningMessages(trimmed, depth, languageRef.current);
      historyRef.current = opening;
      turnRef.current = runTurn(opening, false, "done");
    },
    [clearPlayback, runTurn],
  );

  const startLessonFromSource = useCallback(
    (title: string, sourceText: string, depth: LessonDepth = "standard") => {
      const trimmed = title.trim();
      if (!trimmed || !sourceText.trim()) return;
      abortRef.current?.abort();
      clearPlayback();
      setTopic(trimmed);
      setBoardItems([]);
      setTranscript([]);
      setError(null);
      setPhase("teaching");
      const opening = buildLessonFromSourceMessages(
        trimmed,
        sourceText,
        depth,
        languageRef.current,
      );
      historyRef.current = opening;
      turnRef.current = runTurn(opening, false, "done");
    },
    [clearPlayback, runTurn],
  );

  const teachSlide = useCallback(
    (index: number) => {
      const slides = docSlidesRef.current;
      if (index < 0 || index >= slides.length) return;
      abortRef.current?.abort();
      clearPlayback();
      currentSlideRef.current = index;
      setCurrentSlide(index);
      setFocusLine(null);
      setBoardItems([]);
      setTranscript([]);
      setError(null);
      setPhase("teaching");
      const outline = slides
        .map(
          (s, i) =>
            `Slide ${i + 1}: ${(s.lines[0] || "(untitled slide)").slice(0, 70)}`,
        )
        .join("\n");
      const messages = buildSlideLessonMessages(
        docTitleRef.current,
        slides[index].lines,
        index + 1,
        slides.length,
        outline,
        docDepthRef.current,
        languageRef.current,
      );
      historyRef.current = messages;
      turnRef.current = runTurn(messages, false, "done");
    },
    [clearPlayback, runTurn],
  );

  const startDocument = useCallback(
    (slides: DocSlide[], title: string, depth: LessonDepth = "standard") => {
      if (slides.length === 0) return;
      docSlidesRef.current = slides;
      docTitleRef.current = title.trim() || "Your document";
      docDepthRef.current = depth;
      recordLesson(docTitleRef.current, "document"); // log it in the journey
      setDocSlides(slides);
      setDocTitle(docTitleRef.current);
      setMode("document");
      setTopic(docTitleRef.current);
      // Show slide 1 first (don't auto-teach) so the student can read / skip ahead
      // and press "Teach this slide" when ready.
      currentSlideRef.current = 0;
      setCurrentSlide(0);
      setFocusLine(null);
      setBoardItems([]);
      setTranscript([]);
      setError(null);
      setPhase("paused");
    },
    [],
  );

  const goToSlide = useCallback((index: number) => teachSlide(index), [teachSlide]);
  const nextSlide = useCallback(
    () => teachSlide(currentSlideRef.current + 1),
    [teachSlide],
  );
  const prevSlide = useCallback(
    () => teachSlide(currentSlideRef.current - 1),
    [teachSlide],
  );

  // Free navigation: just SHOW a slide (stop any teaching, clear the board) so the
  // student can flip through / skip ahead and read before the tutor explains.
  const viewSlide = useCallback(
    (index: number) => {
      const slides = docSlidesRef.current;
      if (index < 0 || index >= slides.length) return;
      abortRef.current?.abort();
      clearPlayback();
      currentSlideRef.current = index;
      setCurrentSlide(index);
      setFocusLine(null);
      setBoardItems([]);
      setTranscript([]);
      setError(null);
      setPhase("paused"); // "paused" in doc mode = showing a slide, ready to teach
    },
    [clearPlayback],
  );

  const interrupt = useCallback(
    (question: string) => {
      const trimmed = question.trim();
      if (!trimmed) return;
      void (async () => {
        // Stop teaching + playback, let the current turn settle (records any
        // partial output), then ask the question.
        abortRef.current?.abort();
        clearPlayback();
        await turnRef.current?.catch(() => undefined);
        const studentId = nextId("t");
        setTranscript((prev) => [
          ...prev,
          { id: studentId, role: "student", text: trimmed },
        ]);
        setPhase("answering");
        setError(null);
        const messages = [
          ...historyRef.current,
          studentQuestionMessage(trimmed, languageRef.current),
        ];
        historyRef.current = messages;
        turnRef.current = runTurn(messages, true, "paused");
        await turnRef.current;
      })();
    },
    [clearPlayback, runTurn],
  );

  // Vision interrupt: the student drew/circled on the board. Capture is done by
  // the UI; here we send that image to the vision model and play back its answer.
  const askAboutBoard = useCallback(
    (question: string, imageDataUrl: string, boardText: string) => {
      void (async () => {
        abortRef.current?.abort();
        clearPlayback();
        await turnRef.current?.catch(() => undefined);
        const drawId = nextId("t");
        setTranscript((prev) => [
          ...prev,
          {
            id: drawId,
            role: "student",
            text: question.trim() || "✍️ (about what I drew on the board)",
          },
        ]);
        setPhase("answering");
        setError(null);
        historyRef.current = [
          ...historyRef.current,
          {
            role: "user",
            content: `[The student drew on the whiteboard and asked] ${question.trim()}`,
          },
        ];
        const stream = (args: {
          signal: AbortSignal;
          onDelta: (text: string) => void;
        }) => streamVision({ imageDataUrl, question, boardText, ...args });
        turnRef.current = runTurn(historyRef.current, true, "paused", stream, true);
        await turnRef.current;
      })();
    },
    [clearPlayback, runTurn],
  );

  const resume = useCallback(() => {
    void (async () => {
      clearPlayback();
      setPhase("teaching");
      setError(null);
      const messages = [...historyRef.current, resumeMessage()];
      historyRef.current = messages;
      turnRef.current = runTurn(messages, false, "done");
      await turnRef.current;
    })();
  }, [clearPlayback, runTurn]);

  // "Go deeper", "Give an example", "Quiz me" — continue the same lesson with a
  // specific instruction. Only meaningful once a lesson exists.
  const followUp = useCallback(
    (instruction: string) => {
      if (historyRef.current.length === 0) return;
      void (async () => {
        abortRef.current?.abort();
        clearPlayback();
        await turnRef.current?.catch(() => undefined);
        setError(null);
        setPhase("teaching");
        const messages = [...historyRef.current, followUpMessage(instruction)];
        historyRef.current = messages;
        turnRef.current = runTurn(messages, false, "done");
        await turnRef.current;
      })();
    },
    [clearPlayback, runTurn],
  );

  const stop = useCallback(() => {
    abortRef.current?.abort();
    clearPlayback();
    setPhase((prev) =>
      prev === "teaching" || prev === "answering" ? "paused" : prev,
    );
  }, [clearPlayback]);

  const reset = useCallback(() => {
    abortRef.current?.abort();
    clearPlayback();
    historyRef.current = [];
    rawAccumRef.current = "";
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // ignore storage errors
    }
    docSlidesRef.current = [];
    currentSlideRef.current = 0;
    setMode("topic");
    setDocSlides([]);
    setDocTitle("");
    setCurrentSlide(0);
    setTopic("");
    setBoardItems([]);
    setTranscript([]);
    setError(null);
    setPhase("idle");
  }, [clearPlayback]);

  return {
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
    startLessonFromSource,
    mode,
    docSlides,
    docTitle,
    currentSlide,
    focusLine,
    startDocument,
    goToSlide,
    viewSlide,
    nextSlide,
    prevSlide,
    interrupt,
    askAboutBoard,
    resume,
    followUp,
    skipPacing,
    stop,
    reset,
    refreshStatus,
    setVoiceControl,
  };
}
