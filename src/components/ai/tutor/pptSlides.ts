import type { DocSlide } from "./pdfSlides";

// Native .pptx support: a .pptx is a ZIP of XML. We read it with JSZip and pull
// each slide's text out of ppt/slides/slideN.xml. (PowerPoint can't be rendered
// to an image in the browser, so slides show their text points, no preview.)

/** Pull visual lines of text from one slide's XML (grouped by paragraph). */
function linesFromSlideXml(xml: string): string[] {
  const lines: string[] = [];
  // Each <a:p> is a paragraph (a visual line/bullet). Within it, <a:t> are text runs.
  const paragraphs = xml.match(/<a:p[\s>][\s\S]*?<\/a:p>/g) ?? [];
  for (const p of paragraphs) {
    const runs = [...p.matchAll(/<a:t>([\s\S]*?)<\/a:t>/g)].map((m) =>
      decodeXml(m[1]),
    );
    const line = runs.join("").replace(/\s+/g, " ").trim();
    if (line) lines.push(line);
  }
  return lines;
}

function decodeXml(s: string): string {
  return s
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, "&");
}

/** Load a .pptx as teaching slides (text only — no rendered preview). */
export async function loadPptxSlides(
  file: File,
  maxSlides = 60,
): Promise<DocSlide[]> {
  const JSZip = (await import("jszip")).default;
  const zip = await JSZip.loadAsync(await file.arrayBuffer());

  // Slide files are ppt/slides/slideN.xml — order by N.
  const slideNames = Object.keys(zip.files)
    .filter((n) => /^ppt\/slides\/slide\d+\.xml$/.test(n))
    .sort((a, b) => {
      const na = Number(a.match(/slide(\d+)\.xml$/)![1]);
      const nb = Number(b.match(/slide(\d+)\.xml$/)![1]);
      return na - nb;
    })
    .slice(0, maxSlides);

  const slides: DocSlide[] = [];
  for (let i = 0; i < slideNames.length; i += 1) {
    const xml = await zip.files[slideNames[i]].async("text");
    const lines = linesFromSlideXml(xml);
    slides.push({ index: i, image: "", lines, text: lines.join("\n") });
  }
  return slides;
}
