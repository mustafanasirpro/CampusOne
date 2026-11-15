import * as pdfjsLib from "pdfjs-dist";
import workerUrl from "pdfjs-dist/build/pdf.worker.min.mjs?url";

import type {
  TimetableData,
  TimetableSection,
  TimetableSession,
} from "./types";

pdfjsLib.GlobalWorkerOptions.workerSrc = workerUrl;

/**
 * Parses an aSc Timetables export (COMSATS-style grid) into structured,
 * per-section sessions. The grid is reconstructed from text coordinates:
 * days are detected per page and carried forward, section rows come from the
 * left column, and every class cell is anchored on its instructor line so that
 * labs / two-hour blocks that span two columns stay intact.
 */

const DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
const SLOT_ORDER = ["1", "2", "3", "PB", "4", "5"];
// Column x-bands [min, max, slotKey] from the fixed aSc header geometry.
const COLS: Array<[number, number, string]> = [
  [115, 200, "1"],
  [200, 315, "2"],
  [315, 430, "3"],
  [430, 547, "PB"],
  [547, 660, "4"],
  [660, 800, "5"],
];
const SLOT_TIME: Record<string, [string, string]> = {
  "1": ["08:30", "09:55"],
  "2": ["09:55", "11:20"],
  "3": ["11:20", "12:45"],
  PB: ["12:45", "13:40"],
  "4": ["13:40", "15:05"],
  "5": ["15:05", "16:30"],
};
const SLOTS = [
  { key: "1", label: "1", start: "08:30", end: "09:55" },
  { key: "2", label: "2", start: "09:55", end: "11:20" },
  { key: "3", label: "3", start: "11:20", end: "12:45" },
  { key: "PB", label: "Prayer Break", start: "12:45", end: "13:40" },
  { key: "4", label: "4", start: "13:40", end: "15:05" },
  { key: "5", label: "5", start: "15:05", end: "16:30" },
];

const SECTION_RE = /^([A-Z]{2}\d{2})-([A-Z]+)-(\d+)-([A-Z])\s*\((\d+)\)/;
const INSTR_RE = /^(CS|SE|MT|EE|ME|MS|VS|CVE|CE)\.?\s+\S/;
const ROOM_RE = /(^CLab-\d+)|(^[A-Z]\d[\w.]*\s*(\(\d+\))?$)|(\(\d+\)$)|Lab$/;

interface Item {
  x: number;
  y: number;
  w: number;
  s: string;
}

function colOf(x: number): string | null {
  for (const [a, b, k] of COLS) if (x >= a && x < b) return k;
  return null;
}

function classify(lines: string[]): {
  instructor: string | null;
  room: string | null;
  course: string;
} {
  let instructor: string | null = null;
  let room: string | null = null;
  const courseParts: string[] = [];
  for (const ln of lines) {
    if (!instructor && INSTR_RE.test(ln)) {
      instructor = ln;
      continue;
    }
    if (ROOM_RE.test(ln) && !/\d\s*hr/i.test(ln) && !/^[A-Z][a-z]/.test(ln)) {
      room = ln;
      continue;
    }
    courseParts.push(ln);
  }
  return { instructor, room, course: courseParts.join(" ").replace(/\s+/g, " ").trim() };
}

export async function parseAscTimetable(file: File): Promise<TimetableData> {
  const data = await file.arrayBuffer();
  const pdf = await pdfjsLib.getDocument({ data }).promise;
  const sessions: TimetableSession[] = [];
  let currentDay: string | null = null;

  for (let p = 1; p <= pdf.numPages; p += 1) {
    const page = await pdf.getPage(p);
    const content = await page.getTextContent();
    const items: Item[] = content.items
      .map((raw) => {
        const it = raw as { str?: string; width?: number; transform?: number[] };
        return {
          x: it.transform?.[4] ?? 0,
          y: it.transform?.[5] ?? 0,
          w: it.width ?? 0,
          s: (it.str ?? "").trim(),
        };
      })
      .filter((it) => it.s);

    const dayItem = items.find((it) => DAYS.includes(it.s));
    if (dayItem) currentDay = dayItem.s;
    if (!currentDay) continue;

    const labels = items
      .filter((it) => it.x < 115 && SECTION_RE.test(it.s))
      .sort((a, b) => b.y - a.y);
    if (!labels.length) continue;

    const ys = labels.map((l) => l.y);
    labels.forEach((label, idx) => {
      const top = idx === 0 ? label.y + 24 : (ys[idx - 1] + label.y) / 2;
      const bot = idx === labels.length - 1 ? label.y - 24 : (label.y + ys[idx + 1]) / 2;
      const match = label.s.match(SECTION_RE);
      if (!match) return;
      const [, term, program, semester, section, strength] = match;

      const cells = items.filter(
        (it) => it.x >= 112 && it.y > bot && it.y <= top && it.y <= 500,
      );
      if (!cells.length) return;

      const cx = (it: Item) => it.x + it.w / 2;
      const instrs = cells.filter((it) => INSTR_RE.test(it.s)).sort((a, b) => a.x - b.x);
      const groups: Array<{ items: Item[]; minX: number; maxRight: number; anchor: number }> = [];
      if (instrs.length) {
        for (const ins of instrs) {
          groups.push({ items: [ins], minX: ins.x, maxRight: ins.x + ins.w, anchor: cx(ins) });
        }
        for (const it of cells) {
          if (instrs.includes(it)) continue;
          let best = groups[0];
          let bestDist = Infinity;
          for (const g of groups) {
            const d = Math.abs(cx(it) - g.anchor);
            if (d < bestDist) {
              bestDist = d;
              best = g;
            }
          }
          best.items.push(it);
          best.minX = Math.min(best.minX, it.x);
          best.maxRight = Math.max(best.maxRight, it.x + it.w);
        }
      } else {
        const byCol = new Map<string, { items: Item[]; minX: number; maxRight: number; anchor: number }>();
        for (const it of cells) {
          const c = colOf(cx(it));
          if (!c) continue;
          const g = byCol.get(c) ?? { items: [], minX: 1e9, maxRight: 0, anchor: 0 };
          g.items.push(it);
          g.minX = Math.min(g.minX, it.x);
          g.maxRight = Math.max(g.maxRight, it.x + it.w);
          byCol.set(c, g);
        }
        groups.push(...byCol.values());
      }

      for (const g of groups) {
        g.items.sort((a, b) => b.y - a.y);
        const { instructor, room, course } = classify(g.items.map((i) => i.s));
        if (!course && !instructor && !room) continue;
        const startSlot = colOf(g.minX + 8) ?? colOf((g.minX + g.maxRight) / 2);
        if (!startSlot) continue;
        const is2hr = /\(2\s*hr\)/i.test(course);
        const isLab = /\(lab\)/i.test(course) || (room ?? "").includes("Lab") || /CLab/.test(room ?? "");
        const wide = g.maxRight - g.minX > 150;
        const span = is2hr || wide ? 2 : 1;
        const si = SLOT_ORDER.indexOf(startSlot);
        const endSlot = span === 2 && si >= 0 && si < 5 ? SLOT_ORDER[si + 1] : startSlot;
        sessions.push({
          sectionId: `${term}-${program}-${semester}-${section}`,
          day: currentDay as string,
          slot: startSlot,
          start: SLOT_TIME[startSlot][0],
          end: SLOT_TIME[endSlot][1],
          span,
          course,
          instructor,
          room,
          lab: isLab,
          // strength carried on the section, not the session
        });
        void term;
        void program;
        void semester;
        void section;
        void strength;
      }
    });
  }

  // Recover missing lab subjects from the same section + instructor lecture.
  for (const s of sessions) {
    if (s.course || !s.instructor) continue;
    const lecture = sessions.find(
      (o) =>
        o.course &&
        o.sectionId === s.sectionId &&
        o.instructor === s.instructor &&
        !/\(lab\)/i.test(o.course),
    );
    if (lecture) {
      s.course = `${lecture.course.replace(/\s*\(2\s*hr\)/i, "").trim()} (Lab)`;
      s.lab = true;
    }
  }
  for (const s of sessions) {
    if (!s.course) {
      s.lab = s.lab || /lab/i.test(s.room ?? "");
      s.course = s.lab ? "Lab session" : "Class";
    }
  }

  const sectionMap = new Map<string, TimetableSection>();
  for (const s of sessions) {
    if (sectionMap.has(s.sectionId)) continue;
    const m = s.sectionId.match(/^([A-Z]{2}\d{2})-([A-Z]+)-(\d+)-([A-Z])$/);
    if (!m) continue;
    sectionMap.set(s.sectionId, {
      id: s.sectionId,
      term: m[1],
      program: m[2],
      semester: Number(m[3]),
      section: m[4],
      strength: 0,
    });
  }
  const sections = [...sectionMap.values()].sort((a, b) => a.id.localeCompare(b.id));

  return {
    meta: {
      university: "Imported timetable",
      term: sections[0]?.term ?? "",
      generatedOn: new Date().toISOString().slice(0, 10),
      source: "aSc Timetables (PDF import)",
      importedAt: new Date().toISOString(),
      days: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"],
      slots: SLOTS,
    },
    sections,
    sessions,
  };
}
