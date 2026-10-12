// Teaching images from two sources, all free:
//   - SEARCH: Openverse (broad Creative-Commons web images, no key) → Wikipedia.
//   - GENERATE: Pollinations (free, no key) by default; Gemini optionally (needs
//     billing — set VITE_IMAGE_GEN_PROVIDER=gemini + GEMINI_API_KEY).
// Images are shown via <img src>, so cross-origin display is fine without CORS.

import { geminiKeyParam } from "@/api/apiKeys";

export interface TopicImage {
  url: string;
  title: string;
  generated?: boolean;
}

export const IMAGE_GEN_ENABLED =
  (import.meta.env.VITE_IMAGE_GEN as string | undefined)?.trim() !== "off";

const IMAGE_GEN_PROVIDER =
  (import.meta.env.VITE_IMAGE_GEN_PROVIDER as string | undefined)?.trim() ===
  "gemini"
    ? "gemini"
    : "pollinations";

const GEMINI_BASE = (
  (import.meta.env.VITE_GEMINI_BASE as string | undefined)?.trim() ||
  "https://generativelanguage.googleapis.com"
).replace(/\/+$/, "");

const searchCache = new Map<string, TopicImage[]>();

// Our deployed site is https, so an http:// image is blocked as mixed content and
// never loads. Upgrade every candidate to https (hosts that don't support it just
// fail onError and we fall through to the next, reliably-https candidate).
function toHttps(url: string): string {
  return url.startsWith("http://") ? `https://${url.slice(7)}` : url;
}

// --- Web search ------------------------------------------------------------

async function searchOpenverse(
  query: string,
  signal?: AbortSignal,
): Promise<{ url?: string; thumbnail?: string; title?: string } | null> {
  const params = new URLSearchParams({
    q: query,
    page_size: "8",
    mature: "false",
  });
  const res = await fetch(`https://api.openverse.org/v1/images/?${params}`, {
    headers: { Accept: "application/json" },
    signal,
  });
  if (!res.ok) return null;
  const data = (await res.json()) as {
    results?: { title?: string; url?: string; thumbnail?: string }[];
  };
  return (data.results ?? []).find((r) => r.url || r.thumbnail) ?? null;
}

interface WikiPage {
  index?: number;
  title?: string;
  thumbnail?: { source?: string };
}

async function searchWikipedia(
  query: string,
  signal?: AbortSignal,
): Promise<TopicImage | null> {
  const params = new URLSearchParams({
    action: "query",
    format: "json",
    prop: "pageimages",
    piprop: "thumbnail",
    pithumbsize: "640",
    generator: "search",
    gsrsearch: query,
    gsrlimit: "4",
    gsrnamespace: "0",
    origin: "*",
  });
  const res = await fetch(`https://en.wikipedia.org/w/api.php?${params}`, {
    signal,
  });
  if (!res.ok) return null;
  const data = (await res.json()) as {
    query?: { pages?: Record<string, WikiPage> };
  };
  const pages = Object.values(data.query?.pages ?? {})
    .filter((p) => p.thumbnail?.source)
    .sort((a, b) => (a.index ?? 999) - (b.index ?? 999));
  const best = pages[0];
  if (!best?.thumbnail?.source) return null;
  return { url: best.thumbnail.source, title: best.title ?? query };
}

async function searchCandidates(
  query: string,
  signal?: AbortSignal,
): Promise<TopicImage[]> {
  const key = query.trim().toLowerCase();
  if (!key) return [];
  if (searchCache.has(key)) return searchCache.get(key) ?? [];
  const out: TopicImage[] = [];
  // Openverse (broad, web-wide). The thumbnail is served from api.openverse.org
  // (always https + reliable) so it leads; the original source image follows.
  try {
    const ov = await searchOpenverse(query, signal);
    const title = ov?.title ?? query;
    if (ov?.thumbnail) out.push({ url: toHttps(ov.thumbnail), title });
    if (ov?.url && ov.url !== ov.thumbnail) {
      out.push({ url: toHttps(ov.url), title });
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
  }
  // Wikipedia (reliable Wikimedia CDN, always https) — strong fallback.
  try {
    const wk = await searchWikipedia(query, signal);
    if (wk) out.splice(1, 0, { ...wk, url: toHttps(wk.url) });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
  }
  const deduped = out.filter(
    (c, i, a) => a.findIndex((x) => x.url === c.url) === i,
  );
  searchCache.set(key, deduped);
  return deduped;
}

// --- Generation ------------------------------------------------------------

function pollinationsUrl(query: string): string {
  const prompt = `${query}, clear labeled educational diagram, simple flat illustration, white background`;
  const params = new URLSearchParams({
    width: "640",
    height: "440",
    nologo: "true",
    model: "flux",
  });
  return `https://image.pollinations.ai/prompt/${encodeURIComponent(prompt)}?${params}`;
}

async function generateWithGemini(
  query: string,
  signal?: AbortSignal,
): Promise<string | null> {
  try {
    const res = await fetch(
      `${GEMINI_BASE}/v1beta/models/gemini-2.5-flash-image:generateContent?${geminiKeyParam()}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        signal,
        body: JSON.stringify({
          contents: [
            {
              parts: [
                {
                  text: `A clear, labeled educational diagram: ${query}. Simple flat illustration, white background, suitable for teaching.`,
                },
              ],
            },
          ],
          generationConfig: { responseModalities: ["IMAGE"] },
        }),
      },
    );
    if (!res.ok) return null; // e.g. 429 (billing not enabled) → caller falls back
    const data = (await res.json()) as {
      candidates?: { content?: { parts?: { inlineData?: { mimeType?: string; data?: string } }[] } }[];
    };
    const part = data.candidates?.[0]?.content?.parts?.find((p) => p.inlineData?.data);
    const inline = part?.inlineData;
    if (!inline?.data) return null;
    return `data:${inline.mimeType ?? "image/png"};base64,${inline.data}`;
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    return null;
  }
}

/**
 * Ordered list of image candidates for a board beat. The UI tries them in order,
 * advancing on any load error or timeout, so a slow/broken source never leaves a
 * blank. When `generate` is set, a generated illustration leads, with real web
 * images as fallbacks.
 */
export async function collectImageCandidates(
  query: string,
  generate: boolean,
  signal?: AbortSignal,
): Promise<TopicImage[]> {
  const out: TopicImage[] = [];
  if (generate && IMAGE_GEN_ENABLED) {
    if (IMAGE_GEN_PROVIDER === "pollinations") {
      out.push({ url: pollinationsUrl(query), title: query, generated: true });
    } else {
      const dataUrl = await generateWithGemini(query, signal);
      if (dataUrl) out.push({ url: dataUrl, title: query, generated: true });
    }
  }
  const searched = await searchCandidates(query, signal);
  out.push(...searched);
  // Last resort: a free generated illustration (https, no key) so an image beat is
  // never left blank when web search finds nothing or every source fails to load.
  if (IMAGE_GEN_ENABLED && !out.some((c) => c.generated)) {
    out.push({ url: pollinationsUrl(query), title: query, generated: true });
  }
  return out;
}

// --- Prefetch --------------------------------------------------------------

const preloaded = new Set<string>();

/**
 * Warm an image for a beat BEFORE it's shown: resolves the candidates (filling the
 * search cache) and preloads the first couple of URLs into the browser cache. Fired
 * while the lesson is still being prepared/played, so by the time the image beat is
 * revealed it appears instantly instead of starting to load after the lesson ends.
 * Fire-and-forget; failures are harmless (the UI still tries at reveal time).
 */
export function prefetchImage(query: string, generate: boolean): void {
  if (!query.trim() || typeof Image === "undefined") return;
  void collectImageCandidates(query, generate)
    .then((list) => {
      for (const c of list.slice(0, 2)) {
        if (preloaded.has(c.url)) continue;
        preloaded.add(c.url);
        const img = new Image();
        img.decoding = "async";
        img.src = c.url; // kicks off the network fetch into the browser cache
      }
    })
    .catch(() => {
      // Ignore — the BoardImage component will try again when the beat shows.
    });
}
