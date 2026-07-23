import { render, screen } from "@testing-library/react";
import { vi } from "vitest";

import {
  getAuraSetupReferences,
  listAuraAuditEvents,
  listAuraBuildings,
  listAuraOfferingConflicts,
  listAuraOfferings,
  listAuraPrograms,
  listAuraTeachingGroups,
  listAuraTravelRules,
} from "@/api/auraApi";
import { AuraOperationsPanel } from "@/components/aura/AuraOperationsPanel";

vi.mock("@/api/auraApi", () => ({
  applyAuraRepair: vi.fn(),
  createAuraBuilding: vi.fn(),
  createAuraOfferingConflict: vi.fn(),
  createAuraTeachingGroup: vi.fn(),
  createAuraTravelRule: vi.fn(),
  getAuraAnalytics: vi.fn(),
  getAuraSetupReferences: vi.fn(),
  getAuraScopedTimetable: vi.fn(),
  listAuraAuditEvents: vi.fn(),
  listAuraBuildings: vi.fn(),
  listAuraOfferingConflicts: vi.fn(),
  listAuraOfferings: vi.fn(),
  listAuraPrograms: vi.fn(),
  listAuraTeachingGroups: vi.fn(),
  listAuraTravelRules: vi.fn(),
  previewAuraRepair: vi.fn(),
  updateAuraBuilding: vi.fn(),
  updateAuraOfferingConflict: vi.fn(),
  updateAuraTeachingGroup: vi.fn(),
  updateAuraTravelRule: vi.fn(),
}));

describe("AuraOperationsPanel", () => {
  it("replaces loading with safe empty operational forms", async () => {
    vi.mocked(listAuraBuildings).mockResolvedValue([]);
    vi.mocked(listAuraTeachingGroups).mockResolvedValue([]);
    vi.mocked(listAuraOfferingConflicts).mockResolvedValue([]);
    vi.mocked(listAuraTravelRules).mockResolvedValue([]);
    vi.mocked(listAuraAuditEvents).mockResolvedValue([]);
    vi.mocked(listAuraOfferings).mockResolvedValue([]);
    vi.mocked(listAuraPrograms).mockResolvedValue([]);
    vi.mocked(getAuraSetupReferences).mockResolvedValue({ courses: [], departments: [], students: [], universityId: "university-1" });

    render(
      <AuraOperationsPanel
        clashes={[]}
        onChanged={vi.fn().mockResolvedValue(undefined)}
        sessions={[]}
        termId="term-1"
        universityId="university-1"
      />,
    );

    expect(await screen.findByText("Scheduling operations")).toBeInTheDocument();
    expect(screen.getByText("Buildings")).toBeInTheDocument();
    expect(screen.getByText("Teaching groups")).toBeInTheDocument();
    expect(screen.queryByText("Loading AURA operations")).not.toBeInTheDocument();
  });

  it("shows an actionable error when operational data fails", async () => {
    vi.mocked(listAuraBuildings).mockRejectedValue(new Error("offline"));
    vi.mocked(listAuraTeachingGroups).mockResolvedValue([]);
    vi.mocked(listAuraOfferingConflicts).mockResolvedValue([]);
    vi.mocked(listAuraTravelRules).mockResolvedValue([]);
    vi.mocked(listAuraAuditEvents).mockResolvedValue([]);
    vi.mocked(listAuraOfferings).mockResolvedValue([]);
    vi.mocked(listAuraPrograms).mockResolvedValue([]);
    vi.mocked(getAuraSetupReferences).mockResolvedValue({ courses: [], departments: [], students: [], universityId: "university-1" });

    render(
      <AuraOperationsPanel
        clashes={[]}
        onChanged={vi.fn().mockResolvedValue(undefined)}
        sessions={[]}
        termId="term-1"
        universityId="university-1"
      />,
    );

    expect(await screen.findByText("AURA operations could not be loaded."))
      .toBeInTheDocument();
  });
});
