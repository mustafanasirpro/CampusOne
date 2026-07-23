import { CalendarClock } from "lucide-react";
import { useEffect, useState } from "react";

import { ApiError } from "@/api/apiClient";
import { getMyAuraInstructorTimetable, listAvailableAuraTerms } from "@/api/auraApi";
import {
  Card,
  CardContent,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
} from "@/components/common";
import type { AuraScopedTimetable, AuraTerm } from "@/types/aura";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

const dayNames = ["", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

export function InstructorTimetablePage() {
  useDocumentTitle("Teaching Timetable");
  const [terms, setTerms] = useState<AuraTerm[]>([]);
  const [termId, setTermId] = useState("");
  const [timetable, setTimetable] = useState<AuraScopedTimetable | null>(null);
  const [isLoadingTerms, setIsLoadingTerms] = useState(true);
  const [isLoadingTimetable, setIsLoadingTimetable] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void listAvailableAuraTerms(controller.signal)
      .then((items) => {
        if (!active) return;
        setTerms(items);
        const firstTermId = items[0]?.id ?? "";
        if (firstTermId) setIsLoadingTimetable(true);
        setTermId(firstTermId);
      })
      .catch((cause: unknown) => active && setError(message(cause)))
      .finally(() => active && setIsLoadingTerms(false));
    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  useEffect(() => {
    if (!termId) return undefined;
    const controller = new AbortController();
    let active = true;
    void getMyAuraInstructorTimetable(termId, controller.signal)
      .then((result) => active && setTimetable(result))
      .catch((cause: unknown) => active && setError(message(cause)))
      .finally(() => active && setIsLoadingTimetable(false));
    return () => {
      active = false;
      controller.abort();
    };
  }, [termId]);

  const selectTerm = (nextTermId: string) => {
    setTermId(nextTermId);
    setIsLoadingTimetable(true);
    setError(null);
    setTimetable(null);
  };

  return (
    <div className="grid gap-6 pb-8">
      <PageHeader
        description="Your published teaching schedule, grouped by day and room."
        eyebrow="AURA"
        title="Teaching timetable"
      />
      {terms.length ? (
        <label className="grid max-w-md gap-1.5 text-sm font-medium text-slate-700">
          Academic term
          <select className="rounded-xl border border-slate-300 bg-white px-3 py-2" onChange={(event) => selectTerm(event.target.value)} value={termId}>
            {terms.map((term) => <option key={term.id} value={term.id}>{term.name}</option>)}
          </select>
        </label>
      ) : null}
      {error ? <ErrorMessage message={error} /> : null}
      {isLoadingTerms || isLoadingTimetable ? (
        <Card><CardContent className="grid min-h-56 place-items-center"><LoadingSpinner label="Loading teaching timetable" /></CardContent></Card>
      ) : timetable?.sessions.length ? (
        <div className="grid gap-4 lg:grid-cols-2">
          {dayNames.slice(1).map((day, index) => {
            const rows = timetable.sessions.filter((session) => session.dayOfWeek === index + 1);
            if (!rows.length) return null;
            return (
              <Card key={day}>
                <CardContent className="grid gap-3">
                  <h2 className="font-bold text-slate-950">{day}</h2>
                  {rows.map((session) => (
                    <div className="rounded-xl border border-slate-200 p-3 text-sm" key={session.id}>
                      <p className="font-semibold text-slate-950">{session.courseCode} · {session.sectionName}</p>
                      <p className="mt-1 text-slate-500">{session.startsAt}–{session.endsAt} · {session.roomName}</p>
                    </div>
                  ))}
                </CardContent>
              </Card>
            );
          })}
        </div>
      ) : (
        <EmptyState description="No published teaching sessions are assigned to you for this term." icon={<CalendarClock className="size-6" />} title="No teaching timetable" />
      )}
    </div>
  );
}

function message(error: unknown) {
  return error instanceof ApiError
    ? error.message
    : "Your teaching timetable could not be loaded.";
}
