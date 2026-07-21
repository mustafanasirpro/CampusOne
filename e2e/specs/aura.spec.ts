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
  await sessionSelect.selectOption({ index: 1 });
  await page.getByLabel("New room").selectOption(fixture.roomIds[0]);
  await page.getByLabel("New timeslot").selectOption(fixture.timeslotIds[2]);
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
