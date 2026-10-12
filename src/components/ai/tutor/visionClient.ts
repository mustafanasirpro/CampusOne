// Vision ("see the student's drawing / pointing") support. Captures the
// whiteboard as an image and sends it to a multimodal model. Provider is
// configurable; defaults to OpenRouter (which has free vision models). Gemini is
// supported too but needs a project with generateContent access.
//
// Routed through Vite proxies so the API key stays server-side:
//   OpenRouter → "/openrouter"  (Authorization injected)
//   Gemini     → "/gemini"      (key query param injected)

import { keyHeader } from "@/api/apiKeys";
import { SYSTEM_PROMPT, TutorModelError, visionUserText } from "@/api/tutorClient";

export type VisionProvider = "openrouter" | "gemini" | "off";

export const VISION_PROVIDER: VisionProvider = (() => {
  const v = (import.meta.env.VITE_VISION_PROVIDER as string | undefined)?.trim();
  if (v === "gemini" || v === "off") return v;
  return "openrouter";
})();

export const VISION_ENABLED = VISION_PROVIDER !== "off";

const VISION_MODEL =
  (import.meta.env.VITE_VISION_MODEL as string | undefined)?.trim() ||
  (VISION_PROVIDER === "gemini"
    ? "gemini-3.8-flash"
    : "meta-llama/llama-3.2-11b-vision-instruct:free");

const OPENROUTER_PROXY = (
  (import.meta.env.VITE_OPENROUTER_PROXY as string | undefined)?.trim() ||
  "/openrouter"
).replace(/\/+$/, "");

const GEMINI_PROXY = (
  (import.meta.env.VITE_GEMINI_PROXY as string | undefined)?.trim() || "/gemini"
).replace(/\/+$/, "");

/** Capture an element (the whiteboard) to a JPEG data URL. */
export async function captureElement(el: HTMLElement): Promise<string> {
  const { toJpeg } = await import("html-to-image");
  return toJpeg(el, {
    quality: 0.85,
    backgroundColor: "#ffffff",
    pixelRatio: 1.5,
    cacheBust: true,
  });
}

interface VisionArgs {
  imageDataUrl: string;
  question: string;
  boardText: string;
  signal: AbortSignal;
  onDelta: (text: string) => void;
}

/** Ask the vision model about the captured board; streams back teaching beats. */
export async function streamVision(args: VisionArgs): Promise<string> {
  if (VISION_PROVIDER === "gemini") return visionGemini(args);
  return visionOpenRouter(args);
}

async function visionOpenRouter({
  imageDataUrl,
  question,
  boardText,
  signal,
  onDelta,
}: VisionArgs): Promise<string> {
  let response: Response;
  try {
    response = await fetch(`${OPENROUTER_PROXY}/api/v1/chat/completions`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...keyHeader("openrouter") },
      signal,
      body: JSON.stringify({
        model: VISION_MODEL,
        stream: true,
        temperature: 0.5,
        max_tokens: 700,
        messages: [
          { role: "system", content: SYSTEM_PROMPT },
          {
            role: "user",
            content: [
              { type: "text", text: visionUserText(question, boardText) },
              { type: "image_url", image_url: { url: imageDataUrl } },
            ],
          },
        ],
      }),
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new TutorModelError("Could not reach the vision model.");
  }
  if (!response.ok || !response.body) {
    if (response.status === 401 || response.status === 403) {
      throw new TutorModelError(
        "Vision needs an API key. Add a free OPENROUTER_API_KEY to your .env.",
      );
    }
    throw new TutorModelError(`The vision model errored (HTTP ${response.status}).`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  // Accumulate content and (for reasoning models) reasoning separately, then
  // emit the best answer once at the end — vision answers are short, and this is
  // robust to models that put the reply in either field.
  let content = "";
  let reasoning = "";
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    let idx: number;
    while ((idx = buffer.indexOf("\n")) !== -1) {
      const line = buffer.slice(0, idx).trim();
      buffer = buffer.slice(idx + 1);
      if (!line.startsWith("data:")) continue;
      const payload = line.slice(5).trim();
      if (!payload || payload === "[DONE]") continue;
      try {
        const frame = JSON.parse(payload) as {
          choices?: { delta?: { content?: string; reasoning?: string } }[];
        };
        const delta = frame.choices?.[0]?.delta;
        if (delta?.content) content += delta.content;
        if (delta?.reasoning) reasoning += delta.reasoning;
      } catch {
        // ignore partial frames
      }
    }
  }
  const answer = (content.trim() || reasoning.trim()).trim();
  if (answer) onDelta(answer);
  return answer;
}

async function visionGemini({
  imageDataUrl,
  question,
  boardText,
  signal,
  onDelta,
}: VisionArgs): Promise<string> {
  const b64 = imageDataUrl.split(",")[1] ?? "";
  const mime = imageDataUrl.slice(5, imageDataUrl.indexOf(";")) || "image/jpeg";
  let response: Response;
  try {
    response = await fetch(
      `${GEMINI_PROXY}/v1beta/models/${VISION_MODEL}:generateContent`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json", ...keyHeader("gemini") },
        signal,
        body: JSON.stringify({
          systemInstruction: { parts: [{ text: SYSTEM_PROMPT }] },
          contents: [
            {
              parts: [
                { text: visionUserText(question, boardText) },
                { inlineData: { mimeType: mime, data: b64 } },
              ],
            },
          ],
        }),
      },
    );
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new TutorModelError("Could not reach the vision model.");
  }
  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      throw new TutorModelError(
        "Vision is blocked for this Gemini project. Use a key with API access, or switch to OpenRouter.",
      );
    }
    throw new TutorModelError(`The vision model errored (HTTP ${response.status}).`);
  }
  const data = (await response.json()) as {
    candidates?: { content?: { parts?: { text?: string }[] } }[];
  };
  const text =
    data.candidates?.[0]?.content?.parts?.map((p) => p.text ?? "").join("") ?? "";
  if (text) onDelta(text);
  return text;
}
