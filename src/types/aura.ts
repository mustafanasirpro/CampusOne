export interface PageResponse<T> {
  content: T[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AuraTerm {
  code: string;
  createdAt: string;
  endsOn: string;
  id: string;
  name: string;
  startsOn: string;
  status: string;
  universityId: string;
  updatedAt: string;
}

export interface AuraRoom {
  active: boolean;
  building: string | null;
  capacity: number;
  id: string;
  name: string;
  roomType: string;
  universityId: string;
}

export interface AuraTimeslot {
  active: boolean;
  dayOfWeek: number;
  endsAt: string;
  id: string;
  label: string;
  startsAt: string;
  universityId: string;
}

export interface AuraInstructor {
  active: boolean;
  displayName: string;
  email: string | null;
  id: string;
  maxHoursPerWeek: number;
  universityId: string;
  userId: string | null;
}

export interface AuraOffering {
  courseCode: string;
  courseId: string;
  courseTitle: string;
  expectedStudents: number;
  id: string;
  instructorId: string;
  instructorName: string;
  sectionId: string;
  sectionName: string;
  status: string;
  termId: string;
}

export interface AuraMeetingRequirement {
  durationSlots: number;
  id: string;
  meetingType: string;
  notes: string | null;
  offeringId: string;
  requiredCapacity: number;
  roomType: string;
  sessionsPerWeek: number;
}

export interface AuraReadinessIssue {
  code: string;
  message: string;
  severity: string;
  targetId: string | null;
  targetType: string;
}

export interface AuraReadiness {
  activeInstructors: number;
  activeOfferings: number;
  activeRooms: number;
  activeTimeslots: number;
  issues: AuraReadinessIssue[];
  meetingRequirements: number;
  ready: boolean;
  termId: string;
}

export interface AuraGenerationRun {
  cancelledAt: string | null;
  completedAt: string | null;
  createdAt: string;
  id: string;
  message: string | null;
  revisionId: string | null;
  score: string | null;
  startedAt: string | null;
  status: string;
  termId: string;
  terminationSeconds: number;
}

export interface AuraTimetableVersion {
  createdAt: string;
  generationRunId: string | null;
  id: string;
  notes: string | null;
  publishedAt: string | null;
  score: string | null;
  status: string;
  termId: string;
  versionNumber: number;
}

export interface AuraSession {
  courseCode: string;
  courseTitle: string;
  dayOfWeek: number;
  endsAt: string;
  id: string;
  instructorName: string;
  locked: boolean;
  roomName: string;
  sectionName: string;
  source: string;
  startsAt: string;
}

export interface AuraClash {
  clashType: string;
  detectedAt: string;
  id: string;
  message: string;
  primarySessionId: string | null;
  resolvedAt: string | null;
  secondarySessionId: string | null;
  severity: string;
  versionId: string;
}

export interface AuraMetrics {
  offerings: number;
  publishedVersions: number;
  rooms: number;
  scheduledSessions: number;
  termId: string;
  timeslots: number;
  unresolvedClashes: number;
  versions: number;
}

export interface CreateAuraTermRequest {
  code: string;
  endsOn: string;
  name: string;
  startsOn: string;
  universityId: string;
}
