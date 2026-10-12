import { describe, expect, it } from "vitest";

import { cleanLesson } from "./useTutor";
import type { LessonStep } from "@/types/tutor";

let n = 0;
const beat = (say: string, board?: LessonStep["board"]): LessonStep => ({
  id: `s${(n += 1)}`,
  say,
  board: board ?? null,
});

describe("cleanLesson", () => {
  it("keeps a clean lesson unchanged", () => {
    const steps = [
      beat("Let's learn loops."),
      beat("", { kind: "title", text: "Loops" }),
      beat("A loop repeats code."),
      beat("Example: print 1 to 5."),
    ];
    expect(cleanLesson(steps)).toHaveLength(4);
  });

  it("drops paraphrased duplicate narration beats", () => {
    const steps = [
      beat("Great! Let's try one quick example together."),
      beat("No problem! Let's do it fast."),
      beat("Perfect! Let's try one quick example."),
    ];
    const kept = cleanLesson(steps);
    // The first and third are near-duplicates; the third is removed.
    expect(kept.length).toBeLessThan(3);
    expect(kept[0].say).toBe("Great! Let's try one quick example together.");
  });

  it("drops beats with a duplicate board line", () => {
    const steps = [
      beat("Here is the formula.", { kind: "equation", text: "mid = (low + high) / 2" }),
      beat("Again the formula.", { kind: "equation", text: "mid = (low + high) / 2" }),
    ];
    expect(cleanLesson(steps)).toHaveLength(1);
  });

  it("cuts the lesson at a second title (model restart)", () => {
    const steps = [
      beat("Intro."),
      beat("", { kind: "title", text: "Binary Search" }),
      beat("Point one."),
      beat("", { kind: "title", text: "Binary Search" }), // restart begins
      beat("Duplicate point."),
    ];
    const kept = cleanLesson(steps);
    expect(kept).toHaveLength(3);
    expect(kept.at(-1)?.say).toBe("Point one.");
  });

  it("does not over-trim short distinct beats", () => {
    const steps = [beat("low = 0"), beat("high = 4"), beat("mid = 2")];
    expect(cleanLesson(steps)).toHaveLength(3);
  });
});
