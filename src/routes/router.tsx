import { Navigate, createBrowserRouter } from "react-router-dom";

import { AppLayout, PublicLayout } from "@/components/layout";
import {
  AiAssistantPage,
  DashboardPage,
  DiscussionsPage,
  EventsPage,
  InternshipsPage,
  LandingPage,
  LeaderboardPage,
  LoginPage,
  MarketplacePage,
  NotFoundPage,
  NotesPage,
  ProfilePage,
  SettingsPage,
  SignupPage,
} from "@/pages";
import { paths } from "@/routes/paths";

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
      { path: paths.dashboard, element: <DashboardPage /> },
      { path: "/home", element: <Navigate replace to={paths.dashboard} /> },
      { path: paths.profile, element: <ProfilePage /> },
      { path: paths.notes, element: <NotesPage /> },
      { path: paths.discussions, element: <DiscussionsPage /> },
      { path: paths.marketplace, element: <MarketplacePage /> },
      { path: paths.internships, element: <InternshipsPage /> },
      { path: paths.events, element: <EventsPage /> },
      { path: paths.leaderboard, element: <LeaderboardPage /> },
      { path: paths.assistant, element: <AiAssistantPage /> },
      { path: paths.settings, element: <SettingsPage /> },
    ],
  },
  { path: "*", element: <NotFoundPage /> },
]);

