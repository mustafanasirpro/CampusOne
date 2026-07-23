import { execFileSync } from "node:child_process";
import { mkdir, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

import { e2eUsers, type AuraE2eFixture } from "./fixtures";

const apiBase = process.env.PLAYWRIGHT_BACKEND_URL
  ?? "http://127.0.0.1:18083/api/v1";
const universityId = "8d9f5b4a-8f3c-4e6b-9a7d-1c2e3f4a5b60";
const departmentId = "7c8e4a3b-6d2f-4b5a-8c9e-0f1a2b3c4d50";

export default async function globalSetup() {
  await register(e2eUsers.admin);
  await register(e2eUsers.student);

  const psql = process.env.PLAYWRIGHT_PSQL
    ?? "C:\\Program Files\\PostgreSQL\\17\\bin\\psql.exe";
  const database = process.env.PLAYWRIGHT_DATABASE ?? "campusone_aura_e2e";
  const port = process.env.PLAYWRIGHT_POSTGRES_PORT ?? "55440";
  const promoteSql = `
    INSERT INTO user_roles (user_id, role_id)
    SELECT users.id, roles.id
    FROM users CROSS JOIN roles
    WHERE LOWER(users.email) = LOWER('${e2eUsers.admin.email}')
      AND roles.name = 'ADMIN'
    ON CONFLICT DO NOTHING;
    SELECT id FROM users
    WHERE LOWER(email) = LOWER('${e2eUsers.student.email}');`;
  const userRows = execFileSync(psql, [
    "-h", "127.0.0.1", "-p", port, "-U", "postgres", "-d", database,
    "-v", "ON_ERROR_STOP=1", "-Atc", promoteSql,
  ], { encoding: "utf8" }).trim().split(/\r?\n/);
  const studentUserId = userRows.at(-1);
  if (!studentUserId) throw new Error("The local AURA E2E student was not created.");

  const session = await post<{ accessToken: string }>("/auth/login", {
    email: e2eUsers.admin.email,
    password: e2eUsers.admin.password,
  });
  const adminUserId = execFileSync(psql, [
    "-h", "127.0.0.1", "-p", port, "-U", "postgres", "-d", database,
    "-v", "ON_ERROR_STOP=1", "-Atc",
    `SELECT id FROM users WHERE LOWER(email) = LOWER('${e2eUsers.admin.email}');`,
  ], { encoding: "utf8" }).trim();
  if (!adminUserId) throw new Error("The local AURA E2E administrator was not created.");
  const headers = { Authorization: `Bearer ${session.accessToken}` };
  const references = await get<SetupReferences>(
    "/admin/aura/setup-references",
    headers,
  );
  if (references.universityId !== universityId || references.courses.length < 2) {
    throw new Error("AURA E2E reference data is incomplete.");
  }

  const term = await post<Resource>("/admin/aura/terms", {
    universityId,
    code: "E2E-FALL-2026",
    name: "E2E Fall 2026",
    startsOn: "2026-09-01",
    endsOn: "2026-12-31",
  }, headers);
  const program = await post<Resource>("/admin/aura/programs", {
    universityId,
    departmentId,
    code: "E2E-CS",
    name: "E2E Computer Science",
  }, headers);
  const batch = await post<Resource>("/admin/aura/batches", {
    programId: program.id,
    code: "E2E-2024",
    admissionYear: 2024,
  }, headers);
  const section = await post<Resource>("/admin/aura/sections", {
    batchId: batch.id,
    code: "E2E-A",
    displayName: "E2E Section A",
    studentCount: 30,
  }, headers);
  const instructor = await post<Resource>("/admin/aura/instructors", {
    universityId,
    userId: adminUserId,
    displayName: "E2E Instructor",
    email: "aura-instructor-e2e@campusone.test",
    maxHoursPerWeek: 20,
  }, headers);
  const rooms = await Promise.all([
    ["E2E-C101", "E2E Room C101"],
    ["E2E-C102", "E2E Room C102"],
  ].map(([, name]) => post<Resource>("/admin/aura/rooms", {
    universityId,
    building: "E2E Building",
    name,
    capacity: 50,
    roomType: "CLASSROOM",
    facilities: ["PROJECTOR"],
  }, headers)));
  const timeslots = await Promise.all([
    ["09:00:00", "10:00:00", "Monday 09:00"],
    ["10:00:00", "11:00:00", "Monday 10:00"],
    ["11:00:00", "12:00:00", "Monday 11:00"],
  ].map(([startsAt, endsAt, label]) => post<Resource>(
    "/admin/aura/timeslots",
    { universityId, dayOfWeek: 1, startsAt, endsAt, label },
    headers,
  )));

  const offerings: Resource[] = [];
  for (const course of references.courses.slice(0, 3)) {
    const offering = await post<Resource>("/admin/aura/offerings", {
      termId: term.id,
      courseId: course.id,
      sectionId: section.id,
      instructorId: instructor.id,
      expectedStudents: 30,
    }, headers);
    offerings.push(offering);
    await post<Resource>("/admin/aura/meeting-requirements", {
      offeringId: offering.id,
      meetingType: "LECTURE",
      sessionsPerWeek: 1,
      durationSlots: 1,
      roomType: "CLASSROOM",
      requiredCapacity: 30,
      requiredFacilities: ["PROJECTOR"],
      notes: "Playwright deterministic fixture",
    }, headers);
  }
  await post<Resource>("/admin/aura/registrations", {
    termId: term.id,
    studentUserId,
    offeringId: offerings[0].id,
    registrationType: "PRIMARY_SECTION",
    homeSectionId: section.id,
    teachingSectionId: section.id,
  }, headers);

  const readiness = await get<{ ready: boolean; issues: Array<{ severity: string }> }>(
    `/admin/aura/terms/${term.id}/readiness`, headers,
  );
  if (!readiness.ready) {
    throw new Error(`AURA E2E fixture is not ready: ${JSON.stringify(readiness.issues)}`);
  }
  const run = await post<{ id: string }>(`/admin/aura/terms/${term.id}/runs`, {
    terminationSeconds: 2,
    notes: "Playwright fixture generation",
    profile: "BALANCED",
    randomSeed: 20260719,
  }, headers);
  const completed = await pollGeneration(run.id, headers);
  if (completed.status !== "COMPLETED") {
    throw new Error(`AURA fixture generation failed: ${completed.message ?? completed.status}`);
  }
  const versions = await get<Array<Resource>>(
    `/admin/aura/terms/${term.id}/versions`, headers,
  );
  const version = versions[0];
  if (!version) throw new Error("AURA generation did not create a timetable version.");
  await post(`/admin/aura/versions/${version.id}/publish`, {}, headers);
  const sessions = await get<Array<SessionResource>>(
    `/admin/aura/versions/${version.id}/sessions`, headers,
  );
  if (!sessions.length) throw new Error("AURA generation did not create timetable sessions.");

  const fixture: AuraE2eFixture = {
    courseIds: references.courses.slice(0, 3).map((course) => course.id),
    courseCodes: references.courses.slice(0, 3).map((course) => course.code),
    departmentId,
    instructorId: instructor.id,
    offeringIds: offerings.map((offering) => offering.id),
    programId: program.id,
    roomIds: rooms.map((room) => room.id),
    sectionId: section.id,
    sessionCourseCodes: sessions.map((session) => session.courseCode),
    sessionIds: sessions.map((session) => session.id),
    sessionRoomIds: sessions.map((session) => session.roomId),
    sessionTimeslotIds: sessions.map((session) => session.timeslotId),
    studentUserId,
    termId: term.id,
    timeslotIds: timeslots.map((slot) => slot.id),
    versionId: version.id,
  };
  const repositoryRoot = path.resolve(
    path.dirname(fileURLToPath(import.meta.url)),
    "..",
  );
  const resultDirectory = path.join(repositoryRoot, "test-results");
  await mkdir(resultDirectory, { recursive: true });
  await writeFile(
    path.join(resultDirectory, "aura-e2e-fixture.json"),
    JSON.stringify(fixture, null, 2),
    "utf8",
  );
}

async function register(user: typeof e2eUsers.admin | typeof e2eUsers.student) {
  await post("/auth/register", {
    fullName: user.fullName,
    email: user.email,
    password: user.password,
    universityId,
    departmentId,
    semester: 5,
  });
}

async function pollGeneration(
  runId: string,
  headers: Record<string, string>,
) {
  const deadline = Date.now() + 30_000;
  let run: { status: string; message?: string };
  do {
    await new Promise((resolve) => setTimeout(resolve, 400));
    run = await get(`/admin/aura/runs/${runId}`, headers);
  } while (["QUEUED", "RUNNING"].includes(run.status) && Date.now() < deadline);
  return run;
}

async function get<T>(pathName: string, headers: Record<string, string> = {}) {
  return request<T>(pathName, { headers });
}

async function post<T = unknown>(
  pathName: string,
  body: unknown,
  headers: Record<string, string> = {},
) {
  return request<T>(pathName, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...headers },
    body: JSON.stringify(body),
  });
}

async function request<T>(pathName: string, init: RequestInit) {
  const response = await fetch(`${apiBase}${pathName}`, init);
  if (!response.ok) {
    throw new Error(`${init.method ?? "GET"} ${pathName} returned ${response.status}: ${await response.text()}`);
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

interface Resource { id: string }
interface SessionResource extends Resource { courseCode: string; roomId: string; timeslotId: string }
interface SetupReferences {
  universityId: string;
  courses: Array<{ id: string; code: string }>;
}
