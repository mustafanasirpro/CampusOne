import { apiRequest } from "@/api/apiClient";
import type {
  AuraAvailability,
  AuraClash,
  AuraGenerationRun,
  AuraInstructor,
  AuraMeetingRequirement,
  AuraMetrics,
  AuraOffering,
  AuraReadiness,
  AuraRoom,
  AuraSession,
  AuraTerm,
  AuraTimeslot,
  AuraTimetableVersion,
  CreateAuraInstructorAvailabilityRequest,
  CreateAuraRoomAvailabilityRequest,
  CreateAuraSectionAvailabilityRequest,
  CreateAuraTermRequest,
  PageResponse,
} from "@/types/aura";

const adminAuraPath = "/admin/aura";

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

export function getAuraReadiness(termId: string, signal?: AbortSignal) {
  return apiRequest<AuraReadiness>(
    `${adminAuraPath}/terms/${termId}/readiness`,
    { signal },
  );
}

export function startAuraGeneration(termId: string, terminationSeconds = 30) {
  return apiRequest<AuraGenerationRun>(
    `${adminAuraPath}/terms/${termId}/runs`,
    {
      body: JSON.stringify({ terminationSeconds }),
      method: "POST",
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
