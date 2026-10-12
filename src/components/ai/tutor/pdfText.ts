import * as pdfjsLib from "pdfjs-dist";
import workerUrl from "pdfjs-dist/build/pdf.worker.min.mjs?url";

pdfjsLib.GlobalWorkerOptions.workerSrc = workerUrl;

/** Extracts plain text from a PDF file (client-side), capped to maxChars. */
export async function extractPdfText(
  file: File,
  maxChars = 9000,
): Promise<string> {
  const data = await file.arrayBuffer();
  const pdf = await pdfjsLib.getDocument({ data }).promise;
  const parts: string[] = [];
  let total = 0;
  for (let i = 1; i <= pdf.numPages && total < maxChars; i += 1) {
    const page = await pdf.getPage(i);
    const content = await page.getTextContent();
    const pageText = content.items
      .map((item) => (item as { str?: string }).str ?? "")
      .join(" ")
      .replace(/\s+/g, " ")
      .trim();
    if (pageText) {
      parts.push(pageText);
      total += pageText.length;
    }
  }
  return parts.join("\n\n").slice(0, maxChars).trim();
}
