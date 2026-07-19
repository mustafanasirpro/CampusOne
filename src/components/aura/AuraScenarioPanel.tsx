import { AlertTriangle, FlaskConical } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { ApiError } from "@/api/apiClient";
import {
  createAuraEmergencyRepair,
  listAuraEmergencyRepairs,
  listAuraWhatIf,
  runAuraWhatIf,
} from "@/api/auraApi";
import { Badge, Button, Card, CardContent, CardHeader, CardTitle, ErrorMessage } from "@/components/common";
import type { AuraEmergencyRepair, AuraSession, AuraTimetableVersion, AuraWhatIfResult } from "@/types/aura";

const scenarioOptions = [
  { emergency: "ROOM_CLOSURE", label: "Room unavailable", type: "ROOM_UNAVAILABLE", value: "room" },
  { emergency: "INSTRUCTOR_ABSENCE", label: "Instructor unavailable", type: "INSTRUCTOR_UNAVAILABLE", value: "instructor" },
  { emergency: "TIMESLOT_CANCELLATION", label: "Timeslot unavailable", type: "TIMESLOT_REMOVED", value: "timeslot" },
] as const;

export function AuraScenarioPanel({
  selectedVersionId,
  sessions,
  termId,
  versions,
  onChanged,
}: {
  onChanged: () => Promise<void>;
  selectedVersionId: string;
  sessions: AuraSession[];
  termId: string;
  versions: AuraTimetableVersion[];
}) {
  const [scenarioType, setScenarioType] = useState("ROOM_UNAVAILABLE");
  const [resourceId, setResourceId] = useState("");
  const [reason, setReason] = useState("");
  const [whatIfResults, setWhatIfResults] = useState<AuraWhatIfResult[]>([]);
  const [emergencies, setEmergencies] = useState<AuraEmergencyRepair[]>([]);
  const [pending, setPending] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void Promise.all([
      listAuraWhatIf(termId, controller.signal),
      listAuraEmergencyRepairs(termId, controller.signal),
    ]).then(([whatIf, emergency]) => {
      if (!active) return;
      setWhatIfResults(whatIf);
      setEmergencies(emergency);
    }).catch((requestError: unknown) => {
      if (active) setError(apiMessage(requestError));
    });
    return () => {
      active = false;
      controller.abort();
    };
  }, [termId]);

  const option = scenarioOptions.find((entry) => entry.type === scenarioType) ?? scenarioOptions[0];
  const resources = useMemo(() => {
    const byId = new Map<string, string>();
    sessions.forEach((session) => {
      if (option.value === "room") byId.set(session.roomId, session.roomName);
      if (option.value === "instructor") byId.set(session.instructorId, session.instructorName);
      if (option.value === "timeslot") byId.set(session.timeslotId, `Day ${session.dayOfWeek} · ${session.startsAt}–${session.endsAt}`);
    });
    return [...byId.entries()];
  }, [option.value, sessions]);
  const published = versions.find((version) => version.status === "PUBLISHED");

  const act = async (key: string, task: () => Promise<void>) => {
    setPending(key);
    setError(null);
    try {
      await task();
    } catch (requestError) {
      setError(apiMessage(requestError));
    } finally {
      setPending(null);
    }
  };

  return (
    <Card>
      <CardHeader><CardTitle>Simulation and emergency planning</CardTitle></CardHeader>
      <CardContent className="grid gap-4">
        {error ? <ErrorMessage message={error} /> : null}
        <div className="grid gap-3 lg:grid-cols-3">
          <label className="grid gap-1 text-sm font-medium text-slate-700">
            Scenario
            <select className="rounded-xl border border-slate-300 px-3 py-2" onChange={(event) => {
              setScenarioType(event.target.value);
              setResourceId("");
            }} value={scenarioType}>
              {scenarioOptions.map((entry) => <option key={entry.type} value={entry.type}>{entry.label}</option>)}
            </select>
          </label>
          <label className="grid gap-1 text-sm font-medium text-slate-700">
            Affected resource
            <select className="rounded-xl border border-slate-300 px-3 py-2" onChange={(event) => setResourceId(event.target.value)} value={resourceId}>
              <option value="">Choose from this timetable</option>
              {resources.map(([id, label]) => <option key={id} value={id}>{label}</option>)}
            </select>
          </label>
          <label className="grid gap-1 text-sm font-medium text-slate-700">
            Emergency reason
            <input className="rounded-xl border border-slate-300 px-3 py-2" maxLength={500} onChange={(event) => setReason(event.target.value)} placeholder="Required for an emergency draft" value={reason} />
          </label>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button disabled={!selectedVersionId || !resourceId} loading={pending === "what-if"} onClick={() => void act("what-if", async () => {
            const result = await runAuraWhatIf(termId, selectedVersionId, scenarioType, resourceId);
            setWhatIfResults((current) => [result, ...current]);
          })}>
            <FlaskConical className="size-4" /> Run what-if
          </Button>
          <Button disabled={!published || !resourceId || !reason.trim()} loading={pending === "emergency"} onClick={() => void act("emergency", async () => {
            const result = await createAuraEmergencyRepair(termId, published!.id, option.emergency, resourceId, reason.trim());
            setEmergencies((current) => [result, ...current]);
            setReason("");
            await onChanged();
          })} variant="outline">
            <AlertTriangle className="size-4" /> Create emergency draft
          </Button>
        </div>
        {whatIfResults.at(0) ? (
          <div className="rounded-xl bg-indigo-50 p-3 text-sm text-indigo-900">
            <p className="font-semibold">Latest what-if · {whatIfResults[0].scenarioType.replaceAll("_", " ")}</p>
            <p>{whatIfResults[0].affectedSessions} directly affected session{whatIfResults[0].affectedSessions === 1 ? "" : "s"}</p>
            <p>{whatIfResults[0].recommendation}</p>
          </div>
        ) : null}
        {emergencies.length ? (
          <div className="flex flex-wrap gap-2">
            {emergencies.slice(0, 5).map((emergency) => (
              <Badge key={emergency.id}>{emergency.emergencyType.replaceAll("_", " ")} · {emergency.status.replaceAll("_", " ")}</Badge>
            ))}
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}

function apiMessage(error: unknown) {
  return error instanceof ApiError
    ? error.message
    : "The scheduling scenario could not be completed. Please try again.";
}
