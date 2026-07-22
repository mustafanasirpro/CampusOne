import { CheckCircle2, Database, RefreshCw } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { ApiError } from "@/api/apiClient";
import {
  createAuraBatch,
  createAuraCalendarException,
  createAuraInstructor,
  createAuraMeetingRequirement,
  createAuraOffering,
  createAuraProgram,
  createAuraRoom,
  createAuraSection,
  createAuraTimeslot,
  deactivateAuraCalendarException,
  getAuraSetupReferences,
  listAuraBatches,
  listAuraCalendarExceptions,
  listAuraInstructors,
  listAuraOfferings,
  listAuraPrograms,
  listAuraRooms,
  listAuraSections,
  listAuraTimeslots,
  setAuraResourceActive,
  upsertAuraInstructorAvailability,
  upsertAuraRoomAvailability,
  upsertAuraSectionAvailability,
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
  AuraAvailabilityType,
  AuraBatch,
  AuraCalendarException,
  AuraInstructor,
  AuraOffering,
  AuraProgram,
  AuraRoom,
  AuraSection,
  AuraSetupReferences,
  AuraTimeslot,
} from "@/types/aura";

interface AuraSetupPanelProps {
  onChanged: () => Promise<void>;
  termId: string;
  universityId: string;
}

const inputClass =
  "w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-100";

const emptySetup = {
  batches: [] as AuraBatch[],
  instructors: [] as AuraInstructor[],
  offerings: [] as AuraOffering[],
  programs: [] as AuraProgram[],
  rooms: [] as AuraRoom[],
  sections: [] as AuraSection[],
  timeslots: [] as AuraTimeslot[],
};

export function AuraSetupPanel({
  onChanged,
  termId,
  universityId,
}: AuraSetupPanelProps) {
  const [references, setReferences] = useState<AuraSetupReferences | null>(null);
  const [setup, setSetup] = useState(emptySetup);
  const [calendarExceptions, setCalendarExceptions] = useState<
    AuraCalendarException[]
  >([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [busyAction, setBusyAction] = useState<string | null>(null);

  const [programForm, setProgramForm] = useState({
    code: "",
    departmentId: "",
    name: "",
  });
  const [batchForm, setBatchForm] = useState({
    admissionYear: String(new Date().getFullYear()),
    code: "",
    programId: "",
  });
  const [sectionForm, setSectionForm] = useState({
    batchId: "",
    code: "",
    displayName: "",
    studentCount: "40",
  });
  const [instructorForm, setInstructorForm] = useState({
    displayName: "",
    email: "",
    maxHoursPerWeek: "18",
  });
  const [roomForm, setRoomForm] = useState({
    building: "",
    capacity: "40",
    facilities: [] as string[],
    name: "",
    roomType: "CLASSROOM",
  });
  const [timeslotForm, setTimeslotForm] = useState({
    dayOfWeek: "1",
    endsAt: "10:00",
    label: "",
    startsAt: "09:00",
  });
  const [offeringForm, setOfferingForm] = useState({
    courseId: "",
    expectedStudents: "40",
    instructorId: "",
    sectionId: "",
  });
  const [requirementForm, setRequirementForm] = useState({
    durationSlots: "1",
    meetingType: "LECTURE",
    notes: "",
    offeringId: "",
    requiredCapacity: "40",
    requiredFacilities: [] as string[],
    roomType: "CLASSROOM",
    sessionsPerWeek: "1",
  });
  const [availabilityForm, setAvailabilityForm] = useState({
    availability: "UNAVAILABLE" as AuraAvailabilityType,
    reason: "",
    targetId: "",
    targetType: "INSTRUCTOR" as "INSTRUCTOR" | "ROOM" | "SECTION",
    timeslotId: "",
  });
  const [calendarForm, setCalendarForm] = useState({
    endsOn: "",
    exceptionType: "HOLIDAY",
    facility: "",
    reason: "",
    targetId: "",
    timeslotId: "",
    startsOn: "",
  });

  const loadSetup = useCallback(
    async (signal?: AbortSignal) => {
      if (!universityId || !termId) return;
      const [
        referenceResponse,
        programs,
        batches,
        sections,
        instructors,
        rooms,
        timeslots,
        offerings,
        exceptions,
      ] = await Promise.all([
        getAuraSetupReferences(signal),
        listAuraPrograms(universityId, signal),
        listAuraBatches(undefined, signal),
        listAuraSections(undefined, signal),
        listAuraInstructors(universityId, signal),
        listAuraRooms(universityId, signal),
        listAuraTimeslots(universityId, signal),
        listAuraOfferings(termId, signal),
        listAuraCalendarExceptions(termId, signal),
      ]);
      setReferences(referenceResponse);
      setSetup({
        batches,
        instructors,
        offerings,
        programs,
        rooms,
        sections,
        timeslots,
      });
      setCalendarExceptions(exceptions);
    },
    [termId, universityId],
  );

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    const setupRequest = Promise.resolve().then(() =>
      loadSetup(controller.signal),
    );
    void setupRequest
      .then(() => {
        if (active) setError(null);
      })
      .catch((requestError: unknown) => {
        if (active && !controller.signal.aborted) {
          setError(messageFor(requestError, "Setup data could not be loaded."));
        }
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [loadSetup]);

  const targetOptions = useMemo(() => {
    if (availabilityForm.targetType === "ROOM") {
      return setup.rooms.map((room) => ({ id: room.id, label: room.name }));
    }
    if (availabilityForm.targetType === "SECTION") {
      return setup.sections.map((section) => ({
        id: section.id,
        label: section.displayName,
      }));
    }
    return setup.instructors.map((instructor) => ({
      id: instructor.id,
      label: instructor.displayName,
    }));
  }, [availabilityForm.targetType, setup]);

  const calendarTargetOptions = useMemo(() => {
    if (["ROOM_CLOSURE", "FACILITY_OUTAGE"].includes(calendarForm.exceptionType)) {
      return setup.rooms.map((room) => ({ id: room.id, label: room.name }));
    }
    if (calendarForm.exceptionType === "SECTION_RESTRICTION") {
      return setup.sections.map((section) => ({ id: section.id, label: section.displayName }));
    }
    if (calendarForm.exceptionType === "INSTRUCTOR_ABSENCE") {
      return setup.instructors.map((instructor) => ({ id: instructor.id, label: instructor.displayName }));
    }
    return [];
  }, [calendarForm.exceptionType, setup]);

  const runAction = async (
    actionName: string,
    action: () => Promise<unknown>,
    reset: () => void,
  ) => {
    setBusyAction(actionName);
    setError(null);
    try {
      await action();
      reset();
      await Promise.all([loadSetup(), onChanged()]);
    } catch (requestError) {
      setError(messageFor(requestError, "This setup record could not be saved."));
    } finally {
      setBusyAction(null);
    }
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="grid min-h-48 place-items-center">
          <LoadingSpinner label="Loading timetable setup" />
        </CardContent>
      </Card>
    );
  }

  return (
    <section className="grid gap-4" aria-labelledby="aura-setup-title">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-700">
            Scheduling inputs
          </p>
          <h2 className="mt-1 text-xl font-bold text-slate-950" id="aura-setup-title">
            Complete term setup
          </h2>
          <p className="mt-1 max-w-3xl text-sm text-slate-500">
            Add academic structure, teaching resources, meeting needs, and availability without leaving the workbench.
          </p>
        </div>
        <Button
          onClick={() => void loadSetup().catch((requestError: unknown) => {
            setError(messageFor(requestError, "Setup data could not be refreshed."));
          })}
          variant="outline"
        >
          <RefreshCw className="size-4" />
          Refresh setup
        </Button>
      </div>

      {error ? <ErrorMessage message={error} /> : null}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SetupCount label="Programs" value={setup.programs.length} />
        <SetupCount label="Sections" value={setup.sections.length} />
        <SetupCount label="Instructors" value={setup.instructors.length} />
        <SetupCount label="Rooms / timeslots" value={setup.rooms.length + setup.timeslots.length} />
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <SetupCard title="1. Academic program">
          <SelectInput
            label="Department"
            onChange={(departmentId) => setProgramForm((current) => ({ ...current, departmentId }))}
            options={(references?.departments ?? []).map((department) => ({
              label: `${department.code} · ${department.name}`,
              value: department.id,
            }))}
            value={programForm.departmentId}
          />
          <TextInput label="Program code" onChange={(code) => setProgramForm((current) => ({ ...current, code }))} placeholder="BSCS" value={programForm.code} />
          <TextInput label="Program name" onChange={(name) => setProgramForm((current) => ({ ...current, name }))} placeholder="Bachelor of Science in Computer Science" value={programForm.name} />
          <Button
            disabled={!programForm.departmentId || !programForm.code.trim() || !programForm.name.trim()}
            loading={busyAction === "program"}
            onClick={() => void runAction("program", () => createAuraProgram({ ...programForm, universityId }), () => setProgramForm({ code: "", departmentId: "", name: "" }))}
          >Save program</Button>
        </SetupCard>

        <SetupCard title="2. Batch / cohort">
          <SelectInput label="Program" onChange={(programId) => setBatchForm((current) => ({ ...current, programId }))} options={setup.programs.map((program) => ({ label: `${program.code} · ${program.name}`, value: program.id }))} value={batchForm.programId} />
          <TextInput label="Batch code" onChange={(code) => setBatchForm((current) => ({ ...current, code }))} placeholder="FA24" value={batchForm.code} />
          <TextInput label="Admission year" onChange={(admissionYear) => setBatchForm((current) => ({ ...current, admissionYear }))} type="number" value={batchForm.admissionYear} />
          <Button disabled={!batchForm.programId || !batchForm.code.trim()} loading={busyAction === "batch"} onClick={() => void runAction("batch", () => createAuraBatch({ admissionYear: Number(batchForm.admissionYear), code: batchForm.code, programId: batchForm.programId }), () => setBatchForm((current) => ({ ...current, code: "" })))}>Save batch</Button>
        </SetupCard>

        <SetupCard title="3. Section">
          <SelectInput label="Batch" onChange={(batchId) => setSectionForm((current) => ({ ...current, batchId }))} options={setup.batches.map((batch) => ({ label: `${batch.code} · ${batch.admissionYear}`, value: batch.id }))} value={sectionForm.batchId} />
          <TextInput label="Section code" onChange={(code) => setSectionForm((current) => ({ ...current, code }))} placeholder="4A" value={sectionForm.code} />
          <TextInput label="Display name" onChange={(displayName) => setSectionForm((current) => ({ ...current, displayName }))} placeholder="BSCS-4A" value={sectionForm.displayName} />
          <TextInput label="Expected students" onChange={(studentCount) => setSectionForm((current) => ({ ...current, studentCount }))} type="number" value={sectionForm.studentCount} />
          <Button disabled={!sectionForm.batchId || !sectionForm.code.trim() || !sectionForm.displayName.trim()} loading={busyAction === "section"} onClick={() => void runAction("section", () => createAuraSection({ batchId: sectionForm.batchId, code: sectionForm.code, displayName: sectionForm.displayName, studentCount: Number(sectionForm.studentCount) }), () => setSectionForm((current) => ({ ...current, code: "", displayName: "" })))}>Save section</Button>
        </SetupCard>

        <SetupCard title="4. Instructor">
          <TextInput label="Full name" onChange={(displayName) => setInstructorForm((current) => ({ ...current, displayName }))} placeholder="Dr Ahmed Khan" value={instructorForm.displayName} />
          <TextInput label="Email (optional)" onChange={(email) => setInstructorForm((current) => ({ ...current, email }))} placeholder="ahmed@university.edu" type="email" value={instructorForm.email} />
          <TextInput label="Maximum weekly hours" onChange={(maxHoursPerWeek) => setInstructorForm((current) => ({ ...current, maxHoursPerWeek }))} type="number" value={instructorForm.maxHoursPerWeek} />
          <Button disabled={!instructorForm.displayName.trim()} loading={busyAction === "instructor"} onClick={() => void runAction("instructor", () => createAuraInstructor({ displayName: instructorForm.displayName, email: instructorForm.email || undefined, maxHoursPerWeek: Number(instructorForm.maxHoursPerWeek), universityId }), () => setInstructorForm((current) => ({ ...current, displayName: "", email: "" })))}>Save instructor</Button>
        </SetupCard>

        <SetupCard title="5. Room">
          <TextInput label="Room name" onChange={(name) => setRoomForm((current) => ({ ...current, name }))} placeholder="CS Lab 1" value={roomForm.name} />
          <TextInput label="Building (optional)" onChange={(building) => setRoomForm((current) => ({ ...current, building }))} placeholder="Academic Block" value={roomForm.building} />
          <TextInput label="Capacity" onChange={(capacity) => setRoomForm((current) => ({ ...current, capacity }))} type="number" value={roomForm.capacity} />
          <SelectInput label="Room type" onChange={(roomType) => setRoomForm((current) => ({ ...current, roomType }))} options={roomTypes.map((value) => ({ label: value.replaceAll("_", " "), value }))} value={roomForm.roomType} />
          <FacilityCheckboxes label="Available facilities" onChange={(facilities) => setRoomForm((current) => ({ ...current, facilities }))} value={roomForm.facilities} />
          <Button disabled={!roomForm.name.trim()} loading={busyAction === "room"} onClick={() => void runAction("room", () => createAuraRoom({ building: roomForm.building || undefined, capacity: Number(roomForm.capacity), facilities: roomForm.facilities, name: roomForm.name, roomType: roomForm.roomType, universityId }), () => setRoomForm((current) => ({ ...current, building: "", facilities: [], name: "" })))}>Save room</Button>
        </SetupCard>

        <SetupCard title="6. Weekly timeslot">
          <SelectInput label="Day" onChange={(dayOfWeek) => setTimeslotForm((current) => ({ ...current, dayOfWeek }))} options={dayOptions} value={timeslotForm.dayOfWeek} />
          <div className="grid gap-3 sm:grid-cols-2">
            <TextInput label="Starts" onChange={(startsAt) => setTimeslotForm((current) => ({ ...current, startsAt }))} type="time" value={timeslotForm.startsAt} />
            <TextInput label="Ends" onChange={(endsAt) => setTimeslotForm((current) => ({ ...current, endsAt }))} type="time" value={timeslotForm.endsAt} />
          </div>
          <TextInput label="Label" onChange={(label) => setTimeslotForm((current) => ({ ...current, label }))} placeholder="Monday 09:00–10:00" value={timeslotForm.label} />
          <Button disabled={!timeslotForm.label.trim() || timeslotForm.startsAt >= timeslotForm.endsAt} loading={busyAction === "timeslot"} onClick={() => void runAction("timeslot", () => createAuraTimeslot({ dayOfWeek: Number(timeslotForm.dayOfWeek), endsAt: timeslotForm.endsAt, label: timeslotForm.label, startsAt: timeslotForm.startsAt, universityId }), () => setTimeslotForm((current) => ({ ...current, label: "" })))}>Save timeslot</Button>
        </SetupCard>

        <SetupCard title="7. Course offering">
          <SelectInput label="Course" onChange={(courseId) => setOfferingForm((current) => ({ ...current, courseId }))} options={(references?.courses ?? []).map((course) => ({ label: `${course.code} · ${course.name}`, value: course.id }))} value={offeringForm.courseId} />
          <SelectInput label="Section" onChange={(sectionId) => setOfferingForm((current) => ({ ...current, sectionId }))} options={setup.sections.map((section) => ({ label: section.displayName, value: section.id }))} value={offeringForm.sectionId} />
          <SelectInput label="Instructor" onChange={(instructorId) => setOfferingForm((current) => ({ ...current, instructorId }))} options={setup.instructors.map((instructor) => ({ label: instructor.displayName, value: instructor.id }))} value={offeringForm.instructorId} />
          <TextInput label="Expected enrollment" onChange={(expectedStudents) => setOfferingForm((current) => ({ ...current, expectedStudents }))} type="number" value={offeringForm.expectedStudents} />
          <Button disabled={!offeringForm.courseId || !offeringForm.sectionId || !offeringForm.instructorId} loading={busyAction === "offering"} onClick={() => void runAction("offering", () => createAuraOffering({ courseId: offeringForm.courseId, expectedStudents: Number(offeringForm.expectedStudents), instructorId: offeringForm.instructorId, sectionId: offeringForm.sectionId, termId }), () => setOfferingForm((current) => ({ ...current, courseId: "" })))}>Save offering</Button>
        </SetupCard>

        <SetupCard title="8. Meeting requirement">
          <SelectInput label="Offering" onChange={(offeringId) => setRequirementForm((current) => ({ ...current, offeringId }))} options={setup.offerings.map((offering) => ({ label: `${offering.courseCode} · ${offering.sectionName}`, value: offering.id }))} value={requirementForm.offeringId} />
          <div className="grid gap-3 sm:grid-cols-2">
            <SelectInput label="Meeting type" onChange={(meetingType) => setRequirementForm((current) => ({ ...current, meetingType }))} options={meetingTypes.map((value) => ({ label: value, value }))} value={requirementForm.meetingType} />
            <SelectInput label="Room type" onChange={(roomType) => setRequirementForm((current) => ({ ...current, roomType }))} options={roomTypes.map((value) => ({ label: value.replaceAll("_", " "), value }))} value={requirementForm.roomType} />
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            <TextInput label="Per week" onChange={(sessionsPerWeek) => setRequirementForm((current) => ({ ...current, sessionsPerWeek }))} type="number" value={requirementForm.sessionsPerWeek} />
            <TextInput label="Slot duration" onChange={(durationSlots) => setRequirementForm((current) => ({ ...current, durationSlots }))} type="number" value={requirementForm.durationSlots} />
            <TextInput label="Capacity" onChange={(requiredCapacity) => setRequirementForm((current) => ({ ...current, requiredCapacity }))} type="number" value={requirementForm.requiredCapacity} />
          </div>
          <TextInput label="Notes (optional)" onChange={(notes) => setRequirementForm((current) => ({ ...current, notes }))} placeholder="Special scheduling guidance" value={requirementForm.notes} />
          <FacilityCheckboxes label="Required facilities" onChange={(requiredFacilities) => setRequirementForm((current) => ({ ...current, requiredFacilities }))} value={requirementForm.requiredFacilities} />
          <Button disabled={!requirementForm.offeringId} loading={busyAction === "requirement"} onClick={() => void runAction("requirement", () => createAuraMeetingRequirement({ durationSlots: Number(requirementForm.durationSlots), meetingType: requirementForm.meetingType, notes: requirementForm.notes || undefined, offeringId: requirementForm.offeringId, requiredCapacity: Number(requirementForm.requiredCapacity), requiredFacilities: requirementForm.requiredFacilities, roomType: requirementForm.roomType, sessionsPerWeek: Number(requirementForm.sessionsPerWeek) }), () => setRequirementForm((current) => ({ ...current, notes: "", requiredFacilities: [] })))}>Save requirement</Button>
        </SetupCard>

        <SetupCard title="9. Availability preference">
          <SelectInput label="Resource type" onChange={(targetType) => setAvailabilityForm((current) => ({ ...current, targetId: "", targetType: targetType as typeof current.targetType }))} options={[{ label: "Instructor", value: "INSTRUCTOR" }, { label: "Room", value: "ROOM" }, { label: "Section", value: "SECTION" }]} value={availabilityForm.targetType} />
          <SelectInput label="Resource" onChange={(targetId) => setAvailabilityForm((current) => ({ ...current, targetId }))} options={targetOptions.map((target) => ({ label: target.label, value: target.id }))} value={availabilityForm.targetId} />
          <SelectInput label="Timeslot" onChange={(timeslotId) => setAvailabilityForm((current) => ({ ...current, timeslotId }))} options={setup.timeslots.map((timeslot) => ({ label: `${dayName(timeslot.dayOfWeek)} · ${timeslot.label}`, value: timeslot.id }))} value={availabilityForm.timeslotId} />
          <SelectInput label="Preference" onChange={(availability) => setAvailabilityForm((current) => ({ ...current, availability: availability as AuraAvailabilityType }))} options={availabilityTypes.map((value) => ({ label: value, value }))} value={availabilityForm.availability} />
          <TextInput label="Reason (optional)" onChange={(reason) => setAvailabilityForm((current) => ({ ...current, reason }))} placeholder="Department meeting" value={availabilityForm.reason} />
          <Button disabled={!availabilityForm.targetId || !availabilityForm.timeslotId} loading={busyAction === "availability"} onClick={() => void runAction("availability", () => saveAvailability(availabilityForm), () => setAvailabilityForm((current) => ({ ...current, reason: "" })))}>Save availability</Button>
        </SetupCard>

        <SetupCard title="10. Calendar exception">
          <SelectInput
            label="Exception type"
            onChange={(exceptionType) => setCalendarForm((current) => ({
              ...current,
              exceptionType,
              facility: "",
              targetId: "",
              timeslotId: "",
            }))}
            options={calendarExceptionTypes.map((value) => ({
              label: value.replaceAll("_", " "),
              value,
            }))}
            value={calendarForm.exceptionType}
          />
          <div className="grid gap-3 sm:grid-cols-2">
            <TextInput label="Starts on" onChange={(startsOn) => setCalendarForm((current) => ({ ...current, startsOn }))} type="date" value={calendarForm.startsOn} />
            <TextInput label="Ends on" onChange={(endsOn) => setCalendarForm((current) => ({ ...current, endsOn }))} type="date" value={calendarForm.endsOn} />
          </div>
          {calendarTargetOptions.length ? (
            <SelectInput label="Affected resource" onChange={(targetId) => setCalendarForm((current) => ({ ...current, targetId }))} options={calendarTargetOptions.map((target) => ({ label: target.label, value: target.id }))} value={calendarForm.targetId} />
          ) : null}
          {calendarForm.exceptionType === "TIMESLOT_CANCELLATION" ? (
            <SelectInput label="Cancelled timeslot" onChange={(timeslotId) => setCalendarForm((current) => ({ ...current, timeslotId }))} options={setup.timeslots.map((timeslot) => ({ label: `${dayName(timeslot.dayOfWeek)} · ${timeslot.label}`, value: timeslot.id }))} value={calendarForm.timeslotId} />
          ) : null}
          {calendarForm.exceptionType === "FACILITY_OUTAGE" ? (
            <SelectInput label="Unavailable facility" onChange={(facility) => setCalendarForm((current) => ({ ...current, facility }))} options={facilityOptions.map((value) => ({ label: value.replaceAll("_", " "), value }))} value={calendarForm.facility} />
          ) : null}
          <TextInput label="Reason" onChange={(reason) => setCalendarForm((current) => ({ ...current, reason }))} placeholder="University holiday" value={calendarForm.reason} />
          <Button
            disabled={!calendarForm.startsOn || !calendarForm.endsOn || !calendarForm.reason.trim() || requiresCalendarTarget(calendarForm.exceptionType) && !calendarForm.targetId || calendarForm.exceptionType === "TIMESLOT_CANCELLATION" && !calendarForm.timeslotId || calendarForm.exceptionType === "FACILITY_OUTAGE" && !calendarForm.facility}
            loading={busyAction === "calendar"}
            onClick={() => void runAction("calendar", () => createAuraCalendarException(toCalendarRequest(termId, calendarForm)), () => setCalendarForm((current) => ({ ...current, facility: "", reason: "", targetId: "", timeslotId: "" })))}
          >Save exception</Button>
          {calendarExceptions.length ? (
            <div className="grid gap-2 border-t border-slate-100 pt-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Saved exceptions</p>
              {calendarExceptions.filter((entry) => entry.active).slice(0, 5).map((entry) => (
                <div className="flex flex-col gap-2 rounded-xl bg-slate-50 p-3 text-sm sm:flex-row sm:items-center sm:justify-between" key={entry.id}>
                  <div>
                    <p className="font-semibold text-slate-900">{entry.exceptionType.replaceAll("_", " ")}</p>
                    <p className="text-slate-500">{entry.startsOn} – {entry.endsOn} · {entry.reason}</p>
                  </div>
                  <Button
                    loading={busyAction === `calendar-${entry.id}`}
                    onClick={() => void runAction(`calendar-${entry.id}`, () => deactivateAuraCalendarException(entry.id, entry.version), () => undefined)}
                    variant="outline"
                  >Deactivate</Button>
                </div>
              ))}
            </div>
          ) : null}
        </SetupCard>
      </div>

      <Card>
        <CardHeader><CardTitle>Manage saved setup</CardTitle></CardHeader>
        <CardContent className="grid gap-2">
          {[
            ...setup.programs.map((item) => ({ active: item.active, id: item.id, label: `Program · ${item.code}`, type: "program", version: item.version })),
            ...setup.batches.map((item) => ({ active: item.active, id: item.id, label: `Batch · ${item.code}`, type: "batch", version: item.version })),
            ...setup.sections.map((item) => ({ active: item.active, id: item.id, label: `Section · ${item.displayName}`, type: "section", version: item.version })),
            ...setup.instructors.map((item) => ({ active: item.active, id: item.id, label: `Instructor · ${item.displayName}`, type: "instructor", version: item.version })),
            ...setup.rooms.map((item) => ({ active: item.active, id: item.id, label: `Room · ${item.name}`, type: "room", version: item.version })),
            ...setup.timeslots.map((item) => ({ active: item.active, id: item.id, label: `Timeslot · ${item.label}`, type: "timeslot", version: item.version })),
            ...setup.offerings.map((item) => ({ active: item.status === "ACTIVE", id: item.id, label: `Offering · ${item.courseCode} ${item.sectionName}`, type: "offering", version: item.version })),
          ].map((item) => (
            <div className="flex flex-col gap-2 rounded-xl border border-slate-200 p-3 sm:flex-row sm:items-center sm:justify-between" key={`${item.type}-${item.id}`}>
              <div>
                <p className="text-sm font-semibold text-slate-900">{item.label}</p>
                <p className="text-xs text-slate-500">{item.active ? "Active" : "Inactive"}</p>
              </div>
              <Button
                loading={busyAction === `state-${item.id}`}
                onClick={() => void runAction(
                  `state-${item.id}`,
                  () => setAuraResourceActive(
                    item.type,
                    item.id,
                    !item.active,
                    item.version,
                    item.active ? "Deactivated from setup management" : "Reactivated from setup management",
                  ),
                  () => undefined,
                )}
                variant="outline"
              >{item.active ? `Deactivate ${item.label}` : `Activate ${item.label}`}</Button>
            </div>
          ))}
        </CardContent>
      </Card>

      {!setup.programs.length && !setup.rooms.length && !setup.instructors.length ? (
        <EmptyState description="Start with a program, then add its batch and section alongside instructors, rooms, and weekly timeslots." icon={<Database className="size-6" />} title="Build the scheduling foundation" />
      ) : (
        <div className="flex items-center gap-2 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
          <CheckCircle2 className="size-4 shrink-0" />
          Saved setup records are immediately included in readiness validation.
        </div>
      )}
    </section>
  );
}

function saveAvailability(form: {
  availability: AuraAvailabilityType;
  reason: string;
  targetId: string;
  targetType: "INSTRUCTOR" | "ROOM" | "SECTION";
  timeslotId: string;
}) {
  const common = {
    availability: form.availability,
    reason: form.reason || undefined,
    timeslotId: form.timeslotId,
  };
  if (form.targetType === "ROOM") {
    return upsertAuraRoomAvailability({ ...common, roomId: form.targetId });
  }
  if (form.targetType === "SECTION") {
    return upsertAuraSectionAvailability({ ...common, sectionId: form.targetId });
  }
  return upsertAuraInstructorAvailability({ ...common, instructorId: form.targetId });
}

function SetupCard({ children, title }: { children: React.ReactNode; title: string }) {
  return (
    <Card>
      <CardHeader><CardTitle>{title}</CardTitle></CardHeader>
      <CardContent className="grid gap-3">{children}</CardContent>
    </Card>
  );
}

function SetupCount({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4">
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-bold text-slate-950">{value}</p>
    </div>
  );
}

function TextInput({ label, onChange, placeholder, type = "text", value }: { label: string; onChange: (value: string) => void; placeholder?: string; type?: string; value: string }) {
  return (
    <label className="grid gap-1.5 text-sm font-medium text-slate-700">
      {label}
      <input className={inputClass} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} type={type} value={value} />
    </label>
  );
}

function SelectInput({ label, onChange, options, value }: { label: string; onChange: (value: string) => void; options: Array<{ label: string; value: string }>; value: string }) {
  return (
    <label className="grid gap-1.5 text-sm font-medium text-slate-700">
      {label}
      <select className={inputClass} onChange={(event) => onChange(event.target.value)} value={value}>
        <option value="">Select {label.toLowerCase()}</option>
        {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
      </select>
    </label>
  );
}

function FacilityCheckboxes({ label, onChange, value }: { label: string; onChange: (value: string[]) => void; value: string[] }) {
  return (
    <fieldset className="grid gap-2">
      <legend className="text-sm font-medium text-slate-700">{label}</legend>
      <div className="grid gap-2 sm:grid-cols-2">
        {facilityOptions.map((facility) => (
          <label className="flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-xs font-medium text-slate-600" key={facility}>
            <input
              checked={value.includes(facility)}
              onChange={(event) => onChange(event.target.checked ? [...value, facility] : value.filter((entry) => entry !== facility))}
              type="checkbox"
            />
            {facility.replaceAll("_", " ")}
          </label>
        ))}
      </div>
    </fieldset>
  );
}

function requiresCalendarTarget(exceptionType: string) {
  return [
    "INSTRUCTOR_ABSENCE",
    "ROOM_CLOSURE",
    "SECTION_RESTRICTION",
    "FACILITY_OUTAGE",
  ].includes(exceptionType);
}

function toCalendarRequest(
  termId: string,
  form: {
    endsOn: string;
    exceptionType: string;
    facility: string;
    reason: string;
    startsOn: string;
    targetId: string;
    timeslotId: string;
  },
) {
  return {
    endsOn: form.endsOn,
    exceptionType: form.exceptionType,
    facility: form.facility || undefined,
    instructorId:
      form.exceptionType === "INSTRUCTOR_ABSENCE" ? form.targetId : undefined,
    reason: form.reason,
    roomId: ["ROOM_CLOSURE", "FACILITY_OUTAGE"].includes(form.exceptionType)
      ? form.targetId
      : undefined,
    sectionId:
      form.exceptionType === "SECTION_RESTRICTION" ? form.targetId : undefined,
    startsOn: form.startsOn,
    termId,
    timeslotId:
      form.exceptionType === "TIMESLOT_CANCELLATION"
        ? form.timeslotId
        : undefined,
  };
}

function messageFor(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}

function dayName(day: number) {
  return ["", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"][day] ?? `Day ${day}`;
}

const dayOptions = [
  { label: "Monday", value: "1" },
  { label: "Tuesday", value: "2" },
  { label: "Wednesday", value: "3" },
  { label: "Thursday", value: "4" },
  { label: "Friday", value: "5" },
  { label: "Saturday", value: "6" },
  { label: "Sunday", value: "7" },
];
const roomTypes = ["CLASSROOM", "LAB", "LECTURE_HALL", "SEMINAR_ROOM"];
const meetingTypes = ["LECTURE", "LAB", "TUTORIAL", "SEMINAR"];
const availabilityTypes: AuraAvailabilityType[] = ["AVAILABLE", "UNAVAILABLE", "AVOID", "PREFERRED"];
const facilityOptions = [
  "PROJECTOR",
  "SMART_BOARD",
  "COMPUTERS",
  "INTERNET",
  "LAB_EQUIPMENT",
  "ACCESSIBLE",
  "AIR_CONDITIONING",
  "VIDEO_CONFERENCING",
  "SPECIALIZED_SOFTWARE",
  "OTHER",
];
const calendarExceptionTypes = [
  "HOLIDAY",
  "NON_TEACHING_DAY",
  "UNIVERSITY_EVENT",
  "INSTRUCTOR_ABSENCE",
  "ROOM_CLOSURE",
  "SECTION_RESTRICTION",
  "TIMESLOT_CANCELLATION",
  "FACILITY_OUTAGE",
];
