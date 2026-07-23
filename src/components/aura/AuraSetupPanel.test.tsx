import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";

import {
  getAuraSetupReferences,
  listAuraBatches,
  listAuraCalendarExceptions,
  listAuraInstructors,
  listAuraOfferings,
  listAuraPrograms,
  listAuraRooms,
  listAuraSections,
  listAuraTimeslots,
  setAuraResourceActive,
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
  getAuraSetupReferences: vi.fn(),
  listAuraBatches: vi.fn(),
  listAuraCalendarExceptions: vi.fn(),
  listAuraInstructors: vi.fn(),
  listAuraOfferings: vi.fn(),
  listAuraPrograms: vi.fn(),
  listAuraRooms: vi.fn(),
  listAuraSections: vi.fn(),
  listAuraTimeslots: vi.fn(),
  setAuraResourceActive: vi.fn(),
  upsertAuraInstructorAvailability: vi.fn(),
  upsertAuraRoomAvailability: vi.fn(),
  upsertAuraSectionAvailability: vi.fn(),
}));

describe("AuraSetupPanel", () => {
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
      name: "Deactivate Program · CS",
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
});
