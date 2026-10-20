// Types for the live whiteboard tutor (Phase 1: local Qwen3 via Ollama, text-first).
//
// The model streams a lesson as JSON Lines (JSONL): one teaching "beat" per line.
// Each beat has what the tutor SAYS (narration) and, optionally, what it draws on
// the BOARD. Keeping one beat per line makes the stream easy to parse incrementally
// so the whiteboard fills in live, step by step.

export type LessonDepth = "quick" | "standard" | "deep";

/**
 * Teaching language (what the tutor SAYS vs what it WRITES on the board):
 * - "english"    — speak English,    write English (default).
 * - "urdu"       — speak Urdu (Urdu script, read aloud by an Urdu voice),
 *                  write Roman Urdu on the board.
 * - "roman-urdu" — speak Roman Urdu (read by a normal English voice, so it needs
 *                  no Urdu voice and works on any device), write English on the board.
 */
export type LessonLanguage = "english" | "urdu" | "roman-urdu";

export type BoardBlockKind =
  | "title"
  | "text"
  | "bullet"
  | "equation"
  | "code"
  | "note"
  | "diagram"
  | "array"
  | "flow"
  | "graph"
  | "tree"
  | "image"
  | "clear";

/** A labeled pointer that sits above a specific cell of an `array` visual. */
export interface ArrayPointer {
  label: string;
  at: number;
}

/** A labeled point plotted on a `graph` visual (x/y in 0..10). */
export interface GraphPoint {
  x: number;
  y: number;
  label?: string;
}

export interface BoardBlock {
  kind: BoardBlockKind;
  /** Primary text for title/text/bullet/equation/code/note. */
  text?: string;
  /** Language hint for code blocks. */
  lang?: string;
  /**
   * For `diagram`: labeled nodes in a left-to-right row.
   * For `tree`: a binary tree in LEVEL ORDER — index i's children are at 2i+1
   * and 2i+2; use "null" or "" for a missing child.
   */
  nodes?: string[];
  /** Arrows between node indices, e.g. [[0,1],[1,2]]. */
  arrows?: [number, number][];
  /** Cell values for an `array` visual (e.g. a sorted list). */
  cells?: (string | number)[];
  /** Labeled pointers above array cells (e.g. low/mid/high). */
  pointers?: ArrayPointer[];
  /** Ordered steps for a `flow` flowchart; a step ending in "?" is a decision. */
  steps?: string[];
  /** Axis labels for a `graph` visual. */
  xLabel?: string;
  yLabel?: string;
  /** Overall shape of the plotted line on a `graph`. */
  shape?: "line" | "up" | "down" | "parabola" | "wave";
  /** Labeled points plotted on a `graph` (x/y each in 0..10). */
  points?: GraphPoint[];
  /** For `image`: what to search for / generate (a photo or diagram is shown). */
  query?: string;
  /** For `image`: true = GENERATE a custom illustration; false/omit = search the web. */
  generate?: boolean;
}

export interface LessonStep {
  /** Stable id assigned on the client as steps arrive. */
  id: string;
  /** Conversational narration for this beat (shown in the transcript). */
  say: string;
  /** Optional thing drawn on the whiteboard for this beat. */
  board?: BoardBlock | null;
  /** Marks a step produced while answering a student interruption. */
  answer?: boolean;
  /** Slide mode: the 1-based slide line number this beat is teaching (for highlight). */
  focus?: number;
}

/** A rendered block on the whiteboard (board blocks flattened from steps). */
export interface BoardItem extends BoardBlock {
  id: string;
  answer?: boolean;
}

export type TutorPhase =
  | "idle"
  | "teaching"
  | "listening" // student interrupted; tutor paused, awaiting / answering the question
  | "answering"
  | "paused" // finished answering, lesson can resume
  | "done"
  | "error";

/** One entry of the running transcript (narration + raised-hand questions). */
export interface TranscriptEntry {
  id: string;
  role: "tutor" | "student";
  text: string;
  answer?: boolean;
}

/** Chat message sent to the model (Ollama /api/chat format). */
export interface TutorChatMessage {
  role: "system" | "user" | "assistant";
  content: string;
}
