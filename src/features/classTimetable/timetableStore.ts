import type { TimetableData } from "./types";

/**
 * Where the class schedule lives. The app ships a seed parsed from the
 * university's published PDF (served from /timetables/fa26.json). When an admin
 * uploads a new PDF it is parsed in the browser and kept as a local override.
 *
 * NOTE: the override is per-browser (localStorage). True "every user sees the
 * admin's upload" needs a backend endpoint to persist the parsed JSON centrally;
 * `saveTimetable` is the single seam to swap for that call later.
 */

const OVERRIDE_KEY = "campusone.classTimetable.override";
const SECTION_KEY = "campusone.classTimetable.section";
const SEED_URL = "/timetables/fa26.json";

function safeGet(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function safeSet(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    /* storage unavailable (private mode) — non-fatal */
  }
}

export function hasLocalOverride(): boolean {
  return safeGet(OVERRIDE_KEY) !== null;
}

export async function loadTimetable(signal?: AbortSignal): Promise<TimetableData> {
  const override = safeGet(OVERRIDE_KEY);
  if (override) {
    try {
      return JSON.parse(override) as TimetableData;
    } catch {
      /* corrupt override — fall through to the seed */
    }
  }
  const response = await fetch(SEED_URL, { signal });
  if (!response.ok) {
    throw new Error("The class schedule could not be loaded.");
  }
  return (await response.json()) as TimetableData;
}

export function saveTimetable(data: TimetableData): void {
  safeSet(OVERRIDE_KEY, JSON.stringify(data));
}

export function clearOverride(): void {
  try {
    localStorage.removeItem(OVERRIDE_KEY);
  } catch {
    /* non-fatal */
  }
}

export function loadSectionPreference(): string | null {
  return safeGet(SECTION_KEY);
}

export function saveSectionPreference(sectionId: string): void {
  safeSet(SECTION_KEY, sectionId);
}
