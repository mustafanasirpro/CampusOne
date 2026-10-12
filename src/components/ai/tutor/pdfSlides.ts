import * as pdfjsLib from "pdfjs-dist";
import workerUrl from "pdfjs-dist/build/pdf.worker.min.mjs?url";

pdfjsLib.GlobalWorkerOptions.workerSrc = workerUrl;

export interface DocSlide {
  index: number;
  /** Rendered page image (data URL) shown in the slide panel. */
  image: string;
  /** Each visual line / point of text on the slide, top to bottom. */
  lines: string[];
  /** The slide's full text, joined. */
  text: string;
}

interface TextItemLike {
  str?: string;
  transform?: number[];
}

function extractLines(items: TextItemLike[]): string[] {
  const rows = new Map<number, { x: number; str: string }[]>();
  for (const item of items) {
    if (!item.str?.trim() || !item.transform) continue;
    // Group by y (transform[5]); round so items on the same line cluster.
    const y = Math.round(item.transform[5]);
    const list = rows.get(y) ?? [];
    list.push({ x: item.transform[4] ?? 0, str: item.str });
    rows.set(y, list);
  }
  return [...rows.keys()]
    .sort((a, b) => b - a) // PDF y grows upward → top line first
    .map((y) =>
      rows
        .get(y)!
        .sort((a, b) => a.x - b.x)
        .map((i) => i.str)
        .join(" ")
        .replace(/\s+/g, " ")
        .trim(),
    )
    .filter(Boolean);
}

/** Load a PDF as teaching slides: render each page to an image + extract its lines. */
export async function loadPdfSlides(
  file: File,
  maxPages = 40,
): Promise<DocSlide[]> {
  const data = await file.arrayBuffer();
  const pdf = await pdfjsLib.getDocument({ data }).promise;
  const count = Math.min(pdf.numPages, maxPages);
  const slides: DocSlide[] = [];

  for (let i = 1; i <= count; i += 1) {
    const page = await pdf.getPage(i);

    // Render to a canvas sized to ~1000px wide for a crisp slide image.
    const base = page.getViewport({ scale: 1 });
    const scale = Math.min(2, Math.max(1, 1000 / base.width));
    const viewport = page.getViewport({ scale });
    const canvas = document.createElement("canvas");
    canvas.width = Math.round(viewport.width);
    canvas.height = Math.round(viewport.height);
    const ctx = canvas.getContext("2d");
    let image = "";
    if (ctx) {
      await page.render({ canvasContext: ctx, viewport }).promise;
      image = canvas.toDataURL("image/jpeg", 0.82);
    }

    const content = await page.getTextContent();
    const items = content.items as TextItemLike[];
    const lines = extractLines(items);
    const text = lines.join("\n");
    slides.push({ index: i - 1, image, lines, text });
  }
  return slides;
}
