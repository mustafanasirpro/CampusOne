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
  deleteAuraStudentAvailability,
  getAuraSetupReferences,
  listAuraBatches,
  listAuraBuildings,
  listAuraCalendarExceptions,
  listAuraInstructors,
  listAuraMeetingRequirements,
  listAuraOfferings,
  listAuraPrograms,
  listAuraRooms,
  listAuraSections,
  listAuraStudentAvailability,
  listAuraTeachingGroups,
  listAuraTimeslots,
  setAuraResourceActive,
  upsertAuraInstructorAvailability,
  upsertAuraRoomAvailability,
  upsertAuraSectionAvailability,
  upsertAuraStudentAvailability,
  updateAuraProgram,
  updateAuraBatch,
  updateAuraSection,
  updateAuraInstructor,
  updateAuraRoom,
  updateAuraTimeslot,
  updateAuraOffering,
  updateAuraMeetingRequirement,
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
  AuraBuilding,
  AuraCalendarException,
  AuraInstructor,
  AuraMeetingRequirement,
  AuraOffering,
  AuraProgram,
  AuraRoom,
  AuraSection,
  AuraSetupReferences,
  AuraStudentAvailability,
  AuraTeachingGroup,
  AuraTimeslot,
} from "@/types/aura";

interface AuraSetupPanelProps {
  onChanged: () => Promise<void>;
  termId: string;
  universityId: string;
}

const inputClass =
  "w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-100";
const managedPageSize = 12;

const emptySetup = {
  batches: [] as AuraBatch[],
  instructors: [] as AuraInstructor[],
  offerings: [] as AuraOffering[],
  programs: [] as AuraProgram[],
  requirements: [] as AuraMeetingRequirement[],
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
  const [studentAvailability, setStudentAvailability] = useState<
    AuraStudentAvailability[]
  >([]);
  const [buildings, setBuildings] = useState<AuraBuilding[]>([]);
  const [teachingGroups, setTeachingGroups] = useState<AuraTeachingGroup[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [editingProgramId, setEditingProgramId] = useState<string | null>(null);
  const [editingBatchId, setEditingBatchId] = useState<string | null>(null);
  const [editingSectionId, setEditingSectionId] = useState<string | null>(null);
  const [editingInstructorId, setEditingInstructorId] = useState<string | null>(null);
  const [editingRoomId, setEditingRoomId] = useState<string | null>(null);
  const [editingTimeslotId, setEditingTimeslotId] = useState<string | null>(null);
  const [editingOfferingId, setEditingOfferingId] = useState<string | null>(null);
  const [editingRequirementId, setEditingRequirementId] = useState<string | null>(null);
  const [managedQuery, setManagedQuery] = useState("");
  const [managedPage, setManagedPage] = useState(0);

  const [programForm, setProgramForm] = useState({
    code: "",
    departmentId: "",
    name: "",
    numberOfSemesters: "8",
  });
  const [batchForm, setBatchForm] = useState({
    admissionYear: String(new Date().getFullYear()),
    code: "",
    expectedGraduationYear: "",
    programId: "",
  });
  const [sectionForm, setSectionForm] = useState({
    batchId: "",
    code: "",
    displayName: "",
    hardDailyLoad: "8",
    homeBuildingId: "",
    preferredDailyLoad: "6",
    semesterNumber: "",
    studentCount: "40",
    termId,
  });
  const [instructorForm, setInstructorForm] = useState({
    departmentId: "",
    displayName: "",
    email: "",
    employeeCode: "",
    hardDailyLoad: "8",
    homeBuildingId: "",
    maxHoursPerWeek: "18",
    maximumConsecutiveSlots: "4",
    preferredDailyLoad: "6",
    preferredWeeklyLoad: "16",
  });
  const [roomForm, setRoomForm] = useState({
    building: "",
    buildingId: "",
    capacity: "40",
    displayName: "",
    facilities: [] as string[],
    name: "",
    roomCode: "",
    roomType: "CLASSROOM",
  });
  const [timeslotForm, setTimeslotForm] = useState({
    dayOfWeek: "1",
    endsAt: "10:00",
    label: "",
    slotOrder: "",
    slotType: "INSTRUCTIONAL",
    startsAt: "09:00",
    termId,
  });
  const [offeringForm, setOfferingForm] = useState({
    combinedSections: false,
    courseId: "",
    electiveGroup: "",
    expectedStudents: "40",
    instructorId: "",
    maximumEnrollment: "",
    notes: "",
    offeringCode: "",
    parallelGroup: "",
    sectionId: "",
    status: "ACTIVE",
  });
  const managedItems = useMemo(() => buildManagedItems(setup), [setup]);
  const filteredManagedItems = useMemo(() => {
    const normalized = managedQuery.trim().toLocaleLowerCase();
    return normalized
      ? managedItems.filter((item) =>
          `${item.type} ${item.label}`.toLocaleLowerCase().includes(normalized),
        )
      : managedItems;
  }, [managedItems, managedQuery]);
  const managedPages = Math.max(
    1,
    Math.ceil(filteredManagedItems.length / managedPageSize),
  );
  const safeManagedPage = Math.min(managedPage, managedPages - 1);
  const visibleManagedItems = filteredManagedItems.slice(
    safeManagedPage * managedPageSize,
    (safeManagedPage + 1) * managedPageSize,
  );
  const [requirementForm, setRequirementForm] = useState({
    active: true,
    allowedDays: "",
    customWeeks: "",
    durationSlots: "1",
    fixedRoomId: "",
    fixedTimeslotId: "",
    lectureBeforeLinked: false,
    linkedRequirementId: "",
    maximumOccurrencesPerDay: "1",
    meetingType: "LECTURE",
    minimumDaySeparation: "0",
    notes: "",
    offeringId: "",
    pinned: false,
    preferredDays: "",
    preferredEndTime: "",
    preferredStartTime: "",
    prohibitedDays: "",
    requiredCapacity: "40",
    requiredFacilities: [] as string[],
    roomType: "CLASSROOM",
    sameRoomPreferred: false,
    sessionsPerWeek: "1",
    teachingGroupId: "",
    weekPattern: "EVERY_WEEK",
  });
  const [availabilityForm, setAvailabilityForm] = useState({
    availability: "UNAVAILABLE" as AuraAvailabilityType,
    reason: "",
    targetId: "",
    targetType: "INSTRUCTOR" as
      | "INSTRUCTOR"
      | "ROOM"
      | "SECTION"
      | "STUDENT",
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
        savedBuildings,
        savedTeachingGroups,
        programs,
        batches,
        sections,
        instructors,
        rooms,
        timeslots,
        offerings,
        exceptions,
        savedStudentAvailability,
      ] = await Promise.all([
        getAuraSetupReferences(signal),
        listAuraBuildings(signal),
        listAuraTeachingGroups(termId, signal),
        listAuraPrograms(universityId, signal),
        listAuraBatches(undefined, signal),
        listAuraSections(undefined, signal),
        listAuraInstructors(universityId, signal),
        listAuraRooms(universityId, signal),
        listAuraTimeslots(universityId, signal),
        listAuraOfferings(termId, signal),
        listAuraCalendarExceptions(termId, signal),
        listAuraStudentAvailability(termId, undefined, signal),
      ]);
      const requirements = (await Promise.all(
        offerings.map((offering) =>
          listAuraMeetingRequirements(offering.id, signal),
        ),
      )).flat();
      setReferences(referenceResponse);
      setBuildings(savedBuildings);
      setTeachingGroups(savedTeachingGroups);
      setSetup({
        batches,
        instructors,
        offerings,
        programs,
        requirements,
        rooms,
        sections,
        timeslots,
      });
      setCalendarExceptions(exceptions);
      setStudentAvailability(savedStudentAvailability);
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
    if (availabilityForm.targetType === "STUDENT") {
      return (references?.students ?? []).map((student) => ({
        id: student.id,
        label: `${student.code} · ${student.name}`,
      }));
    }
    return setup.instructors.map((instructor) => ({
      id: instructor.id,
      label: instructor.displayName,
    }));
  }, [availabilityForm.targetType, references, setup]);

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
          <TextInput label="Semesters" onChange={(numberOfSemesters) => setProgramForm((current) => ({ ...current, numberOfSemesters }))} type="number" value={programForm.numberOfSemesters} />
          <Button
            disabled={!programForm.departmentId || !programForm.code.trim() || !programForm.name.trim()}
            loading={busyAction === "program"}
            onClick={() => void runAction("program", () => {
              const existing = setup.programs.find((program) => program.id === editingProgramId);
              return existing
                ? updateAuraProgram(existing.id, {
                    active: existing.active,
                    code: programForm.code,
                    departmentId: programForm.departmentId,
                    name: programForm.name,
                    numberOfSemesters: Number(programForm.numberOfSemesters),
                    version: existing.version,
                  })
                : createAuraProgram({
                    code: programForm.code,
                    departmentId: programForm.departmentId,
                    name: programForm.name,
                    universityId,
                  });
            }, () => {
              setEditingProgramId(null);
              setProgramForm({ code: "", departmentId: "", name: "", numberOfSemesters: "8" });
            })}
          >{editingProgramId ? "Save program changes" : "Save program"}</Button>
          {editingProgramId ? (
            <Button onClick={() => {
              setEditingProgramId(null);
              setProgramForm({ code: "", departmentId: "", name: "", numberOfSemesters: "8" });
            }} variant="outline">Cancel editing</Button>
          ) : null}
        </SetupCard>

        <SetupCard title="2. Batch / cohort">
          <SelectInput label="Program" onChange={(programId) => setBatchForm((current) => ({ ...current, programId }))} options={setup.programs.map((program) => ({ label: `${program.code} · ${program.name}`, value: program.id }))} value={batchForm.programId} />
          <TextInput label="Batch code" onChange={(code) => setBatchForm((current) => ({ ...current, code }))} placeholder="FA24" value={batchForm.code} />
          <TextInput label="Admission year" onChange={(admissionYear) => setBatchForm((current) => ({ ...current, admissionYear }))} type="number" value={batchForm.admissionYear} />
          <TextInput label="Expected graduation year (optional)" onChange={(expectedGraduationYear) => setBatchForm((current) => ({ ...current, expectedGraduationYear }))} type="number" value={batchForm.expectedGraduationYear} />
          <Button disabled={!batchForm.programId || !batchForm.code.trim()} loading={busyAction === "batch"} onClick={() => void runAction("batch", () => {
            const existing = setup.batches.find((batch) => batch.id === editingBatchId);
            return existing ? updateAuraBatch(existing.id, {
              active: existing.active,
              admissionYear: Number(batchForm.admissionYear),
              code: batchForm.code,
              expectedGraduationYear: batchForm.expectedGraduationYear ? Number(batchForm.expectedGraduationYear) : undefined,
              version: existing.version,
            }) : createAuraBatch({ admissionYear: Number(batchForm.admissionYear), code: batchForm.code, programId: batchForm.programId });
          }, () => {
            setEditingBatchId(null);
            setBatchForm((current) => ({ ...current, code: "", expectedGraduationYear: "" }));
          })}>{editingBatchId ? "Save batch changes" : "Save batch"}</Button>
        </SetupCard>

        <SetupCard title="3. Section">
          <SelectInput label="Batch" onChange={(batchId) => setSectionForm((current) => ({ ...current, batchId }))} options={setup.batches.map((batch) => ({ label: `${batch.code} · ${batch.admissionYear}`, value: batch.id }))} value={sectionForm.batchId} />
          <TextInput label="Section code" onChange={(code) => setSectionForm((current) => ({ ...current, code }))} placeholder="4A" value={sectionForm.code} />
          <TextInput label="Display name" onChange={(displayName) => setSectionForm((current) => ({ ...current, displayName }))} placeholder="BSCS-4A" value={sectionForm.displayName} />
          <TextInput label="Expected students" onChange={(studentCount) => setSectionForm((current) => ({ ...current, studentCount }))} type="number" value={sectionForm.studentCount} />
          {editingSectionId ? (
            <>
              <div className="grid gap-3 sm:grid-cols-3">
                <TextInput label="Semester" onChange={(semesterNumber) => setSectionForm((current) => ({ ...current, semesterNumber }))} type="number" value={sectionForm.semesterNumber} />
                <TextInput label="Maximum daily slots" onChange={(hardDailyLoad) => setSectionForm((current) => ({ ...current, hardDailyLoad }))} type="number" value={sectionForm.hardDailyLoad} />
                <TextInput label="Preferred daily slots" onChange={(preferredDailyLoad) => setSectionForm((current) => ({ ...current, preferredDailyLoad }))} type="number" value={sectionForm.preferredDailyLoad} />
              </div>
              <SelectInput label="Home building" onChange={(homeBuildingId) => setSectionForm((current) => ({ ...current, homeBuildingId }))} options={buildings.map((building) => ({ label: `${building.code} · ${building.name}`, value: building.id }))} value={sectionForm.homeBuildingId} />
            </>
          ) : null}
          <Button disabled={!sectionForm.batchId || !sectionForm.code.trim() || !sectionForm.displayName.trim()} loading={busyAction === "section"} onClick={() => void runAction("section", () => {
            const existing = setup.sections.find((section) => section.id === editingSectionId);
            return existing ? updateAuraSection(existing.id, {
              active: existing.active,
              batchId: sectionForm.batchId,
              code: sectionForm.code,
              displayName: sectionForm.displayName,
              hardDailyLoad: Number(sectionForm.hardDailyLoad),
              homeBuildingId: sectionForm.homeBuildingId || undefined,
              preferredDailyLoad: Number(sectionForm.preferredDailyLoad),
              semesterNumber: sectionForm.semesterNumber ? Number(sectionForm.semesterNumber) : undefined,
              studentCount: Number(sectionForm.studentCount),
              termId,
              version: existing.version,
            }) : createAuraSection({ batchId: sectionForm.batchId, code: sectionForm.code, displayName: sectionForm.displayName, studentCount: Number(sectionForm.studentCount) });
          }, () => {
            setEditingSectionId(null);
            setSectionForm((current) => ({ ...current, code: "", displayName: "" }));
          })}>{editingSectionId ? "Save section changes" : "Save section"}</Button>
        </SetupCard>

        <SetupCard title="4. Instructor">
          <TextInput label="Full name" onChange={(displayName) => setInstructorForm((current) => ({ ...current, displayName }))} placeholder="Dr Ahmed Khan" value={instructorForm.displayName} />
          <TextInput label="Email (optional)" onChange={(email) => setInstructorForm((current) => ({ ...current, email }))} placeholder="ahmed@university.edu" type="email" value={instructorForm.email} />
          <TextInput label="Maximum weekly hours" onChange={(maxHoursPerWeek) => setInstructorForm((current) => ({ ...current, maxHoursPerWeek }))} type="number" value={instructorForm.maxHoursPerWeek} />
          {editingInstructorId ? (
            <>
              <div className="grid gap-3 sm:grid-cols-2">
                <SelectInput label="Department" onChange={(departmentId) => setInstructorForm((current) => ({ ...current, departmentId }))} options={(references?.departments ?? []).map((department) => ({ label: `${department.code} · ${department.name}`, value: department.id }))} value={instructorForm.departmentId} />
                <TextInput label="Employee code" onChange={(employeeCode) => setInstructorForm((current) => ({ ...current, employeeCode }))} value={instructorForm.employeeCode} />
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                <TextInput label="Maximum daily slots" onChange={(hardDailyLoad) => setInstructorForm((current) => ({ ...current, hardDailyLoad }))} type="number" value={instructorForm.hardDailyLoad} />
                <TextInput label="Maximum consecutive slots" onChange={(maximumConsecutiveSlots) => setInstructorForm((current) => ({ ...current, maximumConsecutiveSlots }))} type="number" value={instructorForm.maximumConsecutiveSlots} />
                <TextInput label="Preferred weekly slots" onChange={(preferredWeeklyLoad) => setInstructorForm((current) => ({ ...current, preferredWeeklyLoad }))} type="number" value={instructorForm.preferredWeeklyLoad} />
                <TextInput label="Preferred daily slots" onChange={(preferredDailyLoad) => setInstructorForm((current) => ({ ...current, preferredDailyLoad }))} type="number" value={instructorForm.preferredDailyLoad} />
              </div>
              <SelectInput label="Home building" onChange={(homeBuildingId) => setInstructorForm((current) => ({ ...current, homeBuildingId }))} options={buildings.map((building) => ({ label: `${building.code} · ${building.name}`, value: building.id }))} value={instructorForm.homeBuildingId} />
            </>
          ) : null}
          <Button disabled={!instructorForm.displayName.trim()} loading={busyAction === "instructor"} onClick={() => void runAction("instructor", () => {
            const existing = setup.instructors.find((instructor) => instructor.id === editingInstructorId);
            return existing ? updateAuraInstructor(existing.id, {
              active: existing.active,
              departmentId: instructorForm.departmentId || undefined,
              displayName: instructorForm.displayName,
              email: instructorForm.email || undefined,
              employeeCode: instructorForm.employeeCode || undefined,
              hardDailyLoad: Number(instructorForm.hardDailyLoad),
              homeBuildingId: instructorForm.homeBuildingId || undefined,
              maxHoursPerWeek: Number(instructorForm.maxHoursPerWeek),
              maximumConsecutiveSlots: Number(instructorForm.maximumConsecutiveSlots),
              preferredDailyLoad: Number(instructorForm.preferredDailyLoad),
              preferredWeeklyLoad: Number(instructorForm.preferredWeeklyLoad),
              userId: existing.userId ?? undefined,
              version: existing.version,
            }) : createAuraInstructor({ displayName: instructorForm.displayName, email: instructorForm.email || undefined, maxHoursPerWeek: Number(instructorForm.maxHoursPerWeek), universityId });
          }, () => {
            setEditingInstructorId(null);
            setInstructorForm((current) => ({ ...current, displayName: "", email: "" }));
          })}>{editingInstructorId ? "Save instructor changes" : "Save instructor"}</Button>
        </SetupCard>

        <SetupCard title="5. Room">
          <TextInput label="Room name" onChange={(name) => setRoomForm((current) => ({ ...current, name }))} placeholder="CS Lab 1" value={roomForm.name} />
          {editingRoomId ? (
            <>
              <SelectInput label="Building" onChange={(buildingId) => setRoomForm((current) => ({ ...current, buildingId }))} options={buildings.map((building) => ({ label: `${building.code} · ${building.name}`, value: building.id }))} value={roomForm.buildingId} />
              <TextInput label="Room code" onChange={(roomCode) => setRoomForm((current) => ({ ...current, roomCode }))} value={roomForm.roomCode} />
              <TextInput label="Display name" onChange={(displayName) => setRoomForm((current) => ({ ...current, displayName }))} value={roomForm.displayName} />
            </>
          ) : (
            <TextInput label="Building (optional)" onChange={(building) => setRoomForm((current) => ({ ...current, building }))} placeholder="Academic Block" value={roomForm.building} />
          )}
          <TextInput label="Capacity" onChange={(capacity) => setRoomForm((current) => ({ ...current, capacity }))} type="number" value={roomForm.capacity} />
          <SelectInput label="Room type" onChange={(roomType) => setRoomForm((current) => ({ ...current, roomType }))} options={roomTypes.map((value) => ({ label: value.replaceAll("_", " "), value }))} value={roomForm.roomType} />
          <FacilityCheckboxes label="Available facilities" onChange={(facilities) => setRoomForm((current) => ({ ...current, facilities }))} value={roomForm.facilities} />
          <Button disabled={!roomForm.name.trim()} loading={busyAction === "room"} onClick={() => void runAction("room", () => {
            const existing = setup.rooms.find((room) => room.id === editingRoomId);
            return existing ? updateAuraRoom(existing.id, {
              active: existing.active,
              buildingId: roomForm.buildingId || undefined,
              capacity: Number(roomForm.capacity),
              displayName: roomForm.displayName || roomForm.name,
              facilities: roomForm.facilities,
              name: roomForm.name,
              roomCode: roomForm.roomCode || undefined,
              roomType: roomForm.roomType,
              version: existing.version,
            }) : createAuraRoom({ building: roomForm.building || undefined, capacity: Number(roomForm.capacity), facilities: roomForm.facilities, name: roomForm.name, roomType: roomForm.roomType, universityId });
          }, () => {
            setEditingRoomId(null);
            setRoomForm((current) => ({ ...current, building: "", facilities: [], name: "" }));
          })}>{editingRoomId ? "Save room changes" : "Save room"}</Button>
        </SetupCard>

        <SetupCard title="6. Weekly timeslot">
          <SelectInput label="Day" onChange={(dayOfWeek) => setTimeslotForm((current) => ({ ...current, dayOfWeek }))} options={dayOptions} value={timeslotForm.dayOfWeek} />
          <div className="grid gap-3 sm:grid-cols-2">
            <TextInput label="Starts" onChange={(startsAt) => setTimeslotForm((current) => ({ ...current, startsAt }))} type="time" value={timeslotForm.startsAt} />
            <TextInput label="Ends" onChange={(endsAt) => setTimeslotForm((current) => ({ ...current, endsAt }))} type="time" value={timeslotForm.endsAt} />
          </div>
          <TextInput label="Label" onChange={(label) => setTimeslotForm((current) => ({ ...current, label }))} placeholder="Monday 09:00–10:00" value={timeslotForm.label} />
          {editingTimeslotId ? (
            <div className="grid gap-3 sm:grid-cols-2">
              <TextInput label="Display order" onChange={(slotOrder) => setTimeslotForm((current) => ({ ...current, slotOrder }))} type="number" value={timeslotForm.slotOrder} />
              <SelectInput label="Slot type" onChange={(slotType) => setTimeslotForm((current) => ({ ...current, slotType }))} options={[{ label: "Instructional", value: "INSTRUCTIONAL" }, { label: "Break", value: "BREAK" }]} value={timeslotForm.slotType} />
            </div>
          ) : null}
          <Button disabled={!timeslotForm.label.trim() || timeslotForm.startsAt >= timeslotForm.endsAt} loading={busyAction === "timeslot"} onClick={() => void runAction("timeslot", () => {
            const existing = setup.timeslots.find((timeslot) => timeslot.id === editingTimeslotId);
            return existing ? updateAuraTimeslot(existing.id, {
              active: existing.active,
              dayOfWeek: Number(timeslotForm.dayOfWeek),
              endsAt: timeslotForm.endsAt,
              label: timeslotForm.label,
              slotOrder: timeslotForm.slotOrder ? Number(timeslotForm.slotOrder) : undefined,
              slotType: timeslotForm.slotType,
              startsAt: timeslotForm.startsAt,
              termId,
              version: existing.version,
            }) : createAuraTimeslot({ dayOfWeek: Number(timeslotForm.dayOfWeek), endsAt: timeslotForm.endsAt, label: timeslotForm.label, startsAt: timeslotForm.startsAt, universityId });
          }, () => {
            setEditingTimeslotId(null);
            setTimeslotForm((current) => ({ ...current, label: "" }));
          })}>{editingTimeslotId ? "Save timeslot changes" : "Save timeslot"}</Button>
        </SetupCard>

        <SetupCard title="7. Course offering">
          <SelectInput label="Course" onChange={(courseId) => setOfferingForm((current) => ({ ...current, courseId }))} options={(references?.courses ?? []).map((course) => ({ label: `${course.code} · ${course.name}`, value: course.id }))} value={offeringForm.courseId} />
          <SelectInput label="Section" onChange={(sectionId) => setOfferingForm((current) => ({ ...current, sectionId }))} options={setup.sections.map((section) => ({ label: section.displayName, value: section.id }))} value={offeringForm.sectionId} />
          <SelectInput label="Instructor" onChange={(instructorId) => setOfferingForm((current) => ({ ...current, instructorId }))} options={setup.instructors.map((instructor) => ({ label: instructor.displayName, value: instructor.id }))} value={offeringForm.instructorId} />
          <TextInput label="Expected enrollment" onChange={(expectedStudents) => setOfferingForm((current) => ({ ...current, expectedStudents }))} type="number" value={offeringForm.expectedStudents} />
          {editingOfferingId ? (
            <>
              <div className="grid gap-3 sm:grid-cols-2">
                <TextInput label="Offering code" onChange={(offeringCode) => setOfferingForm((current) => ({ ...current, offeringCode }))} value={offeringForm.offeringCode} />
                <TextInput label="Maximum enrollment" onChange={(maximumEnrollment) => setOfferingForm((current) => ({ ...current, maximumEnrollment }))} type="number" value={offeringForm.maximumEnrollment} />
                <TextInput label="Parallel group" onChange={(parallelGroup) => setOfferingForm((current) => ({ ...current, parallelGroup }))} value={offeringForm.parallelGroup} />
                <TextInput label="Elective group" onChange={(electiveGroup) => setOfferingForm((current) => ({ ...current, electiveGroup }))} value={offeringForm.electiveGroup} />
              </div>
              <TextInput label="Scheduling notes" onChange={(notes) => setOfferingForm((current) => ({ ...current, notes }))} value={offeringForm.notes} />
              <div className="grid gap-3 sm:grid-cols-2">
                <SelectInput label="Status" onChange={(status) => setOfferingForm((current) => ({ ...current, status }))} options={[{ label: "Active", value: "ACTIVE" }, { label: "Inactive", value: "INACTIVE" }, { label: "Archived", value: "ARCHIVED" }]} value={offeringForm.status} />
                <BooleanInput checked={offeringForm.combinedSections} label="Combined sections" onChange={(combinedSections) => setOfferingForm((current) => ({ ...current, combinedSections }))} />
              </div>
            </>
          ) : null}
          <Button disabled={!offeringForm.courseId || !offeringForm.sectionId || !offeringForm.instructorId} loading={busyAction === "offering"} onClick={() => void runAction("offering", () => {
            const existing = setup.offerings.find((offering) => offering.id === editingOfferingId);
            return existing ? updateAuraOffering(existing.id, {
              combinedSections: offeringForm.combinedSections,
              courseId: offeringForm.courseId,
              electiveGroup: offeringForm.electiveGroup || undefined,
              expectedStudents: Number(offeringForm.expectedStudents),
              instructorId: offeringForm.instructorId,
              maximumEnrollment: offeringForm.maximumEnrollment ? Number(offeringForm.maximumEnrollment) : undefined,
              notes: offeringForm.notes || undefined,
              offeringCode: offeringForm.offeringCode || undefined,
              parallelGroup: offeringForm.parallelGroup || undefined,
              sectionId: offeringForm.sectionId,
              status: offeringForm.status,
              version: existing.version,
            }) : createAuraOffering({ courseId: offeringForm.courseId, expectedStudents: Number(offeringForm.expectedStudents), instructorId: offeringForm.instructorId, sectionId: offeringForm.sectionId, termId });
          }, () => {
            setEditingOfferingId(null);
            setOfferingForm((current) => ({ ...current, courseId: "" }));
          })}>{editingOfferingId ? "Save offering changes" : "Save offering"}</Button>
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
          {editingRequirementId ? (
            <>
              <div className="grid gap-3 sm:grid-cols-3">
                <TextInput label="Allowed days" onChange={(allowedDays) => setRequirementForm((current) => ({ ...current, allowedDays }))} placeholder="1,2,3,4,5" value={requirementForm.allowedDays} />
                <TextInput label="Preferred days" onChange={(preferredDays) => setRequirementForm((current) => ({ ...current, preferredDays }))} placeholder="2,4" value={requirementForm.preferredDays} />
                <TextInput label="Prohibited days" onChange={(prohibitedDays) => setRequirementForm((current) => ({ ...current, prohibitedDays }))} placeholder="6,7" value={requirementForm.prohibitedDays} />
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                <TextInput label="Preferred start" onChange={(preferredStartTime) => setRequirementForm((current) => ({ ...current, preferredStartTime }))} type="time" value={requirementForm.preferredStartTime} />
                <TextInput label="Preferred end" onChange={(preferredEndTime) => setRequirementForm((current) => ({ ...current, preferredEndTime }))} type="time" value={requirementForm.preferredEndTime} />
                <TextInput label="Minimum day separation" onChange={(minimumDaySeparation) => setRequirementForm((current) => ({ ...current, minimumDaySeparation }))} type="number" value={requirementForm.minimumDaySeparation} />
                <TextInput label="Maximum occurrences per day" onChange={(maximumOccurrencesPerDay) => setRequirementForm((current) => ({ ...current, maximumOccurrencesPerDay }))} type="number" value={requirementForm.maximumOccurrencesPerDay} />
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                <SelectInput label="Fixed room" onChange={(fixedRoomId) => setRequirementForm((current) => ({ ...current, fixedRoomId }))} options={setup.rooms.map((room) => ({ label: room.displayName || room.name, value: room.id }))} value={requirementForm.fixedRoomId} />
                <SelectInput label="Fixed timeslot" onChange={(fixedTimeslotId) => setRequirementForm((current) => ({ ...current, fixedTimeslotId }))} options={setup.timeslots.map((timeslot) => ({ label: `${dayName(timeslot.dayOfWeek)} · ${timeslot.label}`, value: timeslot.id }))} value={requirementForm.fixedTimeslotId} />
                <SelectInput label="Teaching group" onChange={(teachingGroupId) => setRequirementForm((current) => ({ ...current, teachingGroupId }))} options={teachingGroups.filter((group) => group.offeringId === requirementForm.offeringId).map((group) => ({ label: `${group.code} · ${group.displayName}`, value: group.id }))} value={requirementForm.teachingGroupId} />
                <SelectInput label="Linked requirement" onChange={(linkedRequirementId) => setRequirementForm((current) => ({ ...current, linkedRequirementId }))} options={setup.requirements.filter((requirement) => requirement.offeringId === requirementForm.offeringId && requirement.id !== editingRequirementId).map((requirement) => ({ label: `${requirement.meetingType} · ${requirement.id.slice(0, 8)}`, value: requirement.id }))} value={requirementForm.linkedRequirementId} />
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                <SelectInput label="Week pattern" onChange={(weekPattern) => setRequirementForm((current) => ({ ...current, weekPattern }))} options={weekPatterns.map((value) => ({ label: value.replaceAll("_", " "), value }))} value={requirementForm.weekPattern} />
                {requirementForm.weekPattern === "CUSTOM_WEEK_SET" ? (
                  <TextInput label="Custom weeks" onChange={(customWeeks) => setRequirementForm((current) => ({ ...current, customWeeks }))} placeholder="1,3,5,7" value={requirementForm.customWeeks} />
                ) : null}
              </div>
              <div className="grid gap-2 sm:grid-cols-2">
                <BooleanInput checked={requirementForm.sameRoomPreferred} label="Prefer the same room" onChange={(sameRoomPreferred) => setRequirementForm((current) => ({ ...current, sameRoomPreferred }))} />
                <BooleanInput checked={requirementForm.pinned} label="Pin fixed assignment" onChange={(pinned) => setRequirementForm((current) => ({ ...current, pinned }))} />
                <BooleanInput checked={requirementForm.lectureBeforeLinked} label="Schedule lecture before linked activity" onChange={(lectureBeforeLinked) => setRequirementForm((current) => ({ ...current, lectureBeforeLinked }))} />
              </div>
            </>
          ) : null}
          <Button disabled={!requirementForm.offeringId} loading={busyAction === "requirement"} onClick={() => void runAction("requirement", () => {
            const existing = setup.requirements.find((requirement) => requirement.id === editingRequirementId);
            return existing ? updateAuraMeetingRequirement(existing.id, {
              active: existing.active,
              allowedDays: parseIntegerList(requirementForm.allowedDays, 1, 7),
              customWeeks: requirementForm.weekPattern === "CUSTOM_WEEK_SET" ? parseIntegerList(requirementForm.customWeeks, 1, 53) : undefined,
              durationSlots: Number(requirementForm.durationSlots),
              fixedRoomId: requirementForm.fixedRoomId || undefined,
              fixedTimeslotId: requirementForm.fixedTimeslotId || undefined,
              lectureBeforeLinked: requirementForm.lectureBeforeLinked,
              linkedRequirementId: requirementForm.linkedRequirementId || undefined,
              maximumOccurrencesPerDay: Number(requirementForm.maximumOccurrencesPerDay),
              meetingType: requirementForm.meetingType,
              minimumDaySeparation: Number(requirementForm.minimumDaySeparation),
              notes: requirementForm.notes || undefined,
              pinned: requirementForm.pinned,
              preferredDays: parseIntegerList(requirementForm.preferredDays, 1, 7),
              preferredEndTime: requirementForm.preferredEndTime || undefined,
              preferredStartTime: requirementForm.preferredStartTime || undefined,
              prohibitedDays: parseIntegerList(requirementForm.prohibitedDays, 1, 7),
              requiredCapacity: Number(requirementForm.requiredCapacity),
              requiredFacilities: requirementForm.requiredFacilities,
              roomType: requirementForm.roomType,
              sameRoomPreferred: requirementForm.sameRoomPreferred,
              sessionsPerWeek: Number(requirementForm.sessionsPerWeek),
              teachingGroupId: requirementForm.teachingGroupId || undefined,
              version: existing.version,
              weekPattern: requirementForm.weekPattern,
            }) : createAuraMeetingRequirement({ durationSlots: Number(requirementForm.durationSlots), meetingType: requirementForm.meetingType, notes: requirementForm.notes || undefined, offeringId: requirementForm.offeringId, requiredCapacity: Number(requirementForm.requiredCapacity), requiredFacilities: requirementForm.requiredFacilities, roomType: requirementForm.roomType, sessionsPerWeek: Number(requirementForm.sessionsPerWeek) });
          }, () => {
            setEditingRequirementId(null);
            setRequirementForm((current) => ({ ...current, notes: "", requiredFacilities: [] }));
          })}>{editingRequirementId ? "Save requirement changes" : "Save requirement"}</Button>
        </SetupCard>

        <SetupCard title="9. Availability preference">
          <SelectInput label="Resource type" onChange={(targetType) => setAvailabilityForm((current) => ({ ...current, targetId: "", targetType: targetType as typeof current.targetType }))} options={[{ label: "Instructor", value: "INSTRUCTOR" }, { label: "Room", value: "ROOM" }, { label: "Section", value: "SECTION" }, { label: "Student", value: "STUDENT" }]} value={availabilityForm.targetType} />
          <SelectInput label="Resource" onChange={(targetId) => setAvailabilityForm((current) => ({ ...current, targetId }))} options={targetOptions.map((target) => ({ label: target.label, value: target.id }))} value={availabilityForm.targetId} />
          <SelectInput label="Timeslot" onChange={(timeslotId) => setAvailabilityForm((current) => ({ ...current, timeslotId }))} options={setup.timeslots.map((timeslot) => ({ label: `${dayName(timeslot.dayOfWeek)} · ${timeslot.label}`, value: timeslot.id }))} value={availabilityForm.timeslotId} />
          <SelectInput label="Preference" onChange={(availability) => setAvailabilityForm((current) => ({ ...current, availability: availability as AuraAvailabilityType }))} options={availabilityTypes.map((value) => ({ label: value, value }))} value={availabilityForm.availability} />
          <TextInput label="Reason (optional)" onChange={(reason) => setAvailabilityForm((current) => ({ ...current, reason }))} placeholder="Department meeting" value={availabilityForm.reason} />
          <Button disabled={!availabilityForm.targetId || !availabilityForm.timeslotId} loading={busyAction === "availability"} onClick={() => void runAction("availability", () => saveAvailability(availabilityForm, termId), () => setAvailabilityForm((current) => ({ ...current, reason: "" })))}>Save availability</Button>
          {studentAvailability.length ? (
            <div className="grid gap-2 border-t border-slate-100 pt-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                Saved student preferences
              </p>
              {studentAvailability.slice(0, 8).map((entry) => {
                const student = references?.students.find(
                  (candidate) => candidate.id === entry.studentUserId,
                );
                const timeslot = setup.timeslots.find(
                  (candidate) => candidate.id === entry.timeslotId,
                );
                return (
                  <div
                    className="flex flex-col gap-2 rounded-xl bg-slate-50 p-3 text-sm text-slate-700 sm:flex-row sm:items-center sm:justify-between"
                    key={entry.id}
                  >
                    <p>
                      <span className="font-semibold text-slate-900">
                        {student?.name ?? student?.code ?? "Student"}
                      </span>
                      {" · "}
                      {timeslot?.label ?? "Timeslot"}
                      {" · "}
                      {entry.availability.replaceAll("_", " ")}
                    </p>
                    <Button
                      loading={busyAction === `student-availability-${entry.id}`}
                      onClick={() => void runAction(
                        `student-availability-${entry.id}`,
                        () => deleteAuraStudentAvailability(entry.id, entry.version),
                        () => undefined,
                      )}
                      variant="outline"
                    >
                      Remove
                    </Button>
                  </div>
                );
              })}
            </div>
          ) : null}
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
          <label className="grid gap-1.5 text-sm font-medium text-slate-700">
            Search saved setup
            <input
              className={inputClass}
              onChange={(event) => {
                setManagedQuery(event.target.value);
                setManagedPage(0);
              }}
              placeholder="Search by type, code, or name"
              type="search"
              value={managedQuery}
            />
          </label>
          {!filteredManagedItems.length ? (
            <EmptyState
              description={managedQuery.trim()
                ? "Try another code, name, or setup type."
                : "Create the first setup record above."}
              title={managedQuery.trim()
                ? "No matching setup records"
                : "No setup records yet"}
            />
          ) : null}
          {visibleManagedItems.map((item) => (
            <div className="flex flex-col gap-2 rounded-xl border border-slate-200 p-3 sm:flex-row sm:items-center sm:justify-between" key={`${item.type}-${item.id}`}>
              <div>
                <p className="text-sm font-semibold text-slate-900">{item.label}</p>
                <p className="text-xs text-slate-500">{item.active ? "Active" : "Inactive"}</p>
              </div>
              <div className="flex flex-wrap gap-2">
                {item.type === "program" ? (
                  <Button
                    onClick={() => {
                      const program = setup.programs.find((candidate) => candidate.id === item.id);
                      if (!program) return;
                      setEditingProgramId(program.id);
                      setProgramForm({
                        code: program.code,
                        departmentId: program.departmentId,
                        name: program.name,
                        numberOfSemesters: String(program.numberOfSemesters),
                      });
                    }}
                    variant="outline"
                  >Edit</Button>
                ) : null}
                {item.type === "batch" ? (
                  <Button
                    onClick={() => {
                      const batch = setup.batches.find((candidate) => candidate.id === item.id);
                      if (!batch) return;
                      setEditingBatchId(batch.id);
                      setBatchForm({
                        admissionYear: String(batch.admissionYear),
                        code: batch.code,
                        expectedGraduationYear: batch.expectedGraduationYear ? String(batch.expectedGraduationYear) : "",
                        programId: batch.programId,
                      });
                    }}
                    variant="outline"
                  >Edit</Button>
                ) : null}
                {item.type === "section" ? (
                  <Button
                    onClick={() => {
                      const section = setup.sections.find((candidate) => candidate.id === item.id);
                      if (!section) return;
                      setEditingSectionId(section.id);
                      setSectionForm({
                        batchId: section.batchId,
                        code: section.code,
                        displayName: section.displayName,
                        hardDailyLoad: String(section.hardDailyLoad),
                        homeBuildingId: section.homeBuildingId ?? "",
                        preferredDailyLoad: String(section.preferredDailyLoad),
                        semesterNumber: section.semesterNumber ? String(section.semesterNumber) : "",
                        studentCount: String(section.studentCount),
                        termId: section.termId ?? termId,
                      });
                    }}
                    variant="outline"
                  >Edit</Button>
                ) : null}
                {item.type === "instructor" ? (
                  <Button
                    onClick={() => {
                      const instructor = setup.instructors.find((candidate) => candidate.id === item.id);
                      if (!instructor) return;
                      setEditingInstructorId(instructor.id);
                      setInstructorForm({
                        departmentId: instructor.departmentId ?? "",
                        displayName: instructor.displayName,
                        email: instructor.email ?? "",
                        employeeCode: instructor.employeeCode ?? "",
                        hardDailyLoad: String(instructor.hardDailyLoad),
                        homeBuildingId: instructor.homeBuildingId ?? "",
                        maxHoursPerWeek: String(instructor.maxHoursPerWeek),
                        maximumConsecutiveSlots: String(instructor.maximumConsecutiveSlots),
                        preferredDailyLoad: String(instructor.preferredDailyLoad),
                        preferredWeeklyLoad: String(instructor.preferredWeeklyLoad),
                      });
                    }}
                    variant="outline"
                  >Edit</Button>
                ) : null}
                {item.type === "room" ? (
                  <Button
                    onClick={() => {
                      const room = setup.rooms.find((candidate) => candidate.id === item.id);
                      if (!room) return;
                      setEditingRoomId(room.id);
                      setRoomForm({
                        building: room.building ?? "",
                        buildingId: room.buildingId ?? "",
                        capacity: String(room.capacity),
                        displayName: room.displayName,
                        facilities: room.facilities,
                        name: room.name,
                        roomCode: room.roomCode ?? "",
                        roomType: room.roomType,
                      });
                    }}
                    variant="outline"
                  >Edit</Button>
                ) : null}
                {item.type === "timeslot" ? (
                  <Button
                    onClick={() => {
                      const timeslot = setup.timeslots.find((candidate) => candidate.id === item.id);
                      if (!timeslot) return;
                      setEditingTimeslotId(timeslot.id);
                      setTimeslotForm({
                        dayOfWeek: String(timeslot.dayOfWeek),
                        endsAt: timeslot.endsAt.slice(0, 5),
                        label: timeslot.label,
                        slotOrder: timeslot.slotOrder ? String(timeslot.slotOrder) : "",
                        slotType: timeslot.slotType,
                        startsAt: timeslot.startsAt.slice(0, 5),
                        termId: timeslot.termId ?? termId,
                      });
                    }}
                    variant="outline"
                  >Edit</Button>
                ) : null}
                {item.type === "offering" ? (
                  <Button
                    onClick={() => {
                      const offering = setup.offerings.find((candidate) => candidate.id === item.id);
                      if (!offering) return;
                      setEditingOfferingId(offering.id);
                      setOfferingForm({
                        combinedSections: offering.combinedSections,
                        courseId: offering.courseId,
                        electiveGroup: offering.electiveGroup ?? "",
                        expectedStudents: String(offering.expectedStudents),
                        instructorId: offering.instructorId,
                        maximumEnrollment: offering.maximumEnrollment ? String(offering.maximumEnrollment) : "",
                        notes: offering.notes ?? "",
                        offeringCode: offering.offeringCode ?? "",
                        parallelGroup: offering.parallelGroup ?? "",
                        sectionId: offering.sectionId,
                        status: offering.status,
                      });
                    }}
                    variant="outline"
                  >Edit</Button>
                ) : null}
                {item.type === "requirement" ? (
                  <Button
                    onClick={() => {
                      const requirement = setup.requirements.find((candidate) => candidate.id === item.id);
                      if (!requirement) return;
                      setEditingRequirementId(requirement.id);
                      setRequirementForm({
                        active: requirement.active,
                        allowedDays: requirement.allowedDays.join(","),
                        customWeeks: requirement.customWeeks.join(","),
                        durationSlots: String(requirement.durationSlots),
                        fixedRoomId: requirement.fixedRoomId ?? "",
                        fixedTimeslotId: requirement.fixedTimeslotId ?? "",
                        lectureBeforeLinked: requirement.lectureBeforeLinked,
                        linkedRequirementId: requirement.linkedRequirementId ?? "",
                        maximumOccurrencesPerDay: String(requirement.maximumOccurrencesPerDay),
                        meetingType: requirement.meetingType,
                        minimumDaySeparation: String(requirement.minimumDaySeparation),
                        notes: requirement.notes ?? "",
                        offeringId: requirement.offeringId,
                        pinned: requirement.pinned,
                        preferredDays: requirement.preferredDays.join(","),
                        preferredEndTime: requirement.preferredEndTime?.slice(0, 5) ?? "",
                        preferredStartTime: requirement.preferredStartTime?.slice(0, 5) ?? "",
                        prohibitedDays: requirement.prohibitedDays.join(","),
                        requiredCapacity: String(requirement.requiredCapacity),
                        requiredFacilities: requirement.requiredFacilities,
                        roomType: requirement.roomType,
                        sameRoomPreferred: requirement.sameRoomPreferred,
                        sessionsPerWeek: String(requirement.sessionsPerWeek),
                        teachingGroupId: requirement.teachingGroupId ?? "",
                        weekPattern: requirement.weekPattern,
                      });
                    }}
                    variant="outline"
                  >Edit</Button>
                ) : null}
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
            </div>
          ))}
          {filteredManagedItems.length > managedPageSize ? (
            <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-100 pt-3">
              <p className="text-sm text-slate-500">
                Page {safeManagedPage + 1} of {managedPages} · {filteredManagedItems.length} records
              </p>
              <div className="flex gap-2">
                <Button
                  disabled={safeManagedPage === 0}
                  onClick={() => setManagedPage((page) => Math.max(0, page - 1))}
                  variant="outline"
                >
                  Previous
                </Button>
                <Button
                  disabled={safeManagedPage + 1 >= managedPages}
                  onClick={() => setManagedPage((page) => Math.min(managedPages - 1, page + 1))}
                  variant="outline"
                >
                  Next
                </Button>
              </div>
            </div>
          ) : null}
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

type ManagedResourceType =
  | "program"
  | "batch"
  | "section"
  | "instructor"
  | "room"
  | "timeslot"
  | "offering"
  | "requirement";

interface ManagedResource {
  active: boolean;
  id: string;
  label: string;
  type: ManagedResourceType;
  version: number;
}

function buildManagedItems(setup: typeof emptySetup): ManagedResource[] {
  return [
    ...setup.programs.map((item) => ({
      active: item.active,
      id: item.id,
      label: `Program · ${item.code} · ${item.name}`,
      type: "program" as const,
      version: item.version,
    })),
    ...setup.batches.map((item) => ({
      active: item.active,
      id: item.id,
      label: `Batch · ${item.code}`,
      type: "batch" as const,
      version: item.version,
    })),
    ...setup.sections.map((item) => ({
      active: item.active,
      id: item.id,
      label: `Section · ${item.code} · ${item.displayName}`,
      type: "section" as const,
      version: item.version,
    })),
    ...setup.instructors.map((item) => ({
      active: item.active,
      id: item.id,
      label: `Instructor · ${item.displayName}`,
      type: "instructor" as const,
      version: item.version,
    })),
    ...setup.rooms.map((item) => ({
      active: item.active,
      id: item.id,
      label: `Room · ${item.name}`,
      type: "room" as const,
      version: item.version,
    })),
    ...setup.timeslots.map((item) => ({
      active: item.active,
      id: item.id,
      label: `Timeslot · ${item.label}`,
      type: "timeslot" as const,
      version: item.version,
    })),
    ...setup.offerings.map((item) => ({
      active: item.status === "ACTIVE",
      id: item.id,
      label: `Offering · ${item.courseCode} · ${item.sectionName}`,
      type: "offering" as const,
      version: item.version,
    })),
    ...setup.requirements.map((item) => ({
      active: item.active,
      id: item.id,
      label: `Requirement · ${item.meetingType}`,
      type: "requirement" as const,
      version: item.version,
    })),
  ];
}

function saveAvailability(form: {
  availability: AuraAvailabilityType;
  reason: string;
  targetId: string;
  targetType: "INSTRUCTOR" | "ROOM" | "SECTION" | "STUDENT";
  timeslotId: string;
}, termId: string) {
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
  if (form.targetType === "STUDENT") {
    return upsertAuraStudentAvailability({
      ...common,
      studentUserId: form.targetId,
      termId,
    });
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

function BooleanInput({ checked, label, onChange }: { checked: boolean; label: string; onChange: (checked: boolean) => void }) {
  return (
    <label className="flex items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700">
      <input
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        type="checkbox"
      />
      {label}
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

function parseIntegerList(value: string, minimum: number, maximum: number) {
  if (!value.trim()) return [];
  return [...new Set(value.split(",").map((part) => Number(part.trim())))]
    .filter((entry) => Number.isInteger(entry) && entry >= minimum && entry <= maximum)
    .sort((left, right) => left - right);
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
const roomTypes = [
  "CLASSROOM",
  "LAB",
  "LECTURE_HALL",
  "LECTURE_ROOM",
  "COMPUTER_LAB",
  "SCIENCE_LAB",
  "SEMINAR_ROOM",
  "AUDITORIUM",
  "STUDIO",
  "WORKSHOP",
  "ONLINE",
  "OTHER",
];
const meetingTypes = [
  "LECTURE",
  "LAB",
  "TUTORIAL",
  "SEMINAR",
  "PROJECT",
  "WORKSHOP",
  "ONLINE",
  "HYBRID",
  "OTHER",
];
const weekPatterns = [
  "EVERY_WEEK",
  "ODD_WEEK",
  "EVEN_WEEK",
  "CUSTOM_WEEK_SET",
];
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
