import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";

import {
  applyAuraImport,
  previewAuraImport,
  validateAuraImport,
} from "@/api/auraApi";
import { AuraImportPanel } from "@/components/aura/AuraImportPanel";

vi.mock("@/api/auraApi", () => ({
  applyAuraImport: vi.fn(),
  previewAuraImport: vi.fn(),
  validateAuraImport: vi.fn(),
}));

describe("AuraImportPanel", () => {
  it("previews an uploaded CSV and exposes its row count", async () => {
    vi.mocked(previewAuraImport).mockResolvedValue({
      id: "import-1",
      termId: "term-1",
      importType: "TIMETABLE",
      createdAt: "2026-07-19T11:00:00Z",
      fileFormat: "CSV",
      status: "PREVIEWED",
      originalFilename: "schedule.csv",
      ocrRequired: false,
      selectedSource: "schedule.csv",
      sources: ["schedule.csv"],
      headers: ["courseCode", "section", "day", "start", "room"],
      rows: [{
        courseCode: "CSC275",
        section: "A",
        day: "Monday",
        start: "09:00",
        room: "C-101",
      }],
      suggestedMapping: {
        courseCode: "courseCode",
        section: "section",
        day: "day",
        start: "start",
        room: "room",
      },
      totalRows: 1,
      truncated: false,
      warnings: [],
    });
    const user = userEvent.setup();
    render(<AuraImportPanel termId="term-1" />);

    await user.upload(
      screen.getByLabelText("Scheduling data file"),
      new File(["courseCode,section,day,start,room\nCSC275,A,Monday,09:00,C-101"],
        "schedule.csv", { type: "text/csv" }),
    );
    await user.click(screen.getByRole("button", { name: /preview import/i }));

    expect(await screen.findByText("Rows found")).toBeInTheDocument();
    expect(screen.getAllByText("1").length).toBeGreaterThan(0);
    expect(previewAuraImport).toHaveBeenCalledWith(
      "term-1",
      "TIMETABLE",
      expect.any(File),
      undefined,
    );
    expect(validateAuraImport).not.toHaveBeenCalled();
    expect(applyAuraImport).not.toHaveBeenCalled();
  });

  it("stops submitting and shows an actionable preview error", async () => {
    vi.mocked(previewAuraImport).mockRejectedValue(new Error("network"));
    const user = userEvent.setup();
    render(<AuraImportPanel termId="term-1" />);

    await user.upload(
      screen.getByLabelText("Scheduling data file"),
      new File(["bad"], "bad.csv", { type: "text/csv" }),
    );
    await user.click(screen.getByRole("button", { name: /preview import/i }));

    expect(await screen.findByText("The file could not be previewed."))
      .toBeInTheDocument();
    expect(screen.getByRole("button", { name: /preview import/i }))
      .toBeEnabled();
  });
});
