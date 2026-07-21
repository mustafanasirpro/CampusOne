import { render, screen } from "@testing-library/react";
import { vi } from "vitest";

import {
  listAvailableAuraTerms,
} from "@/api/auraApi";
import { PersonalTimetablePage } from "@/pages/PersonalTimetablePage";

vi.mock("@/api/auraApi", () => ({
  downloadMyAuraTimetableCalendar: vi.fn(),
  getMyAuraTimetable: vi.fn(),
  listAvailableAuraTerms: vi.fn(),
  listMyAuraRegistrations: vi.fn(),
  listMyAuraResolutionCases: vi.fn(),
  requestAuraResolution: vi.fn(),
}));

describe("PersonalTimetablePage", () => {
  it("renders a stable empty state when no published term is available", async () => {
    vi.mocked(listAvailableAuraTerms).mockResolvedValue([]);

    render(<PersonalTimetablePage />);

    expect(await screen.findByText("No timetable to show")).toBeInTheDocument();
    expect(screen.getByText(
      "A published timetable is not available for your university yet.",
    )).toBeInTheDocument();
    expect(screen.queryByText("Loading your timetable")).not.toBeInTheDocument();
  });

  it("replaces loading with a safe error when terms cannot be loaded", async () => {
    vi.mocked(listAvailableAuraTerms).mockRejectedValue(new Error("offline"));

    render(<PersonalTimetablePage />);

    expect(await screen.findByText("Your timetable could not be loaded."))
      .toBeInTheDocument();
    expect(screen.queryByText("Loading your timetable")).not.toBeInTheDocument();
  });
});
