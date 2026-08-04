import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, vi } from "vitest";

import {
  getAuraSetupReferences,
  listAuraBatches,
  listAuraBuildings,
  listAuraCalendarExceptions,
  listAuraInstructors,
  listAuraOfferings,
  listAuraPrograms,
  listAuraRooms,
  listAuraSections,
  listAuraStudentAvailability,
  listAuraTeachingGroups,
  listAuraTimeslots,
  setAuraResourceActive,
  upsertAuraStudentAvailability,
  updateAuraProgram,
  updateAuraSection,
} from "@/api/auraApi";
import { AuraSetupPanel } from "@/components/aura/AuraSetupPanel";

vi.mock("@/api/auraApi", () => ({
  createAuraBatch: vi.fn(),
  createAuraCalendarException: vi.fn(),
  createAuraInstructor: vi.fn(),
  createAuraMeetingRequirement: vi.fn(),
  createAuraOffering: vi.fn(),
  createAuraProgram: vi.fn(),
  createAuraRoom: vi.fn(),
  createAuraSection: vi.fn(),
  createAuraTimeslot: vi.fn(),
  deactivateAuraCalendarException: vi.fn(),
  deleteAuraStudentAvailability: vi.fn(),
  getAuraSetupReferences: vi.fn(),
  listAuraBatches: vi.fn(),
  listAuraBuildings: vi.fn(),
  listAuraCalendarExceptions: vi.fn(),
  listAuraInstructors: vi.fn(),
  listAuraMeetingRequirements: vi.fn(),
  listAuraOfferings: vi.fn(),
  listAuraPrograms: vi.fn(),
  listAuraRooms: vi.fn(),
  listAuraSections: vi.fn(),
  listAuraStudentAvailability: vi.fn(),
  listAuraTeachingGroups: vi.fn(),
  listAuraTimeslots: vi.fn(),
  setAuraResourceActive: vi.fn(),
  upsertAuraInstructorAvailability: vi.fn(),
  upsertAuraRoomAvailability: vi.fn(),
  upsertAuraSectionAvailability: vi.fn(),
  upsertAuraStudentAvailability: vi.fn(),
  updateAuraProgram: vi.fn(),
  updateAuraBatch: vi.fn(),
  updateAuraSection: vi.fn(),
  updateAuraInstructor: vi.fn(),
  updateAuraRoom: vi.fn(),
  updateAuraTimeslot: vi.fn(),
  updateAuraOffering: vi.fn(),
  updateAuraMeetingRequirement: vi.fn(),
}));

describe("AuraSetupPanel", () => {
  beforeEach(() => {
    vi.mocked(listAuraBuildings).mockResolvedValue([]);
    vi.mocked(listAuraTeachingGroups).mockResolvedValue([]);
  });

  it("loads saved setup and applies an optimistic deactivate action", async () => {
    vi.mocked(getAuraSetupReferences).mockResolvedValue({
      courses: [], departments: [], students: [], universityId: "university-1",
    });
    vi.mocked(listAuraPrograms).mockResolvedValue([{
      active: true,
      code: "CS",
      departmentId: "department-1",
      id: "program-1",
      name: "Computer Science",
      numberOfSemesters: 8,
      universityId: "university-1",
      version: 3,
    }]);
    vi.mocked(listAuraBatches).mockResolvedValue([]);
    vi.mocked(listAuraSections).mockResolvedValue([]);
    vi.mocked(listAuraInstructors).mockResolvedValue([]);
    vi.mocked(listAuraRooms).mockResolvedValue([]);
    vi.mocked(listAuraTimeslots).mockResolvedValue([]);
    vi.mocked(listAuraOfferings).mockResolvedValue([]);
    vi.mocked(listAuraCalendarExceptions).mockResolvedValue([]);
    vi.mocked(listAuraStudentAvailability).mockResolvedValue([]);
    vi.mocked(setAuraResourceActive).mockResolvedValue({
      active: false,
      id: "program-1",
      resourceType: "PROGRAM",
      updatedAt: "2026-07-22T00:00:00Z",
      version: 4,
    });
    const changed = vi.fn().mockResolvedValue(undefined);

    render(<AuraSetupPanel onChanged={changed} termId="term-1" universityId="university-1" />);

    const deactivate = await screen.findByRole("button", {
      name: "Deactivate Program · CS · Computer Science",
    });
    await userEvent.click(deactivate);

    expect(setAuraResourceActive).toHaveBeenCalledWith(
      "program",
      "program-1",
      false,
      3,
      "Deactivated from setup management",
    );
    expect(changed).toHaveBeenCalled();
  });

  it("saves a university-scoped student availability preference", async () => {
    vi.mocked(getAuraSetupReferences).mockResolvedValue({
      courses: [],
      departments: [],
      students: [{
        code: "ST-1",
        id: "student-1",
        name: "Student One",
        parentId: "",
      }],
      universityId: "university-1",
    });
    vi.mocked(listAuraPrograms).mockResolvedValue([]);
    vi.mocked(listAuraBatches).mockResolvedValue([]);
    vi.mocked(listAuraSections).mockResolvedValue([]);
    vi.mocked(listAuraInstructors).mockResolvedValue([]);
    vi.mocked(listAuraRooms).mockResolvedValue([]);
    vi.mocked(listAuraOfferings).mockResolvedValue([]);
    vi.mocked(listAuraCalendarExceptions).mockResolvedValue([]);
    vi.mocked(listAuraStudentAvailability).mockResolvedValue([]);
    vi.mocked(listAuraTimeslots).mockResolvedValue([{
      active: true,
      dayOfWeek: 1,
      endsAt: "10:00:00",
      id: "slot-1",
      label: "Monday 09:00",
      slotOrder: 1,
      slotType: "INSTRUCTIONAL",
      startsAt: "09:00:00",
      termId: "term-1",
      universityId: "university-1",
      version: 0,
    }]);
    vi.mocked(upsertAuraStudentAvailability).mockResolvedValue({
      availability: "UNAVAILABLE",
      id: "availability-1",
      reason: null,
      studentUserId: "student-1",
      termId: "term-1",
      timeslotId: "slot-1",
      version: 0,
    });

    render(
      <AuraSetupPanel
        onChanged={vi.fn().mockResolvedValue(undefined)}
        termId="term-1"
        universityId="university-1"
      />,
    );

    await userEvent.selectOptions(
      await screen.findByLabelText("Resource type"),
      "STUDENT",
    );
    await userEvent.selectOptions(screen.getByLabelText("Resource"), "student-1");
    await userEvent.selectOptions(screen.getByLabelText("Timeslot"), "slot-1");
    await userEvent.click(
      screen.getByRole("button", { name: "Save availability" }),
    );

    expect(upsertAuraStudentAvailability).toHaveBeenCalledWith({
      availability: "UNAVAILABLE",
      reason: undefined,
      studentUserId: "student-1",
      termId: "term-1",
      timeslotId: "slot-1",
    });
  });

  it("edits a program without discarding its lifecycle fields", async () => {
    vi.mocked(getAuraSetupReferences).mockResolvedValue({
      courses: [],
      departments: [{ code: "CS", id: "department-1", name: "Computing", parentId: "" }],
      students: [],
      universityId: "university-1",
    });
    vi.mocked(listAuraPrograms).mockResolvedValue([{
      active: true,
      code: "CS",
      departmentId: "department-1",
      id: "program-1",
      name: "Computer Science",
      numberOfSemesters: 8,
      universityId: "university-1",
      version: 3,
    }]);
    vi.mocked(listAuraBatches).mockResolvedValue([]);
    vi.mocked(listAuraSections).mockResolvedValue([]);
    vi.mocked(listAuraInstructors).mockResolvedValue([]);
    vi.mocked(listAuraRooms).mockResolvedValue([]);
    vi.mocked(listAuraTimeslots).mockResolvedValue([]);
    vi.mocked(listAuraOfferings).mockResolvedValue([]);
    vi.mocked(listAuraCalendarExceptions).mockResolvedValue([]);
    vi.mocked(listAuraStudentAvailability).mockResolvedValue([]);
    vi.mocked(updateAuraProgram).mockResolvedValue({
      active: true,
      id: "program-1",
      resourceType: "PROGRAM",
      updatedAt: "2026-07-26T00:00:00Z",
      version: 4,
    });

    render(<AuraSetupPanel onChanged={vi.fn().mockResolvedValue(undefined)} termId="term-1" universityId="university-1" />);

    await userEvent.click(await screen.findByRole("button", { name: "Edit" }));
    await userEvent.clear(screen.getByLabelText("Program name"));
    await userEvent.type(screen.getByLabelText("Program name"), "BS Computer Science");
    await userEvent.click(screen.getByRole("button", { name: "Save program changes" }));

    expect(updateAuraProgram).toHaveBeenCalledWith("program-1", {
      active: true,
      code: "CS",
      departmentId: "department-1",
      name: "BS Computer Science",
      numberOfSemesters: 8,
      version: 3,
    });
  });

  it("edits section load policy and building without exposing identifiers", async () => {
    vi.mocked(getAuraSetupReferences).mockResolvedValue({
      courses: [],
      departments: [],
      students: [],
      universityId: "university-1",
    });
    vi.mocked(listAuraBuildings).mockResolvedValue([{
      active: true,
      code: "ENG",
      id: "building-1",
      minimumTransitionMinutes: 5,
      name: "Engineering Block",
      version: 0,
    }]);
    vi.mocked(listAuraPrograms).mockResolvedValue([]);
    vi.mocked(listAuraBatches).mockResolvedValue([]);
    vi.mocked(listAuraSections).mockResolvedValue([{
      active: true,
      batchId: "batch-1",
      code: "4A",
      displayName: "BSCS-4A",
      hardDailyLoad: 8,
      homeBuildingId: null,
      id: "section-1",
      preferredDailyLoad: 6,
      semesterNumber: 4,
      studentCount: 40,
      termId: "term-1",
      version: 2,
    }]);
    vi.mocked(listAuraInstructors).mockResolvedValue([]);
    vi.mocked(listAuraRooms).mockResolvedValue([]);
    vi.mocked(listAuraTimeslots).mockResolvedValue([]);
    vi.mocked(listAuraOfferings).mockResolvedValue([]);
    vi.mocked(listAuraCalendarExceptions).mockResolvedValue([]);
    vi.mocked(listAuraStudentAvailability).mockResolvedValue([]);
    vi.mocked(updateAuraSection).mockResolvedValue({
      active: true,
      id: "section-1",
      resourceType: "SECTION",
      updatedAt: "2026-07-26T00:00:00Z",
      version: 3,
    });

    render(<AuraSetupPanel onChanged={vi.fn().mockResolvedValue(undefined)} termId="term-1" universityId="university-1" />);

    await userEvent.click(await screen.findByRole("button", { name: "Edit" }));
    await userEvent.clear(screen.getByLabelText("Maximum daily slots"));
    await userEvent.type(screen.getByLabelText("Maximum daily slots"), "7");
    await userEvent.selectOptions(screen.getByLabelText("Home building"), "building-1");
    await userEvent.click(screen.getByRole("button", { name: "Save section changes" }));

    expect(updateAuraSection).toHaveBeenCalledWith("section-1", expect.objectContaining({
      hardDailyLoad: 7,
      homeBuildingId: "building-1",
      preferredDailyLoad: 6,
      semesterNumber: 4,
      termId: "term-1",
      version: 2,
    }));
  });
});
