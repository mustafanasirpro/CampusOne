import { lazy, Suspense, type ReactNode } from "react";
import { Navigate, createBrowserRouter } from "react-router-dom";

import { PageLoadingState } from "@/components/common";
import { AppLayout, PublicLayout } from "@/components/layout";
import { LandingPage } from "@/pages/LandingPage";
import { LoginPage } from "@/pages/LoginPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { SignupPage } from "@/pages/SignupPage";
import { paths } from "@/routes/paths";

const DashboardPage = lazy(async () => {
  const module = await import("@/pages/DashboardPage");
  return { default: module.DashboardPage };
});

const ProfilePage = lazy(async () => {
  const module = await import("@/pages/ProfilePage");
  return { default: module.ProfilePage };
});

const NotesPage = lazy(async () => {
  const module = await import("@/pages/NotesPage");
  return { default: module.NotesPage };
});

const DiscussionsPage = lazy(async () => {
  const module = await import("@/pages/DiscussionsPage");
  return { default: module.DiscussionsPage };
});

const MarketplacePage = lazy(async () => {
  const module = await import("@/pages/MarketplacePage");
  return { default: module.MarketplacePage };
});

const InternshipsPage = lazy(async () => {
  const module = await import("@/pages/InternshipsPage");
  return { default: module.InternshipsPage };
});

const EventsPage = lazy(async () => {
  const module = await import("@/pages/EventsPage");
  return { default: module.EventsPage };
});

const LeaderboardPage = lazy(async () => {
  const module = await import("@/pages/LeaderboardPage");
  return { default: module.LeaderboardPage };
});

const AiAssistantPage = lazy(async () => {
  const module = await import("@/pages/AiAssistantPage");
  return { default: module.AiAssistantPage };
});

const SettingsPage = lazy(async () => {
  const module = await import("@/pages/SettingsPage");
  return { default: module.SettingsPage };
});

function lazyRoute(element: ReactNode, path: string) {
  return (
    <Suspense fallback={<PageLoadingState routePath={path} />}>
      {element}
    </Suspense>
  );
}

export const router = createBrowserRouter([
  {
    element: <PublicLayout />,
    children: [
      { index: true, element: <LandingPage /> },
      { path: paths.login, element: <LoginPage /> },
      { path: paths.signup, element: <SignupPage /> },
    ],
  },
  {
    element: <AppLayout />,
    children: [
      {
        path: paths.dashboard,
        element: lazyRoute(<DashboardPage />, paths.dashboard),
      },
      { path: "/home", element: <Navigate replace to={paths.dashboard} /> },
      {
        path: paths.profile,
        element: lazyRoute(<ProfilePage />, paths.profile),
      },
      {
        path: paths.notes,
        element: lazyRoute(<NotesPage />, paths.notes),
      },
      {
        path: paths.discussions,
        element: lazyRoute(<DiscussionsPage />, paths.discussions),
      },
      {
        path: paths.marketplace,
        element: lazyRoute(<MarketplacePage />, paths.marketplace),
      },
      {
        path: paths.internships,
        element: lazyRoute(<InternshipsPage />, paths.internships),
      },
      {
        path: paths.events,
        element: lazyRoute(<EventsPage />, paths.events),
      },
      {
        path: paths.leaderboard,
        element: lazyRoute(<LeaderboardPage />, paths.leaderboard),
      },
      {
        path: paths.assistant,
        element: lazyRoute(<AiAssistantPage />, paths.assistant),
      },
      {
        path: paths.settings,
        element: lazyRoute(<SettingsPage />, paths.settings),
      },
    ],
  },
  { path: "*", element: <NotFoundPage /> },
]);
