import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";

import {
  listAuraEmergencyRepairs,
  listAuraWhatIf,
  runAuraWhatIf,
} from "@/api/auraApi";
import { AuraScenarioPanel } from "@/components/aura/AuraScenarioPanel";
import type { AuraSession, AuraTimetableVersion } from "@/types/aura";

vi.mock("@/api/auraApi", () => ({
  createAuraEmergencyRepair: vi.fn(),
  listAuraEmergencyRepairs: vi.fn(),
  listAuraWhatIf: vi.fn(),
  runAuraWhatIf: vi.fn(),
}));

const version: AuraTimetableVersion = {
  createdAt: "2026-07-26T10:00:00Z",
  generationRunId: null,
  id: "version-1",
  notes: null,
  publishedAt: "2026-07-26T10:00:00Z",
  score: "0hard/0medium/0soft",
  status: "PUBLISHED",
  termId: "term-1",
  versionNumber: 1,
};

const session: AuraSession = {
  courseCode: "CS101",
  courseTitle: "Programming Fundamentals",
  dayOfWeek: 1,
  endsAt: "10:00:00",
  id: "session-1",
  instructorId: "instructor-1",
  instructorName: "Dr Ahmed",
  locked: false,
  meetingRequirementId: "requirement-1",
  offeringId: "offering-1",
  roomId: "room-1",
  roomName: "Room 101",
  roomType: "LECTURE",
  sectionId: "section-1",
  sectionName: "Section A",
  source: "SOLVER",
  startsAt: "09:00:00",
  timeslotId: "timeslot-1",
  versionId: "version-1",
};

describe("AuraScenarioPanel", () => {
  beforeEach(() => {
    vi.mocked(listAuraWhatIf).mockResolvedValue([]);
    vi.mocked(listAuraEmergencyRepairs).mockResolvedValue([]);
  });

  it("shows loading and then a useful empty state", async () => {
    render(
      <AuraScenarioPanel
        onChanged={vi.fn()}
        selectedVersionId="version-1"
        sessions={[session]}
        termId="term-1"
        versions={[version]}
      />,
    );

    expect(screen.getByText("Loading scheduling scenarios")).toBeInTheDocument();
    expect(await screen.findByText("No scheduling scenarios yet")).toBeInTheDocument();
  });

  it("runs a selected what-if scenario and renders the result", async () => {
    vi.mocked(runAuraWhatIf).mockResolvedValue({
      affectedSessions: 1,
      baselineScore: "0hard/0medium/-8soft",
      candidateCount: 12,
      clashesAdded: 0,
      clashesRemoved: 0,
      completedAt: "2026-07-26T10:01:00Z",
      createdAt: "2026-07-26T10:00:00Z",
      id: "what-if-1",
      feasible: true,
      projectedScore: "0hard/0medium/-12soft",
      recommendation: "A review draft is feasible.",
      scenarioType: "ROOM_UNAVAILABLE",
      sourceVersionId: "version-1",
      status: "COMPLETED",
      termId: "term-1",
      terminationReason: "COMPLETED",
    });
    const user = userEvent.setup();

    render(
      <AuraScenarioPanel
        onChanged={vi.fn()}
        selectedVersionId="version-1"
        sessions={[session]}
        termId="term-1"
        versions={[version]}
      />,
    );

    await screen.findByText("No scheduling scenarios yet");
    await user.selectOptions(screen.getByLabelText("Affected resource"), "room-1");
    await user.click(screen.getByRole("button", { name: /Run what-if/ }));

    await waitFor(() =>
      expect(runAuraWhatIf).toHaveBeenCalledWith(
        "term-1",
        "version-1",
        "ROOM_UNAVAILABLE",
        "room-1",
      ),
    );
    expect(await screen.findByText("A review draft is feasible.")).toBeInTheDocument();
    expect(screen.getByText(/Feasible.*0hard.*-8soft.*0hard.*-12soft/))
      .toBeInTheDocument();
  });
});
