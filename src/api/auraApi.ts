import { apiDownload, apiRequest } from "@/api/apiClient";
import type {
  AuraAvailability,
  AuraAnalytics,
  AuraAuditEvent,
  AuraBuilding,
  AuraCapabilities,
  AuraBatch,
  AuraCalendarException,
  AuraConstraintProfile,
  AuraConstraintProfileName,
  AuraClash,
  AuraGenerationRun,
  AuraInstructor,
  AuraImportPreview,
  AuraImportValidation,
  AuraMeetingRequirement,
  AuraMetrics,
  AuraOffering,
  AuraProgram,
  AuraReadiness,
  AuraResolutionCase,
  AuraWhatIfResult,
  AuraEmergencyRepair,
  AuraRoom,
  AuraSection,
  AuraSession,
  AuraTerm,
  AuraSetupReferences,
  AuraStudentRegistration,
  AuraPersonalTimetable,
  AuraTimeslot,
  AuraTimetableVersion,
  AuraVersionComparison,
  AuraMovePreview,
  AuraOfferingConflict,
  AuraRepairPlan,
  AuraScopedTimetable,
  AuraTeachingGroup,
  AuraTravelRule,
  CreateAuraInstructorAvailabilityRequest,
  CreateAuraCalendarExceptionRequest,
  CreateAuraInstructorRequest,
  CreateAuraBatchRequest,
  CreateAuraMeetingRequirementRequest,
  CreateAuraOfferingRequest,
  CreateAuraProgramRequest,
  CreateAuraRoomRequest,
  CreateAuraRoomAvailabilityRequest,
  CreateAuraSectionRequest,
  CreateAuraSectionAvailabilityRequest,
  CreateAuraStudentRegistrationRequest,
  CreateAuraTermRequest,
  CreateAuraTimeslotRequest,
  PageResponse,
} from "@/types/aura";

const adminAuraPath = "/admin/aura";
const auraPath = "/aura";

export function getAuraCapabilities(signal?: AbortSignal) {
  return apiRequest<AuraCapabilities>(`${auraPath}/capabilities`, { signal });
}

function queryString(parameters: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(parameters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  const value = query.toString();
  return value ? `?${value}` : "";
}

export function listAuraTerms(signal?: AbortSignal) {
  return apiRequest<PageResponse<AuraTerm>>(
    `${adminAuraPath}/terms${queryString({ size: 50 })}`,
    { signal },
  );
}

export function createAuraTerm(request: CreateAuraTermRequest) {
  return apiRequest<AuraTerm>(`${adminAuraPath}/terms`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function getAuraSetupReferences(signal?: AbortSignal) {
  return apiRequest<AuraSetupReferences>(`${adminAuraPath}/setup-references`, {
    signal,
  });
}

export function createAuraProgram(request: CreateAuraProgramRequest) {
  return apiRequest<AuraProgram>(`${adminAuraPath}/programs`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function listAuraPrograms(universityId: string, signal?: AbortSignal) {
  return apiRequest<AuraProgram[]>(
    `${adminAuraPath}/programs${queryString({ universityId })}`,
    { signal },
  );
}

export function createAuraBatch(request: CreateAuraBatchRequest) {
  return apiRequest<AuraBatch>(`${adminAuraPath}/batches`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function listAuraBatches(programId?: string, signal?: AbortSignal) {
  return apiRequest<AuraBatch[]>(
    `${adminAuraPath}/batches${queryString({ programId })}`,
    { signal },
  );
}

export function createAuraSection(request: CreateAuraSectionRequest) {
  return apiRequest<AuraSection>(`${adminAuraPath}/sections`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function listAuraSections(batchId?: string, signal?: AbortSignal) {
  return apiRequest<AuraSection[]>(
    `${adminAuraPath}/sections${queryString({ batchId })}`,
    { signal },
  );
}

export function createAuraInstructor(request: CreateAuraInstructorRequest) {
  return apiRequest<AuraInstructor>(`${adminAuraPath}/instructors`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function createAuraRoom(request: CreateAuraRoomRequest) {
  return apiRequest<AuraRoom>(`${adminAuraPath}/rooms`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function createAuraTimeslot(request: CreateAuraTimeslotRequest) {
  return apiRequest<AuraTimeslot>(`${adminAuraPath}/timeslots`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function createAuraOffering(request: CreateAuraOfferingRequest) {
  return apiRequest<AuraOffering>(`${adminAuraPath}/offerings`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function createAuraMeetingRequirement(
  request: CreateAuraMeetingRequirementRequest,
) {
  return apiRequest<AuraMeetingRequirement>(
    `${adminAuraPath}/meeting-requirements`,
    { body: JSON.stringify(request), method: "POST" },
  );
}

export function createAuraCalendarException(
  request: CreateAuraCalendarExceptionRequest,
) {
  return apiRequest<AuraCalendarException>(
    `${adminAuraPath}/calendar-exceptions`,
    { body: JSON.stringify(request), method: "POST" },
  );
}

export function listAuraCalendarExceptions(
  termId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraCalendarException[]>(
    `${adminAuraPath}/terms/${termId}/calendar-exceptions`,
    { signal },
  );
}

export function deactivateAuraCalendarException(
  exceptionId: string,
  version: number,
) {
  return apiRequest<void>(
    `${adminAuraPath}/calendar-exceptions/${exceptionId}${queryString({ version })}`,
    { method: "DELETE" },
  );
}

export function getAuraReadiness(termId: string, signal?: AbortSignal) {
  return apiRequest<AuraReadiness>(
    `${adminAuraPath}/terms/${termId}/readiness`,
    { signal },
  );
}

export function startAuraGeneration(
  termId: string,
  terminationSeconds = 30,
  profile: AuraConstraintProfileName = "BALANCED",
  randomSeed = 0,
) {
  return apiRequest<AuraGenerationRun>(
    `${adminAuraPath}/terms/${termId}/runs`,
    {
      body: JSON.stringify({
        profile,
        randomSeed,
        terminationSeconds,
      }),
      method: "POST",
    },
  );
}

export function getAuraConstraintProfile(
  termId: string,
  profile: AuraConstraintProfileName,
  signal?: AbortSignal,
) {
  return apiRequest<AuraConstraintProfile>(
    `${adminAuraPath}/terms/${termId}/constraint-profile${queryString({ profile })}`,
    { signal },
  );
}

export function replaceAuraConstraintProfile(
  termId: string,
  profile: AuraConstraintProfileName,
  weights: AuraConstraintProfile["weights"],
) {
  return apiRequest<AuraConstraintProfile>(
    `${adminAuraPath}/terms/${termId}/constraint-profile`,
    {
      body: JSON.stringify({ profile, weights }),
      method: "PUT",
    },
  );
}

export function getAuraGenerationRun(
  runId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraGenerationRun>(`${adminAuraPath}/runs/${runId}`, {
    signal,
  });
}

export function cancelAuraGeneration(runId: string) {
  return apiRequest<AuraGenerationRun>(
    `${adminAuraPath}/runs/${runId}/cancel`,
    { method: "POST" },
  );
}

export function listAuraVersions(termId: string, signal?: AbortSignal) {
  return apiRequest<AuraTimetableVersion[]>(
    `${adminAuraPath}/terms/${termId}/versions`,
    { signal },
  );
}

export function publishAuraVersion(versionId: string) {
  return apiRequest<AuraTimetableVersion>(
    `${adminAuraPath}/versions/${versionId}/publish`,
    { method: "POST" },
  );
}

export function cloneAuraVersion(versionId: string, notes?: string) {
  return apiRequest<AuraTimetableVersion>(
    `${adminAuraPath}/versions/${versionId}/clone`,
    { body: JSON.stringify({ notes }), method: "POST" },
  );
}

export function archiveAuraVersion(versionId: string) {
  return apiRequest<AuraTimetableVersion>(
    `${adminAuraPath}/versions/${versionId}/archive`,
    { method: "POST" },
  );
}

export function compareAuraVersions(versionId: string, otherVersionId: string) {
  return apiRequest<AuraVersionComparison>(
    `${adminAuraPath}/versions/${versionId}/compare/${otherVersionId}`,
  );
}

export async function downloadAuraVersion(versionId: string, format: string) {
  const result = await apiDownload(
    `${adminAuraPath}/versions/${versionId}/export${queryString({ format })}`,
  );
  const url = URL.createObjectURL(result.blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = result.filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

export function previewAuraSessionMove(
  sessionId: string,
  roomId: string,
  timeslotId: string,
) {
  return apiRequest<AuraMovePreview>(
    `${adminAuraPath}/sessions/${sessionId}/move-preview`,
    { body: JSON.stringify({ roomId, timeslotId }), method: "POST" },
  );
}

export function applyAuraSessionMove(
  sessionId: string,
  roomId: string,
  timeslotId: string,
  reason: string,
) {
  return apiRequest<AuraSession>(
    `${adminAuraPath}/sessions/${sessionId}/move`,
    { body: JSON.stringify({ roomId, timeslotId, reason }), method: "PATCH" },
  );
}

export function previewAuraSessionSwap(
  sessionId: string,
  otherSessionId: string,
) {
  return apiRequest<AuraMovePreview>(
    `${adminAuraPath}/sessions/${sessionId}/swap-preview`,
    { body: JSON.stringify({ otherSessionId }), method: "POST" },
  );
}

export function applyAuraSessionSwap(
  sessionId: string,
  otherSessionId: string,
  reason: string,
) {
  return apiRequest<AuraSession[]>(
    `${adminAuraPath}/sessions/${sessionId}/swap`,
    { body: JSON.stringify({ otherSessionId, reason }), method: "PATCH" },
  );
}

export function setAuraSessionPinned(
  sessionId: string,
  pinned: boolean,
  reason?: string,
) {
  return apiRequest<AuraSession>(`${adminAuraPath}/sessions/${sessionId}/pin`, {
    body: JSON.stringify({ pinned, reason }),
    method: "PATCH",
  });
}

export function listAuraSessions(
  versionId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraSession[]>(
    `${adminAuraPath}/versions/${versionId}/sessions`,
    { signal },
  );
}

export function listAuraClashes(versionId: string, signal?: AbortSignal) {
  return apiRequest<AuraClash[]>(
    `${adminAuraPath}/versions/${versionId}/clashes`,
    { signal },
  );
}

export function getAuraMetrics(termId: string, signal?: AbortSignal) {
  return apiRequest<AuraMetrics>(
    `${adminAuraPath}/terms/${termId}/metrics`,
    { signal },
  );
}

export function listAuraRooms(universityId: string, signal?: AbortSignal) {
  return apiRequest<AuraRoom[]>(
    `${adminAuraPath}/rooms${queryString({ universityId })}`,
    { signal },
  );
}

export function listAuraTimeslots(
  universityId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraTimeslot[]>(
    `${adminAuraPath}/timeslots${queryString({ universityId })}`,
    { signal },
  );
}

export function upsertAuraInstructorAvailability(
  request: CreateAuraInstructorAvailabilityRequest,
) {
  return apiRequest<AuraAvailability>(
    `${adminAuraPath}/instructor-availability`,
    {
      body: JSON.stringify(request),
      method: "POST",
    },
  );
}

export function listAuraInstructorAvailability(
  instructorId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraAvailability[]>(
    `${adminAuraPath}/instructors/${instructorId}/availability`,
    { signal },
  );
}

export function upsertAuraRoomAvailability(
  request: CreateAuraRoomAvailabilityRequest,
) {
  return apiRequest<AuraAvailability>(
    `${adminAuraPath}/room-availability`,
    {
      body: JSON.stringify(request),
      method: "POST",
    },
  );
}

export function listAuraRoomAvailability(
  roomId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraAvailability[]>(
    `${adminAuraPath}/rooms/${roomId}/availability`,
    { signal },
  );
}

export function upsertAuraSectionAvailability(
  request: CreateAuraSectionAvailabilityRequest,
) {
  return apiRequest<AuraAvailability>(
    `${adminAuraPath}/section-availability`,
    {
      body: JSON.stringify(request),
      method: "POST",
    },
  );
}

export function listAuraSectionAvailability(
  sectionId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraAvailability[]>(
    `${adminAuraPath}/sections/${sectionId}/availability`,
    { signal },
  );
}

export function listAuraInstructors(
  universityId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraInstructor[]>(
    `${adminAuraPath}/instructors${queryString({ universityId })}`,
    { signal },
  );
}

export function listAuraOfferings(termId: string, signal?: AbortSignal) {
  return apiRequest<AuraOffering[]>(
    `${adminAuraPath}/terms/${termId}/offerings`,
    { signal },
  );
}

export function listAuraMeetingRequirements(
  offeringId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraMeetingRequirement[]>(
    `${adminAuraPath}/offerings/${offeringId}/meeting-requirements`,
    { signal },
  );
}

export function createAuraStudentRegistration(
  request: CreateAuraStudentRegistrationRequest,
) {
  return apiRequest<AuraStudentRegistration>(`${adminAuraPath}/registrations`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function listAuraStudentRegistrations(
  termId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraStudentRegistration[]>(
    `${adminAuraPath}/terms/${termId}/registrations`,
    { signal },
  );
}

export function getMyAuraTimetable(termId: string, signal?: AbortSignal) {
  return apiRequest<AuraPersonalTimetable>(
    `${auraPath}/me/timetable${queryString({ termId })}`,
    { signal },
  );
}

export async function downloadMyAuraTimetableCalendar(termId: string) {
  const result = await apiDownload(
    `${auraPath}/me/timetable.ics${queryString({ termId })}`,
  );
  const url = URL.createObjectURL(result.blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = result.filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

export function listAvailableAuraTerms(signal?: AbortSignal) {
  return apiRequest<AuraTerm[]>(`${auraPath}/terms`, { signal });
}

export function listMyAuraRegistrations(
  termId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraStudentRegistration[]>(
    `${auraPath}/me/registrations${queryString({ termId })}`,
    { signal },
  );
}

export function previewAuraImport(
  termId: string,
  importType: string,
  file: File,
  source?: string,
) {
  const body = new FormData();
  body.append("file", file);
  return apiRequest<AuraImportPreview>(
    `${adminAuraPath}/terms/${termId}/imports/preview${queryString({
      importType,
      source,
    })}`,
    { body, method: "POST" },
  );
}

export function validateAuraImport(
  jobId: string,
  mapping: Record<string, string>,
  saveAsProfile?: string,
) {
  return apiRequest<AuraImportValidation>(
    `${adminAuraPath}/imports/${jobId}/validate`,
    {
      body: JSON.stringify({ mapping, saveAsProfile: saveAsProfile || null }),
      method: "POST",
    },
  );
}

export function applyAuraImport(jobId: string) {
  return apiRequest<import("@/types/aura").AuraImportApplyResult>(
    `${adminAuraPath}/imports/${jobId}/apply`,
    { method: "POST" },
  );
}

export function requestAuraResolution(
  termId: string,
  registrationId: string,
  caseType: string,
  summary: string,
) {
  return apiRequest<AuraResolutionCase>(`${auraPath}/me/resolution-cases`, {
    body: JSON.stringify({ termId, registrationId, caseType, summary }),
    method: "POST",
  });
}

export function listMyAuraResolutionCases(
  termId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraResolutionCase[]>(
    `${auraPath}/me/resolution-cases${queryString({ termId })}`,
    { signal },
  );
}

export function listAuraResolutionCases(
  termId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraResolutionCase[]>(
    `${adminAuraPath}/terms/${termId}/resolution-cases`,
    { signal },
  );
}

export function analyzeAuraResolutionCase(caseId: string) {
  return apiRequest<AuraResolutionCase>(
    `${adminAuraPath}/resolution-cases/${caseId}/analyze`,
    { method: "POST" },
  );
}

function resolutionDecision(
  action: "approve" | "reject" | "apply",
  caseId: string,
  suggestionId: string | undefined,
  reason: string,
  version: number,
) {
  return apiRequest<AuraResolutionCase>(
    `${adminAuraPath}/resolution-cases/${caseId}/${action}`,
    { body: JSON.stringify({ suggestionId, reason, version }), method: "POST" },
  );
}

export const approveAuraResolutionCase = (
  caseId: string,
  suggestionId: string,
  reason: string,
  version: number,
) => resolutionDecision("approve", caseId, suggestionId, reason, version);

export const rejectAuraResolutionCase = (
  caseId: string,
  reason: string,
  version: number,
) => resolutionDecision("reject", caseId, undefined, reason, version);

export const applyAuraResolutionCase = (
  caseId: string,
  suggestionId: string,
  reason: string,
  version: number,
) => resolutionDecision("apply", caseId, suggestionId, reason, version);

export function runAuraWhatIf(
  termId: string,
  sourceVersionId: string,
  scenarioType: string,
  resourceId: string,
) {
  return apiRequest<AuraWhatIfResult>(`${adminAuraPath}/terms/${termId}/what-if`, {
    body: JSON.stringify({
      scenarioInput: { resourceId },
      scenarioType,
      sourceVersionId,
    }),
    method: "POST",
  });
}

export function listAuraWhatIf(termId: string, signal?: AbortSignal) {
  return apiRequest<AuraWhatIfResult[]>(
    `${adminAuraPath}/terms/${termId}/what-if`,
    { signal },
  );
}

export function createAuraEmergencyRepair(
  termId: string,
  sourceVersionId: string,
  emergencyType: string,
  affectedResourceId: string,
  reason: string,
) {
  return apiRequest<AuraEmergencyRepair>(
    `${adminAuraPath}/terms/${termId}/emergency-repairs`,
    {
      body: JSON.stringify({
        affectedResourceId,
        emergencyType,
        reason,
        sourceVersionId,
      }),
      method: "POST",
    },
  );
}

export function listAuraEmergencyRepairs(
  termId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraEmergencyRepair[]>(
    `${adminAuraPath}/terms/${termId}/emergency-repairs`,
    { signal },
  );
}

export function listAuraBuildings(signal?: AbortSignal) {
  return apiRequest<AuraBuilding[]>(`${adminAuraPath}/buildings`, { signal });
}

export function createAuraBuilding(request: {
  code: string;
  minimumTransitionMinutes: number;
  name: string;
}) {
  return apiRequest<AuraBuilding>(`${adminAuraPath}/buildings`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function setAuraResourceActive(
  resourceType: string,
  resourceId: string,
  active: boolean,
  version: number,
  reason: string,
) {
  return apiRequest<import("@/types/aura").AuraMutationResult>(
    `${adminAuraPath}/${resourceType}/${resourceId}/active-state`,
    {
      body: JSON.stringify({ active, reason, version }),
      method: "PATCH",
    },
  );
}

export function updateAuraBuilding(
  buildingId: string,
  request: Omit<AuraBuilding, "id">,
) {
  return apiRequest<import("@/types/aura").AuraMutationResult>(
    `${adminAuraPath}/buildings/${buildingId}`,
    { body: JSON.stringify(request), method: "PATCH" },
  );
}

export function listAuraTeachingGroups(termId: string, signal?: AbortSignal) {
  return apiRequest<AuraTeachingGroup[]>(
    `${adminAuraPath}/terms/${termId}/teaching-groups`,
    { signal },
  );
}

export function createAuraTeachingGroup(request: {
  active: boolean;
  capacity: number | null;
  code: string;
  displayName: string;
  groupType: AuraTeachingGroup["groupType"];
  offeringId: string;
}) {
  return apiRequest<AuraTeachingGroup>(`${adminAuraPath}/teaching-groups`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function updateAuraTeachingGroup(
  groupId: string,
  request: Omit<AuraTeachingGroup, "id" | "termId">,
) {
  return apiRequest<AuraTeachingGroup>(`${adminAuraPath}/teaching-groups/${groupId}`, {
    body: JSON.stringify(request),
    method: "PATCH",
  });
}

export function listAuraOfferingConflicts(
  termId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraOfferingConflict[]>(
    `${adminAuraPath}/terms/${termId}/offering-conflicts`,
    { signal },
  );
}

export function createAuraOfferingConflict(request: {
  active: boolean;
  leftOfferingId: string;
  reason: string;
  rightOfferingId: string;
  severity: AuraOfferingConflict["severity"];
  source: string;
  termId: string;
}) {
  return apiRequest<AuraOfferingConflict>(
    `${adminAuraPath}/offering-conflicts`,
    { body: JSON.stringify(request), method: "POST" },
  );
}

export function updateAuraOfferingConflict(
  conflictId: string,
  request: Omit<AuraOfferingConflict, "id">,
) {
  return apiRequest<AuraOfferingConflict>(
    `${adminAuraPath}/offering-conflicts/${conflictId}`,
    { body: JSON.stringify(request), method: "PATCH" },
  );
}

export function listAuraTravelRules(signal?: AbortSignal) {
  return apiRequest<AuraTravelRule[]>(`${adminAuraPath}/travel-rules`, {
    signal,
  });
}

export function createAuraTravelRule(request: {
  active: boolean;
  difficulty: AuraTravelRule["difficulty"];
  fromBuilding: string;
  minutes: number;
  toBuilding: string;
}) {
  return apiRequest<AuraTravelRule>(`${adminAuraPath}/travel-rules`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function updateAuraTravelRule(
  ruleId: string,
  request: Omit<AuraTravelRule, "id">,
) {
  return apiRequest<AuraTravelRule>(`${adminAuraPath}/travel-rules/${ruleId}`, {
    body: JSON.stringify(request),
    method: "PATCH",
  });
}

export function listAuraAuditEvents(
  termId?: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraAuditEvent[]>(
    `${adminAuraPath}/audit${queryString({ termId, size: 25 })}`,
    { signal },
  );
}

export function getAuraAnalytics(
  termId: string,
  versionId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraAnalytics>(
    `${adminAuraPath}/terms/${termId}/analytics${queryString({ versionId })}`,
    { signal },
  );
}

export function getAuraScopedTimetable(
  versionId: string,
  scope = "WEEK",
  scopeId?: string,
  dayOfWeek?: number,
  signal?: AbortSignal,
) {
  return apiRequest<AuraScopedTimetable>(
    `${adminAuraPath}/versions/${versionId}/timetable-view${queryString({
      dayOfWeek,
      scope,
      scopeId,
    })}`,
    { signal },
  );
}

export function previewAuraRepair(
  versionId: string,
  request: { clashId?: string; reason: string; sessionId?: string },
) {
  return apiRequest<AuraRepairPlan>(
    `${adminAuraPath}/versions/${versionId}/repair-preview`,
    { body: JSON.stringify(request), method: "POST" },
  );
}

export function applyAuraRepair(planId: string, previewToken: string) {
  return apiRequest<AuraRepairPlan>(
    `${adminAuraPath}/repair-plans/${planId}/apply`,
    { body: JSON.stringify({ previewToken }), method: "POST" },
  );
}

export function getMyAuraInstructorTimetable(
  termId: string,
  signal?: AbortSignal,
) {
  return apiRequest<AuraScopedTimetable>(
    `${auraPath}/me/instructor-timetable${queryString({ termId })}`,
    { signal },
  );
}
