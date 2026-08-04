import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";

import {
  analyzeAuraResolutionCase,
  listAuraResolutionCases,
} from "@/api/auraApi";
import { AuraResolutionPanel } from "@/components/aura/AuraResolutionPanel";

vi.mock("@/api/auraApi", () => ({
  analyzeAuraResolutionCase: vi.fn(),
  applyAuraResolutionCase: vi.fn(),
  approveAuraResolutionCase: vi.fn(),
  listAuraResolutionCases: vi.fn(),
  rejectAuraResolutionCase: vi.fn(),
}));

describe("AuraResolutionPanel", () => {
  it("shows a safe empty state when there are no resolution cases", async () => {
    vi.mocked(listAuraResolutionCases).mockResolvedValue([]);

    render(<AuraResolutionPanel termId="term-1" />);

    expect(await screen.findByText("No resolution cases")).toBeInTheDocument();
    expect(screen.queryByText("Loading resolution cases")).not.toBeInTheDocument();
  });

  it("analyzes an open case once and refreshes the list", async () => {
    vi.mocked(listAuraResolutionCases).mockResolvedValue([
      {
        caseType: "STUDENT_CLASH",
        createdAt: "2026-07-26T10:00:00Z",
        id: "case-1",
        registrationId: "registration-1",
        reviewReason: null,
        status: "OPEN",
        studentName: "Aisha Khan",
        studentUserId: "student-1",
        suggestions: [],
        summary: "Two registered sessions overlap.",
        termId: "term-1",
        updatedAt: "2026-07-26T10:00:00Z",
        version: 0,
      },
    ]);
    vi.mocked(analyzeAuraResolutionCase).mockResolvedValue({
      caseType: "STUDENT_CLASH",
      createdAt: "2026-07-26T10:00:00Z",
      id: "case-1",
      registrationId: "registration-1",
      reviewReason: null,
      status: "SUGGESTED",
      studentName: "Aisha Khan",
      studentUserId: "student-1",
      suggestions: [],
      summary: "Two registered sessions overlap.",
      termId: "term-1",
      updatedAt: "2026-07-26T10:01:00Z",
      version: 1,
    });
    const user = userEvent.setup();

    render(<AuraResolutionPanel termId="term-1" />);

    await user.click(await screen.findByRole("button", { name: /Analyze/ }));
    await waitFor(() => expect(analyzeAuraResolutionCase).toHaveBeenCalledWith("case-1"));
    expect(listAuraResolutionCases).toHaveBeenCalledTimes(2);
  });
});
