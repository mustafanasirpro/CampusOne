import { describe, expect, it } from "vitest";

import {
  LessonStepParser,
  ThinkStripper,
  parseTopicList,
} from "@/api/tutorClient";

describe("ThinkStripper", () => {
  it("removes a whole <think> block in one chunk", () => {
    const s = new ThinkStripper();
    const out = s.push("<think>reasoning here</think>REAL") + s.flush();
    expect(out).toBe("REAL");
  });

  it("passes through text when there is no think block", () => {
    const s = new ThinkStripper();
    expect(s.push("hello ") + s.push("world") + s.flush()).toBe("hello world");
  });

  it("handles a think tag split across chunks", () => {
    const s = new ThinkStripper();
    let out = s.push("keep<thi");
    out += s.push("nk>junk</thi");
    out += s.push("nk>more");
    out += s.flush();
    expect(out).toBe("keepmore");
  });

  it("drops an unclosed think block entirely", () => {
    const s = new ThinkStripper();
    const out = s.push("<think>still thinking") + s.flush();
    expect(out).toBe("");
  });

  it("keeps real lesson JSON that follows the reasoning", () => {
    const s = new ThinkStripper();
    const raw =
      '<think>I will plan: {"say":"fake"}</think>\n{"say":"real","board":null}\n';
    const out = s.push(raw) + s.flush();
    const steps = new LessonStepParser("x").push(out);
    expect(steps).toHaveLength(1);
    expect(steps[0].say).toBe("real");
  });
});

describe("parseTopicList", () => {
  it("parses a proper JSON array", () => {
    expect(parseTopicList('["Hash tables", "Graph traversal"]')).toEqual([
      "Hash tables",
      "Graph traversal",
    ]);
  });

  it("tolerates an unquoted bracket list (what small models often emit)", () => {
    expect(
      parseTopicList("[Divide and conquer algorithms, Cellular respiration]"),
    ).toEqual(["Divide and conquer algorithms", "Cellular respiration"]);
  });

  it("handles a numbered / bulleted list and strips prose markers", () => {
    const raw = "1. Thermodynamics\n2. Integrals in calculus\n- Vectors";
    expect(parseTopicList(raw)).toEqual([
      "Thermodynamics",
      "Integrals in calculus",
      "Vectors",
    ]);
  });
});

describe("LessonStepParser", () => {
  it("parses compact one-object-per-line JSONL", () => {
    const parser = new LessonStepParser("s");
    const steps = parser.push(
      '{"say":"Hi","board":null}\n{"say":"Look","board":{"kind":"title","text":"Arrays"}}\n',
    );
    expect(steps).toHaveLength(2);
    expect(steps[0].say).toBe("Hi");
    expect(steps[0].board).toBeNull();
    expect(steps[1].board?.kind).toBe("title");
    expect(steps[1].board?.text).toBe("Arrays");
  });

  it("parses pretty-printed multi-line objects (the real Qwen3 output)", () => {
    // Exactly the shape that broke the line-based parser.
    const pretty = `{
  "say": "Hi there! Ready to learn?",
  "board": null
}
{
  "say": "We start with a sorted list.",
  "board": {
    "kind": "title",
    "text": "Binary Search Basics"
  }
}`;
    const parser = new LessonStepParser("s");
    const steps = [...parser.push(pretty), ...parser.flush()];
    expect(steps).toHaveLength(2);
    expect(steps[0].say).toBe("Hi there! Ready to learn?");
    expect(steps[1].board?.text).toBe("Binary Search Basics");
  });

  it("reassembles objects split across streamed chunks", () => {
    const parser = new LessonStepParser("s");
    const out = [
      ...parser.push('{"say":"par'),
      ...parser.push('t one","bo'),
      ...parser.push('ard":{"kind":"bullet","text":"ok"}}'),
      ...parser.flush(),
    ];
    expect(out).toHaveLength(1);
    expect(out[0].say).toBe("part one");
    expect(out[0].board?.kind).toBe("bullet");
  });

  it("ignores array brackets, commas, and ```json fences between objects", () => {
    const parser = new LessonStepParser("s");
    const steps = [
      ...parser.push('```json\n[\n{"say":"a","board":null},\n'),
      ...parser.push('{"say":"b","board":null}\n]\n```'),
      ...parser.flush(),
    ];
    expect(steps.map((s) => s.say)).toEqual(["a", "b"]);
  });

  it("does not miscount braces that appear inside strings", () => {
    const parser = new LessonStepParser("s");
    const steps = parser.push(
      '{"say":"use {} braces","board":{"kind":"code","text":"if (x) { go() }","lang":"js"}}\n',
    );
    expect(steps).toHaveLength(1);
    expect(steps[0].say).toBe("use {} braces");
    expect(steps[0].board?.text).toBe("if (x) { go() }");
  });

  it("marks answer steps when constructed in answer mode", () => {
    const parser = new LessonStepParser("a", true);
    const steps = parser.push('{"say":"clarifying","board":null}\n');
    expect(steps[0].answer).toBe(true);
  });

  it("assigns stable unique ids across pushes", () => {
    const parser = new LessonStepParser("s");
    const first = parser.push('{"say":"one","board":null}\n');
    const second = parser.push('{"say":"two","board":null}\n');
    expect(first[0].id).not.toBe(second[0].id);
  });
});
