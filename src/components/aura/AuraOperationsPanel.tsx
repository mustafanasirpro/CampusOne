import { Activity, Building2, GitMerge, Route, Wrench } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/api/apiClient";
import {
  applyAuraRepair,
  createAuraBuilding,
  createAuraOfferingConflict,
  createAuraTeachingGroup,
  createAuraTravelRule,
  getAuraAnalytics,
  getAuraSetupReferences,
  getAuraScopedTimetable,
  listAuraAuditEvents,
  listAuraBuildings,
  listAuraOfferingConflicts,
  listAuraOfferings,
  listAuraPrograms,
  listAuraTeachingGroups,
  listAuraTravelRules,
  previewAuraRepair,
  updateAuraBuilding,
  updateAuraOfferingConflict,
  updateAuraTeachingGroup,
  updateAuraTravelRule,
} from "@/api/auraApi";
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
} from "@/components/common";
import type {
  AuraAnalytics,
  AuraAuditEvent,
  AuraBuilding,
  AuraClash,
  AuraOffering,
  AuraOfferingConflict,
  AuraProgram,
  AuraRepairPlan,
  AuraScopedTimetable,
  AuraSession,
  AuraTeachingGroup,
  AuraTravelRule,
  AuraSetupReferences,
} from "@/types/aura";

interface AuraOperationsPanelProps {
  clashes: AuraClash[];
  onChanged: () => Promise<void>;
  sessions: AuraSession[];
  termId: string;
  universityId: string;
  versionId?: string;
}

const inputClass =
  "w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100";

export function AuraOperationsPanel({
  clashes,
  onChanged,
  sessions,
  termId,
  universityId,
  versionId,
}: AuraOperationsPanelProps) {
  const [buildings, setBuildings] = useState<AuraBuilding[]>([]);
  const [groups, setGroups] = useState<AuraTeachingGroup[]>([]);
  const [conflicts, setConflicts] = useState<AuraOfferingConflict[]>([]);
  const [offerings, setOfferings] = useState<AuraOffering[]>([]);
  const [programs, setPrograms] = useState<AuraProgram[]>([]);
  const [references, setReferences] = useState<AuraSetupReferences | null>(null);
  const [travelRules, setTravelRules] = useState<AuraTravelRule[]>([]);
  const [audit, setAudit] = useState<AuraAuditEvent[]>([]);
  const [analytics, setAnalytics] = useState<AuraAnalytics | null>(null);
  const [view, setView] = useState<AuraScopedTimetable | null>(null);
  const [repair, setRepair] = useState<AuraRepairPlan | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [busy, setBusy] = useState<string | null>(null);
  const [buildingForm, setBuildingForm] = useState({ code: "", name: "" });
  const [groupForm, setGroupForm] = useState({
    capacity: "40",
    code: "",
    displayName: "",
    groupType: "LAB" as AuraTeachingGroup["groupType"],
    offeringId: "",
  });
  const [conflictForm, setConflictForm] = useState({
    leftOfferingId: "",
    reason: "",
    rightOfferingId: "",
  });
  const [travelForm, setTravelForm] = useState({
    fromBuilding: "",
    minutes: "10",
    toBuilding: "",
  });
  const [repairReason, setRepairReason] = useState("");
  const [viewScope, setViewScope] = useState("WEEK");
  const [viewResource, setViewResource] = useState("");
  const [viewDay, setViewDay] = useState("1");

  const load = useCallback(
    async (signal?: AbortSignal) => {
      const [nextBuildings, nextGroups, nextConflicts, nextTravel, nextAudit, nextOfferings, nextPrograms, nextReferences] =
        await Promise.all([
          listAuraBuildings(signal),
          listAuraTeachingGroups(termId, signal),
          listAuraOfferingConflicts(termId, signal),
          listAuraTravelRules(signal),
          listAuraAuditEvents(termId, signal),
          listAuraOfferings(termId, signal),
          listAuraPrograms(universityId, signal),
          getAuraSetupReferences(signal),
        ]);
      setBuildings(nextBuildings);
      setGroups(nextGroups);
      setConflicts(nextConflicts);
      setTravelRules(nextTravel);
      setAudit(nextAudit);
      setOfferings(nextOfferings);
      setPrograms(nextPrograms);
      setReferences(nextReferences);
      if (versionId) {
        const [nextAnalytics, nextView] = await Promise.all([
          getAuraAnalytics(termId, versionId, signal),
          getAuraScopedTimetable(versionId, "WEEK", undefined, undefined, signal),
        ]);
        setAnalytics(nextAnalytics);
        setView(nextView);
      } else {
        setAnalytics(null);
        setView(null);
      }
    },
    [termId, universityId, versionId],
  );

  const timetableOptions = (() => {
    const unique = (items: Array<{ label: string; value: string }>) =>
      [...new Map(items.map((item) => [item.value, item])).values()];
    switch (viewScope) {
      case "INSTRUCTOR": return unique(sessions.map((item) => ({ label: item.instructorName, value: item.instructorId })));
      case "SECTION": return unique(sessions.map((item) => ({ label: item.sectionName, value: item.sectionId })));
      case "ROOM": return unique(sessions.map((item) => ({ label: item.roomName, value: item.roomId })));
      case "OFFERING": return offerings.map((item) => ({ label: `${item.courseCode} · ${item.sectionName}`, value: item.id }));
      case "COURSE": return unique(offerings.map((item) => ({ label: `${item.courseCode} · ${item.courseTitle}`, value: item.courseId })));
      case "PROGRAM": return programs.map((item) => ({ label: `${item.code} · ${item.name}`, value: item.id }));
      case "DEPARTMENT": return (references?.departments ?? []).map((item) => ({ label: `${item.code} · ${item.name}`, value: item.id }));
      default: return [];
    }
  })();

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    async function loadOperations() {
      try {
        await load(controller.signal);
        if (active) setError(null);
      } catch (cause: unknown) {
        if (active && !controller.signal.aborted) {
          setError(messageFor(cause, "AURA operations could not be loaded."));
        }
      } finally {
        if (active) setIsLoading(false);
      }
    }

    void loadOperations();
    return () => {
      active = false;
      controller.abort();
    };
  }, [load]);

  const mutate = async (name: string, action: () => Promise<unknown>) => {
    setBusy(name);
    setError(null);
    try {
      await action();
      await Promise.all([load(), onChanged()]);
    } catch (cause) {
      setError(messageFor(cause, "The scheduling change could not be saved."));
    } finally {
      setBusy(null);
    }
  };

  const loadTimetableView = async () => {
    if (!versionId) return;
    setBusy("view");
    setError(null);
    try {
      setView(await getAuraScopedTimetable(
        versionId,
        viewScope,
        ["WEEK", "UNIVERSITY", "DAY"].includes(viewScope) ? undefined : viewResource,
        viewScope === "DAY" ? Number(viewDay) : undefined,
      ));
    } catch (cause) {
      setError(messageFor(cause, "The selected timetable view could not be loaded."));
    } finally {
      setBusy(null);
    }
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="grid min-h-44 place-items-center">
          <LoadingSpinner label="Loading AURA operations" />
        </CardContent>
      </Card>
    );
  }

  return (
    <section className="grid gap-4" aria-labelledby="aura-operations-title">
      <div>
        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-700">
          Operations and policy
        </p>
        <h2 className="mt-1 text-xl font-bold text-slate-950" id="aura-operations-title">
          Scheduling operations
        </h2>
        <p className="mt-1 text-sm text-slate-500">
          Manage teaching groups, travel policy, hard conflicts, repairs, and traceable changes.
        </p>
      </div>
      {error ? <ErrorMessage message={error} /> : null}

      <div className="grid gap-4 xl:grid-cols-2">
        <OperationCard icon={Building2} title="Buildings">
          <Field label="Building code" onChange={(code) => setBuildingForm((old) => ({ ...old, code }))} value={buildingForm.code} />
          <Field label="Building name" onChange={(name) => setBuildingForm((old) => ({ ...old, name }))} value={buildingForm.name} />
          <Button
            disabled={!buildingForm.code.trim() || !buildingForm.name.trim()}
            loading={busy === "building"}
            onClick={() => void mutate("building", async () => {
              await createAuraBuilding({ ...buildingForm, minimumTransitionMinutes: 5 });
              setBuildingForm({ code: "", name: "" });
            })}
          >Save building</Button>
          <ManagedSummary
            busy={busy}
            items={buildings.map((item) => ({ active: item.active, id: item.id, label: `${item.code} · ${item.name}` }))}
            onToggle={(id) => {
              const item = buildings.find((entry) => entry.id === id);
              if (item) void mutate(`building-${id}`, () => updateAuraBuilding(id, { ...item, active: !item.active }));
            }}
          />
        </OperationCard>

        <OperationCard icon={GitMerge} title="Teaching groups">
          <Select label="Offering" onChange={(offeringId) => setGroupForm((old) => ({ ...old, offeringId }))} options={offerings.map((item) => ({ label: `${item.courseCode} · ${item.sectionName}`, value: item.id }))} value={groupForm.offeringId} />
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Group code" onChange={(code) => setGroupForm((old) => ({ ...old, code }))} value={groupForm.code} />
            <Field label="Display name" onChange={(displayName) => setGroupForm((old) => ({ ...old, displayName }))} value={groupForm.displayName} />
          </div>
          <Select label="Group type" onChange={(groupType) => setGroupForm((old) => ({ ...old, groupType: groupType as AuraTeachingGroup["groupType"] }))} options={["LECTURE", "LAB", "TUTORIAL"].map((value) => ({ label: value, value }))} value={groupForm.groupType} />
          <Field label="Capacity" onChange={(capacity) => setGroupForm((old) => ({ ...old, capacity }))} type="number" value={groupForm.capacity} />
          <Button
            disabled={!groupForm.offeringId || !groupForm.code.trim() || !groupForm.displayName.trim()}
            loading={busy === "group"}
            onClick={() => void mutate("group", async () => {
              await createAuraTeachingGroup({ ...groupForm, active: true, capacity: Number(groupForm.capacity) || null });
              setGroupForm((old) => ({ ...old, code: "", displayName: "" }));
            })}
          >Save teaching group</Button>
          <ManagedSummary
            busy={busy}
            items={groups.map((item) => ({ active: item.active, id: item.id, label: `${item.code} · ${item.displayName}` }))}
            onToggle={(id) => {
              const item = groups.find((entry) => entry.id === id);
              if (item) void mutate(`group-${id}`, () => updateAuraTeachingGroup(id, { ...item, active: !item.active }));
            }}
          />
        </OperationCard>

        <OperationCard icon={Activity} title="Offering conflicts">
          <Select label="First offering" onChange={(leftOfferingId) => setConflictForm((old) => ({ ...old, leftOfferingId }))} options={offeringOptions(offerings)} value={conflictForm.leftOfferingId} />
          <Select label="Conflicting offering" onChange={(rightOfferingId) => setConflictForm((old) => ({ ...old, rightOfferingId }))} options={offeringOptions(offerings)} value={conflictForm.rightOfferingId} />
          <Field label="Reason" onChange={(reason) => setConflictForm((old) => ({ ...old, reason }))} value={conflictForm.reason} />
          <Button
            disabled={!conflictForm.leftOfferingId || !conflictForm.rightOfferingId || conflictForm.leftOfferingId === conflictForm.rightOfferingId || !conflictForm.reason.trim()}
            loading={busy === "conflict"}
            onClick={() => void mutate("conflict", async () => {
              await createAuraOfferingConflict({ ...conflictForm, active: true, severity: "HARD", source: "MANUAL", termId });
              setConflictForm({ leftOfferingId: "", reason: "", rightOfferingId: "" });
            })}
          >Save hard conflict</Button>
          <ManagedSummary
            busy={busy}
            items={conflicts.map((item) => ({ active: item.active, id: item.id, label: `${item.severity} · ${item.reason}` }))}
            onToggle={(id) => {
              const item = conflicts.find((entry) => entry.id === id);
              if (item) void mutate(`conflict-${id}`, () => updateAuraOfferingConflict(id, { ...item, active: !item.active }));
            }}
          />
        </OperationCard>

        <OperationCard icon={Route} title="Building travel rules">
          <Select label="From building" onChange={(fromBuilding) => setTravelForm((old) => ({ ...old, fromBuilding }))} options={buildingOptions(buildings)} value={travelForm.fromBuilding} />
          <Select label="To building" onChange={(toBuilding) => setTravelForm((old) => ({ ...old, toBuilding }))} options={buildingOptions(buildings)} value={travelForm.toBuilding} />
          <Field label="Minimum minutes" onChange={(minutes) => setTravelForm((old) => ({ ...old, minutes }))} type="number" value={travelForm.minutes} />
          <Button
            disabled={!travelForm.fromBuilding || !travelForm.toBuilding || travelForm.fromBuilding === travelForm.toBuilding}
            loading={busy === "travel"}
            onClick={() => void mutate("travel", async () => {
              await createAuraTravelRule({ ...travelForm, active: true, difficulty: "NORMAL", minutes: Number(travelForm.minutes) });
              setTravelForm({ fromBuilding: "", minutes: "10", toBuilding: "" });
            })}
          >Save travel rule</Button>
          <ManagedSummary
            busy={busy}
            items={travelRules.map((item) => ({ active: item.active, id: item.id, label: `${item.fromBuilding} → ${item.toBuilding} · ${item.minutes} min` }))}
            onToggle={(id) => {
              const item = travelRules.find((entry) => entry.id === id);
              if (item) void mutate(`travel-${id}`, () => updateAuraTravelRule(id, { ...item, active: !item.active }));
            }}
          />
        </OperationCard>
      </div>

      {versionId ? (
        <div className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
          <OperationCard icon={Wrench} title="Localized repair">
            <p className="text-sm text-slate-500">
              Preview a bounded, minimum-disruption move before applying it to a draft.
            </p>
            <Field label="Repair reason" onChange={setRepairReason} value={repairReason} />
            <Button
              disabled={!repairReason.trim() || (!clashes[0] && !sessions[0])}
              loading={busy === "repair-preview"}
              onClick={() => void mutate("repair-preview", async () => {
                const result = await previewAuraRepair(versionId, {
                  clashId: clashes[0]?.id,
                  reason: repairReason,
                  sessionId: clashes[0] ? undefined : sessions[0]?.id,
                });
                setRepair(result);
              })}
            >Preview localized repair</Button>
            {repair ? (
              <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm">
                <p className="font-semibold text-slate-900">{repair.message}</p>
                <p className="mt-1 text-slate-500">
                  {repair.impact.sessionsMoved} session moved · disruption {repair.impact.disruptionScore}
                </p>
                {repair.feasible && repair.previewToken ? (
                  <Button
                    loading={busy === "repair-apply"}
                    onClick={() => void mutate("repair-apply", async () => {
                      const result = await applyAuraRepair(repair.id, repair.previewToken ?? "");
                      setRepair(result);
                    })}
                    variant="outline"
                  >Apply reviewed repair</Button>
                ) : null}
              </div>
            ) : null}
          </OperationCard>

          <OperationCard icon={Activity} title="Timetable views">
            <Select
              label="View by"
              onChange={(scope) => { setViewScope(scope); setViewResource(""); }}
              options={["WEEK", "DAY", "INSTRUCTOR", "SECTION", "ROOM", "COURSE", "OFFERING", "PROGRAM", "DEPARTMENT", "UNIVERSITY"].map((value) => ({ label: value.replaceAll("_", " "), value }))}
              value={viewScope}
            />
            {viewScope === "DAY" ? (
              <Select label="Day" onChange={setViewDay} options={["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"].map((label, index) => ({ label, value: String(index + 1) }))} value={viewDay} />
            ) : timetableOptions.length ? (
              <Select label="Timetable resource" onChange={setViewResource} options={timetableOptions} value={viewResource} />
            ) : null}
            <Button
              disabled={!versionId || (!["WEEK", "UNIVERSITY", "DAY"].includes(viewScope) && !viewResource)}
              loading={busy === "view"}
              onClick={() => void loadTimetableView()}
            >Load timetable view</Button>
            <p className="text-sm font-semibold text-slate-900">{view?.scopeLabel ?? "Weekly timetable"}</p>
            <Summary items={(view?.sessions ?? []).slice(0, 10).map((item) => `${item.courseCode} · ${item.sectionName} · ${item.roomName}`)} />
          </OperationCard>

          <OperationCard icon={Activity} title="Operational insight">
            <div className="grid grid-cols-2 gap-3 text-sm">
              <Metric label="Scheduled sessions" value={view?.sessions.length ?? 0} />
              <Metric label="Open clashes" value={analytics?.unresolvedClashes ?? 0} />
              <Metric label="Repair previews" value={analytics?.repairPlans ?? 0} />
              <Metric label="Room use" value={`${Math.round(analytics?.averageRoomCapacityUtilization ?? 0)}%`} />
            </div>
            <div className="border-t border-slate-100 pt-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Recent audit trail</p>
              <Summary items={audit.slice(0, 5).map((item) => `${item.actorName} · ${item.action.replaceAll("_", " ")}`)} />
            </div>
          </OperationCard>
        </div>
      ) : null}
    </section>
  );
}

function OperationCard({ children, icon: Icon, title }: { children: React.ReactNode; icon: typeof Activity; title: string }) {
  return (
    <Card>
      <CardHeader><CardTitle><span className="flex items-center gap-2"><Icon className="size-4 text-brand-700" />{title}</span></CardTitle></CardHeader>
      <CardContent className="grid gap-3">{children}</CardContent>
    </Card>
  );
}

function Field({ label, onChange, type = "text", value }: { label: string; onChange: (value: string) => void; type?: string; value: string }) {
  return <label className="grid gap-1.5 text-sm font-medium text-slate-700">{label}<input className={inputClass} onChange={(event) => onChange(event.target.value)} type={type} value={value} /></label>;
}

function Select({ label, onChange, options, value }: { label: string; onChange: (value: string) => void; options: Array<{ label: string; value: string }>; value: string }) {
  return <label className="grid gap-1.5 text-sm font-medium text-slate-700">{label}<select className={inputClass} onChange={(event) => onChange(event.target.value)} value={value}><option value="">Select {label.toLowerCase()}</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>;
}

function Summary({ items }: { items: string[] }) {
  if (!items.length) return <EmptyState description="Add the first record when this rule is needed." title="No records yet" />;
  return <ul className="grid gap-1 border-t border-slate-100 pt-3 text-sm text-slate-600">{items.slice(0, 5).map((item) => <li className="rounded-lg bg-slate-50 px-3 py-2" key={item}>{item}</li>)}</ul>;
}

function ManagedSummary({
  busy,
  items,
  onToggle,
}: {
  busy: string | null;
  items: Array<{ active: boolean; id: string; label: string }>;
  onToggle: (id: string) => void;
}) {
  if (!items.length) return <EmptyState description="Add the first record when this rule is needed." title="No records yet" />;
  return (
    <ul className="grid gap-2 border-t border-slate-100 pt-3 text-sm text-slate-600">
      {items.slice(0, 5).map((item) => (
        <li className="flex flex-col gap-2 rounded-lg bg-slate-50 px-3 py-2 sm:flex-row sm:items-center sm:justify-between" key={item.id}>
          <span>{item.label}</span>
          <Button loading={busy?.endsWith(item.id) ?? false} onClick={() => onToggle(item.id)} variant="outline">
            {item.active ? `Deactivate ${item.label}` : `Activate ${item.label}`}
          </Button>
        </li>
      ))}
    </ul>
  );
}

function Metric({ label, value }: { label: string; value: number | string }) {
  return <div className="rounded-xl bg-slate-50 p-3"><p className="text-xs text-slate-500">{label}</p><p className="mt-1 text-xl font-bold text-slate-950">{value}</p></div>;
}

function offeringOptions(offerings: AuraOffering[]) {
  return offerings.map((item) => ({ label: `${item.courseCode} · ${item.sectionName}`, value: item.id }));
}

function buildingOptions(buildings: AuraBuilding[]) {
  return buildings.filter((item) => item.active).map((item) => ({ label: item.name, value: item.name }));
}

function messageFor(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}
