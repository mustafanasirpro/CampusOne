import {
  ArrowRight,
  BookOpenCheck,
  CalendarDays,
  CheckCircle2,
  ChevronRight,
  Download,
  FileText,
  GraduationCap,
  MessageCircle,
  Star,
  ThumbsUp,
} from "lucide-react";
import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { EventCard, InternshipCard, StatCard } from "@/components/cards";
import {
  Avatar,
  Badge,
  Button,
  Card,
  CardContent,
  SearchBar,
  SectionTitle,
  useToast,
} from "@/components/common";
import {
  dashboardAnnouncements,
  dashboardDiscussions,
  dashboardEvents,
  dashboardInternships,
  dashboardNotes,
  dashboardQuickActions,
  dashboardStats,
  dashboardStudent,
  dashboardStudyPrompt,
} from "@/data/dashboard";
import { paths } from "@/routes/paths";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

const announcementTones = {
  brand: {
    icon: "bg-brand-50 text-brand-600",
    accent: "bg-brand-500",
  },
  emerald: {
    icon: "bg-emerald-50 text-emerald-600",
    accent: "bg-emerald-500",
  },
  amber: {
    icon: "bg-amber-50 text-amber-600",
    accent: "bg-amber-500",
  },
  rose: {
    icon: "bg-rose-50 text-rose-600",
    accent: "bg-rose-500",
  },
};

const quickActionTones = {
  brand: "bg-brand-50 text-brand-600 group-hover:bg-brand-600",
  emerald: "bg-emerald-50 text-emerald-600 group-hover:bg-emerald-600",
  amber: "bg-amber-50 text-amber-600 group-hover:bg-amber-500",
  sky: "bg-sky-50 text-sky-600 group-hover:bg-sky-600",
};

function getGreeting() {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}

export function DashboardPage() {
  const [mobileSearch, setMobileSearch] = useState("");
  const [savedInternships, setSavedInternships] = useState<Set<string>>(
    new Set(),
  );
  const [rsvpedEvents, setRsvpedEvents] = useState<Set<string>>(new Set());
  const { showToast } = useToast();
  const navigate = useNavigate();
  const currentDate = useMemo(
    () =>
      new Intl.DateTimeFormat("en-PK", {
        weekday: "long",
        month: "long",
        day: "numeric",
        year: "numeric",
      }).format(new Date()),
    [],
  );

  useDocumentTitle("Home · CampusOne");

  const handleMobileSearch = (value: string) => {
    showToast({
      title: "Search ready",
      message: value
        ? `Searching CampusOne for “${value}” in this frontend demo.`
        : "Type a note, discussion, event, or opportunity.",
    });
  };

  const toggleInternship = (id: string, role: string) => {
    const isSaved = savedInternships.has(id);
    setSavedInternships((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
    showToast({
      title: isSaved ? "Removed from saved" : "Internship saved",
      message: role,
      variant: isSaved ? "info" : "success",
    });
  };

  const handleRsvp = (title: string) => {
    const isAttending = rsvpedEvents.has(title);
    setRsvpedEvents((current) => {
      const next = new Set(current);
      if (next.has(title)) next.delete(title);
      else next.add(title);
      return next;
    });
    showToast({
      title: isAttending ? "RSVP removed" : "You’re on the list",
      message: title,
      variant: isAttending ? "info" : "success",
    });
  };

  return (
    <div className="grid gap-8 pb-8">
      <SearchBar
        className="lg:hidden"
        onSearch={handleMobileSearch}
        onValueChange={setMobileSearch}
        placeholder="Search notes, discussions, events..."
        value={mobileSearch}
      />

      <section className="relative overflow-hidden rounded-3xl bg-slate-950 shadow-xl shadow-slate-950/10">
        <div className="absolute -left-20 top-0 size-72 rounded-full bg-brand-600/25 blur-3xl" />
        <div className="absolute -bottom-32 right-0 size-96 rounded-full bg-emerald-500/15 blur-3xl" />
        <div className="relative grid gap-8 p-6 sm:p-8 xl:grid-cols-[1fr_22rem] xl:items-center">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <Badge className="gap-1.5 bg-white/10 text-brand-100 ring-white/10">
                <CalendarDays className="size-3.5" />
                {currentDate}
              </Badge>
              <Badge className="bg-emerald-400/10 text-emerald-200 ring-emerald-300/20">
                Spring 2026
              </Badge>
            </div>
            <h1 className="mt-5 text-3xl font-bold tracking-tight text-white sm:text-4xl">
              {getGreeting()}, {dashboardStudent.name.split(" ")[0]}!
            </h1>
            <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-slate-300">
              <span className="flex items-center gap-1.5">
                <GraduationCap className="size-4 text-brand-300" />
                {dashboardStudent.university}
              </span>
              <span className="hidden size-1 rounded-full bg-slate-600 sm:block" />
              <span>{dashboardStudent.department}</span>
              <span className="hidden size-1 rounded-full bg-slate-600 sm:block" />
              <span>{dashboardStudent.semester}</span>
            </div>
            <p className="mt-5 max-w-2xl text-sm leading-6 text-slate-300 sm:text-base">
              Small, focused steps beat last-minute stress. You’re making good
              progress—keep the momentum going today.
            </p>
          </div>

          <div className="rounded-2xl border border-white/10 bg-white/[0.07] p-5 backdrop-blur">
            <div className="flex items-start gap-3">
              <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-500/20 text-brand-200">
                <dashboardStudyPrompt.icon className="size-5" />
              </span>
              <div>
                <p className="text-sm font-semibold text-white">
                  {dashboardStudyPrompt.title}
                </p>
                <p className="mt-1 text-xs leading-5 text-slate-400">
                  {dashboardStudyPrompt.description}
                </p>
              </div>
            </div>
            <div className="mt-4">
              <div className="flex items-center justify-between text-xs">
                <span className="font-medium text-slate-300">Weekly goal</span>
                <span className="font-semibold text-brand-200">
                  {dashboardStudent.weeklyGoal}%
                </span>
              </div>
              <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-white/10">
                <div
                  aria-label="Weekly study goal"
                  aria-valuemax={100}
                  aria-valuemin={0}
                  aria-valuenow={dashboardStudent.weeklyGoal}
                  className="h-full rounded-full bg-gradient-to-r from-brand-400 to-emerald-400 transition-[width] duration-700 ease-out"
                  role="progressbar"
                  style={{ width: `${dashboardStudent.weeklyGoal}%` }}
                />
              </div>
            </div>
            <Link
              className="mt-4 inline-flex items-center gap-1.5 text-xs font-semibold text-brand-200 transition hover:text-white"
              to={paths.assistant}
            >
              Build my study plan
              <ArrowRight className="size-3.5" />
            </Link>
          </div>
        </div>
      </section>

      <section aria-labelledby="dashboard-stats">
        <h2 className="sr-only" id="dashboard-stats">
          Campus statistics
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {dashboardStats.map((stat) => (
            <div
              className="transition duration-200 hover:-translate-y-1 [&>div]:h-full [&>div]:transition-shadow [&>div]:hover:shadow-xl"
              key={stat.label}
            >
              <StatCard
                change={stat.change}
                icon={stat.icon}
                label={stat.label}
                value={stat.value.toLocaleString()}
              />
            </div>
          ))}
        </div>
      </section>

      <section>
        <SectionTitle
          description="Jump back into the things students do most."
          title="Quick actions"
        />
        <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {dashboardQuickActions.map((action) => (
            <Link
              className="group flex items-center gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition duration-200 hover:-translate-y-1 hover:border-brand-200 hover:shadow-lg"
              key={action.label}
              to={action.path}
            >
              <span
                className={cn(
                  "grid size-11 shrink-0 place-items-center rounded-xl transition duration-200 group-hover:text-white",
                  quickActionTones[action.tone],
                )}
              >
                <action.icon className="size-5" />
              </span>
              <span className="min-w-0">
                <span className="block text-sm font-semibold text-slate-900">
                  {action.label}
                </span>
                <span className="mt-0.5 block text-xs text-slate-500">
                  {action.description}
                </span>
              </span>
              <ChevronRight className="ml-auto size-4 text-slate-300 transition group-hover:translate-x-0.5 group-hover:text-brand-500" />
            </Link>
          ))}
        </div>
      </section>

      <section>
        <SectionTitle
          action={
            <Button
              onClick={() =>
                showToast({
                  title: "Campus announcements",
                  message: "You are viewing the latest four campus updates.",
                })
              }
              size="sm"
              variant="ghost"
            >
              View all
              <ArrowRight className="size-3.5" />
            </Button>
          }
          description="Important updates from your university community."
          title="Campus announcements"
        />
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {dashboardAnnouncements.map((announcement) => {
            const tone = announcementTones[announcement.tone];

            return (
              <Card
                className="group relative h-full overflow-hidden transition duration-200 hover:-translate-y-1 hover:shadow-xl"
                key={announcement.id}
              >
                <span
                  className={cn(
                    "absolute inset-x-0 top-0 h-1",
                    tone.accent,
                  )}
                />
                <CardContent className="flex h-full flex-col p-5">
                  <div className="flex items-start justify-between gap-3">
                    <span
                      className={cn(
                        "grid size-10 place-items-center rounded-xl",
                        tone.icon,
                      )}
                    >
                      <announcement.icon className="size-4.5" />
                    </span>
                    <Badge>{announcement.category}</Badge>
                  </div>
                  <h3 className="mt-4 font-semibold leading-6 text-slate-950">
                    {announcement.title}
                  </h3>
                  <p className="mt-2 flex-1 text-sm leading-6 text-slate-500">
                    {announcement.description}
                  </p>
                  <button
                    className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3 text-xs font-semibold text-brand-700"
                    onClick={() =>
                      showToast({
                        title: announcement.title,
                        message: announcement.description,
                      })
                    }
                    type="button"
                  >
                    <span className="font-medium text-slate-400">
                      {announcement.posted}
                    </span>
                    <span className="flex items-center gap-1">
                      View update
                      <ChevronRight className="size-3.5 transition group-hover:translate-x-0.5" />
                    </span>
                  </button>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </section>

      <div className="grid gap-8 2xl:grid-cols-2">
        <section>
          <SectionTitle
            action={
              <Link
                className="inline-flex items-center gap-1 text-sm font-semibold text-brand-700 hover:text-brand-800"
                to={paths.notes}
              >
                Browse notes
                <ArrowRight className="size-3.5" />
              </Link>
            }
            description="Highly rated resources from your courses."
            title="Recent notes"
          />
          <div className="mt-4 grid gap-3">
            {dashboardNotes.map((note) => (
              <Card
                className="group transition duration-200 hover:border-brand-200 hover:shadow-lg"
                key={note.id}
              >
                <CardContent className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center">
                  <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-brand-50 text-brand-600">
                    <FileText className="size-5" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge variant="brand">{note.course}</Badge>
                      <span className="flex items-center gap-1 text-xs font-semibold text-amber-600">
                        <Star className="size-3.5 fill-amber-400 text-amber-400" />
                        {note.rating}
                      </span>
                    </div>
                    <h3 className="mt-2 truncate font-semibold text-slate-950">
                      {note.title}
                    </h3>
                    <p className="mt-1 text-xs text-slate-500">
                      {note.teacher} · Uploaded by {note.uploadedBy} ·{" "}
                      {note.pages} pages
                    </p>
                  </div>
                  <Button
                    className="w-full sm:w-auto"
                    onClick={() =>
                      showToast({
                        title: "Download started",
                        message: `${note.title} is a demo file.`,
                        variant: "success",
                      })
                    }
                    size="sm"
                    variant="outline"
                  >
                    <Download className="size-3.5" />
                    Download
                  </Button>
                </CardContent>
              </Card>
            ))}
          </div>
        </section>

        <section>
          <SectionTitle
            action={
              <Link
                className="inline-flex items-center gap-1 text-sm font-semibold text-brand-700 hover:text-brand-800"
                to={paths.discussions}
              >
                All discussions
                <ArrowRight className="size-3.5" />
              </Link>
            }
            description="Conversations students are joining right now."
            title="Trending discussions"
          />
          <div className="mt-4 grid gap-3">
            {dashboardDiscussions.map((discussion) => (
              <Card
                className="group transition duration-200 hover:border-brand-200 hover:shadow-lg"
                key={discussion.id}
              >
                <CardContent className="p-4">
                  <div className="flex items-center gap-3">
                    <Avatar name={discussion.author} size="sm" />
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-slate-800">
                        {discussion.author}
                      </p>
                      <p className="text-xs text-slate-400">
                        {discussion.time}
                      </p>
                    </div>
                    <Badge className="ml-auto">{discussion.category}</Badge>
                  </div>
                  <h3 className="mt-3 font-semibold leading-6 text-slate-950 transition group-hover:text-brand-700">
                    {discussion.title}
                  </h3>
                  <div className="mt-4 flex flex-wrap items-center gap-4">
                    <span className="flex items-center gap-1.5 text-xs font-medium text-slate-500">
                      <ThumbsUp className="size-3.5" />
                      {discussion.upvotes} upvotes
                    </span>
                    <span className="flex items-center gap-1.5 text-xs font-medium text-slate-500">
                      <MessageCircle className="size-3.5" />
                      {discussion.comments} comments
                    </span>
                    <Button
                      className="ml-auto"
                      onClick={() => navigate(paths.discussions)}
                      size="sm"
                      variant="ghost"
                    >
                      View discussion
                      <ChevronRight className="size-3.5" />
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </section>
      </div>

      <section>
        <SectionTitle
          action={
            <Link
              className="inline-flex items-center gap-1 text-sm font-semibold text-brand-700 hover:text-brand-800"
              to={paths.events}
            >
              View calendar
              <ArrowRight className="size-3.5" />
            </Link>
          }
          description="Workshops, societies, sports, and campus experiences."
          title="Upcoming events"
        />
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {dashboardEvents.map((event) => (
            <div
              className="transition duration-200 hover:-translate-y-1 [&>div]:h-full [&>div]:transition-shadow [&>div]:hover:shadow-xl"
              key={event.title}
            >
              <EventCard
                event={event}
                onRsvp={() => handleRsvp(event.title)}
              />
            </div>
          ))}
        </div>
      </section>

      <section>
        <SectionTitle
          action={
            <Link
              className="inline-flex items-center gap-1 text-sm font-semibold text-brand-700 hover:text-brand-800"
              to={paths.internships}
            >
              Explore opportunities
              <ArrowRight className="size-3.5" />
            </Link>
          }
          description="Hand-picked roles with upcoming application deadlines."
          title="Internship highlights"
        />
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {dashboardInternships.map((internship) => (
            <div
              className="transition duration-200 hover:-translate-y-1 [&>div]:h-full [&>div]:transition-shadow [&>div]:hover:shadow-xl"
              key={internship.id}
            >
              <InternshipCard
                internship={{
                  ...internship,
                  saved: savedInternships.has(internship.id),
                }}
                onApply={() =>
                  showToast({
                    title: "Application link ready",
                    message: `${internship.company} applications are a frontend demo.`,
                  })
                }
                onSave={() =>
                  toggleInternship(internship.id, internship.role)
                }
              />
            </div>
          ))}
        </div>
      </section>

      <Card className="overflow-hidden border-brand-200 bg-gradient-to-r from-brand-50 via-white to-emerald-50">
        <CardContent className="flex flex-col gap-5 p-6 sm:flex-row sm:items-center">
          <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-brand-600 text-white shadow-lg shadow-brand-600/20">
            <BookOpenCheck className="size-5" />
          </span>
          <div className="flex-1">
            <h2 className="font-semibold text-slate-950">
              You’re building a strong study streak
            </h2>
            <p className="mt-1 text-sm leading-6 text-slate-500">
              Three focused sessions this week. Complete one more to unlock the
              Consistent Learner badge.
            </p>
          </div>
          <div className="flex items-center gap-2 text-sm font-semibold text-emerald-700">
            <CheckCircle2 className="size-5" />
            3 of 4 sessions
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
