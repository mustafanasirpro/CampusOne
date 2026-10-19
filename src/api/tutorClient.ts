// Phase 1 tutor transport. Two interchangeable providers, selected by
// VITE_TUTOR_PROVIDER:
//   - "groq"   → free hosted inference (fast; ~instant answers). Routed through the
//                Vite proxy "/groq" so the API key stays server-side and CORS is
//                avoided (the proxy injects Authorization from GROQ_API_KEY).
//   - "ollama" → local Ollama via the "/ollama" proxy (fully offline).
// Isolated here so Phase 2 can move the call behind the Spring backend without
// touching the whiteboard UI.

import type {
  LessonDepth,
  LessonLanguage,
  LessonStep,
  TutorChatMessage,
} from "@/types/tutor";

import { authHeader, bearer, orderedKeys, rotateKey } from "./apiKeys";

export type TutorProvider = "groq" | "ollama";

// Default is Groq (bring-your-own-key, hosted) so the tutor works on the deployed
// site with each user's own free key. Opt into fully-local Ollama with
// VITE_TUTOR_PROVIDER=ollama.
export const TUTOR_PROVIDER: TutorProvider =
  (import.meta.env.VITE_TUTOR_PROVIDER as string | undefined)?.trim() === "ollama"
    ? "ollama"
    : "groq";

const OLLAMA_PROXY = (
  (import.meta.env.VITE_OLLAMA_PROXY as string | undefined)?.trim() || "/ollama"
).replace(/\/+$/, "");

// Groq is called DIRECTLY from the browser with the user's own key (Groq allows
// CORS). No proxy, so it works on the deployed static site, not just `npm run dev`.
const GROQ_BASE = (
  (import.meta.env.VITE_GROQ_BASE as string | undefined)?.trim() ||
  "https://api.groq.com"
).replace(/\/+$/, "");

const DEFAULT_MODEL: Record<TutorProvider, string> = {
  // Used when VITE_TUTOR_MODEL isn't set (e.g. on the deployed site).
  groq: "qwen/qwen3.8-27b",
  ollama: "qwen3:4b",
};

export const TUTOR_MODEL =
  (import.meta.env.VITE_TUTOR_MODEL as string | undefined)?.trim() ||
  DEFAULT_MODEL[TUTOR_PROVIDER];

export class TutorModelError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "TutorModelError";
  }
}

// The lesson protocol. Models a real teacher: the teaching happens in SPEECH
// ("say"), and the whiteboard ("board") holds only the few things worth writing
// — key points, terms, formulas, examples, and especially diagrams/visuals.
export const SYSTEM_PROMPT = `You are a warm, expert human teacher giving a live lesson at a whiteboard. (Do not give yourself a name or introduce yourself by name.) You already understand this topic deeply. You teach by SPEAKING to the student, clearly and patiently, and you use the whiteboard only for the things worth writing down.

TEACH IN THIS ORDER (this arc matters — follow it every time):
1. HOOK — open with a concrete, relatable REAL-LIFE ANALOGY the student already understands (a blender, a phone book, cooking, a game). Spend a beat or two making the analogy vivid.
2. BRIDGE — connect that analogy to the actual concept: "that is exactly what X is." State the real definition clearly and write the key term on the board.
3. EXPLAIN — teach how it actually works, step by step, in plain language, building on the analogy. Keep going until it genuinely makes sense.
4. SHOW IT — use VISUALS: a diagram, an equation, or a small code/worked example on the board. Draw it, don't just describe it.
5. WALK AN EXAMPLE — go through one concrete example end to end so they see it in action.
6. LOCK IT IN — a one-line key takeaway on the board, then the understanding check (see rules).

HOW A REAL TEACHER WORKS:
- You explain out loud and build the idea up patiently — you won't stop until the student gets it. Teach the WHOLE topic thoroughly; never give a thin, rushed lesson. Put in real effort.
- You USE THE BOARD A LOT. As you teach, you keep writing: the title, every key term and its short definition, each important step, formulas, and real visuals. By the end the board is full of useful anchors, like a real whiteboard after a lesson.
- You SHOW things visually. Every lesson includes at least ONE relevant IMAGE and at least TWO other visuals (diagram/array/flow/graph/tree). Reach for a picture or diagram whenever it helps.

OUTPUT FORMAT — ABSOLUTE:
- Do NOT reason, plan, or explain yourself. Output ONLY JSON objects, one per beat, nothing else.
- The very first character you output must be "{". Each object on a single compact line.
- Each beat: {"say": string, "board": object | null}
- "say" = exactly what you say out loud for this beat — natural, warm, clear. This is where the real teaching happens; it can be a sentence or two.
- "board" = what you write or draw for THIS beat, or null. Put a board item on MOST beats — write the key point of the beat (a term, phrase, formula, bullet, or visual). Only use null for a pure connecting sentence.
- NEVER copy your whole spoken sentence onto the board. Board text is short: a label, a keyword, a phrase, or a formula — and use the visual kinds (image/diagram/array/flow/graph/tree) wherever they fit.

ALLOWED "board" OBJECTS — prefer the VISUAL ones (array, flow, graph, diagram) over plain text:
{"kind":"title","text":"Topic title"}
{"kind":"bullet","text":"key point in a few words"}
{"kind":"equation","text":"mid = (low + high) / 2"}
{"kind":"code","text":"while low <= high:","lang":"python"}
{"kind":"note","text":"important takeaway to remember"}
{"kind":"diagram","nodes":["Client","API","Server"],"arrows":[[0,1],[1,2]]}   // labeled boxes connected by arrows (systems, pipelines, relationships)
{"kind":"array","cells":[1,3,5,7,9,11],"pointers":[{"label":"low","at":0},{"label":"mid","at":2},{"label":"high","at":5}]}   // a real array of cells with index numbers and labeled pointers (arrays, strings, binary search, sorting, loops)
{"kind":"flow","steps":["Start","Set low, high","mid = (low+high)/2","arr[mid] == target?","Return mid"]}   // a top-down flowchart; a step ending with "?" becomes a decision diamond (algorithms, processes, step-by-step logic)
{"kind":"graph","xLabel":"time","yLabel":"speed","shape":"up","points":[{"x":2,"y":2,"label":"start"},{"x":8,"y":8,"label":"end"}]}   // coordinate axes with a line and labeled points. shape is one of line|up|down|parabola|wave. x and y are 0..10 (functions, trends, physics, math)
{"kind":"tree","nodes":["8","3","10","1","6","null","14"]}   // a binary tree in LEVEL ORDER: node i's children are at 2i+1 and 2i+2; use "null" for a missing child (trees, BSTs, heaps, hierarchies, decision trees)
{"kind":"image","query":"human heart anatomy diagram","text":"The human heart"}   // shows a REAL photo/diagram searched from the web. "text" is the caption. Use for real-world things: anatomy, biology (cells, organs), geography (maps, landmarks, flags), astronomy (planets), history (people, artifacts, events), real objects, artworks. Make the query specific and concrete.
{"kind":"image","query":"a ball rolling down a frictionless ramp showing forces","text":"Forces on the ramp","generate":true}   // GENERATES a custom illustration for the query. Use "generate":true when you need a specific made-up scenario, a simplified concept picture, or a labeled illustration that a real photo wouldn't capture. Keep the query a vivid, concrete visual description.

RULES:
- Greet briefly, write the title once (board: title), then START with the real-life analogy (step 1 above).
- Follow the HOOK → BRIDGE → EXPLAIN → SHOW IT → EXAMPLE → LOCK IT IN arc, in that order. Teach the topic FULLY and thoroughly — cover the important parts, don't be brief or skip steps.
- REQUIRED every lesson: at least ONE {"kind":"image",...} AND at least TWO other visuals (diagram/array/flow/graph/tree). Pick the type that fits the content.
- The required image should show the ACTUAL SUBJECT of the lesson, with a concrete searchable query — e.g. "water cycle diagram", "animal cell diagram labeled", "Saturn planet", "Eiffel Tower". Do NOT make the image an abstract analogy.
- If you want to picture an abstract idea or a made-up scenario, use "generate":true for that image instead of searching.
- Put the subject image early (right after the analogy/definition) so the student sees what you're teaching.
- When you walk the example, SHOW it on the matching visual (e.g. an array with low/mid/high pointers as you search).
- Use the board on most beats — the board should end up full of the lesson's key terms, steps, visuals, and the image.
- END by CHECKING their understanding: your final beat must ask ONE clear question for the student to answer in their own words (speak it warmly, e.g. "Before we wrap up — can you tell me, in your own words, why...?"). Do not just recap; invite them to show they got it, then stop.
- Begin your reply with "{" immediately. No preamble, no commentary.`;

const LANGUAGE_HINT: Record<LessonLanguage, string> = {
  english: "",
  "roman-urdu":
    ' LANGUAGE — VERY IMPORTANT: Teach ENTIRELY in Roman Urdu (Urdu written in English/Latin letters, NOT Urdu script). EVERYTHING you SAY (every "say" value) and EVERYTHING you WRITE on the board (every "text", title, bullet, note, label, caption) must be in natural, friendly Roman Urdu. Keep standard technical/English terms in English where that is normal (e.g. "array", "loop", "binary search", "force"), but all the teaching and explanation around them is in Roman Urdu. Example beat: {"say":"Chalo ise asaan tareeqe se samajhte hain. Binary search ka matlab hai sorted list me se cheez ko aadha aadha kar ke dhoondna.","board":{"kind":"title","text":"Binary Search kya hai?"}}.',
  "urdu-voice":
    ' LANGUAGE — VERY IMPORTANT: SPEAK in natural Urdu: every "say" value MUST be written in URDU SCRIPT (اردو) so it is read aloud correctly in Urdu. But everything you WRITE on the board (every "text", title, bullet, note, label, caption) must be in ROMAN URDU (Latin letters) mixed with the normal English technical terms, so it is easy to read. So: "say" = Urdu script; board "text" = Roman Urdu + English terms. Example beat: {"say":"چلو اسے آسان طریقے سے سمجھتے ہیں۔ بائنری سرچ کا مطلب ہے کہ sorted list میں سے چیز کو آدھا آدھا کر کے ڈھونڈنا۔","board":{"kind":"title","text":"Binary Search kya hai?"}}.',
};

function withLanguage(content: string, language: LessonLanguage): string {
  return language === "english" ? content : content + LANGUAGE_HINT[language];
}

const DEPTH_HINT: Record<LessonDepth, string> = {
  quick:
    "Keep it focused but still follow the full arc with an image and visuals: a quick analogy, the concept, the key visuals, a short example, then the check.",
  standard:
    "Teach it thoroughly across the full arc: a vivid analogy, the concept and its parts clearly, an image plus visuals, a full worked example, then the check. Use the board generously.",
  deep: "Teach it in real depth: a rich analogy, the concept and all its important parts, an image plus several visuals, a full worked example, a common pitfall, then the check. Fill the board.",
};

export function buildLessonOpeningMessages(
  topic: string,
  depth: LessonDepth = "standard",
  language: LessonLanguage = "english",
): TutorChatMessage[] {
  return [
    // Reasoning is disabled via the `think: false` request parameter (see
    // streamChat). We deliberately do NOT add "/no_think" here: on this Ollama
    // build that keyword forces the model to route everything into a thinking
    // trace and emit no content at all.
    { role: "system", content: SYSTEM_PROMPT },
    {
      role: "user",
      content: withLanguage(
        `Teach me this topic on the whiteboard, step by step: "${topic.trim()}". ${DEPTH_HINT[depth]}`,
        language,
      ),
    },
  ];
}

/** Build a lesson that teaches from the student's own uploaded material. */
export function buildLessonFromSourceMessages(
  title: string,
  sourceText: string,
  depth: LessonDepth = "standard",
  language: LessonLanguage = "english",
): TutorChatMessage[] {
  return [
    { role: "system", content: SYSTEM_PROMPT },
    {
      role: "user",
      content: withLanguage(
        `Teach me from my own material below. Use "${title.trim()}" as the lesson title. Base the lesson on these notes — explain the key ideas in them clearly, in your own words, with your analogy, images, and visuals so I actually understand them (don't just read them back). ${DEPTH_HINT[depth]}`,
        language,
      ) + `\n\n--- MY MATERIAL ---\n${sourceText}`,
    },
  ];
}

/** Build a lesson that teaches ONE slide of an uploaded document, deeply. */
export function buildSlideLessonMessages(
  docTitle: string,
  slideLines: string[],
  slideNo: number,
  total: number,
  outline: string,
  depth: LessonDepth = "standard",
  language: LessonLanguage = "english",
): TutorChatMessage[] {
  const numbered = slideLines.length
    ? slideLines.map((l, i) => `${i + 1}. ${l}`).join("\n")
    : "(this slide has no extractable text — it is likely an image/diagram; teach what it shows)";
  return [
    { role: "system", content: SYSTEM_PROMPT },
    {
      role: "user",
      content: `We are studying the document "${docTitle.trim()}" together, one slide at a time. This is SLIDE ${slideNo} of ${total}.

Full document outline (for your context):
${outline}

THE CONTENT ON THIS SLIDE (teach THIS slide now, point by point):
${numbered}

Teach THIS slide DEEPLY and completely:
- Go through its points/lines IN ORDER and EXPLAIN each one in your own words — refer to them out loud, e.g. "The first point here says…", "Points 3 and 5 are really about…".
- In EACH beat, add a "focus" field = the slide line number (from the numbered list above) you are teaching in that beat, so the student can follow which line you're on. Example beat: {"say":"This first point tells us…","board":null,"focus":1}. Omit "focus" only for a pure intro/recap/check beat.
- Don't just read the slide back — teach the real concept behind each point, with your own analogy, and draw diagrams/visuals/images on the whiteboard to make it clear.
- Cover everything on the slide, including any diagram or figure it shows.
- You can skip a long intro — dive into teaching this slide's content.
- End by checking the student understands THIS slide. ${DEPTH_HINT[depth]}${LANGUAGE_HINT[language]}`,
    },
  ];
}

/** The text sent with a snapshot of the whiteboard when the student draws on it. */
export function visionUserText(question: string, boardText: string): string {
  const q = question.trim();
  return `The student is looking at your whiteboard (attached image). They have DRAWN or CIRCLED something on it to point at what they mean.${
    boardText ? `\n\nWhat's currently written on the board:\n${boardText}` : ""
  }

The student asks: "${q || "What does this part mean? Explain what I circled."}"

Look carefully at what they drew/circled in the image, figure out exactly which part they're pointing at, and answer THAT — clearly and briefly, in the same JSON beat format. Use the board (a quick diagram/note/visual) if it helps. Don't re-teach everything. Begin with "{".`;
}

/** The JSONL text the assistant has produced so far, as one assistant turn. */
export function assistantTurnFromRaw(raw: string): TutorChatMessage {
  return { role: "assistant", content: raw.trim() };
}

export function studentQuestionMessage(
  message: string,
  language: LessonLanguage = "english",
): TutorChatMessage {
  return {
    role: "user",
    content: withLanguage(
      `The student just said: "${message.trim()}". Respond as their live tutor, briefly, in the same JSON format:
- If it's a QUESTION, answer it directly in 1-2 spoken sentences (add one short board item only if it truly helps).
- If it's their ATTEMPT to ANSWER your check question: judge whether they understand the IDEA (not exact keywords). If they've GOT IT, confirm warmly and give a one-line recap. If they're CLOSE, give a gentle hint that nudges them the right way (do not just say wrong) and ask them to try again. If they're OFF, kindly point to the exact gap, re-explain just that part simply (a diagram/example if it helps), and ask them to try once more.
Do NOT move on until they clearly understand. Do NOT re-teach the whole lesson. Begin with "{".`,
      language,
    ),
  };
}

export function resumeMessage(): TutorChatMessage {
  return {
    role: "user",
    content:
      "Good, they understand now. Resume teaching the lesson from where you left off. Do not repeat what you already covered. Continue in the same JSON Lines format.",
  };
}

export function followUpMessage(instruction: string): TutorChatMessage {
  return {
    role: "user",
    content: `${instruction.trim()} Keep teaching on the whiteboard in the same JSON format, and do not repeat what you already covered.`,
  };
}

// --- Incremental JSONL -> LessonStep parser ---------------------------------

/**
 * Accumulates streamed assistant text and yields a LessonStep for every complete
 * top-level JSON object — by matching braces, not newlines. Small models often
 * pretty-print JSON across multiple lines (or wrap it in a `[...]` array or ```json
 * fences), so a line-based parser misses everything; brace-matching is robust to
 * all of those. String contents and escapes are respected so braces inside text
 * never confuse the depth counter.
 */
export class LessonStepParser {
  private buffer = "";
  private counter = 0;

  constructor(
    private readonly idPrefix: string,
    private readonly markAnswer: boolean = false,
  ) {}

  /** Feed a chunk of assistant text; returns any newly completed steps. */
  push(chunk: string): LessonStep[] {
    this.buffer += chunk;
    return this.drain();
  }

  /** Nothing is buffered between objects worth flushing, but keep the API. */
  flush(): LessonStep[] {
    return this.drain();
  }

  /** Extract every complete top-level {...} object currently in the buffer. */
  private drain(): LessonStep[] {
    const steps: LessonStep[] = [];
    let depth = 0;
    let inString = false;
    let escaped = false;
    let start = -1;
    let consumedTo = 0;

    for (let i = 0; i < this.buffer.length; i += 1) {
      const ch = this.buffer[i];
      if (inString) {
        if (escaped) escaped = false;
        else if (ch === "\\") escaped = true;
        else if (ch === '"') inString = false;
        continue;
      }
      if (ch === '"') {
        inString = true;
      } else if (ch === "{") {
        if (depth === 0) start = i;
        depth += 1;
      } else if (ch === "}") {
        if (depth > 0) {
          depth -= 1;
          if (depth === 0 && start >= 0) {
            const fragment = this.buffer.slice(start, i + 1);
            const step = this.parseObject(fragment);
            if (step) steps.push(step);
            consumedTo = i + 1;
            start = -1;
          }
        }
      }
    }

    // Keep only the unparsed tail (an in-progress object), dropping consumed text
    // and any inter-object junk like commas, brackets, or fences.
    this.buffer = this.buffer.slice(depth > 0 && start >= 0 ? start : consumedTo);
    return steps;
  }

  private parseObject(fragment: string): LessonStep | null {
    let parsed: unknown;
    try {
      parsed = JSON.parse(fragment);
    } catch {
      return null;
    }
    if (!parsed || typeof parsed !== "object") return null;
    const obj = parsed as Record<string, unknown>;
    const say = typeof obj.say === "string" ? obj.say : "";
    const board =
      obj.board && typeof obj.board === "object"
        ? (obj.board as LessonStep["board"])
        : null;
    if (!say && !board) return null;
    const focus =
      typeof obj.focus === "number" && obj.focus > 0
        ? Math.round(obj.focus)
        : undefined;
    this.counter += 1;
    return {
      id: `${this.idPrefix}-${this.counter}`,
      say,
      board,
      answer: this.markAnswer,
      focus,
    };
  }
}

interface StreamOptions {
  messages: TutorChatMessage[];
  signal: AbortSignal;
  onDelta: (text: string) => void;
}

/**
 * Removes Qwen3 `<think>…</think>` reasoning from a token stream, robust to the
 * tags arriving split across chunks. Everything between an opening and closing
 * think tag (and the tags themselves) is dropped; normal text passes through.
 * Without this, a model that ignores `/no_think` would stream its reasoning —
 * including example JSON — straight onto the whiteboard.
 */
export class ThinkStripper {
  private pending = "";
  private inThink = false;

  push(chunk: string): string {
    this.pending += chunk;
    let out = "";
    // Process whenever we can resolve a tag; otherwise hold a safe tail back in
    // case a tag is straddling the chunk boundary.
    for (;;) {
      if (this.inThink) {
        const close = this.pending.indexOf("</think>");
        if (close === -1) {
          this.pending = keepTail(this.pending, "</think>");
          return out;
        }
        this.pending = this.pending.slice(close + "</think>".length);
        this.inThink = false;
      } else {
        const open = this.pending.indexOf("<think>");
        if (open === -1) {
          const safe = keepTail(this.pending, "<think>");
          out += this.pending.slice(0, this.pending.length - safe.length);
          this.pending = safe;
          return out;
        }
        out += this.pending.slice(0, open);
        this.pending = this.pending.slice(open + "<think>".length);
        this.inThink = true;
      }
    }
  }

  flush(): string {
    const rest = this.inThink ? "" : this.pending;
    this.pending = "";
    return rest;
  }
}

/** Length of the longest suffix of `text` that is a prefix of `tag`. */
function keepTail(text: string, tag: string): string {
  const max = Math.min(text.length, tag.length - 1);
  for (let n = max; n > 0; n -= 1) {
    if (tag.startsWith(text.slice(text.length - n))) return text.slice(text.length - n);
  }
  return "";
}

/**
 * Streams a chat completion from the active provider, calling onDelta with each
 * content chunk (with any `<think>` reasoning stripped out). Resolves with the
 * full think-free assistant text once the stream completes.
 */
export async function streamChat(options: StreamOptions): Promise<string> {
  return TUTOR_PROVIDER === "groq"
    ? streamGroq(options)
    : streamOllama(options);
}

/** Shared sink: strips `<think>` and forwards + accumulates clean text. */
function makeEmitter(onDelta: (t: string) => void) {
  const stripper = new ThinkStripper();
  let full = "";
  return {
    push: (content: string) => {
      const clean = stripper.push(content);
      if (clean) {
        full += clean;
        onDelta(clean);
      }
    },
    finish: () => {
      const rest = stripper.flush();
      if (rest) {
        full += rest;
        onDelta(rest);
      }
      return full;
    },
  };
}

async function streamOllama({
  messages,
  signal,
  onDelta,
}: StreamOptions): Promise<string> {
  let response: Response;
  try {
    response = await fetch(`${OLLAMA_PROXY}/api/chat`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      signal,
      body: JSON.stringify({
        model: TUTOR_MODEL,
        stream: true,
        think: false,
        keep_alive: "10m",
        messages,
        options: { temperature: 0.6, top_p: 0.9, num_predict: 1600 },
      }),
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new TutorModelError(
      "Could not reach the local tutor model. Is Ollama running? (ollama serve)",
    );
  }

  if (!response.ok || !response.body) {
    if (response.status === 404) {
      throw new TutorModelError(
        `Model "${TUTOR_MODEL}" is not installed. Run: ollama pull ${TUTOR_MODEL}`,
      );
    }
    throw new TutorModelError(
      `The tutor model returned an error (HTTP ${response.status}).`,
    );
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  const emitter = makeEmitter(onDelta);
  let transportBuffer = "";

  // Ollama streams JSONL: {"message":{"content":"..."},"done":false}
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    transportBuffer += decoder.decode(value, { stream: true });
    let idx: number;
    while ((idx = transportBuffer.indexOf("\n")) !== -1) {
      const line = transportBuffer.slice(0, idx).trim();
      transportBuffer = transportBuffer.slice(idx + 1);
      if (!line) continue;
      try {
        const frame = JSON.parse(line) as {
          message?: { content?: string };
          error?: string;
        };
        if (frame.error) throw new TutorModelError(frame.error);
        if (frame.message?.content) emitter.push(frame.message.content);
      } catch (error) {
        if (error instanceof TutorModelError) throw error;
        // Ignore partial/garbled transport lines.
      }
    }
  }
  return emitter.finish();
}

function abortableSleep(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal.aborted) {
      reject(new DOMException("Aborted", "AbortError"));
      return;
    }
    const timer = setTimeout(() => {
      signal.removeEventListener("abort", onAbort);
      resolve();
    }, ms);
    const onAbort = () => {
      clearTimeout(timer);
      reject(new DOMException("Aborted", "AbortError"));
    };
    signal.addEventListener("abort", onAbort, { once: true });
  });
}

async function streamGroq({
  messages,
  signal,
  onDelta,
}: StreamOptions): Promise<string> {
  const postWith = (key: string) =>
    fetch(`${GROQ_BASE}/openai/v1/chat/completions`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...bearer(key) },
      signal,
      body: JSON.stringify({
        model: TUTOR_MODEL,
        stream: true,
        temperature: 0.6,
        top_p: 0.9,
        // Use the full free-tier budget (~1000 output tokens/min) for a thorough,
        // visual-rich lesson.
        max_tokens: 1000,
        // Qwen3 reasons by default; "none" makes it answer directly and instantly.
        reasoning_effort: "none",
        messages,
      }),
    });

  // Fail over across the user's keys: if one is rate-limited (429) or rejected
  // (401/403), immediately try the next key. Only if the LAST remaining key is
  // still rate-limited do we wait for the per-minute reset and retry it.
  const keys = orderedKeys("groq");
  const list = keys.length ? keys : [""]; // no key → one attempt that 401s cleanly
  let response: Response;
  try {
    response = await postWith(list[0]);
    for (let i = 1; i < list.length; i += 1) {
      if (response.ok) break;
      if (response.status === 429 || response.status === 401 || response.status === 403) {
        rotateKey("groq"); // deprioritise the failing key for next time
        response = await postWith(list[i]);
      } else break;
    }
    // Last key still throttled → wait out the reset and retry it a couple of times.
    for (let attempt = 0; attempt < 3 && response.status === 429; attempt += 1) {
      const retryAfter = Number(response.headers.get("retry-after"));
      const waitMs = Math.min(
        Number.isFinite(retryAfter) && retryAfter > 0
          ? retryAfter * 1000 + 500
          : 8000,
        65000,
      );
      await abortableSleep(waitMs, signal);
      response = await postWith(list[list.length - 1]);
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new TutorModelError(
      "Could not reach the tutor service. Check your internet connection.",
    );
  }

  if (!response.ok || !response.body) {
    if (response.status === 401 || response.status === 403) {
      throw new TutorModelError(
        "Your Groq API key is missing or invalid. Open “API keys” and paste a valid free key (starts with gsk_).",
      );
    }
    if (response.status === 429) {
      throw new TutorModelError(
        "All your Groq keys have hit the free per-minute limit. Add another free key under “API keys” to keep going, or wait about a minute.",
      );
    }
    throw new TutorModelError(
      `The tutor service returned an error (HTTP ${response.status}).`,
    );
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  const emitter = makeEmitter(onDelta);
  let transportBuffer = "";

  // Groq (OpenAI-compatible) streams Server-Sent Events: lines "data: {json}",
  // each json = {choices:[{delta:{content}}]}, terminated by "data: [DONE]".
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    transportBuffer += decoder.decode(value, { stream: true });
    let idx: number;
    while ((idx = transportBuffer.indexOf("\n")) !== -1) {
      const line = transportBuffer.slice(0, idx).trim();
      transportBuffer = transportBuffer.slice(idx + 1);
      if (!line.startsWith("data:")) continue;
      const payload = line.slice(5).trim();
      if (!payload || payload === "[DONE]") continue;
      try {
        const frame = JSON.parse(payload) as {
          choices?: { delta?: { content?: string } }[];
        };
        const content = frame.choices?.[0]?.delta?.content;
        if (content) emitter.push(content);
      } catch {
        // Ignore partial SSE frames; the next read may complete them.
      }
    }
  }
  return emitter.finish();
}

/**
 * Loads the local model into memory ahead of the first lesson so the user doesn't
 * pay the cold-load. No-op for Groq (hosted — already warm). Best-effort.
 */
export async function warmUpModel(): Promise<void> {
  if (TUTOR_PROVIDER !== "ollama") return;
  try {
    await fetch(`${OLLAMA_PROXY}/api/chat`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        model: TUTOR_MODEL,
        stream: false,
        think: false,
        keep_alive: "10m",
        messages: [{ role: "user", content: "hi" }],
        options: { num_predict: 1 },
      }),
    });
  } catch {
    // Warm-up is best-effort; the real request will surface any problem.
  }
}

/**
 * Suggests a few NEW topics to learn next, given what the student has already
 * covered — the "AI picks your next lesson" of the Learning Journey map. One short
 * non-streaming completion; returns [] on any failure so the UI degrades quietly.
 */
export async function suggestNextTopics(
  learned: string[],
  signal?: AbortSignal,
): Promise<string[]> {
  const recent = learned.slice(0, 12);
  const prompt =
    recent.length === 0
      ? "Suggest 4 engaging topics a curious student could learn on a whiteboard tutor. Mix science, math, and computer science."
      : `A student has already learned these topics:\n${recent
          .map((t) => `- ${t}`)
          .join(
            "\n",
          )}\n\nSuggest 4 NEW topics that build on these or open a related direction. Do not repeat any already-learned topic.`;
  const messages: TutorChatMessage[] = [
    {
      role: "system",
      content:
        'You recommend what to learn next. Reply with ONLY a JSON array of 4 short topic titles (3-6 words each), e.g. ["Hash tables", "Graph traversal", "Dynamic programming", "Red-black trees"]. No prose, no numbering.',
    },
    { role: "user", content: prompt },
  ];

  try {
    const raw = await completeOnce(messages, signal);
    const seen = new Set(learned.map((t) => t.trim().toLowerCase()));
    return parseTopicList(raw)
      .filter((t) => t.length > 0 && t.length <= 60 && !seen.has(t.toLowerCase()))
      .slice(0, 4);
  } catch {
    return [];
  }
}

/**
 * Parse a list of topic titles from the model's reply. Prefers a JSON array but
 * tolerates the common un-quoted bracket form (`[A, B, C]`), newline/number lists,
 * and stray prose — small models are inconsistent, so we stay forgiving.
 */
export function parseTopicList(raw: string): string[] {
  const bracket = raw.match(/\[([\s\S]*)\]/);
  if (bracket) {
    try {
      const parsed = JSON.parse(bracket[0]) as unknown;
      if (Array.isArray(parsed)) {
        return parsed
          .filter((t): t is string => typeof t === "string")
          .map((t) => t.trim());
      }
    } catch {
      // Not valid JSON (e.g. unquoted items) — fall through to manual split.
    }
    return splitTopics(bracket[1]);
  }
  return splitTopics(raw);
}

function splitTopics(inner: string): string[] {
  return inner
    .split(/[,\n]/)
    .map((t) =>
      t
        .replace(/^[\s*\-•\d.)"'[\]]+/, "")
        .replace(/["'\].]+$/, "")
        .trim(),
    )
    .filter(Boolean);
}

/** A single non-streaming completion; returns the model's text (both providers). */
async function completeOnce(
  messages: TutorChatMessage[],
  signal?: AbortSignal,
): Promise<string> {
  if (TUTOR_PROVIDER === "groq") {
    const res = await fetch(`${GROQ_BASE}/openai/v1/chat/completions`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeader("groq") },
      signal,
      body: JSON.stringify({
        model: TUTOR_MODEL,
        stream: false,
        temperature: 0.8,
        max_tokens: 120,
        reasoning_effort: "none",
        messages,
      }),
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = (await res.json()) as {
      choices?: { message?: { content?: string } }[];
    };
    return data.choices?.[0]?.message?.content ?? "";
  }
  const res = await fetch(`${OLLAMA_PROXY}/api/chat`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    signal,
    body: JSON.stringify({
      model: TUTOR_MODEL,
      stream: false,
      think: false,
      messages,
      options: { num_predict: 120 },
    }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const data = (await res.json()) as { message?: { content?: string } };
  return data.message?.content ?? "";
}

/** Quick liveness check used by the UI to show setup guidance when unavailable. */
export async function checkTutorModel(): Promise<{
  running: boolean;
  modelInstalled: boolean;
}> {
  if (TUTOR_PROVIDER === "groq") {
    const keys = orderedKeys("groq");
    if (keys.length === 0) return { running: true, modelInstalled: false };
    // Ready if ANY of the user's keys is valid (429 = valid but throttled = ok too).
    try {
      let reachedServer = false;
      for (const key of keys) {
        const res = await fetch(`${GROQ_BASE}/openai/v1/models`, {
          method: "GET",
          headers: { ...bearer(key) },
        });
        reachedServer = true;
        if (res.ok || res.status === 429) return { running: true, modelInstalled: true };
      }
      return { running: reachedServer, modelInstalled: false };
    } catch {
      return { running: false, modelInstalled: false };
    }
  }
  try {
    const res = await fetch(`${OLLAMA_PROXY}/api/tags`, { method: "GET" });
    if (!res.ok) return { running: true, modelInstalled: false };
    const data = (await res.json()) as { models?: { name?: string }[] };
    const names = (data.models ?? []).map((m) => m.name ?? "");
    const base = TUTOR_MODEL.split(":")[0];
    const modelInstalled = names.some(
      (n) => n === TUTOR_MODEL || n.split(":")[0] === base,
    );
    return { running: true, modelInstalled };
  } catch {
    return { running: false, modelInstalled: false };
  }
}
