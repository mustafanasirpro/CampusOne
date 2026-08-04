import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";

import {
  compareAuraVersions,
  createAuraRollbackDraft,
  listAuraRooms,
  listAuraTimeslots,
} from "@/api/auraApi";
import { AuraVersionTools } from "@/components/aura/AuraVersionTools";
import type { AuraTimetableVersion } from "@/types/aura";

vi.mock("@/api/auraApi", () => ({
  applyAuraSessionMove: vi.fn(),
  applyAuraSessionSwap: vi.fn(),
  archiveAuraVersion: vi.fn(),
  cloneAuraVersion: vi.fn(),
  compareAuraVersions: vi.fn(),
  createAuraRollbackDraft: vi.fn(),
  downloadAuraVersion: vi.fn(),
  listAuraRooms: vi.fn(),
  listAuraTimeslots: vi.fn(),
  previewAuraSessionMove: vi.fn(),
  previewAuraSessionSwap: vi.fn(),
  setAuraSessionPinned: vi.fn(),
}));

const versions: AuraTimetableVersion[] = [
  {
    createdAt: "2026-07-26T10:00:00Z",
    generationRunId: null,
    id: "draft-1",
    notes: null,
    publishedAt: null,
    score: "0hard/0medium/0soft",
    status: "DRAFT",
    termId: "term-1",
    versionNumber: 2,
  },
  {
    createdAt: "2026-07-25T10:00:00Z",
    generationRunId: "run-1",
    id: "published-1",
    notes: null,
    publishedAt: "2026-07-25T11:00:00Z",
    score: "0hard/0medium/-10soft",
    status: "PUBLISHED",
    termId: "term-1",
    versionNumber: 1,
  },
];

describe("AuraVersionTools", () => {
  beforeEach(() => {
    vi.mocked(listAuraRooms).mockResolvedValue([]);
    vi.mocked(listAuraTimeslots).mockResolvedValue([]);
  });

  it("loads draft editing references and keeps unsafe actions disabled", async () => {
    render(
      <AuraVersionTools
        onChanged={vi.fn()}
        onVersionSelected={vi.fn()}
        selectedVersionId="draft-1"
        sessions={[]}
        versions={versions}
      />,
    );

    expect(screen.getByText("Version workbench")).toBeInTheDocument();
    await waitFor(() => expect(listAuraRooms).toHaveBeenCalled());
    expect(screen.getByRole("button", { name: /Apply safe move/ })).toBeDisabled();
    expect(screen.getByRole("button", { name: /Apply safe swap/ })).toBeDisabled();
  });

  it("compares selected versions and renders structural differences", async () => {
    vi.mocked(compareAuraVersions).mockResolvedValue({
      addedOccurrences: 1,
      baseVersionId: "draft-1",
      baseScore: "0hard/0medium/0soft",
      changedOccurrences: 2,
      changes: [],
      comparedVersionId: "published-1",
      comparedScore: "0hard/0medium/-10soft",
      hardScoreDelta: 0,
      mediumScoreDelta: 0,
      removedOccurrences: 1,
      softScoreDelta: -10,
      totalOccurrences: 10,
    });
    const user = userEvent.setup();

    render(
      <AuraVersionTools
        onChanged={vi.fn()}
        onVersionSelected={vi.fn()}
        selectedVersionId="draft-1"
        sessions={[]}
        versions={versions}
      />,
    );

    await user.selectOptions(screen.getByLabelText("Compare with"), "published-1");
    await user.click(screen.getByRole("button", { name: "Compare" }));

    expect(await screen.findByText(/2 of 10 occurrences changed/)).toBeInTheDocument();
    expect(screen.getByText(/Score delta.*0 hard.*-10 soft/)).toBeInTheDocument();
    expect(compareAuraVersions).toHaveBeenCalledWith("draft-1", "published-1");
  });

  it("prepares a review-only rollback draft from a historical version", async () => {
    vi.mocked(createAuraRollbackDraft).mockResolvedValue({
      ...versions[0],
      id: "rollback-1",
      versionNumber: 3,
    });
    const onChanged = vi.fn().mockResolvedValue(undefined);
    const onVersionSelected = vi.fn();
    const user = userEvent.setup();

    render(
      <AuraVersionTools
        onChanged={onChanged}
        onVersionSelected={onVersionSelected}
        selectedVersionId="published-1"
        sessions={[]}
        versions={versions}
      />,
    );

    await user.click(screen.getByRole("button", { name: /Prepare rollback/ }));

    await waitFor(() =>
      expect(createAuraRollbackDraft).toHaveBeenCalledWith(
        "published-1",
        "Rollback review from version 1",
      ),
    );
    expect(onChanged).toHaveBeenCalled();
    expect(onVersionSelected).toHaveBeenCalledWith("rollback-1");
  });
});
