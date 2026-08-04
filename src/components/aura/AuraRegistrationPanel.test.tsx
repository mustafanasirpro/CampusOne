import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";

import {
  createAuraStudentRegistration,
  getAuraSetupReferences,
  listAuraOfferings,
  listAuraStudentRegistrations,
} from "@/api/auraApi";
import { AuraRegistrationPanel } from "@/components/aura/AuraRegistrationPanel";

vi.mock("@/api/auraApi", () => ({
  createAuraStudentRegistration: vi.fn(),
  getAuraSetupReferences: vi.fn(),
  listAuraOfferings: vi.fn(),
  listAuraStudentRegistrations: vi.fn(),
}));

describe("AuraRegistrationPanel", () => {
  beforeEach(() => {
    vi.mocked(getAuraSetupReferences).mockResolvedValue({
      courses: [],
      departments: [],
      students: [{ code: "ST-001", id: "student-1", name: "Aisha Khan", parentId: "department-1" }],
      universityId: "university-1",
    });
    vi.mocked(listAuraOfferings).mockResolvedValue([
      {
        combinedSections: false,
        courseCode: "CS101",
        courseId: "course-1",
        courseTitle: "Programming Fundamentals",
        electiveGroup: null,
        expectedStudents: 30,
        id: "offering-1",
        instructorId: "instructor-1",
        instructorName: "Dr Ahmed",
        maximumEnrollment: 40,
        notes: null,
        offeringCode: "CS101-A",
        parallelGroup: null,
        sectionId: "section-1",
        sectionName: "Section A",
        status: "ACTIVE",
        termId: "term-1",
        version: 0,
      },
    ]);
    vi.mocked(listAuraStudentRegistrations).mockResolvedValue([]);
  });

  it("loads an empty registration state and submits a selected student and offering once", async () => {
    const onChanged = vi.fn().mockResolvedValue(undefined);
    vi.mocked(createAuraStudentRegistration).mockResolvedValue({
      courseCode: "CS101",
      courseTitle: "Programming Fundamentals",
      createdAt: "2026-07-26T10:00:00Z",
      equivalentOfferingId: null,
      homeSectionId: null,
      id: "registration-1",
      labGroupId: null,
      lectureGroupId: null,
      offeringId: "offering-1",
      registrationType: "PRIMARY_SECTION",
      status: "ACTIVE",
      studentName: "Aisha Khan",
      studentUserId: "student-1",
      teachingSectionId: null,
      termId: "term-1",
      tutorialGroupId: null,
      updatedAt: "2026-07-26T10:00:00Z",
      version: 0,
    });
    const user = userEvent.setup();

    render(<AuraRegistrationPanel onChanged={onChanged} termId="term-1" />);

    expect(await screen.findByText("No student registrations")).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText("Student"), "student-1");
    await user.selectOptions(screen.getByLabelText("Course offering"), "offering-1");
    await user.click(screen.getByRole("button", { name: "Add registration" }));

    await waitFor(() => expect(createAuraStudentRegistration).toHaveBeenCalledTimes(1));
    expect(createAuraStudentRegistration).toHaveBeenCalledWith({
      offeringId: "offering-1",
      registrationType: "PRIMARY_SECTION",
      studentUserId: "student-1",
      termId: "term-1",
    });
    expect(onChanged).toHaveBeenCalledTimes(1);
  });

  it("replaces loading with an actionable error when registration data fails", async () => {
    vi.mocked(listAuraStudentRegistrations).mockRejectedValue(new Error("offline"));

    render(<AuraRegistrationPanel onChanged={vi.fn()} termId="term-1" />);

    expect(await screen.findByText("Registrations could not be loaded.")).toBeInTheDocument();
    expect(screen.queryByText("Loading registrations")).not.toBeInTheDocument();
  });
});
