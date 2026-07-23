import { render, screen } from "@testing-library/react";
import { vi } from "vitest";

import {
  getMyAuraInstructorTimetable,
  listAvailableAuraTerms,
} from "@/api/auraApi";
import { InstructorTimetablePage } from "@/pages/InstructorTimetablePage";

vi.mock("@/api/auraApi", () => ({
  getMyAuraInstructorTimetable: vi.fn(),
  listAvailableAuraTerms: vi.fn(),
}));

describe("InstructorTimetablePage", () => {
  it("renders an empty teaching schedule safely", async () => {
    vi.mocked(listAvailableAuraTerms).mockResolvedValue([{
      code: "FA26",
      createdAt: "2026-07-21T00:00:00Z",
      endsOn: "2026-12-01",
      id: "term-1",
      name: "Fall 2026",
      startsOn: "2026-09-01",
      status: "READY",
      universityId: "university-1",
      updatedAt: "2026-07-21T00:00:00Z",
      version: 0,
    }]);
    vi.mocked(getMyAuraInstructorTimetable).mockResolvedValue({
      scopeId: "instructor-1",
      scopeLabel: "My teaching timetable",
      scopeType: "INSTRUCTOR",
      sessions: [],
      termId: "term-1",
      versionId: "version-1",
    });

    render(<InstructorTimetablePage />);

    expect(await screen.findByText("No teaching timetable")).toBeInTheDocument();
    expect(screen.queryByText("Loading teaching timetable")).not.toBeInTheDocument();
  });
});
