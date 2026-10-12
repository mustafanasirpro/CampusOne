// The tutor's long-term memory: a persistent, local record of everything the
// student has learned. This powers the "Learning Journey" mastery map — the
// start screen greets returning students with what they've covered, how recently,
// and what to explore next. All local (no account, no server), so it's free and
// private. Topic lessons and whole documents are both recorded.

const JOURNEY_KEY = "campusone.tutor.journey";
const MAX_ENTRIES = 60;

export type JourneyKind = "topic" | "document";

export interface JourneyEntry {
  /** Canonical display title of what was learned. */
  topic: string;
  /** Topic lesson vs a mastered document. */
  kind: JourneyKind;
  /** ISO timestamp of the most recent time this was studied. */
  lastLearned: string;
  /** How many separate sessions have covered this. */
  timesLearned: number;
}

function keyOf(topic: string): string {
  return topic.trim().toLowerCase().replace(/\s+/g, " ");
}

export function loadJourney(): JourneyEntry[] {
  try {
    const raw = localStorage.getItem(JOURNEY_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return [];
    return parsed
      .filter(
        (e): e is JourneyEntry =>
          !!e &&
          typeof (e as JourneyEntry).topic === "string" &&
          typeof (e as JourneyEntry).lastLearned === "string",
      )
      .map((e) => ({
        topic: e.topic,
        kind: e.kind === "document" ? "document" : "topic",
        lastLearned: e.lastLearned,
        timesLearned:
          typeof e.timesLearned === "number" && e.timesLearned > 0
            ? e.timesLearned
            : 1,
      }));
  } catch {
    return [];
  }
}

function save(entries: JourneyEntry[]): void {
  try {
    localStorage.setItem(JOURNEY_KEY, JSON.stringify(entries.slice(0, MAX_ENTRIES)));
  } catch {
    // Storage may be unavailable (private mode / quota) — non-fatal.
  }
}

/**
 * Record that a lesson was completed. Merges with an existing entry for the same
 * topic (case-insensitive): bumps the count and recency. Returns the updated list,
 * most-recent first. Pure given `now`, so it's easy to test.
 */
export function recordLesson(
  topic: string,
  kind: JourneyKind = "topic",
  now: Date = new Date(),
  existing: JourneyEntry[] = loadJourney(),
): JourneyEntry[] {
  const clean = topic.trim();
  if (!clean) return existing;
  const k = keyOf(clean);
  const rest = existing.filter((e) => keyOf(e.topic) !== k);
  const prior = existing.find((e) => keyOf(e.topic) === k);
  const entry: JourneyEntry = {
    topic: clean,
    kind,
    lastLearned: now.toISOString(),
    timesLearned: (prior?.timesLearned ?? 0) + 1,
  };
  const next = [entry, ...rest].sort(
    (a, b) => Date.parse(b.lastLearned) - Date.parse(a.lastLearned),
  );
  save(next);
  return next;
}

export function removeFromJourney(topic: string): JourneyEntry[] {
  const k = keyOf(topic);
  const next = loadJourney().filter((e) => keyOf(e.topic) !== k);
  save(next);
  return next;
}

export function clearJourney(): void {
  try {
    localStorage.removeItem(JOURNEY_KEY);
  } catch {
    // ignore
  }
}

/** Short human label like "today", "yesterday", "3 days ago", "2 weeks ago". */
export function relativeDay(iso: string, now: Date = new Date()): string {
  const then = Date.parse(iso);
  if (Number.isNaN(then)) return "";
  const days = Math.floor((now.getTime() - then) / 86_400_000);
  if (days <= 0) return "today";
  if (days === 1) return "yesterday";
  if (days < 7) return `${days} days ago`;
  if (days < 14) return "last week";
  if (days < 30) return `${Math.floor(days / 7)} weeks ago`;
  if (days < 60) return "last month";
  return `${Math.floor(days / 30)} months ago`;
}

/**
 * A simple learning "streak": the number of consecutive calendar days up to today
 * on which at least one lesson was completed. Rewards coming back daily.
 */
export function studyStreak(
  entries: JourneyEntry[] = loadJourney(),
  now: Date = new Date(),
): number {
  if (entries.length === 0) return 0;
  const days = new Set(
    entries.map((e) => new Date(e.lastLearned).toDateString()),
  );
  let streak = 0;
  const cursor = new Date(now);
  // Allow the streak to start today or yesterday (don't break it before day's end).
  if (!days.has(cursor.toDateString())) {
    cursor.setDate(cursor.getDate() - 1);
    if (!days.has(cursor.toDateString())) return 0;
  }
  while (days.has(cursor.toDateString())) {
    streak += 1;
    cursor.setDate(cursor.getDate() - 1);
  }
  return streak;
}
