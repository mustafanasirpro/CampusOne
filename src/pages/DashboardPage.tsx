import {
  Bell,
  Bot,
  BriefcaseBusiness,
  CalendarDays,
  FilePlus2,
  FileText,
  MessageSquarePlus,
  MessageSquareText,
  Search,
  ShieldCheck,
  ShoppingBag,
  Sparkles,
  Trophy,
  UserRound,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { getNoteManagementStatus } from "@/api/notesApi";
import { useAuth } from "@/auth/useAuth";
import {
  Badge,
  Card,
  CardContent,
  PageHeader,
  SearchBar,
  SectionTitle,
} from "@/components/common";
import { paths } from "@/routes/paths";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

const workspaceCards = [
  {
    description: "Browse, download, rate, and bookmark study material.",
    icon: FileText,
    label: "Notes",
    path: paths.notes,
    tone: "bg-brand-50 text-brand-700",
  },
  {
    description: "Ask questions, share answers, vote, and resolve discussions.",
    icon: MessageSquareText,
    label: "Discussions",
    path: paths.discussions,
    tone: "bg-emerald-50 text-emerald-700",
  },
  {
    description: "Discover student listings or publish something to sell.",
    icon: ShoppingBag,
    label: "Marketplace",
    path: paths.marketplace,
    tone: "bg-violet-50 text-violet-700",
  },
  {
    description: "Find campus activities, organize events, and join in.",
    icon: CalendarDays,
    label: "Events",
    path: paths.events,
    tone: "bg-amber-50 text-amber-700",
  },
  {
    description: "Explore opportunities and save internships for later.",
    icon: BriefcaseBusiness,
    label: "Internships",
    path: paths.internships,
    tone: "bg-sky-50 text-sky-700",
  },
  {
    description: "Review replies, reminders, and important account updates.",
    icon: Bell,
    label: "Notifications",
    path: paths.notifications,
    tone: "bg-rose-50 text-rose-700",
  },
  {
    description: "Search notes, listings, questions, events, and internships.",
    icon: Search,
    label: "Global Search",
    path: paths.search,
    tone: "bg-cyan-50 text-cyan-700",
  },
  {
    description: "Track XP, earned badges, and community rankings.",
    icon: Trophy,
    label: "Leaderboard",
    path: paths.leaderboard,
    tone: "bg-yellow-50 text-yellow-700",
  },
  {
    description: "Chat, explain concepts, and generate study resources.",
    icon: Bot,
    label: "AI Assistant",
    path: paths.assistant,
    tone: "bg-fuchsia-50 text-fuchsia-700",
  },
  {
    description: "Submit reports and access moderation tools when authorized.",
    icon: ShieldCheck,
    label: "Moderation",
    path: paths.admin,
    tone: "bg-slate-100 text-slate-700",
  },
] as const;

const quickActions = [
  {
    adminOnly: true,
    icon: FilePlus2,
    label: "Upload a note",
    path: paths.noteNew,
  },
  {
    adminOnly: false,
    icon: MessageSquarePlus,
    label: "Ask a question",
    path: paths.discussionQuestionNew,
  },
  {
    adminOnly: false,
    icon: CalendarDays,
    label: "Create an event",
    path: paths.eventNew,
  },
  {
    adminOnly: false,
    icon: BriefcaseBusiness,
    label: "Share an internship",
    path: paths.internshipNew,
  },
] as const;

function greeting() {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}

export function DashboardPage() {
  const navigate = useNavigate();
  const { currentUser } = useAuth();
  const [searchValue, setSearchValue] = useState("");
  const [canManageNotes, setCanManageNotes] = useState(false);
  const firstName = currentUser?.fullName.trim().split(/\s+/)[0] || "Student";
  const currentDate = useMemo(
    () =>
      new Intl.DateTimeFormat("en-PK", {
        dateStyle: "full",
      }).format(new Date()),
    [],
  );

  useDocumentTitle("Home · CampusOne");

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    void getNoteManagementStatus(controller.signal)
      .then((status) => {
        if (active) setCanManageNotes(status.canManage);
      })
      .catch(() => {
        if (active) setCanManageNotes(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, []);

  const search = (value: string) => {
    const query = value.trim();
    navigate(
      query
        ? `${paths.search}?q=${encodeURIComponent(query)}`
        : paths.search,
    );
  };

  return (
    <div className="grid gap-8 pb-8">
      <PageHeader
        description="Your university community, study tools, and opportunities are ready in one place."
        eyebrow="Student workspace"
        title={`${greeting()}, ${firstName}`}
      />

      <SearchBar
        className="lg:hidden"
        onSearch={search}
        onValueChange={setSearchValue}
        placeholder="Search across CampusOne..."
        value={searchValue}
      />

      <section className="relative overflow-hidden rounded-3xl bg-slate-950 p-6 text-white shadow-xl sm:p-8">
        <div className="absolute -left-20 top-0 size-72 rounded-full bg-brand-600/25 blur-3xl" />
        <div className="absolute -bottom-28 right-0 size-80 rounded-full bg-emerald-500/15 blur-3xl" />
        <div className="relative flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="max-w-2xl">
            <Badge className="bg-white/10 text-brand-100 ring-white/10">
              {currentDate}
            </Badge>
            <h2 className="mt-5 text-2xl font-bold tracking-tight sm:text-3xl">
              What will you accomplish today?
            </h2>
            <p className="mt-3 text-sm leading-7 text-slate-300 sm:text-base">
              Pick up your coursework, contribute to the community, discover
              an opportunity, or ask the study assistant for a fresh
              explanation.
            </p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row lg:flex-col">
            <Link
              className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-white px-5 text-sm font-semibold text-slate-950 transition hover:bg-brand-50"
              to={paths.notes}
            >
              <FileText className="size-4" />
              Explore notes
            </Link>
            <Link
              className="inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-white/20 bg-white/10 px-5 text-sm font-semibold text-white transition hover:bg-white/15"
              to={paths.assistant}
            >
              <Sparkles className="size-4" />
              Open AI Assistant
            </Link>
          </div>
        </div>
      </section>

      <section>
        <SectionTitle
          description="Common contribution flows, one click away."
          title="Quick actions"
        />
        <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {quickActions
            .filter((action) => !action.adminOnly || canManageNotes)
            .map((action) => (
              <Link
                className="group flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 font-semibold text-slate-800 shadow-sm transition hover:-translate-y-0.5 hover:border-brand-200 hover:text-brand-700 hover:shadow-card"
                key={action.path}
                to={action.path}
              >
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700 transition group-hover:bg-brand-600 group-hover:text-white">
                  <action.icon className="size-4.5" />
                </span>
                {action.label}
              </Link>
            ))}
        </div>
      </section>

      <section>
        <SectionTitle
          description="Every connected CampusOne module is available from this workspace."
          title="Explore CampusOne"
        />
        <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {workspaceCards.map((item) => (
            <Link
              className="group rounded-2xl focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
              key={item.path}
              to={item.path}
            >
              <Card className="h-full transition group-hover:-translate-y-0.5 group-hover:border-brand-200 group-hover:shadow-xl">
                <CardContent className="flex h-full items-start gap-4 p-5">
                  <span
                    className={`grid size-11 shrink-0 place-items-center rounded-xl ${item.tone}`}
                  >
                    <item.icon className="size-5" />
                  </span>
                  <span>
                    <span className="font-semibold text-slate-950">
                      {item.label}
                    </span>
                    <span className="mt-1 block text-sm leading-6 text-slate-500">
                      {item.description}
                    </span>
                  </span>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      </section>

      <Card className="border-brand-200 bg-brand-50/50">
        <CardContent className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
          <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-brand-100 text-brand-700">
            <UserRound className="size-5" />
          </span>
          <div className="flex-1">
            <h2 className="font-semibold text-slate-950">
              Keep your profile current
            </h2>
            <p className="mt-1 text-sm leading-6 text-slate-600">
              Your profile helps classmates recognize your department, skills,
              and academic interests.
            </p>
          </div>
          <Link
            className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-brand-200 bg-white px-4 text-sm font-semibold text-brand-700 transition hover:bg-brand-100"
            to={paths.profile}
          >
            View profile
          </Link>
        </CardContent>
      </Card>

    </div>
  );
}
