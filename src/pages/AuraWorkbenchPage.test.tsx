import { render, screen } from "@testing-library/react";
import { vi } from "vitest";

import { getAuraCapabilities, listAuraTerms } from "@/api/auraApi";
import { getCurrentUserIdentity } from "@/api/userApi";
import { ToastProvider } from "@/components/common";
import { AuraWorkbenchPage } from "@/pages/AuraWorkbenchPage";

vi.mock("@/api/auraApi", () => ({
  createAuraTerm: vi.fn(),
  getAuraCapabilities: vi.fn(),
  getAuraGenerationRun: vi.fn(),
  getAuraMetrics: vi.fn(),
  getAuraReadiness: vi.fn(),
  listAuraClashes: vi.fn(),
  listAuraSessions: vi.fn(),
  listAuraTerms: vi.fn(),
  listAuraVersions: vi.fn(),
  publishAuraVersion: vi.fn(),
  startAuraGeneration: vi.fn(),
}));

vi.mock("@/api/userApi", () => ({
  getCurrentUserIdentity: vi.fn(),
}));

describe("AuraWorkbenchPage", () => {
  it("denies the administration workbench using backend capabilities", async () => {
    vi.mocked(getCurrentUserIdentity).mockResolvedValue(identity);
    vi.mocked(getAuraCapabilities).mockResolvedValue({
      canManage: false,
      universityId: identity.university.id,
    });

    render(
      <ToastProvider>
        <AuraWorkbenchPage />
      </ToastProvider>,
    );

    expect(await screen.findByText(
      "You do not have access to AURA administration",
    )).toBeInTheDocument();
    expect(listAuraTerms).not.toHaveBeenCalled();
  });
});

const identity = {
  avatarUrl: null,
  bio: null,
  coverImageUrl: null,
  department: {
    active: true,
    code: "CS",
    id: "department-1",
    name: "Computer Science",
    universityId: "university-1",
  },
  email: "student@campusone.test",
  fullName: "AURA Student",
  location: null,
  preferences: {
    compactMode: false,
    language: "en",
    theme: "SYSTEM" as const,
  },
  semester: 5,
  skills: [],
  totalXp: 0,
  university: {
    active: true,
    city: "Islamabad",
    id: "university-1",
    name: "CampusOne University",
    shortName: "CAMPUSONE",
    website: null,
  },
  userId: "user-1",
  visibility: "PRIVATE" as const,
};
