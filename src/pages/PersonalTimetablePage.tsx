import { AlertTriangle, CalendarClock, CalendarPlus, Printer } from "lucide-react";
import { useEffect, useState } from "react";

import { ApiError } from "@/api/apiClient";
import {
  getMyAuraTimetable,
  downloadMyAuraTimetableCalendar,
  listAvailableAuraTerms,
  listMyAuraRegistrations,
  listMyAuraResolutionCases,
  requestAuraResolution,
} from "@/api/auraApi";
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
import type {
  AuraPersonalTimetable,
  AuraResolutionCase,
  AuraStudentRegistration,
  AuraTerm,
} from "@/types/aura";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

const dayNames = [
  "",
  "Monday",
  "Tuesday",
  "Wednesday",
  "Thursday",
  "Friday",
  "Saturday",
  "Sunday",
];

export function PersonalTimetablePage() {
  useDocumentTitle("My Timetable");
  const [terms, setTerms] = useState<AuraTerm[]>([]);
  const [selectedTermId, setSelectedTermId] = useState("");
  const [timetable, setTimetable] = useState<AuraPersonalTimetable | null>(null);
  const [registrations, setRegistrations] = useState<AuraStudentRegistration[]>([]);
  const [resolutionCases, setResolutionCases] = useState<AuraResolutionCase[]>([]);
  const [registrationId, setRegistrationId] = useState("");
  const [resolutionSummary, setResolutionSummary] = useState("");
  const [isRequestingResolution, setIsRequestingResolution] = useState(false);
  const [isDownloadingCalendar, setIsDownloadingCalendar] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void listAvailableAuraTerms(controller.signal)
      .then((response) => {
        if (!active) return;
        setTerms(response);
        setSelectedTermId(response.at(0)?.id ?? "");
      })
      .catch((requestError: unknown) => {
        if (active) setError(messageFor(requestError, "Your timetable could not be loaded."));
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  useEffect(() => {
    if (!selectedTermId) {
      return undefined;
    }
    const controller = new AbortController();
    let active = true;
    void Promise.resolve()
      .then(() => {
        if (active) {
          setIsLoading(true);
          setError(null);
        }
        return Promise.all([
          getMyAuraTimetable(selectedTermId, controller.signal),
          listMyAuraRegistrations(selectedTermId, controller.signal),
          listMyAuraResolutionCases(selectedTermId, controller.signal),
        ]);
      })
      .then(([response, registrationResponse, caseResponse]) => {
        if (!active) return;
        setTimetable(response);
        setRegistrations(registrationResponse);
        setResolutionCases(caseResponse);
        setRegistrationId((current) => current || registrationResponse.at(0)?.id || "");
      })
      .catch((requestError: unknown) => {
        if (active) setError(messageFor(requestError, "Your timetable could not be loaded."));
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [selectedTermId]);

  const submitResolutionRequest = async () => {
    if (!selectedTermId || !registrationId || !resolutionSummary.trim()) return;
    setIsRequestingResolution(true);
    setError(null);
    try {
      await requestAuraResolution(
        selectedTermId,
        registrationId,
        "MANUAL_REQUEST",
        resolutionSummary.trim(),
      );
      setResolutionSummary("");
      setResolutionCases(await listMyAuraResolutionCases(selectedTermId));
    } catch (requestError) {
      setError(messageFor(requestError, "Your resolution request could not be submitted."));
    } finally {
      setIsRequestingResolution(false);
    }
  };

  const downloadCalendar = async () => {
    if (!selectedTermId || isDownloadingCalendar) return;
    setIsDownloadingCalendar(true);
    setError(null);
    try {
      await downloadMyAuraTimetableCalendar(selectedTermId);
    } catch (requestError) {
      setError(messageFor(requestError, "Your calendar could not be downloaded."));
    } finally {
      setIsDownloadingCalendar(false);
    }
  };

  return (
    <div className="grid gap-6 pb-8 print:block">
      <PageHeader
        actions={
          <div className="flex flex-wrap gap-2">
            <Button
              disabled={!selectedTermId}
              loading={isDownloadingCalendar}
              onClick={() => void downloadCalendar()}
              variant="outline"
            >
              <CalendarPlus className="size-4" /> Add to calendar
            </Button>
            <Button onClick={() => window.print()} variant="outline">
              <Printer className="size-4" /> Print
            </Button>
          </div>
        }
        description="Your published classes, including repeater, elective, and cross-section registrations."
        eyebrow="AURA"
        title="My timetable"
      />

      {error ? <ErrorMessage message={error} /> : null}

      {terms.length ? (
        <label className="grid max-w-sm gap-1.5 text-sm font-medium text-slate-700 print:hidden">
          Academic term
          <select
            className="rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm"
            onChange={(event) => setSelectedTermId(event.target.value)}
            value={selectedTermId}
          >
            {terms.map((term) => (
              <option key={term.id} value={term.id}>
                {term.name}
              </option>
            ))}
          </select>
        </label>
      ) : null}

      {isLoading ? (
        <Card>
          <CardContent className="grid min-h-56 place-items-center">
            <LoadingSpinner label="Loading your timetable" />
          </CardContent>
        </Card>
      ) : timetable?.sessions.length ? (
        <div className="grid gap-4">
          {timetable.clashes.length ? (
            <div className="grid gap-4 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900 print:hidden">
              <div className="flex items-center gap-2 font-semibold">
                <AlertTriangle className="size-4" />
                {timetable.clashes.length} personal timetable clash
                {timetable.clashes.length === 1 ? "" : "es"}
              </div>
              <ul className="mt-2 list-disc pl-5">
                {timetable.clashes.map((clash) => (
                  <li key={`${clash.leftSessionId}-${clash.rightSessionId}`}>
                    {clash.message}
                  </li>
                ))}
              </ul>
              <div className="grid gap-3 rounded-xl bg-white/70 p-3 sm:grid-cols-2">
                <label className="grid gap-1 font-medium">
                  Registration to review
                  <select
                    className="rounded-xl border border-amber-300 bg-white px-3 py-2"
                    onChange={(event) => setRegistrationId(event.target.value)}
                    value={registrationId}
                  >
                    {registrations.map((registration) => (
                      <option key={registration.id} value={registration.id}>
                        {registration.courseCode} · {registration.registrationType.replaceAll("_", " ")}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="grid gap-1 font-medium">
                  What needs resolving?
                  <input
                    className="rounded-xl border border-amber-300 bg-white px-3 py-2"
                    maxLength={500}
                    onChange={(event) => setResolutionSummary(event.target.value)}
                    placeholder="Describe the classes that overlap"
                    value={resolutionSummary}
                  />
                </label>
                <Button
                  className="sm:col-span-2"
                  disabled={!registrationId || !resolutionSummary.trim()}
                  loading={isRequestingResolution}
                  onClick={() => void submitResolutionRequest()}
                >
                  Request a safe alternative
                </Button>
              </div>
            </div>
          ) : null}
          {resolutionCases.length ? (
            <Card className="print:hidden">
              <CardContent className="grid gap-2">
                <h2 className="font-semibold text-slate-950">My resolution requests</h2>
                {resolutionCases.map((resolutionCase) => (
                  <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-slate-200 p-3 text-sm" key={resolutionCase.id}>
                    <span>{resolutionCase.summary}</span>
                    <Badge>{resolutionCase.status.replaceAll("_", " ")}</Badge>
                  </div>
                ))}
              </CardContent>
            </Card>
          ) : null}
          {dayNames.slice(1).map((dayName, dayIndex) => {
            const sessions = timetable.sessions.filter(
              (session) => session.dayOfWeek === dayIndex + 1,
            );
            if (!sessions.length) return null;
            return (
              <Card key={dayName}>
                <CardContent className="grid gap-3">
                  <h2 className="text-lg font-bold text-slate-950">{dayName}</h2>
                  {sessions.map((session) => (
                    <div
                      className={`grid gap-2 rounded-2xl border p-4 sm:grid-cols-[0.8fr_1.4fr_1fr_1fr] ${
                        session.personalClash
                          ? "border-amber-300 bg-amber-50"
                          : "border-slate-200"
                      }`}
                      key={session.sessionId}
                    >
                      <p className="font-semibold text-slate-950">
                        {session.startsAt}–{session.endsAt}
                      </p>
                      <div>
                        <p className="font-semibold text-slate-950">
                          {session.courseCode} · {session.courseTitle}
                        </p>
                        <Badge>{session.registrationType.replaceAll("_", " ")}</Badge>
                      </div>
                      <p className="text-sm text-slate-600">
                        {session.instructorName}
                      </p>
                      <p className="text-sm text-slate-600">
                        {session.roomName} · {session.sectionName}
                      </p>
                    </div>
                  ))}
                </CardContent>
              </Card>
            );
          })}
        </div>
      ) : (
        <EmptyState
          description={
            terms.length
              ? "No active registrations are scheduled in the published timetable yet."
              : "A published timetable is not available for your university yet."
          }
          icon={<CalendarClock className="size-6" />}
          title="No timetable to show"
        />
      )}
    </div>
  );
}

function messageFor(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}
