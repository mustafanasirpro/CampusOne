import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { vi } from "vitest";

import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { useAuth } from "@/auth/useAuth";

vi.mock("@/auth/useAuth", () => ({ useAuth: vi.fn() }));

describe("ProtectedRoute", () => {
  it("redirects an unauthenticated visitor to sign in", async () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: false,
      isLoading: false,
    } as ReturnType<typeof useAuth>);

    renderAtProtectedRoute();

    expect(await screen.findByText("Sign-in route")).toBeInTheDocument();
  });

  it("renders protected AURA content for an authenticated user", () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
    } as ReturnType<typeof useAuth>);

    renderAtProtectedRoute();

    expect(screen.getByText("Protected AURA content")).toBeInTheDocument();
  });

  it("keeps the route in a loading state while the session is checked", () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: false,
      isLoading: true,
    } as ReturnType<typeof useAuth>);

    renderAtProtectedRoute();

    expect(screen.getByText("Checking your CampusOne session")).toBeInTheDocument();
  });
});

function renderAtProtectedRoute() {
  render(
    <MemoryRouter initialEntries={["/admin/aura"]}>
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/admin/aura" element={<p>Protected AURA content</p>} />
        </Route>
        <Route path="/login" element={<p>Sign-in route</p>} />
      </Routes>
    </MemoryRouter>,
  );
}
