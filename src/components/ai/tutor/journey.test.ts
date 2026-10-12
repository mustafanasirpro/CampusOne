import { beforeEach, describe, expect, it } from "vitest";

import {
  clearJourney,
  recordLesson,
  relativeDay,
  removeFromJourney,
  studyStreak,
  type JourneyEntry,
} from "./journey";

beforeEach(() => {
  clearJourney();
});

const at = (iso: string): Date => new Date(iso);

describe("recordLesson", () => {
  it("adds a new entry at the front", () => {
    const next = recordLesson("Binary search", "topic", at("2026-01-10T10:00:00Z"), []);
    expect(next).toHaveLength(1);
    expect(next[0]).toMatchObject({ topic: "Binary search", timesLearned: 1 });
  });

  it("merges the same topic case-insensitively and bumps the count", () => {
    const first = recordLesson("Loops", "topic", at("2026-01-01T10:00:00Z"), []);
    const second = recordLesson("  loops ", "topic", at("2026-01-02T10:00:00Z"), first);
    expect(second).toHaveLength(1);
    expect(second[0].timesLearned).toBe(2);
    expect(second[0].topic).toBe("loops");
  });

  it("keeps entries sorted most-recent first", () => {
    let list: JourneyEntry[] = [];
    list = recordLesson("A", "topic", at("2026-01-01T10:00:00Z"), list);
    list = recordLesson("B", "topic", at("2026-01-03T10:00:00Z"), list);
    list = recordLesson("C", "topic", at("2026-01-02T10:00:00Z"), list);
    expect(list.map((e) => e.topic)).toEqual(["B", "C", "A"]);
  });

  it("ignores blank topics", () => {
    expect(recordLesson("   ", "topic", at("2026-01-01T10:00:00Z"), [])).toHaveLength(0);
  });
});

describe("removeFromJourney", () => {
  it("removes a topic from storage", () => {
    recordLesson("Graphs", "topic", at("2026-01-01T10:00:00Z"), []);
    const after = removeFromJourney("graphs");
    expect(after).toHaveLength(0);
  });
});

describe("relativeDay", () => {
  const now = at("2026-01-10T12:00:00Z");
  it("labels today, yesterday, and days ago", () => {
    expect(relativeDay("2026-01-10T08:00:00Z", now)).toBe("today");
    expect(relativeDay("2026-01-09T08:00:00Z", now)).toBe("yesterday");
    expect(relativeDay("2026-01-07T08:00:00Z", now)).toBe("3 days ago");
    expect(relativeDay("2026-01-01T08:00:00Z", now)).toBe("last week");
  });
});

describe("studyStreak", () => {
  it("counts consecutive days ending today", () => {
    const now = at("2026-01-10T12:00:00Z");
    const entries: JourneyEntry[] = [
      { topic: "A", kind: "topic", lastLearned: "2026-01-10T09:00:00Z", timesLearned: 1 },
      { topic: "B", kind: "topic", lastLearned: "2026-01-09T09:00:00Z", timesLearned: 1 },
      { topic: "C", kind: "topic", lastLearned: "2026-01-08T09:00:00Z", timesLearned: 1 },
    ];
    expect(studyStreak(entries, now)).toBe(3);
  });

  it("breaks the streak on a gap", () => {
    const now = at("2026-01-10T12:00:00Z");
    const entries: JourneyEntry[] = [
      { topic: "A", kind: "topic", lastLearned: "2026-01-10T09:00:00Z", timesLearned: 1 },
      { topic: "B", kind: "topic", lastLearned: "2026-01-07T09:00:00Z", timesLearned: 1 },
    ];
    expect(studyStreak(entries, now)).toBe(1);
  });

  it("is zero when the last study was days ago", () => {
    const now = at("2026-01-10T12:00:00Z");
    const entries: JourneyEntry[] = [
      { topic: "A", kind: "topic", lastLearned: "2026-01-05T09:00:00Z", timesLearned: 1 },
    ];
    expect(studyStreak(entries, now)).toBe(0);
  });
});
