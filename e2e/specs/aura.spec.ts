import { readFile } from "node:fs/promises";
import path from "node:path";

import { expect, test, type Page } from "@playwright/test";

import { e2eUsers, type AuraE2eFixture } from "../fixtures";

const backend = process.env.PLAYWRIGHT_BACKEND_URL
  ?? "http://127.0.0.1:18083/api/v1";
let fixture: AuraE2eFixture;

test.describe.configure({ mode: "serial" });

test.beforeAll(async () => {
  fixture = JSON.parse(await readFile(
    path.resolve("test-results/aura-e2e-fixture.json"),
    "utf8",
  )) as AuraE2eFixture;
});

test("unauthenticated AURA administration redirects to sign in", async ({ page }) => {
  await page.goto("/admin/aura");
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("heading", { name: "Log in to CampusOne" }))
    .toBeVisible();
});

test("student is denied the AURA administration workbench", async ({ page }) => {
  await login(page, e2eUsers.student.email, e2eUsers.student.password);
  await page.goto("/admin/aura");
  await expect(page.getByText("You do not have access to AURA administration"))
    .toBeVisible();
});

test("admin opens the generated timetable and readiness workbench", async ({ page }) => {
  await login(page, e2eUsers.admin.email, e2eUsers.admin.password);
  await page.goto("/admin/aura");
  await selectFixtureTerm(page);

  await expect(page.getByRole("heading", { name: "AURA Timetable Generator" }))
    .toBeVisible();
  await expect(page.getByLabel("Academic term")).toHaveValue(fixture.termId);
  await expect(page.getByText(fixture.courseCodes[0], { exact: true })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Generation profile" }))
    .toBeVisible();
  await expect(page.getByText("PUBLISHED", { exact: true })).toBeVisible();
});

test("admin persists a room-efficient generation profile", async ({ page }, testInfo) => {
  await login(page, e2eUsers.admin.email, e2eUsers.admin.password);
  await page.goto("/admin/aura");
  await selectFixtureTerm(page);

  await page.getByLabel("Generation profile").selectOption("ROOM_EFFICIENT");
  const sameRoomWeight = page.getByLabel("Same room preference weight");
  const savedWeight = testInfo.project.name === "chromium-mobile" ? "10" : "9";
  await expect(sameRoomWeight).toBeVisible();
  await sameRoomWeight.fill(savedWeight);
  const saved = page.waitForResponse((response) =>
    response.request().method() === "PUT"
      && response.url().includes("/constraint-profile"),
  );
  await page.getByRole("button", { name: "Save profile settings" }).click();
  expect((await saved).status()).toBe(200);

  await page.getByLabel("Generation profile").selectOption("BALANCED");
  await page.getByLabel("Generation profile").selectOption("ROOM_EFFICIENT");
  await expect(sameRoomWeight).toHaveValue(savedWeight);
});

test("published version remains immutable and exports valid CSV", async ({ page }) => {
  await login(page, e2eUsers.admin.email, e2eUsers.admin.password);
  await page.goto("/admin/aura");
  await selectFixtureTerm(page);
  await selectVersionWithStatus(page, "PUBLISHED");
  await expect(page.getByText("PUBLISHED", { exact: true })).toBeVisible();

  await expect(page.getByRole("button", { name: "Archive draft" })).toBeDisabled();
  await expect(page.getByRole("button", { name: "Apply safe move" })).toBeDisabled();
  const downloadPromise = page.waitForEvent("download");
  await page.getByRole("button", { name: "CSV", exact: true }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toMatch(/\.csv$/i);
});

test("draft move preview, apply, pin, unpin, and comparison are connected", async ({ page }) => {
  await login(page, e2eUsers.admin.email, e2eUsers.admin.password);
  await page.goto("/admin/aura");
  await selectFixtureTerm(page);
  await selectVersionWithStatus(page, "PUBLISHED");

  await page.getByRole("button", { name: "Clone to draft" }).click();
  await expect(page.getByRole("button", { name: "Archive draft" })).toBeEnabled();
  const sessionSelect = page.getByLabel("Session", { exact: true });
  await expect(sessionSelect.locator("option")).toHaveCount(fixture.offeringIds.length + 1);
  const draftSessionId = await sessionSelect.locator("option")
    .filter({ hasText: fixture.sessionCourseCodes[0] }).getAttribute("value");
  expect(draftSessionId).toBeTruthy();
  await sessionSelect.selectOption(draftSessionId!);
  const alternateRoomId = fixture.roomIds.find((roomId) => roomId !== fixture.sessionRoomIds[0]);
  expect(alternateRoomId).toBeTruthy();
  await page.getByLabel("New room").selectOption(alternateRoomId!);
  await page.getByLabel("New timeslot").selectOption(fixture.sessionTimeslotIds[0]);
  await page.getByLabel("Change reason", { exact: true }).first()
    .fill("Automated browser verification");
  await page.getByRole("button", { name: "Preview move" }).click();
  await expect(page.getByRole("button", { name: "Apply safe move" })).toBeEnabled();
  await page.getByRole("button", { name: "Apply safe move" }).click();

  const firstSession = page.getByLabel("First session");
  await firstSession.selectOption({ index: 1 });
  await page.getByRole("button", { name: "Pin first session" }).click();
  await expect(page.getByRole("button", { name: "Unpin first session" }))
    .toBeVisible();
  await page.getByRole("button", { name: "Unpin first session" }).click();
  await expect(page.getByRole("button", { name: "Pin first session" }))
    .toBeVisible();

  await page.getByLabel("Compare with").selectOption({ index: 1 });
  await page.getByRole("button", { name: "Compare", exact: true }).click();
  await expect(page.getByText(/occurrences changed/)).toBeVisible();
});

test("student sees only their published personal timetable", async ({ page }) => {
  await login(page, e2eUsers.student.email, e2eUsers.student.password);
  await page.goto("/timetable");

  await expect(page.getByRole("heading", { name: "My timetable" })).toBeVisible();
  await expect(page.getByLabel("Academic term")).toHaveValue(fixture.termId);
  await expect(page.getByText(new RegExp(fixture.courseCodes[0]))).toBeVisible();
});

test("linked instructor sees only their published teaching timetable", async ({ page }) => {
  await login(page, e2eUsers.admin.email, e2eUsers.admin.password);
  await page.goto("/instructor-timetable");

  await expect(page.getByRole("heading", { name: "Teaching timetable", exact: true })).toBeVisible();
  await expect(page.getByLabel("Academic term")).toHaveValue(fixture.termId);
  await expect(page.getByText(new RegExp(fixture.courseCodes[0]))).toBeVisible();
});

test("administrator can query every university-scoped timetable dimension", async ({ page }) => {
  await login(page, e2eUsers.admin.email, e2eUsers.admin.password);
  const token = await page.evaluate(() => localStorage.getItem("campusone.accessToken"));
  expect(token).toBeTruthy();
  const headers = { Authorization: `Bearer ${token}` };
  const scopes = [
    ["WEEK", ""],
    ["UNIVERSITY", ""],
    ["DAY", "&dayOfWeek=1"],
    ["INSTRUCTOR", `&scopeId=${fixture.instructorId}`],
    ["SECTION", `&scopeId=${fixture.sectionId}`],
    ["ROOM", `&scopeId=${fixture.roomIds[0]}`],
    ["COURSE", `&scopeId=${fixture.courseIds[0]}`],
    ["OFFERING", `&scopeId=${fixture.offeringIds[0]}`],
    ["PROGRAM", `&scopeId=${fixture.programId}`],
    ["DEPARTMENT", `&scopeId=${fixture.departmentId}`],
  ];
  for (const [scope, suffix] of scopes) {
    const response = await page.request.get(
      `${backend}/admin/aura/versions/${fixture.versionId}/timetable-view?scope=${scope}${suffix}`,
      { headers },
    );
    expect(response.status(), `${scope} timetable view`).toBe(200);
    expect((await response.json()).scopeType).toBe(scope);
  }

  const idor = await page.request.get(
    `${backend}/admin/aura/versions/${fixture.versionId}/timetable-view?scope=ROOM&scopeId=00000000-0000-4000-8000-000000000999`,
    { headers },
  );
  expect(idor.status()).toBe(404);
});

test("admin manages operational rules and sees persisted analytics and audit", async ({ page }, testInfo) => {
  test.setTimeout(90_000);
  await login(page, e2eUsers.admin.email, e2eUsers.admin.password);
  await page.goto("/admin/aura");
  await selectFixtureTerm(page);

  const suffix = testInfo.project.name === "chromium-mobile" ? "M" : "D";
  const operations = page.getByRole("region", { name: "Scheduling operations" });
  const buildingsCard = operations.getByRole("heading", { name: "Buildings" }).locator("..").locator("..");
  const groupsCard = operations.getByRole("heading", { name: "Teaching groups" }).locator("..").locator("..");
  const conflictsCard = operations.getByRole("heading", { name: "Offering conflicts" }).locator("..").locator("..");
  const travelCard = operations.getByRole("heading", { name: "Building travel rules" }).locator("..").locator("..");
  await expect(operations.getByRole("heading", { name: "Scheduling operations" })).toBeVisible();
  await buildingsCard.getByLabel("Building code").fill(`E2E-${suffix}1`);
  await buildingsCard.getByLabel("Building name").fill(`E2E ${suffix} Building One`);
  await buildingsCard.getByRole("button", { name: "Save building" }).click();
  await expect(buildingsCard.getByText(`E2E-${suffix}1 · E2E ${suffix} Building One`, { exact: true })).toBeVisible();

  await buildingsCard.getByLabel("Building code").fill(`E2E-${suffix}2`);
  await buildingsCard.getByLabel("Building name").fill(`E2E ${suffix} Building Two`);
  await buildingsCard.getByRole("button", { name: "Save building" }).click();
  await travelCard.getByLabel("From building").selectOption({ label: `E2E ${suffix} Building One` });
  await travelCard.getByLabel("To building").selectOption({ label: `E2E ${suffix} Building Two` });
  await travelCard.getByLabel("Minimum minutes").fill("12");
  await travelCard.getByRole("button", { name: "Save travel rule" }).click();
  await expect(travelCard.getByText(`E2E ${suffix} Building One → E2E ${suffix} Building Two · 12 min`, { exact: true })).toBeVisible();

  await groupsCard.getByRole("combobox", { name: "Offering" }).selectOption({ index: 1 });
  await groupsCard.getByLabel("Group code").fill(`LAB-${suffix}`);
  await groupsCard.getByLabel("Display name").fill(`Laboratory ${suffix}`);
  await groupsCard.getByRole("button", { name: "Save teaching group" }).click();
  await expect(groupsCard.getByText(`LAB-${suffix} · Laboratory ${suffix}`, { exact: true })).toBeVisible();

  const firstOfferingIndex = testInfo.project.name === "chromium-mobile" ? 2 : 1;
  const secondOfferingIndex = testInfo.project.name === "chromium-mobile" ? 3 : 2;
  await conflictsCard.getByRole("combobox", { name: "First offering" }).selectOption({ index: firstOfferingIndex });
  await conflictsCard.getByRole("combobox", { name: "Conflicting offering" }).selectOption({ index: secondOfferingIndex });
  await conflictsCard.getByLabel("Reason").fill(`Shared cohort ${suffix}`);
  await conflictsCard.getByRole("button", { name: "Save hard conflict" }).click();
  await expect(conflictsCard.getByText(`HARD · Shared cohort ${suffix}`, { exact: true })).toBeVisible();
  await expect(operations.getByText("Scheduled sessions")).toBeVisible();
  await expect(operations.getByText("OFFERING CONFLICT CREATED", { exact: false }).first()).toBeVisible();

  const deactivateRoom = page.getByRole("button", { name: /Deactivate Room .* E2E Room C101/ });
  await deactivateRoom.click();
  const activateRoom = page.getByRole("button", { name: /Activate Room .* E2E Room C101/ });
  await expect(activateRoom).toBeVisible();
  await activateRoom.click();
  await expect(deactivateRoom).toBeVisible();
  await generateAndPublishCurrentVersion(page, `Operational rules ${suffix}`);
});

test("emergency room closure creates an automatically reassigned review draft", async ({ page }) => {
  await login(page, e2eUsers.admin.email, e2eUsers.admin.password);
  await page.goto("/admin/aura");
  await selectFixtureTerm(page);
  await selectVersionWithStatus(page, "PUBLISHED");

  await page.getByLabel("Scenario").selectOption("ROOM_UNAVAILABLE");
  await page.getByLabel("Affected resource").selectOption({ index: 1 });
  await page.getByLabel("Emergency reason").fill("Automated room closure verification");
  await page.getByRole("button", { name: "Create emergency draft" }).click();

  await expect(page.getByText(/affected sessions? (?:was|were) reassigned in the review draft/)).toBeVisible();
  await expect(page.getByText(/[1-9]\d* reassigned in the review draft/)).toBeVisible();
});

test("localized repair preview is reviewed and applied to an isolated draft", async ({ page }) => {
  await login(page, e2eUsers.admin.email, e2eUsers.admin.password);
  await page.goto("/admin/aura");
  await selectFixtureTerm(page);
  await selectVersionWithStatus(page, "PUBLISHED");

  await page.getByLabel("Repair reason").fill("Resolve a localized scheduling concern");
  await page.getByRole("button", { name: "Preview localized repair" }).click();
  await expect(page.getByText("A minimum-disruption repair is ready for review.")).toBeVisible();
  await page.getByRole("button", { name: "Apply reviewed repair" }).click();
  await expect(page.getByText("The localized repair was applied to the draft.")).toBeVisible();
});

test("student API token cannot cross the AURA admin boundary", async ({ page }) => {
  await login(page, e2eUsers.student.email, e2eUsers.student.password);
  const token = await page.evaluate(() => localStorage.getItem("campusone.accessToken"));
  expect(token).toBeTruthy();

  const response = await page.request.get(
    `${backend}/admin/aura/terms/${fixture.termId}/versions`,
    { headers: { Authorization: `Bearer ${token}` } },
  );
  expect(response.status()).toBe(403);
});

test("AURA remains usable at mobile width", async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== "chromium-mobile", "Mobile project coverage only.");
  await login(page, e2eUsers.admin.email, e2eUsers.admin.password);
  await page.goto("/admin/aura");
  await selectFixtureTerm(page);

  await expect(page.getByRole("heading", { name: "AURA Timetable Generator" }))
    .toBeVisible();
  await expect(page.getByRole("button", { name: "Refresh", exact: true }).first())
    .toBeVisible();
  expect(await page.locator("body").evaluate((body) => body.scrollWidth <= window.innerWidth + 1))
    .toBe(true);
});

test("valid and invalid CSV imports produce actionable browser states", async ({ page }, testInfo) => {
  await login(page, e2eUsers.admin.email, e2eUsers.admin.password);
  await page.goto("/admin/aura");
  await selectFixtureTerm(page);

  await page.getByLabel("Import type").selectOption("ROOMS");
  await page.getByLabel("Scheduling data file").setInputFiles({
    name: "empty.csv",
    mimeType: "text/csv",
    buffer: Buffer.from(""),
  });
  await page.getByRole("button", { name: "Preview import" }).click();
  await expect(page.getByText("Choose a non-empty import file.")).toBeVisible();

  const suffix = testInfo.project.name.replace(/[^a-z]/gi, "").toUpperCase();
  await page.getByLabel("Scheduling data file").setInputFiles({
    name: `rooms-${suffix}.csv`,
    mimeType: "text/csv",
    buffer: Buffer.from(
      `CODE,NAME,BUILDING,CAPACITY,ROOM_TYPE,FACILITIES\nE2E-${suffix},E2E ${suffix} Room,E2E Building,45,CLASSROOM,PROJECTOR\n`,
    ),
  });
  await page.getByRole("button", { name: "Preview import" }).click();
  await expect(page.getByText("Map columns")).toBeVisible();
  await page.getByRole("button", { name: "Validate every row" }).click();
  await expect(page.getByText("1 row passed validation.")).toBeVisible();
});

test("new incomplete term shows readiness blockers and disables generation", async ({ page }, testInfo) => {
  await login(page, e2eUsers.admin.email, e2eUsers.admin.password);
  await page.goto("/admin/aura");
  const suffix = testInfo.project.name === "chromium-mobile" ? "MOBILE" : "DESKTOP";
  await page.getByLabel("Term code").fill(`E2E-EMPTY-${suffix}`);
  await page.getByLabel("Term name").fill(`E2E Empty ${suffix}`);
  await page.getByLabel("Start date").fill("2027-01-01");
  await page.getByLabel("End date").fill("2027-05-31");
  await page.getByRole("button", { name: "Create term" }).click();
  await expect(page.getByText("Setup needs attention")).toBeVisible();
  await expect(page.getByRole("button", { name: "Generate timetable" }))
    .toBeDisabled();
});

async function login(page: Page, email: string, password: string) {
  await page.goto("/login");
  await page.getByLabel("Email address").fill(email);
  await page.getByRole("textbox", { name: "Password", exact: true }).fill(password);
  await page.getByRole("button", { name: "Log in" }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
}

async function selectFixtureTerm(page: Page) {
  await expect(page.getByLabel("Academic term")).toBeVisible();
  await page.getByLabel("Academic term").selectOption(fixture.termId);
  await expect(page.getByLabel("Academic term")).toHaveValue(fixture.termId);
}

async function selectVersionWithStatus(page: Page, status: "DRAFT" | "PUBLISHED") {
  const card = page.getByRole("button").filter({
    has: page.getByText(status, { exact: true }),
  }).first();
  await expect(card).toBeVisible();
  await card.click();
}

async function generateAndPublishCurrentVersion(page: Page, notes: string) {
  const token = await page.evaluate(() => localStorage.getItem("campusone.accessToken"));
  expect(token).toBeTruthy();
  const headers = { Authorization: `Bearer ${token}` };
  const start = await page.request.post(
    `${backend}/admin/aura/terms/${fixture.termId}/runs`,
    {
      headers,
      data: {
        terminationSeconds: 2,
        notes,
        profile: "BALANCED",
        randomSeed: 20260721,
      },
    },
  );
  expect(start.ok()).toBeTruthy();
  const run = await start.json() as { id: string };
  const deadline = Date.now() + 30_000;
  let status = "QUEUED";
  while (["QUEUED", "RUNNING"].includes(status) && Date.now() < deadline) {
    await page.waitForTimeout(400);
    const response = await page.request.get(
      `${backend}/admin/aura/runs/${run.id}`,
      { headers },
    );
    expect(response.ok()).toBeTruthy();
    status = ((await response.json()) as { status: string }).status;
  }
  expect(status).toBe("COMPLETED");

  const versionsResponse = await page.request.get(
    `${backend}/admin/aura/terms/${fixture.termId}/versions`,
    { headers },
  );
  expect(versionsResponse.ok()).toBeTruthy();
  const versions = await versionsResponse.json() as Array<{
    id: string;
    status: string;
  }>;
  const generated = versions.find((version) => version.status === "DRAFT");
  expect(generated).toBeTruthy();
  const publish = await page.request.post(
    `${backend}/admin/aura/versions/${generated!.id}/publish`,
    { headers, data: {} },
  );
  expect(publish.ok()).toBeTruthy();
}
