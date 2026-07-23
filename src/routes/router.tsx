import { lazy, Suspense, type ReactNode } from "react";
import { Navigate, createBrowserRouter } from "react-router-dom";

import { GuestRoute, ProtectedRoute } from "@/auth/ProtectedRoute";
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

const NoteDetailPage = lazy(async () => {
  const module = await import("@/pages/NoteDetailPage");
  return { default: module.NoteDetailPage };
});

const CreateNotePage = lazy(async () => {
  const module = await import("@/pages/CreateNotePage");
  return { default: module.CreateNotePage };
});

const EditNotePage = lazy(async () => {
  const module = await import("@/pages/EditNotePage");
  return { default: module.EditNotePage };
});

const DiscussionsPage = lazy(async () => {
  const module = await import("@/pages/DiscussionsPage");
  return { default: module.DiscussionsPage };
});

const DiscussionQuestionDetailPage = lazy(async () => {
  const module = await import("@/pages/DiscussionQuestionDetailPage");
  return { default: module.DiscussionQuestionDetailPage };
});

const CreateDiscussionQuestionPage = lazy(async () => {
  const module = await import("@/pages/CreateDiscussionQuestionPage");
  return { default: module.CreateDiscussionQuestionPage };
});

const EditDiscussionQuestionPage = lazy(async () => {
  const module = await import("@/pages/EditDiscussionQuestionPage");
  return { default: module.EditDiscussionQuestionPage };
});

const MarketplacePage = lazy(async () => {
  const module = await import("@/pages/MarketplacePage");
  return { default: module.MarketplacePage };
});

const MarketplaceListingDetailPage = lazy(async () => {
  const module = await import("@/pages/MarketplaceListingDetailPage");
  return { default: module.MarketplaceListingDetailPage };
});

const LostFoundPage = lazy(async () => {
  const module = await import("@/pages/LostFoundPage");
  return { default: module.LostFoundPage };
});

const LostFoundItemDetailPage = lazy(async () => {
  const module = await import("@/pages/LostFoundItemDetailPage");
  return { default: module.LostFoundItemDetailPage };
});

const CreateLostFoundItemPage = lazy(async () => {
  const module = await import("@/pages/CreateLostFoundItemPage");
  return { default: module.CreateLostFoundItemPage };
});

const EditLostFoundItemPage = lazy(async () => {
  const module = await import("@/pages/EditLostFoundItemPage");
  return { default: module.EditLostFoundItemPage };
});

const LostFoundClaimsPage = lazy(async () => {
  const module = await import("@/pages/LostFoundClaimsPage");
  return { default: module.LostFoundClaimsPage };
});

const CreateMarketplaceListingPage = lazy(async () => {
  const module = await import("@/pages/CreateMarketplaceListingPage");
  return { default: module.CreateMarketplaceListingPage };
});

const EditMarketplaceListingPage = lazy(async () => {
  const module = await import("@/pages/EditMarketplaceListingPage");
  return { default: module.EditMarketplaceListingPage };
});

const InternshipsPage = lazy(async () => {
  const module = await import("@/pages/InternshipsPage");
  return { default: module.InternshipsPage };
});

const InternshipDetailPage = lazy(async () => {
  const module = await import("@/pages/InternshipDetailPage");
  return { default: module.InternshipDetailPage };
});

const CreateInternshipPage = lazy(async () => {
  const module = await import("@/pages/CreateInternshipPage");
  return { default: module.CreateInternshipPage };
});

const EditInternshipPage = lazy(async () => {
  const module = await import("@/pages/EditInternshipPage");
  return { default: module.EditInternshipPage };
});

const EventsPage = lazy(async () => {
  const module = await import("@/pages/EventsPage");
  return { default: module.EventsPage };
});

const EventDetailPage = lazy(async () => {
  const module = await import("@/pages/EventDetailPage");
  return { default: module.EventDetailPage };
});

const CreateEventPage = lazy(async () => {
  const module = await import("@/pages/CreateEventPage");
  return { default: module.CreateEventPage };
});

const EditEventPage = lazy(async () => {
  const module = await import("@/pages/EditEventPage");
  return { default: module.EditEventPage };
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

const NotificationsPage = lazy(async () => {
  const module = await import("@/pages/NotificationsPage");
  return { default: module.NotificationsPage };
});

const SearchPage = lazy(async () => {
  const module = await import("@/pages/SearchPage");
  return { default: module.SearchPage };
});

const AdminPage = lazy(async () => {
  const module = await import("@/pages/AdminPage");
  return { default: module.AdminPage };
});

const AuraWorkbenchPage = lazy(async () => {
  const module = await import("@/pages/AuraWorkbenchPage");
  return { default: module.AuraWorkbenchPage };
});

const PersonalTimetablePage = lazy(async () => {
  const module = await import("@/pages/PersonalTimetablePage");
  return { default: module.PersonalTimetablePage };
});

const InstructorTimetablePage = lazy(async () => {
  const module = await import("@/pages/InstructorTimetablePage");
  return { default: module.InstructorTimetablePage };
});

const ForgotPasswordPage = lazy(async () => {
  const module = await import("@/pages/ForgotPasswordPage");
  return { default: module.ForgotPasswordPage };
});

const ResetPasswordPage = lazy(async () => {
  const module = await import("@/pages/ResetPasswordPage");
  return { default: module.ResetPasswordPage };
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
      {
        element: <GuestRoute />,
        children: [
          { path: paths.login, element: <LoginPage /> },
          { path: paths.signup, element: <SignupPage /> },
          {
            path: paths.forgotPassword,
            element: lazyRoute(
              <ForgotPasswordPage />,
              paths.forgotPassword,
            ),
          },
          {
            path: paths.resetPassword,
            element: lazyRoute(<ResetPasswordPage />, paths.resetPassword),
          },
        ],
      },
    ],
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          {
            path: paths.dashboard,
            element: lazyRoute(<DashboardPage />, paths.dashboard),
          },
          {
            path: "/home",
            element: <Navigate replace to={paths.dashboard} />,
          },
          {
            path: paths.profile,
            element: lazyRoute(<ProfilePage />, paths.profile),
          },
          {
            path: paths.notes,
            element: lazyRoute(<NotesPage />, paths.notes),
          },
          {
            path: paths.noteNew,
            element: lazyRoute(<CreateNotePage />, paths.noteNew),
          },
          {
            path: "/notes/:noteId/edit",
            element: lazyRoute(<EditNotePage />, paths.notes),
          },
          {
            path: "/notes/:noteId",
            element: lazyRoute(<NoteDetailPage />, paths.notes),
          },
          {
            path: paths.discussions,
            element: lazyRoute(<DiscussionsPage />, paths.discussions),
          },
          {
            path: paths.discussionQuestionNew,
            element: lazyRoute(
              <CreateDiscussionQuestionPage />,
              paths.discussionQuestionNew,
            ),
          },
          {
            path: "/discussions/questions/:questionId/edit",
            element: lazyRoute(
              <EditDiscussionQuestionPage />,
              paths.discussions,
            ),
          },
          {
            path: "/discussions/questions/:questionId",
            element: lazyRoute(
              <DiscussionQuestionDetailPage />,
              paths.discussions,
            ),
          },
          {
            path: paths.marketplace,
            element: lazyRoute(<MarketplacePage />, paths.marketplace),
          },
          {
            path: paths.marketplaceNew,
            element: lazyRoute(
              <CreateMarketplaceListingPage />,
              paths.marketplaceNew,
            ),
          },
          {
            path: "/marketplace/:listingId/edit",
            element: lazyRoute(
              <EditMarketplaceListingPage />,
              paths.marketplace,
            ),
          },
          {
            path: "/marketplace/:listingId",
            element: lazyRoute(
              <MarketplaceListingDetailPage />,
              paths.marketplace,
            ),
          },
          {
            path: paths.lostFound,
            element: lazyRoute(<LostFoundPage />, paths.lostFound),
          },
          {
            path: paths.lostFoundNew,
            element: lazyRoute(
              <CreateLostFoundItemPage />,
              paths.lostFoundNew,
            ),
          },
          {
            path: paths.lostFoundClaims,
            element: lazyRoute(
              <LostFoundClaimsPage />,
              paths.lostFoundClaims,
            ),
          },
          {
            path: "/lost-found/:itemId/edit",
            element: lazyRoute(<EditLostFoundItemPage />, paths.lostFound),
          },
          {
            path: "/lost-found/:itemId",
            element: lazyRoute(
              <LostFoundItemDetailPage />,
              paths.lostFound,
            ),
          },
          {
            path: paths.internships,
            element: lazyRoute(<InternshipsPage />, paths.internships),
          },
          {
            path: paths.internshipNew,
            element: lazyRoute(
              <CreateInternshipPage />,
              paths.internshipNew,
            ),
          },
          {
            path: "/internships/:internshipId/edit",
            element: lazyRoute(
              <EditInternshipPage />,
              paths.internships,
            ),
          },
          {
            path: "/internships/:internshipId",
            element: lazyRoute(
              <InternshipDetailPage />,
              paths.internships,
            ),
          },
          {
            path: paths.events,
            element: lazyRoute(<EventsPage />, paths.events),
          },
          {
            path: paths.eventNew,
            element: lazyRoute(<CreateEventPage />, paths.eventNew),
          },
          {
            path: "/events/:eventId/edit",
            element: lazyRoute(<EditEventPage />, paths.events),
          },
          {
            path: "/events/:eventId",
            element: lazyRoute(<EventDetailPage />, paths.events),
          },
          {
            path: paths.notifications,
            element: lazyRoute(
              <NotificationsPage />,
              paths.notifications,
            ),
          },
          {
            path: paths.search,
            element: lazyRoute(<SearchPage />, paths.search),
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
            path: "/ai-assistant",
            element: <Navigate replace to={paths.assistant} />,
          },
          {
            path: paths.admin,
            element: lazyRoute(<AdminPage />, paths.admin),
          },
          {
            path: paths.adminAura,
            element: lazyRoute(<AuraWorkbenchPage />, paths.adminAura),
          },
          {
            path: paths.timetable,
            element: lazyRoute(<PersonalTimetablePage />, paths.timetable),
          },
          {
            path: paths.instructorTimetable,
            element: lazyRoute(
              <InstructorTimetablePage />,
              paths.instructorTimetable,
            ),
          },
          {
            path: paths.settings,
            element: lazyRoute(<SettingsPage />, paths.settings),
          },
        ],
      },
    ],
  },
  { path: "*", element: <NotFoundPage /> },
]);
