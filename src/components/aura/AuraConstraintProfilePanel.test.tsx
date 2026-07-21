import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";

import {
  getAuraConstraintProfile,
  replaceAuraConstraintProfile,
} from "@/api/auraApi";
import { AuraConstraintProfilePanel } from "@/components/aura/AuraConstraintProfilePanel";

vi.mock("@/api/auraApi", () => ({
  getAuraConstraintProfile: vi.fn(),
  replaceAuraConstraintProfile: vi.fn(),
}));

const profile = {
  termId: "term-1",
  profile: "BALANCED" as const,
  customized: false,
  weights: [
    {
      constraintName: "Room double-booked",
      constraintLevel: "HARD" as const,
      weight: 1,
      active: true,
      customized: false,
    },
    {
      constraintName: "Preferred day",
      constraintLevel: "SOFT" as const,
      weight: 1,
      active: true,
      customized: false,
    },
  ],
};

describe("AuraConstraintProfilePanel", () => {
  it("renders saved profile weights and persists an edited value", async () => {
    vi.mocked(getAuraConstraintProfile).mockResolvedValue(profile);
    vi.mocked(replaceAuraConstraintProfile).mockResolvedValue({
      ...profile,
    });
    const user = userEvent.setup();

    render(
      <AuraConstraintProfilePanel
        onProfileChange={vi.fn()}
        selectedProfile="BALANCED"
        termId="term-1"
      />,
    );

    const input = await screen.findByLabelText("Preferred day weight");
    await user.clear(input);
    await user.type(input, "8");
    await user.click(screen.getByRole("button", { name: /save profile settings/i }));

    await waitFor(() => {
      expect(replaceAuraConstraintProfile).toHaveBeenCalledWith(
        "term-1",
        "BALANCED",
        expect.arrayContaining([
          expect.objectContaining({ constraintName: "Preferred day", weight: 8 }),
        ]),
      );
    });
  });

  it("shows an actionable load failure instead of leaving a spinner", async () => {
    vi.mocked(getAuraConstraintProfile).mockRejectedValue(new Error("offline"));

    render(
      <AuraConstraintProfilePanel
        onProfileChange={vi.fn()}
        selectedProfile="QUALITY"
        termId="term-1"
      />,
    );

    expect(await screen.findByText("Constraint settings could not be loaded."))
      .toBeInTheDocument();
    expect(screen.queryByText("Loading constraint settings")).not.toBeInTheDocument();
  });

  it("notifies the workbench when a profile is selected", async () => {
    vi.mocked(getAuraConstraintProfile).mockResolvedValue(profile);
    const onProfileChange = vi.fn();
    const user = userEvent.setup();

    render(
      <AuraConstraintProfilePanel
        onProfileChange={onProfileChange}
        selectedProfile="BALANCED"
        termId="term-1"
      />,
    );

    await screen.findByLabelText("Preferred day weight");
    await user.selectOptions(
      screen.getByLabelText("Generation profile"),
      "ROOM_EFFICIENT",
    );
    expect(onProfileChange).toHaveBeenCalledWith("ROOM_EFFICIENT");
  });
});
