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
  courseCodes: string[];
  roomIds: string[];
  sectionId: string;
  studentUserId: string;
  termId: string;
  timeslotIds: string[];
  versionId: string;
}
