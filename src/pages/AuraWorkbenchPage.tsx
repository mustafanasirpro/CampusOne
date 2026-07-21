import {
  Activity,
  AlertTriangle,
  CalendarClock,
  CheckCircle2,
  Play,
  RefreshCw,
  Sparkles,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { ApiError } from "@/api/apiClient";
import {
  createAuraTerm,
  getAuraCapabilities,
  getAuraMetrics,
  getAuraReadiness,
  getAuraGenerationRun,
  listAuraClashes,
  listAuraSessions,
  listAuraTerms,
  listAuraVersions,
  publishAuraVersion,
  startAuraGeneration,
} from "@/api/auraApi";
import { getCurrentUserIdentity, type CurrentUserIdentity } from "@/api/userApi";
import {
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  useToast,
} from "@/components/common";
import {
  AuraImportPanel,
  AuraConstraintProfilePanel,
  AuraRegistrationPanel,
  AuraResolutionPanel,
  AuraScenarioPanel,
  AuraSetupPanel,
  AuraVersionTools,
} from "@/components/aura";
import type {
  AuraClash,
  AuraConstraintProfileName,
  AuraGenerationRun,
  AuraMetrics,
  AuraReadiness,
  AuraSession,
  AuraTerm,
  AuraTimetableVersion,
} from "@/types/aura";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

const defaultTermForm = {
  code: "",
  endsOn: "",
  name: "",
  startsOn: "",
};

const dayLabels = ["", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

function apiMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}

function formatDate(value: string | null | undefined) {
  if (!value) return "Not set";
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(
    new Date(value),
  );
}

export function AuraWorkbenchPage() {
  const [currentUser, setCurrentUser] = useState<CurrentUserIdentity | null>(
    null,
  );
  const [terms, setTerms] = useState<AuraTerm[]>([]);
  const [selectedTermId, setSelectedTermId] = useState("");
  const [readiness, setReadiness] = useState<AuraReadiness | null>(null);
  const [metrics, setMetrics] = useState<AuraMetrics | null>(null);
  const [versions, setVersions] = useState<AuraTimetableVersion[]>([]);
  const [selectedVersionId, setSelectedVersionId] = useState("");
  const [sessions, setSessions] = useState<AuraSession[]>([]);
  const [clashes, setClashes] = useState<AuraClash[]>([]);
  const [activeRun, setActiveRun] = useState<AuraGenerationRun | null>(null);
  const [termForm, setTermForm] = useState(defaultTermForm);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreatingTerm, setIsCreatingTerm] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const [generationProfile, setGenerationProfile] =
    useState<AuraConstraintProfileName>("BALANCED");
  const [canManage, setCanManage] = useState<boolean | null>(null);
  const { showToast } = useToast();

  useDocumentTitle("AURA Timetable Generator · CampusOne");

  const selectedTerm = useMemo(
    () => terms.find((term) => term.id === selectedTermId) ?? null,
    [selectedTermId, terms],
  );

  const selectedVersion = useMemo(
    () => versions.find((version) => version.id === selectedVersionId) ?? null,
    [selectedVersionId, versions],
  );

  const loadTerms = useCallback(async (signal?: AbortSignal) => {
    const response = await listAuraTerms(signal);
    setTerms(response.content);
    setSelectedTermId((current) =>
      current || response.content.at(0)?.id || "",
    );
  }, []);

  const loadTermDetails = useCallback(
    async (termId: string, signal?: AbortSignal) => {
      if (!termId) return;
      const [readinessResponse, versionsResponse, metricsResponse] =
        await Promise.all([
          getAuraReadiness(termId, signal),
          listAuraVersions(termId, signal),
          getAuraMetrics(termId, signal),
        ]);
      setReadiness(readinessResponse);
      setVersions(versionsResponse);
      setMetrics(metricsResponse);
      setSelectedVersionId((current) =>
        current && versionsResponse.some((version) => version.id === current)
          ? current
          : versionsResponse.at(0)?.id || "",
      );
      if (versionsResponse.length === 0) {
        setSessions([]);
        setClashes([]);
      }
    },
    [],
  );

  const loadVersionDetails = useCallback(
    async (versionId: string, signal?: AbortSignal) => {
      if (!versionId) {
        return;
      }
      const [sessionsResponse, clashesResponse] = await Promise.all([
        listAuraSessions(versionId, signal),
        listAuraClashes(versionId, signal),
      ]);
      setSessions(sessionsResponse);
      setClashes(clashesResponse);
    },
    [],
  );

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void Promise.all([
      getCurrentUserIdentity(controller.signal),
      getAuraCapabilities(controller.signal),
    ])
      .then(async ([identity, capabilities]) => {
        if (!active) return;
        setCurrentUser(identity);
        setCanManage(capabilities.canManage);
        if (!capabilities.canManage) return;
        const termPage = await listAuraTerms(controller.signal);
        if (!active) return;
        setTerms(termPage.content);
        setSelectedTermId(termPage.content.at(0)?.id || "");
        setError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setError(
          apiMessage(
            requestError,
            "AURA could not be loaded. Please try again.",
          ),
        );
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
    const controller = new AbortController();
    let active = true;
    if (!selectedTermId) {
      return undefined;
    }
    void Promise.all([
      getAuraReadiness(selectedTermId, controller.signal),
      listAuraVersions(selectedTermId, controller.signal),
      getAuraMetrics(selectedTermId, controller.signal),
    ])
      .then(([readinessResponse, versionsResponse, metricsResponse]) => {
        if (!active) return;
        setReadiness(readinessResponse);
        setVersions(versionsResponse);
        setMetrics(metricsResponse);
        setSelectedVersionId((current) =>
          current &&
          versionsResponse.some((version) => version.id === current)
            ? current
            : versionsResponse.at(0)?.id || "",
        );
        if (versionsResponse.length === 0) {
          setSessions([]);
          setClashes([]);
        }
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setError(
          apiMessage(
            requestError,
            "AURA term details could not be loaded.",
          ),
        );
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [selectedTermId]);

  useEffect(() => {
    if (!selectedVersionId) {
      return undefined;
    }
    const controller = new AbortController();
    let active = true;
    void Promise.all([
      listAuraSessions(selectedVersionId, controller.signal),
      listAuraClashes(selectedVersionId, controller.signal),
    ])
      .then(([sessionsResponse, clashesResponse]) => {
        if (!active) return;
        setSessions(sessionsResponse);
        setClashes(clashesResponse);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setError(
          apiMessage(
            requestError,
            "Timetable version details could not be loaded.",
          ),
        );
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [selectedVersionId]);

  useEffect(() => {
    if (!activeRun || !["QUEUED", "RUNNING"].includes(activeRun.status)) {
      return undefined;
    }
    let active = true;
    const handle = window.setInterval(() => {
      void getAuraGenerationRun(activeRun.id)
        .then((run) => {
          if (!active) return;
          setActiveRun(run);
          if (!["QUEUED", "RUNNING"].includes(run.status)) {
            if (selectedTermId) {
              void loadTermDetails(selectedTermId);
            }
          }
        })
        .catch((requestError: unknown) => {
          if (active) {
            setError(apiMessage(
              requestError,
              "Generation status could not be refreshed. Try refreshing the page.",
            ));
          }
        });
    }, 2500);
    return () => {
      active = false;
      window.clearInterval(handle);
    };
  }, [activeRun, loadTermDetails, selectedTermId]);

  const refreshAll = async () => {
    setError(null);
    await loadTerms();
    if (selectedTermId) await loadTermDetails(selectedTermId);
    if (selectedVersionId) await loadVersionDetails(selectedVersionId);
  };

  const handleCreateTerm = async () => {
    if (!currentUser?.university.id) return;
    setIsCreatingTerm(true);
    setError(null);
    try {
      const term = await createAuraTerm({
        ...termForm,
        universityId: currentUser.university.id,
      });
      setTermForm(defaultTermForm);
      setSelectedTermId(term.id);
      await loadTerms();
      showToast({
        title: "Term created",
        message: "AURA is ready for setup data for this term.",
        variant: "success",
      });
    } catch (requestError) {
      setError(apiMessage(requestError, "The term could not be created."));
    } finally {
      setIsCreatingTerm(false);
    }
  };

  const handleGenerate = async () => {
    if (!selectedTermId) return;
    setIsGenerating(true);
    setError(null);
    try {
      const run = await startAuraGeneration(
        selectedTermId,
        30,
        generationProfile,
        0,
      );
      setActiveRun(run);
      showToast({
        title: "Generation started",
        message: "AURA is building a timetable in the background.",
        variant: "success",
      });
    } catch (requestError) {
      setError(apiMessage(requestError, "Generation could not be started."));
    } finally {
      setIsGenerating(false);
    }
  };

  const handlePublish = async () => {
    if (!selectedVersionId) return;
    setIsPublishing(true);
    setError(null);
    try {
      await publishAuraVersion(selectedVersionId);
      if (selectedTermId) await loadTermDetails(selectedTermId);
      showToast({
        title: "Timetable published",
        message: "This version is now the active campus timetable.",
        variant: "success",
      });
    } catch (requestError) {
      setError(apiMessage(requestError, "The timetable could not be published."));
    } finally {
      setIsPublishing(false);
    }
  };

  if (isLoading) {
    return (
      <div className="grid min-h-72 place-items-center rounded-2xl border border-slate-200 bg-white">
        <LoadingSpinner label="Loading AURA" />
      </div>
    );
  }

  if (canManage === false) {
    return (
      <EmptyState
        description="AURA administration is available to university admins. Your personal timetable remains available from My timetable."
        icon={<AlertTriangle className="size-6" />}
        title="You do not have access to AURA administration"
      />
    );
  }

  return (
    <div className="grid gap-6 pb-8">
      <PageHeader
        actions={
          <Button onClick={() => void refreshAll()} variant="outline">
            <RefreshCw className="size-4" />
            Refresh
          </Button>
        }
        description="Generate clash-aware university timetables, inspect readiness, review conflicts, and publish safe versions."
        eyebrow="Automated scheduling"
        title="AURA Timetable Generator"
      />

      {error ? <ErrorMessage message={error} /> : null}

      <section className="grid gap-4 xl:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
        <Card>
          <CardHeader>
            <CardTitle>Create academic term</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3">
            <p className="text-sm text-slate-500">
              Terms are created for {currentUser?.university.shortName ?? "your university"}.
            </p>
            <label className="grid gap-1.5 text-sm font-medium text-slate-700">
              Term code
              <input
                className="rounded-xl border border-slate-300 px-3 py-2 text-sm"
                onChange={(event) =>
                  setTermForm((current) => ({
                    ...current,
                    code: event.target.value,
                  }))
                }
                placeholder="FALL-2026"
                value={termForm.code}
              />
            </label>
            <label className="grid gap-1.5 text-sm font-medium text-slate-700">
              Term name
              <input
                className="rounded-xl border border-slate-300 px-3 py-2 text-sm"
                onChange={(event) =>
                  setTermForm((current) => ({
                    ...current,
                    name: event.target.value,
                  }))
                }
                placeholder="Fall 2026"
                value={termForm.name}
              />
            </label>
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="grid gap-1.5 text-sm font-medium text-slate-700">
                Start date
                <input
                  className="rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  onChange={(event) =>
                    setTermForm((current) => ({
                      ...current,
                      startsOn: event.target.value,
                    }))
                  }
                  type="date"
                  value={termForm.startsOn}
                />
              </label>
              <label className="grid gap-1.5 text-sm font-medium text-slate-700">
                End date
                <input
                  className="rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  onChange={(event) =>
                    setTermForm((current) => ({
                      ...current,
                      endsOn: event.target.value,
                    }))
                  }
                  type="date"
                  value={termForm.endsOn}
                />
              </label>
            </div>
            <Button
              disabled={
                !termForm.code ||
                !termForm.name ||
                !termForm.startsOn ||
                !termForm.endsOn
              }
              loading={isCreatingTerm}
              onClick={() => void handleCreateTerm()}
            >
              Create term
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Term readiness</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4">
            {terms.length ? (
              <>
                <select
                  aria-label="Academic term"
                  className="rounded-xl border border-slate-300 px-3 py-2 text-sm"
                  onChange={(event) => setSelectedTermId(event.target.value)}
                  value={selectedTermId}
                >
                  {terms.map((term) => (
                    <option key={term.id} value={term.id}>
                      {term.name} · {term.status}
                    </option>
                  ))}
                </select>
                {selectedTerm ? (
                  <div className="rounded-2xl bg-slate-50 p-4 text-sm text-slate-600">
                    <p className="font-semibold text-slate-950">
                      {selectedTerm.code}
                    </p>
                    <p>
                      {formatDate(selectedTerm.startsOn)} –{" "}
                      {formatDate(selectedTerm.endsOn)}
                    </p>
                  </div>
                ) : null}
                <div className="grid gap-3 sm:grid-cols-5">
                  <Metric label="Rooms" value={readiness?.activeRooms ?? 0} />
                  <Metric
                    label="Timeslots"
                    value={readiness?.activeTimeslots ?? 0}
                  />
                  <Metric
                    label="Instructors"
                    value={readiness?.activeInstructors ?? 0}
                  />
                  <Metric
                    label="Offerings"
                    value={readiness?.activeOfferings ?? 0}
                  />
                  <Metric
                    label="Meetings"
                    value={readiness?.meetingRequirements ?? 0}
                  />
                </div>
                <div
                  className={
                    readiness?.ready
                      ? "rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800"
                      : "rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800"
                  }
                >
                  <div className="flex items-center gap-2 font-semibold">
                    {readiness?.ready ? (
                      <CheckCircle2 className="size-4" />
                    ) : (
                      <AlertTriangle className="size-4" />
                    )}
                    {readiness?.ready
                      ? "Ready for generation"
                      : "Setup needs attention"}
                  </div>
                  {readiness?.issues.length ? (
                    <ul className="mt-3 list-disc space-y-1 pl-5">
                      {readiness.issues.map((issue) => (
                        <li key={issue.code}>{issue.message}</li>
                      ))}
                    </ul>
                  ) : null}
                </div>
                <Button
                  disabled={!readiness?.ready}
                  loading={isGenerating}
                  onClick={() => void handleGenerate()}
                >
                  <Play className="size-4" />
                  Generate timetable
                </Button>
              </>
            ) : (
              <EmptyState
                description="Create a term, then add rooms, timeslots, instructors, offerings, and meeting requirements through the AURA setup API."
                icon={<CalendarClock className="size-6" />}
                title="No AURA terms yet"
              />
            )}
          </CardContent>
        </Card>
      </section>

      {selectedTermId && currentUser?.university.id ? (
        <AuraSetupPanel
          onChanged={() => loadTermDetails(selectedTermId)}
          termId={selectedTermId}
          universityId={currentUser.university.id}
        />
      ) : null}

      {selectedTermId ? (
        <AuraConstraintProfilePanel
          onProfileChange={setGenerationProfile}
          selectedProfile={generationProfile}
          termId={selectedTermId}
        />
      ) : null}

      {selectedTermId ? (
        <AuraRegistrationPanel
          onChanged={() => loadTermDetails(selectedTermId)}
          termId={selectedTermId}
        />
      ) : null}

      {selectedTermId ? <AuraResolutionPanel termId={selectedTermId} /> : null}

      {selectedTermId ? <AuraImportPanel termId={selectedTermId} /> : null}

      <section className="grid gap-4 xl:grid-cols-4">
        <MetricCard icon={Sparkles} label="Versions" value={metrics?.versions ?? 0} />
        <MetricCard
          icon={CheckCircle2}
          label="Published"
          value={metrics?.publishedVersions ?? 0}
        />
        <MetricCard
          icon={Activity}
          label="Sessions"
          value={metrics?.scheduledSessions ?? 0}
        />
        <MetricCard
          icon={AlertTriangle}
          label="Open clashes"
          value={metrics?.unresolvedClashes ?? 0}
        />
      </section>

      {activeRun ? (
        <Card>
          <CardContent className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="font-semibold text-slate-950">
                Generation run {activeRun.status.toLowerCase()}
              </p>
              <p className="text-sm text-slate-500">
                {activeRun.message ?? "AURA is searching for a clean timetable."}
              </p>
            </div>
            <Badge>{activeRun.score ?? activeRun.status}</Badge>
          </CardContent>
        </Card>
      ) : null}

      <section className="grid gap-4 xl:grid-cols-[0.8fr_1.2fr]">
        <Card>
          <CardHeader>
            <CardTitle>Versions</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3">
            {versions.length ? (
              <>
                {versions.map((version) => (
                  <button
                    className={`rounded-2xl border p-4 text-left transition ${
                      selectedVersionId === version.id
                        ? "border-brand-300 bg-brand-50"
                        : "border-slate-200 bg-white hover:bg-slate-50"
                    }`}
                    key={version.id}
                    onClick={() => setSelectedVersionId(version.id)}
                    type="button"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <p className="font-semibold text-slate-950">
                        Version {version.versionNumber}
                      </p>
                      <Badge>{version.status}</Badge>
                    </div>
                    <p className="mt-1 text-sm text-slate-500">
                      {version.score ?? "Score pending"} ·{" "}
                      {formatDate(version.createdAt)}
                    </p>
                  </button>
                ))}
                <Button
                  disabled={!selectedVersion || selectedVersion.status === "PUBLISHED"}
                  loading={isPublishing}
                  onClick={() => void handlePublish()}
                >
                  Publish selected version
                </Button>
              </>
            ) : (
              <EmptyState
                description="Generate a timetable to create the first version."
                icon={<Sparkles className="size-6" />}
                title="No timetable versions"
              />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Timetable sessions</CardTitle>
          </CardHeader>
          <CardContent>
            {sessions.length ? (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="text-left text-xs uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="py-2 pr-4">Course</th>
                      <th className="py-2 pr-4">Section</th>
                      <th className="py-2 pr-4">Instructor</th>
                      <th className="py-2 pr-4">Room</th>
                      <th className="py-2 pr-4">Time</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {sessions.slice(0, 20).map((session) => (
                      <tr key={session.id}>
                        <td className="py-3 pr-4 font-medium text-slate-950">
                          {session.courseCode}
                        </td>
                        <td className="py-3 pr-4 text-slate-600">
                          {session.sectionName}
                        </td>
                        <td className="py-3 pr-4 text-slate-600">
                          {session.instructorName}
                        </td>
                        <td className="py-3 pr-4 text-slate-600">
                          {session.roomName}
                        </td>
                        <td className="py-3 pr-4 text-slate-600">
                          {dayLabels[session.dayOfWeek]} {session.startsAt}–
                          {session.endsAt}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <EmptyState
                description="Select a generated version to inspect scheduled sessions."
                icon={<CalendarClock className="size-6" />}
                title="No sessions to show"
              />
            )}
          </CardContent>
        </Card>
      </section>

      <AuraVersionTools
        onChanged={refreshAll}
        onVersionSelected={setSelectedVersionId}
        selectedVersionId={selectedVersionId}
        sessions={sessions}
        versions={versions}
      />

      {selectedTermId ? (
        <AuraScenarioPanel
          onChanged={refreshAll}
          selectedVersionId={selectedVersionId}
          sessions={sessions}
          termId={selectedTermId}
          versions={versions}
        />
      ) : null}

      <Card>
        <CardHeader>
          <CardTitle>Clash resolver</CardTitle>
        </CardHeader>
        <CardContent>
          {clashes.length ? (
            <div className="grid gap-3">
              {clashes.map((clash) => (
                <div
                  className="rounded-2xl border border-red-200 bg-red-50 p-4"
                  key={clash.id}
                >
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge>{clash.severity}</Badge>
                    <p className="font-semibold text-red-900">
                      {clash.clashType.replaceAll("_", " ")}
                    </p>
                  </div>
                  <p className="mt-2 text-sm text-red-700">{clash.message}</p>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              description="AURA will list hard timetable conflicts here after generation or manual moves."
              icon={<CheckCircle2 className="size-6" />}
              title="No open clashes"
            />
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-2xl bg-slate-50 p-3">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
        {label}
      </p>
      <p className="mt-1 text-2xl font-bold text-slate-950">{value}</p>
    </div>
  );
}

function MetricCard({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Activity;
  label: string;
  value: number;
}) {
  return (
    <Card>
      <CardContent className="flex items-center gap-3">
        <span className="grid size-10 place-items-center rounded-2xl bg-brand-50 text-brand-700">
          <Icon className="size-5" />
        </span>
        <div>
          <p className="text-sm text-slate-500">{label}</p>
          <p className="text-2xl font-bold text-slate-950">{value}</p>
        </div>
      </CardContent>
    </Card>
  );
}
