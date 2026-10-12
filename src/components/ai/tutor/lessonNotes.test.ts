import { describe, expect, it } from "vitest";

import { buildLessonNotes } from "./lessonNotes";
import type { BoardItem, TranscriptEntry } from "@/types/tutor";

const now = new Date("2026-01-10T12:00:00Z");

describe("buildLessonNotes", () => {
  it("includes the title, key points, narration, and Q&A", () => {
    const board: BoardItem[] = [
      { id: "b1", kind: "title", text: "Binary Search" },
      { id: "b2", kind: "bullet", text: "Needs a sorted array" },
      { id: "b3", kind: "array", cells: [1, 3, 5, 7] },
      { id: "b4", kind: "flow", steps: ["Check middle", "Go left or right?"] },
    ];
    const transcript: TranscriptEntry[] = [
      { id: "t1", role: "tutor", text: "Binary search halves the list each step." },
      { id: "t2", role: "student", text: "What if it's not sorted?" },
      { id: "t3", role: "tutor", text: "Then it won't work — sort it first.", answer: true },
    ];
    const md = buildLessonNotes("Binary Search", board, transcript, now);

    expect(md).toContain("# Binary Search");
    expect(md).toContain("## Key points");
    expect(md).toContain("- Needs a sorted array");
    expect(md).toContain("`[1, 3, 5, 7]`");
    expect(md).toContain("Check middle → Go left or right?");
    expect(md).toContain("## In the tutor's words");
    expect(md).toContain("> Binary search halves the list each step.");
    expect(md).toContain("## Questions you asked");
    expect(md).toContain("**Q: What if it's not sorted?**");
    expect(md).toContain("A: Then it won't work — sort it first.");
  });

  it("omits empty sections and never leaves big gaps", () => {
    const md = buildLessonNotes("Empty", [], [], now);
    expect(md).toContain("# Empty");
    expect(md).not.toContain("## Key points");
    expect(md).not.toMatch(/\n{3,}/);
  });
});
