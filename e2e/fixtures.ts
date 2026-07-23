export const e2eUsers = {
  admin: {
    email: "aura-admin-e2e@campusone.test",
    fullName: "AURA E2E Administrator",
    password: "AuraE2e2026!",
  },
  student: {
    email: "aura-student-e2e@campusone.test",
    fullName: "AURA E2E Student",
    password: "AuraE2e2026!",
  },
} as const;

export interface AuraE2eFixture {
  courseIds: string[];
  courseCodes: string[];
  departmentId: string;
  instructorId: string;
  offeringIds: string[];
  programId: string;
  roomIds: string[];
  sectionId: string;
  sessionCourseCodes: string[];
  sessionIds: string[];
  sessionRoomIds: string[];
  sessionTimeslotIds: string[];
  studentUserId: string;
  termId: string;
  timeslotIds: string[];
  versionId: string;
}
