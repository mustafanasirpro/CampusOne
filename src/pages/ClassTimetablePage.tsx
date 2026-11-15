import {
  CalendarClock,
  ChevronLeft,
  ChevronRight,
  Clock,
  DoorOpen,
  GraduationCap,
  MapPin,
  Upload,
  UserRound,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";

import { useAuth } from "@/auth/useAuth";
import {
  Badge,
  Button,
  Card,
  CardContent,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
} from "@/components/common";
import { parseAscTimetable } from "@/features/classTimetable/ascTimetableParser";
import {
  clearOverride,
  hasLocalOverride,
  loadSectionPreference,
  loadTimetable,
  saveSectionPreference,
  saveTimetable,
} from "@/features/classTimetable/timetableStore";
import type { TimetableData, TimetableSession } from "@/features/classTimetable/types";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

function termLabel(term: string): string {
  const season = term.startsWith("FA") ? "Fall" : term.startsWith("SP") ? "Spring" : term.slice(0, 2);
  const year = term.slice(2);
  return `${season} 20${year}`;
}

function todayDayIndex(days: string[]): number {
  const name = new Date().toLocaleDateString("en-US", { weekday: "long" });
  const idx = days.indexOf(name);
  return idx >= 0 ? idx : 0;
}

const slotRank = (slot: string) => ["1", "2", "3", "PB", "4", "5"].indexOf(slot);

export function ClassTimetablePage() {
  useDocumentTitle("Class Schedule");
  const { currentUser } = useAuth();
  const isAdmin = currentUser?.roles.includes("ADMIN") ?? false;

  const [data, setData] = useState<TimetableData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [term, setTerm] = useState("");
  const [program, setProgram] = useState("");
  const [sectionLetter, setSectionLetter] = useState("");
  const [dayIndex, setDayIndex] = useState(0);
  const [weekView, setWeekView] = useState(false);

  const [isImporting, setIsImporting] = useState(false);
  const [importNote, setImportNote] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const applyLoaded = (loaded: TimetableData) => {
    setData(loaded);
    setDayIndex(todayDayIndex(loaded.meta.days));
    const saved = loadSectionPreference();
    const match = saved ? loaded.sections.find((s) => s.id === saved) : undefined;
    const first = match ?? loaded.sections[0];
    if (first) {
      setTerm(first.term);
      setProgram(first.program);
      setSectionLetter(first.section);
    }
  };

  // All state updates happen inside async callbacks, so this is safe to call
  // straight from an effect without triggering a synchronous cascading render.
  const loadSchedule = (signal?: AbortSignal) =>
    loadTimetable(signal)
      .then(applyLoaded)
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === "AbortError") return;
        setError(cause instanceof Error ? cause.message : "Failed to load schedule.");
      })
      .finally(() => setIsLoading(false));

  // Reload after an admin upload / reset (invoked from an event handler).
  const refresh = () => {
    setIsLoading(true);
    setError(null);
    void loadSchedule();
  };

  useEffect(() => {
    const controller = new AbortController();
    void loadSchedule(controller.signal);
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const terms = useMemo(
    () => [...new Set((data?.sections ?? []).map((s) => s.term))].sort().reverse(),
    [data],
  );
  const programs = useMemo(
    () => [...new Set((data?.sections ?? []).filter((s) => s.term === term).map((s) => s.program))].sort(),
    [data, term],
  );
  const sectionsForChoice = useMemo(
    () =>
      (data?.sections ?? [])
        .filter((s) => s.term === term && s.program === program)
        .sort((a, b) => a.section.localeCompare(b.section)),
    [data, term, program],
  );

  const activeSection = useMemo(
    () => sectionsForChoice.find((s) => s.section === sectionLetter) ?? sectionsForChoice[0],
    [sectionsForChoice, sectionLetter],
  );

  // Cascading selection, event-driven so a batch/program change re-validates the
  // program and section in the same update (no setState-in-effect churn).
  const sectionsFor = (t: string, pr: string) =>
    (data?.sections ?? [])
      .filter((s) => s.term === t && s.program === pr)
      .sort((a, b) => a.section.localeCompare(b.section));
  const programsFor = (t: string) =>
    [...new Set((data?.sections ?? []).filter((s) => s.term === t).map((s) => s.program))].sort();

  const chooseTerm = (nextTerm: string) => {
    setTerm(nextTerm);
    const prog = programsFor(nextTerm);
    const nextProgram = prog.includes(program) ? program : prog[0] ?? "";
    setProgram(nextProgram);
    const secs = sectionsFor(nextTerm, nextProgram);
    if (!secs.some((s) => s.section === sectionLetter)) setSectionLetter(secs[0]?.section ?? "");
  };
  const chooseProgram = (nextProgram: string) => {
    setProgram(nextProgram);
    const secs = sectionsFor(term, nextProgram);
    if (!secs.some((s) => s.section === sectionLetter)) setSectionLetter(secs[0]?.section ?? "");
  };

  useEffect(() => {
    if (activeSection) saveSectionPreference(activeSection.id);
  }, [activeSection]);

  const sessionsForSection = useMemo<TimetableSession[]>(
    () => (data && activeSection ? data.sessions.filter((s) => s.sectionId === activeSection.id) : []),
    [data, activeSection],
  );

  const days = data?.meta.days ?? [];
  const dayName = days[dayIndex] ?? "";

  const sessionsForDay = (name: string) =>
    sessionsForSection.filter((s) => s.day === name).sort((a, b) => slotRank(a.slot) - slotRank(b.slot));

  const handleUpload = async (file: File) => {
    setIsImporting(true);
    setImportNote(null);
    setError(null);
    try {
      const parsed = await parseAscTimetable(file);
      if (!parsed.sections.length) {
        throw new Error("No sections were found in that PDF. Is it an aSc timetable export?");
      }
      saveTimetable(parsed);
      setImportNote(`Imported ${parsed.sections.length} sections and ${parsed.sessions.length} classes.`);
      refresh();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "That PDF could not be parsed.");
    } finally {
      setIsImporting(false);
      if (fileRef.current) fileRef.current.value = "";
    }
  };

  if (isLoading && !data) {
    return (
      <div className="grid min-h-[60vh] place-items-center">
        <LoadingSpinner label="Loading class schedule" />
      </div>
    );
  }

  return (
    <div className="grid gap-6 pb-10">
      <PageHeader
        description="Pick your batch and section once — then see only your own classes, day by day."
        eyebrow={data ? data.meta.university : "Class schedule"}
        title="Class Schedule"
      />

      {error ? <ErrorMessage message={error} /> : null}

      {isAdmin ? (
        <Card className="border-amber-200 bg-amber-50/40">
          <CardContent className="flex flex-col gap-3">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
              <Upload className="size-4 text-amber-700" />
              Admin · publish a timetable PDF
            </div>
            <p className="text-sm text-slate-600">
              Upload the university&apos;s aSc timetable PDF. It is parsed in your browser and every
              section becomes available to students below.
            </p>
            <div className="flex flex-wrap items-center gap-2">
              <input
                accept="application/pdf"
                className="hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (file) void handleUpload(file);
                }}
                ref={fileRef}
                type="file"
              />
              <Button loading={isImporting} onClick={() => fileRef.current?.click()} variant="outline">
                <Upload className="size-4" /> Choose PDF
              </Button>
              {hasLocalOverride() ? (
                <Button
                  onClick={() => {
                    clearOverride();
                    setImportNote("Reverted to the published schedule.");
                    refresh();
                  }}
                  variant="ghost"
                >
                  Reset to published
                </Button>
              ) : null}
              {importNote ? <span className="text-sm text-emerald-700">{importNote}</span> : null}
            </div>
          </CardContent>
        </Card>
      ) : null}

      {/* Section picker */}
      <Card>
        <CardContent className="grid gap-4 sm:grid-cols-3">
          <label className="grid gap-1.5 text-sm font-medium text-slate-700">
            Batch
            <select
              className="rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm"
              onChange={(event) => chooseTerm(event.target.value)}
              value={term}
            >
              {terms.map((t) => (
                <option key={t} value={t}>
                  {termLabel(t)} ({t})
                </option>
              ))}
            </select>
          </label>
          <label className="grid gap-1.5 text-sm font-medium text-slate-700">
            Program
            <select
              className="rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm"
              onChange={(event) => chooseProgram(event.target.value)}
              value={program}
            >
              {programs.map((pr) => (
                <option key={pr} value={pr}>
                  {pr}
                </option>
              ))}
            </select>
          </label>
          <label className="grid gap-1.5 text-sm font-medium text-slate-700">
            Section
            <select
              className="rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm"
              onChange={(event) => setSectionLetter(event.target.value)}
              value={sectionLetter}
            >
              {sectionsForChoice.map((s) => (
                <option key={s.id} value={s.section}>
                  Section {s.section}
                </option>
              ))}
            </select>
          </label>
        </CardContent>
      </Card>

      {activeSection ? (
        <>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <span className="grid size-10 place-items-center rounded-xl bg-brand-50 text-brand-700">
                <GraduationCap className="size-5" />
              </span>
              <div>
                <p className="font-display text-xl font-semibold text-slate-900">{activeSection.id}</p>
                <p className="text-sm text-slate-500">
                  {termLabel(activeSection.term)} · {activeSection.program} · Semester {activeSection.semester} ·
                  Section {activeSection.section}
                </p>
              </div>
            </div>
            <Button onClick={() => setWeekView((value) => !value)} variant="outline">
              <CalendarClock className="size-4" />
              {weekView ? "Day view" : "Whole week"}
            </Button>
          </div>

          {!weekView ? (
            <>
              <div className="flex items-center gap-2">
                <Button
                  aria-label="Previous day"
                  disabled={dayIndex === 0}
                  onClick={() => setDayIndex((value) => Math.max(0, value - 1))}
                  variant="ghost"
                >
                  <ChevronLeft className="size-4" />
                </Button>
                <div className="flex flex-1 flex-wrap gap-1.5">
                  {days.map((name, idx) => (
                    <button
                      className={cn(
                        "rounded-full px-3.5 py-1.5 text-sm font-medium transition",
                        idx === dayIndex
                          ? "bg-brand-600 text-white"
                          : "bg-slate-100 text-slate-600 hover:bg-slate-200",
                      )}
                      key={name}
                      onClick={() => setDayIndex(idx)}
                      type="button"
                    >
                      {name}
                    </button>
                  ))}
                </div>
                <Button
                  aria-label="Next day"
                  disabled={dayIndex >= days.length - 1}
                  onClick={() => setDayIndex((value) => Math.min(days.length - 1, value + 1))}
                  variant="ghost"
                >
                  <ChevronRight className="size-4" />
                </Button>
              </div>
              <DaySchedule day={dayName} sessions={sessionsForDay(dayName)} />
            </>
          ) : (
            <div className="grid gap-4 lg:grid-cols-2">
              {days.map((name) => (
                <div className="grid gap-2" key={name}>
                  <h2 className="font-display text-lg font-semibold text-slate-900">{name}</h2>
                  <DaySchedule compact day={name} sessions={sessionsForDay(name)} />
                </div>
              ))}
            </div>
          )}
        </>
      ) : (
        <EmptyState
          description="No published schedule is available yet."
          icon={<CalendarClock className="size-6" />}
          title="Nothing to show"
        />
      )}
    </div>
  );
}

function DaySchedule({
  day,
  sessions,
  compact = false,
}: {
  day: string;
  sessions: TimetableSession[];
  compact?: boolean;
}) {
  if (!sessions.length) {
    return (
      <Card>
        <CardContent className="grid place-items-center gap-1 py-8 text-center">
          <Clock className="size-5 text-slate-400" />
          <p className="text-sm font-medium text-slate-600">No classes on {day}</p>
          <p className="text-xs text-slate-400">Enjoy the free day.</p>
        </CardContent>
      </Card>
    );
  }
  return (
    <div className="grid gap-3">
      {sessions.map((session, index) => (
        <Card key={`${session.slot}-${index}`}>
          <CardContent
            className={cn(
              "grid items-start gap-3",
              compact ? "sm:grid-cols-[auto_1fr]" : "sm:grid-cols-[130px_1fr]",
            )}
          >
            <div className="flex items-center gap-1.5 text-sm font-semibold text-brand-700">
              <Clock className="size-4 shrink-0" />
              {session.start}–{session.end}
            </div>
            <div className="grid gap-1.5">
              <div className="flex flex-wrap items-center gap-2">
                <p className="font-semibold text-slate-900">{session.course}</p>
                {session.lab ? <Badge variant="brand">Lab</Badge> : null}
              </div>
              <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-500">
                {session.instructor ? (
                  <span className="inline-flex items-center gap-1.5">
                    <UserRound className="size-3.5" />
                    {session.instructor}
                  </span>
                ) : null}
                {session.room ? (
                  <span className="inline-flex items-center gap-1.5">
                    {session.room.toLowerCase().includes("lab") ? (
                      <DoorOpen className="size-3.5" />
                    ) : (
                      <MapPin className="size-3.5" />
                    )}
                    {session.room}
                  </span>
                ) : null}
              </div>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
