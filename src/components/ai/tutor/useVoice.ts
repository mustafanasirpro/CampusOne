import { useCallback, useEffect, useRef, useState } from "react";

// Minimal typings for the Web Speech API (not in the DOM lib).
interface SpeechRecognitionResultLike {
  0: { transcript: string };
  isFinal: boolean;
}
interface SpeechRecognitionEventLike {
  resultIndex?: number;
  results: ArrayLike<SpeechRecognitionResultLike>;
}
interface SpeechRecognitionLike {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  maxAlternatives: number;
  start: () => void;
  stop: () => void;
  abort: () => void;
  onresult: ((event: SpeechRecognitionEventLike) => void) | null;
  onerror: (() => void) | null;
  onend: (() => void) | null;
}
type SpeechRecognitionCtor = new () => SpeechRecognitionLike;

const VOICE_PREF_KEY = "campusone.tutor.voiceOn";

function getRecognitionCtor(): SpeechRecognitionCtor | null {
  if (typeof window === "undefined") return null;
  const w = window as unknown as {
    SpeechRecognition?: SpeechRecognitionCtor;
    webkitSpeechRecognition?: SpeechRecognitionCtor;
  };
  return w.SpeechRecognition ?? w.webkitSpeechRecognition ?? null;
}

const TTS_SUPPORTED =
  typeof window !== "undefined" && "speechSynthesis" in window;

function wordsOf(text: string): Set<string> {
  return new Set(
    text
      .toLowerCase()
      .replace(/[^a-z0-9\s]/g, " ")
      .split(/\s+/)
      .filter(Boolean),
  );
}

export interface UseVoice {
  ttsSupported: boolean;
  sttSupported: boolean;
  voiceOn: boolean;
  toggleVoice: () => void;
  speaking: boolean;
  listening: boolean;
  /** Speak text; resolves when speech ends (or is cancelled / times out). */
  speak: (text: string) => Promise<void>;
  cancelSpeech: () => void;
  /** Set the speaking rate (1 = normal). Scales spoken pace with the UI speed. */
  setRate: (rate: number) => void;
  startListening: (onResult: (transcript: string) => void) => void;
  stopListening: () => void;
  // --- Live voice conversation (hands-free) ---
  /** Whether a live voice session is active. */
  conversing: boolean;
  /** Begin a hands-free session; `onUtterance` fires for each spoken utterance. */
  startConversation: (onUtterance: (transcript: string) => void) => void;
  /** End the live voice session (stops mic + any speech). */
  stopConversation: () => void;
  /** Turn the continuous mic on/off — the console gates this on whose turn it is. */
  setListeningEnabled: (on: boolean) => void;
}

export function useVoice(): UseVoice {
  const sttCtor = getRecognitionCtor();
  const sttSupported = sttCtor != null;

  const [voiceOn, setVoiceOn] = useState<boolean>(() => {
    if (!TTS_SUPPORTED) return false;
    try {
      const saved = localStorage.getItem(VOICE_PREF_KEY);
      return saved === null ? true : saved === "1";
    } catch {
      return true;
    }
  });
  const [speaking, setSpeaking] = useState(false);
  const [listening, setListening] = useState(false);
  const [conversing, setConversing] = useState(false);

  const recognitionRef = useRef<SpeechRecognitionLike | null>(null);
  const preferredVoiceRef = useRef<SpeechSynthesisVoice | null>(null);
  const rateRef = useRef(1);
  // Live conversation: a self-restarting recognition loop, gated by `enabledRef`
  // (the console turns it off while the tutor is talking/thinking, on when it's the
  // student's turn). `onUtteranceRef` receives each completed utterance.
  const enabledRef = useRef(false);
  const onUtteranceRef = useRef<((t: string) => void) | null>(null);
  const convLoopRef = useRef<() => void>(() => {});
  const convErrorsRef = useRef(0); // consecutive mic errors (e.g. permission denied)
  // Echo filter: remember what the tutor just SAID so the always-on mic can ignore
  // the tutor's own voice coming back through the speakers, while still reacting the
  // instant the student says something different (true barge-in / Jarvis).
  const recentSpokenRef = useRef<{ at: number; words: Set<string> }[]>([]);
  const lastSentRef = useRef(0); // debounce rapid duplicate utterances

  // Pick a natural English voice once voices are available.
  useEffect(() => {
    if (!TTS_SUPPORTED) return;
    const pick = () => {
      const voices = window.speechSynthesis.getVoices();
      if (voices.length === 0) return;
      preferredVoiceRef.current =
        voices.find((v) => /en[-_]US/i.test(v.lang) && /google/i.test(v.name)) ??
        voices.find((v) => /en[-_]US/i.test(v.lang) && /natural|aria|jenny/i.test(v.name)) ??
        voices.find((v) => /^en/i.test(v.lang)) ??
        voices[0];
    };
    pick();
    window.speechSynthesis.onvoiceschanged = pick;
    return () => {
      window.speechSynthesis.onvoiceschanged = null;
    };
  }, []);

  useEffect(() => {
    return () => {
      if (TTS_SUPPORTED) window.speechSynthesis.cancel();
      recognitionRef.current?.abort();
    };
  }, []);

  const toggleVoice = useCallback(() => {
    setVoiceOn((prev) => {
      const next = !prev;
      try {
        localStorage.setItem(VOICE_PREF_KEY, next ? "1" : "0");
      } catch {
        // ignore storage errors
      }
      if (!next && TTS_SUPPORTED) window.speechSynthesis.cancel();
      return next;
    });
  }, []);

  const speak = useCallback((text: string): Promise<void> => {
    if (!TTS_SUPPORTED || !text.trim()) return Promise.resolve();
    // Remember these words so the always-on mic can discard them as echo.
    recentSpokenRef.current.push({ at: Date.now(), words: wordsOf(text) });
    const cutoff = Date.now() - 9000;
    recentSpokenRef.current = recentSpokenRef.current.filter((e) => e.at >= cutoff);
    return new Promise<void>((resolve) => {
      try {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(text);
        if (preferredVoiceRef.current) utterance.voice = preferredVoiceRef.current;
        // Clamp to the Web Speech valid range; follows the UI speed control.
        const rate = Math.min(2, Math.max(0.5, rateRef.current));
        utterance.rate = rate;
        utterance.pitch = 1.0;
        let done = false;
        const finish = () => {
          if (done) return;
          done = true;
          clearTimeout(safety);
          setSpeaking(false);
          resolve();
        };
        // Safety net: some browsers don't reliably fire onend. Resolve anyway so
        // playback never stalls — estimate ~1 word / 380ms plus a buffer.
        const words = text.trim().split(/\s+/).length;
        const safety = setTimeout(finish, (words * 420) / rate + 4000);
        utterance.onend = finish;
        utterance.onerror = finish;
        setSpeaking(true);
        window.speechSynthesis.speak(utterance);
      } catch {
        setSpeaking(false);
        resolve();
      }
    });
  }, []);

  const cancelSpeech = useCallback(() => {
    if (TTS_SUPPORTED) window.speechSynthesis.cancel();
    setSpeaking(false);
  }, []);

  const setRate = useCallback((rate: number) => {
    rateRef.current = rate;
  }, []);

  const stopListening = useCallback(() => {
    recognitionRef.current?.stop();
    setListening(false);
  }, []);

  const startListening = useCallback(
    (onResult: (transcript: string) => void) => {
      if (!sttCtor) return;
      // Stop the tutor talking so the mic doesn't hear it (barge-in).
      if (TTS_SUPPORTED) window.speechSynthesis.cancel();
      recognitionRef.current?.abort();
      const recognition = new sttCtor();
      recognition.lang = "en-US";
      recognition.continuous = false;
      recognition.interimResults = false;
      recognition.maxAlternatives = 1;
      recognition.onresult = (event) => {
        const transcript = event.results?.[0]?.[0]?.transcript?.trim();
        if (transcript) onResult(transcript);
      };
      recognition.onerror = () => setListening(false);
      recognition.onend = () => setListening(false);
      recognitionRef.current = recognition;
      try {
        recognition.start();
        setListening(true);
      } catch {
        setListening(false);
      }
    },
    [sttCtor],
  );

  // --- Live voice conversation --------------------------------------------
  // True if the transcript is (mostly) the tutor's own recent speech bouncing back
  // through the speakers. Short utterances (commands like "stop", "go back") always
  // pass so they're never swallowed; longer ones are checked against what the tutor
  // just said. On headphones there's no echo, so this almost never triggers.
  const isLikelyEcho = useCallback((transcript: string): boolean => {
    const words = [...wordsOf(transcript)];
    if (words.length < 4) return false; // let short commands through
    const spoken = new Set<string>();
    for (const e of recentSpokenRef.current) for (const w of e.words) spoken.add(w);
    if (spoken.size === 0) return false;
    let hits = 0;
    for (const w of words) if (spoken.has(w)) hits += 1;
    return hits / words.length >= 0.65;
  }, []);

  // A continuous recogniser that stays on through the whole session (even while the
  // tutor speaks) so the student can barge in anytime. It auto-restarts if it drops.
  const convLoop = useCallback(() => {
    if (!sttCtor || !enabledRef.current) return;
    if (recognitionRef.current) {
      try {
        recognitionRef.current.abort();
      } catch {
        /* ignore */
      }
    }
    const rec = new sttCtor();
    rec.lang = "en-US";
    rec.continuous = true; // stay listening even while the tutor is talking
    rec.interimResults = false;
    rec.maxAlternatives = 1;
    rec.onresult = (event) => {
      if (!enabledRef.current) return;
      // Take the newest final transcript.
      let transcript = "";
      const from = event.resultIndex ?? 0;
      for (let i = from; i < event.results.length; i += 1) {
        const r = event.results[i];
        if (r.isFinal) transcript += r[0].transcript;
      }
      transcript = transcript.trim();
      if (!transcript) return;
      convErrorsRef.current = 0; // a real result → the mic is working
      if (isLikelyEcho(transcript)) return; // the tutor's own voice — ignore it
      const now = Date.now();
      if (now - lastSentRef.current < 900) return; // debounce split sentences
      lastSentRef.current = now;
      onUtteranceRef.current?.(transcript);
    };
    rec.onerror = () => {
      convErrorsRef.current += 1;
      setListening(false);
    };
    rec.onend = () => {
      setListening(false);
      // Keep the mic alive between utterances while it's still the student's turn,
      // but give up after repeated errors (e.g. the user denied microphone access).
      if (enabledRef.current && convErrorsRef.current < 4) {
        setTimeout(() => convLoopRef.current(), 350);
      }
    };
    recognitionRef.current = rec;
    try {
      rec.start();
      setListening(true);
    } catch {
      setListening(false);
    }
  }, [sttCtor, isLikelyEcho]);
  useEffect(() => {
    convLoopRef.current = convLoop;
  }, [convLoop]);

  const setListeningEnabled = useCallback(
    (on: boolean) => {
      if (on === enabledRef.current) return;
      enabledRef.current = on;
      if (on) {
        convErrorsRef.current = 0; // fresh turn — reset the error backoff
        convLoop();
      } else {
        try {
          recognitionRef.current?.abort();
        } catch {
          /* ignore */
        }
        setListening(false);
      }
    },
    [convLoop],
  );

  const startConversation = useCallback(
    (onUtterance: (t: string) => void) => {
      onUtteranceRef.current = onUtterance;
      setConversing(true);
      // The console's effect arms the mic when it's the student's turn.
    },
    [],
  );

  const stopConversation = useCallback(() => {
    onUtteranceRef.current = null;
    enabledRef.current = false;
    setConversing(false);
    setListening(false);
    try {
      recognitionRef.current?.abort();
    } catch {
      /* ignore */
    }
    if (TTS_SUPPORTED) window.speechSynthesis.cancel();
  }, []);

  return {
    ttsSupported: TTS_SUPPORTED,
    sttSupported,
    voiceOn: voiceOn && TTS_SUPPORTED,
    toggleVoice,
    speaking,
    listening,
    speak,
    cancelSpeech,
    setRate,
    startListening,
    stopListening,
    conversing,
    startConversation,
    stopConversation,
    setListeningEnabled,
  };
}
