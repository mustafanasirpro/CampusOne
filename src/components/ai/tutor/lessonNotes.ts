import type { BoardItem, TranscriptEntry } from "@/types/tutor";

// Turn a finished lesson into a clean, printable study sheet the student can keep.
// We prefer the whiteboard's key points (that's the distilled lesson), then append
// the narration as "notes" and any questions asked. Markdown so it opens anywhere.

function boardLine(item: BoardItem): string | null {
  switch (item.kind) {
    case "title":
      return item.text ? `\n## ${item.text}\n` : null;
    case "bullet":
    case "note":
    case "text":
      return item.text ? `- ${item.text}` : null;
    case "equation":
      return item.text ? `- **${item.text}**` : null;
    case "code":
      return item.text ? `\n\`\`\`${item.lang ?? ""}\n${item.text}\n\`\`\`\n` : null;
    case "array":
      return item.cells?.length ? `- \`[${item.cells.join(", ")}]\`` : null;
    case "flow":
      return item.steps?.length ? `- ${item.steps.join(" → ")}` : null;
    case "diagram":
      return item.nodes?.length
        ? `- ${item.nodes.join(" → ")}`
        : item.text
          ? `- ${item.text}`
          : null;
    case "tree":
      return item.nodes?.length
        ? `- Tree: ${item.nodes.filter((n) => n && n !== "null").join(", ")}`
        : null;
    case "graph":
      return item.text
        ? `- ${item.text}`
        : item.yLabel || item.xLabel
          ? `- Graph: ${item.yLabel ?? "y"} vs ${item.xLabel ?? "x"}`
          : null;
    case "image":
      return item.text ? `- 🖼️ ${item.text}` : null;
    case "clear":
      return null;
    default:
      return item.text ? `- ${item.text}` : null;
  }
}

export function buildLessonNotes(
  topic: string,
  boardItems: BoardItem[],
  transcript: TranscriptEntry[],
  now: Date = new Date(),
): string {
  const lines: string[] = [];
  lines.push(`# ${topic || "Lesson notes"}`);
  lines.push("");
  lines.push(`_Learned with CampusOne Live Tutor · ${now.toLocaleDateString()}_`);
  lines.push("");

  const keyPoints = boardItems
    .map(boardLine)
    .filter((l): l is string => l !== null);
  if (keyPoints.length) {
    lines.push("## Key points");
    lines.push(...keyPoints);
    lines.push("");
  }

  const tutorNarration = transcript
    .filter((e) => e.role === "tutor" && !e.answer)
    .map((e) => e.text.trim())
    .filter(Boolean);
  if (tutorNarration.length) {
    lines.push("## In the tutor's words");
    lines.push(...tutorNarration.map((t) => `> ${t}`));
    lines.push("");
  }

  // Pair each student question with the answer that followed it.
  const qa: string[] = [];
  transcript.forEach((entry, i) => {
    if (entry.role !== "student") return;
    const answer = transcript
      .slice(i + 1)
      .find((e) => e.role === "tutor");
    qa.push(`**Q: ${entry.text.trim()}**`);
    if (answer) qa.push(`A: ${answer.text.trim()}`);
    qa.push("");
  });
  if (qa.length) {
    lines.push("## Questions you asked");
    lines.push(...qa);
  }

  return lines.join("\n").replace(/\n{3,}/g, "\n\n").trim() + "\n";
}

function slugify(topic: string): string {
  return (
    topic
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-+|-+$/g, "")
      .slice(0, 50) || "lesson"
  );
}

/** Trigger a download of the notes as a .md file (browser-only). */
export function downloadLessonNotes(topic: string, markdown: string): void {
  if (typeof document === "undefined") return;
  const blob = new Blob([markdown], { type: "text/markdown;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${slugify(topic)}-notes.md`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
