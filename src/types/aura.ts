export interface PageResponse<T> {
  content: T[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AuraMutationResult {
  active: boolean;
  id: string;
  resourceType: string;
  updatedAt: string;
  version: number;
}

export interface AuraBuilding {
  active: boolean;
  code: string;
  id: string;
  minimumTransitionMinutes: number;
  name: string;
  version: number;
}

export interface AuraTeachingGroup {
  active: boolean;
  capacity: number | null;
  code: string;
  displayName: string;
  groupType: "LECTURE" | "LAB" | "TUTORIAL";
  id: string;
  offeringId: string;
  version: number;
}

export interface AuraOfferingConflict {
  active: boolean;
  id: string;
  leftOfferingId: string;
  reason: string;
  rightOfferingId: string;
  severity: "HARD" | "MEDIUM";
  source: string;
  termId: string;
  version: number;
}

export interface AuraTravelRule {
  active: boolean;
  difficulty: "NORMAL" | "DIFFICULT" | "IMPOSSIBLE";
  fromBuilding: string;
  id: string;
  minutes: number;
  toBuilding: string;
  version: number;
}

export interface AuraAuditEvent {
  action: string;
  actorName: string;
  actorUserId: string;
  correlationId: string;
  createdAt: string;
  id: string;
  metadata: Record<string, unknown>;
  result: string;
  summary: string;
  targetId: string | null;
  targetType: string;
  termId: string | null;
}

export interface AuraScopedTimetable {
  scopeId: string | null;
  scopeLabel: string;
  scopeType: string;
  sessions: AuraSession[];
  termId: string;
  versionId: string;
}

export interface AuraAnalytics {
  averageRepairDisruption: number;
  averageRoomCapacityUtilization: number;
  buildingUtilization: Record<string, number>;
  clashesByType: Record<string, number>;
  impossibleRequirements: number;
  instructorLoads: Record<string, number>;
  repairPlans: number;
  roomUtilization: Record<string, number>;
  sectionLoads: Record<string, number>;
  sessionsByDay: Record<string, number>;
  termId: string;
  unresolvedClashes: number;
  versionId: string;
}

export interface AuraRepairMove {
  affectedStudents: number;
  disruptionScore: number;
  originalRoomId: string;
  originalTimeslotId: string;
  proposedRoomId: string;
  proposedTimeslotId: string;
  sessionId: string;
}

export interface AuraRepairPlan {
  appliedAt: string | null;
  draftVersionId: string;
  expiresAt: string;
  feasible: boolean;
  id: string;
  impact: {
    clashesResolved: number;
    dayChanges: number;
    disruptionScore: number;
    instructorsAffected: number;
    newClashes: number;
    roomChanges: number;
    sectionsAffected: number;
    sessionsMoved: number;
    studentsAffected: number;
    timeChanges: number;
  };
  message: string;
  previewToken: string | null;
  proposedMoves: AuraRepairMove[];
  sourceVersionId: string;
  status: string;
  triggerId: string;
  triggerType: string;
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
  version: number;
}

export interface AuraProgram {
  active: boolean;
  code: string;
  departmentId: string;
  id: string;
  name: string;
  universityId: string;
  version: number;
}

export interface AuraBatch {
  active: boolean;
  admissionYear: number;
  code: string;
  id: string;
  programId: string;
  version: number;
}

export interface AuraSection {
  active: boolean;
  batchId: string;
  code: string;
  displayName: string;
  id: string;
  studentCount: number;
  version: number;
}

export interface AuraSetupReference {
  code: string;
  id: string;
  name: string;
  parentId: string;
}

export interface AuraSetupReferences {
  courses: AuraSetupReference[];
  departments: AuraSetupReference[];
  students: AuraSetupReference[];
  universityId: string;
}

export interface AuraStudentRegistration {
  courseCode: string;
  courseTitle: string;
  createdAt: string;
  equivalentOfferingId: string | null;
  homeSectionId: string | null;
  id: string;
  labGroupId: string | null;
  lectureGroupId: string | null;
  offeringId: string;
  registrationType: string;
  status: string;
  studentName: string;
  studentUserId: string;
  teachingSectionId: string | null;
  termId: string;
  tutorialGroupId: string | null;
  updatedAt: string;
  version: number;
}

export interface CreateAuraStudentRegistrationRequest {
  equivalentOfferingId?: string;
  homeSectionId?: string;
  labGroupId?: string;
  lectureGroupId?: string;
  offeringId: string;
  registrationType: string;
  studentUserId: string;
  teachingSectionId?: string;
  termId: string;
  tutorialGroupId?: string;
}

export interface AuraPersonalTimetableEntry {
  courseCode: string;
  courseTitle: string;
  dayOfWeek: number;
  endsAt: string;
  instructorName: string;
  offeringId: string;
  personalClash: boolean;
  registrationType: string;
  roomName: string;
  sectionName: string;
  sessionId: string;
  startsAt: string;
  weekPattern: "EVERY_WEEK" | "ODD_WEEK" | "EVEN_WEEK" | "CUSTOM_WEEK_SET";
  customWeeks: number[];
}

export interface AuraPersonalClash {
  leftSessionId: string;
  message: string;
  rightSessionId: string;
}

export interface AuraPersonalTimetable {
  clashes: AuraPersonalClash[];
  sessions: AuraPersonalTimetableEntry[];
  studentUserId: string;
  termId: string;
}

export interface AuraImportPreview {
  createdAt: string;
  fileFormat: "CSV" | "XLSX" | "XLS" | "PDF";
  headers: string[];
  id: string;
  importType: string;
  ocrRequired: boolean;
  originalFilename: string;
  rows: Array<Record<string, string>>;
  selectedSource: string;
  sources: string[];
  status: string;
  suggestedMapping: Record<string, string>;
  termId: string;
  totalRows: number;
  truncated: boolean;
  warnings: string[];
}

export interface AuraImportRowIssue {
  code: string;
  field: string | null;
  message: string;
  rowNumber: number;
  severity: "ERROR" | "WARNING";
}

export interface AuraImportValidation {
  acceptedRows: number;
  id: string;
  issues: AuraImportRowIssue[];
  mapping: Record<string, string>;
  normalizedPreview: Array<Record<string, string>>;
  rejectedRows: number;
  status: "PREVIEWED" | "VALIDATED";
}

export interface AuraImportApplyResult {
  acceptedRows: number;
  id: string;
  importType: string;
  message: string;
  rejectedRows: number;
  resultVersionId: string | null;
  status: "APPLIED";
}

export interface AuraRoom {
  active: boolean;
  building: string | null;
  capacity: number;
  facilities: string[];
  id: string;
  name: string;
  roomType: string;
  universityId: string;
  version: number;
}

export interface AuraTimeslot {
  active: boolean;
  dayOfWeek: number;
  endsAt: string;
  id: string;
  label: string;
  startsAt: string;
  universityId: string;
  version: number;
}

export type AuraAvailabilityType =
  | "AVAILABLE"
  | "UNAVAILABLE"
  | "AVOID"
  | "PREFERRED";

export interface AuraAvailability {
  availability: AuraAvailabilityType;
  dayOfWeek: number;
  endsAt: string;
  id: string;
  label: string;
  reason: string | null;
  startsAt: string;
  targetId: string;
  timeslotId: string;
}

export interface AuraInstructor {
  active: boolean;
  displayName: string;
  email: string | null;
  id: string;
  maxHoursPerWeek: number;
  universityId: string;
  userId: string | null;
  version: number;
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
  version: number;
}

export interface AuraMeetingRequirement {
  durationSlots: number;
  id: string;
  meetingType: string;
  notes: string | null;
  offeringId: string;
  requiredCapacity: number;
  requiredFacilities: string[];
  roomType: string;
  sessionsPerWeek: number;
  version: number;
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
  profile: AuraConstraintProfileName;
  randomSeed: number;
  candidateCount: number | null;
  terminationReason: string | null;
}

export type AuraConstraintProfileName =
  | "FAST_FEASIBLE"
  | "BALANCED"
  | "COMPACT"
  | "ROOM_EFFICIENT"
  | "INSTRUCTOR_FRIENDLY"
  | "QUALITY"
  | "REPAIR"
  | "WHAT_IF";

export interface AuraConstraintWeight {
  active: boolean;
  constraintLevel: "HARD" | "MEDIUM" | "SOFT";
  constraintName: string;
  customized: boolean;
  weight: number;
}

export interface AuraConstraintProfile {
  profile: AuraConstraintProfileName;
  termId: string;
  weights: AuraConstraintWeight[];
}

export interface AuraCapabilities {
  canManage: boolean;
  universityId: string;
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
  instructorId: string;
  locked: boolean;
  meetingRequirementId: string;
  offeringId: string;
  roomId: string;
  roomName: string;
  roomType: string;
  sectionId: string;
  sectionName: string;
  source: string;
  startsAt: string;
  timeslotId: string;
  versionId: string;
}

export interface AuraVersionSessionChange {
  afterRoomId: string | null;
  afterSessionId: string | null;
  afterTimeslotId: string | null;
  assignmentChanged: boolean;
  beforeRoomId: string | null;
  beforeSessionId: string | null;
  beforeTimeslotId: string | null;
  meetingRequirementId: string;
  occurrenceIndex: number;
}

export interface AuraVersionComparison {
  addedOccurrences: number;
  baseVersionId: string;
  changedOccurrences: number;
  changes: AuraVersionSessionChange[];
  comparedVersionId: string;
  removedOccurrences: number;
  totalOccurrences: number;
}

export interface AuraMovePreview {
  allowed: boolean;
  clashes: AuraClash[];
  message: string;
}

export interface AuraResolutionSuggestion {
  affectedStudents: number;
  appliedAt: string | null;
  changedSessions: number;
  explanation: string;
  hardClashesAdded: number;
  hardClashesRemoved: number;
  id: string;
  rankOrder: number;
  safe: boolean;
  suggestionType: string;
  targetCourseTitle: string | null;
  targetGroupId: string | null;
  targetGroupType: string | null;
  targetGroupCode: string | null;
  targetOfferingCode: string | null;
  targetOfferingId: string | null;
  targetSectionId: string | null;
  targetSectionName: string | null;
}

export interface AuraResolutionCase {
  caseType: string;
  createdAt: string;
  id: string;
  registrationId: string | null;
  reviewReason: string | null;
  status: string;
  studentName: string;
  studentUserId: string;
  suggestions: AuraResolutionSuggestion[];
  summary: string;
  termId: string;
  updatedAt: string;
  version: number;
}

export interface AuraWhatIfResult {
  affectedSessions: number;
  clashesAdded: number;
  clashesRemoved: number;
  completedAt: string | null;
  createdAt: string;
  id: string;
  recommendation: string;
  scenarioType: string;
  sourceVersionId: string;
  status: string;
  termId: string;
}

export interface AuraEmergencyRepair {
  affectedResourceId: string;
  affectedSessions: number;
  message: string;
  reassignedSessions: number;
  createdAt: string;
  draftVersionId: string | null;
  emergencyType: string;
  id: string;
  reason: string;
  sourceVersionId: string;
  status: string;
  termId: string;
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

export interface CreateAuraProgramRequest {
  code: string;
  departmentId: string;
  name: string;
  universityId: string;
}

export interface CreateAuraBatchRequest {
  admissionYear: number;
  code: string;
  programId: string;
}

export interface CreateAuraSectionRequest {
  batchId: string;
  code: string;
  displayName: string;
  studentCount: number;
}

export interface CreateAuraInstructorRequest {
  displayName: string;
  email?: string;
  maxHoursPerWeek: number;
  universityId: string;
  userId?: string;
}

export interface CreateAuraRoomRequest {
  building?: string;
  capacity: number;
  facilities?: string[];
  name: string;
  roomType: string;
  universityId: string;
}

export interface CreateAuraTimeslotRequest {
  dayOfWeek: number;
  endsAt: string;
  label: string;
  startsAt: string;
  universityId: string;
}

export interface CreateAuraOfferingRequest {
  courseId: string;
  expectedStudents: number;
  instructorId: string;
  sectionId: string;
  termId: string;
}

export interface CreateAuraMeetingRequirementRequest {
  durationSlots: number;
  meetingType: string;
  notes?: string;
  offeringId: string;
  requiredCapacity: number;
  requiredFacilities?: string[];
  roomType: string;
  sessionsPerWeek: number;
}

export interface AuraCalendarException {
  active: boolean;
  createdAt: string;
  endsOn: string;
  exceptionType: string;
  facility: string | null;
  id: string;
  instructorId: string | null;
  reason: string;
  roomId: string | null;
  sectionId: string | null;
  startsOn: string;
  termId: string;
  timeslotId: string | null;
  updatedAt: string;
  version: number;
}

export interface CreateAuraCalendarExceptionRequest {
  endsOn: string;
  exceptionType: string;
  facility?: string;
  instructorId?: string;
  reason: string;
  roomId?: string;
  sectionId?: string;
  startsOn: string;
  termId: string;
  timeslotId?: string;
}

export interface CreateAuraInstructorAvailabilityRequest {
  availability: AuraAvailabilityType;
  instructorId: string;
  reason?: string;
  timeslotId: string;
}

export interface CreateAuraRoomAvailabilityRequest {
  availability: AuraAvailabilityType;
  reason?: string;
  roomId: string;
  timeslotId: string;
}

export interface CreateAuraSectionAvailabilityRequest {
  availability: AuraAvailabilityType;
  reason?: string;
  sectionId: string;
  timeslotId: string;
}
