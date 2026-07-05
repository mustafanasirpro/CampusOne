import {
  ArrowRight,
  Bell,
  Bot,
  CalendarDays,
  Check,
  ChevronDown,
  Clock3,
  Download,
  FileText,
  MessageCircle,
  Search,
  Sparkles,
  TrendingUp,
  UsersRound,
} from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";

import { Avatar, Badge, Card, CardContent, Reveal } from "@/components/common";
import { CampusOneLogo } from "@/components/layout";
import {
  campusOneBenefits,
  landingFaqs,
  landingFeatures,
} from "@/data/landing";
import { paths } from "@/routes/paths";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

function DashboardMockup() {
  return (
    <div
      aria-label="CampusOne student dashboard preview"
      className="relative mx-auto w-full max-w-4xl"
      role="img"
    >
      <div className="absolute -inset-8 -z-10 rounded-[3rem] bg-brand-300/20 blur-3xl" />
      <div className="overflow-hidden rounded-[1.75rem] border border-slate-700 bg-slate-950 p-2 shadow-[0_30px_100px_-30px_rgba(15,23,42,0.6)] sm:p-3">
        <div className="overflow-hidden rounded-[1.25rem] bg-slate-50">
          <div className="flex h-11 items-center border-b border-slate-200 bg-white px-4">
            <div className="flex gap-1.5">
              <span className="size-2.5 rounded-full bg-red-400" />
              <span className="size-2.5 rounded-full bg-amber-400" />
              <span className="size-2.5 rounded-full bg-emerald-400" />
            </div>
            <div className="mx-auto flex h-6 w-48 items-center gap-2 rounded-lg bg-slate-100 px-2 text-[8px] text-slate-400 sm:w-64">
              <Search className="size-2.5" />
              Search your campus
            </div>
          </div>

          <div className="flex min-h-[390px] sm:min-h-[460px]">
            <aside className="hidden w-36 shrink-0 border-r border-slate-200 bg-white p-3 sm:block lg:w-44">
              <div className="flex items-center gap-2 px-1 py-2">
                <span className="grid size-7 place-items-center rounded-lg bg-brand-600 text-white">
                  <Sparkles className="size-3.5" />
                </span>
                <span className="text-[10px] font-bold text-slate-900">
                  CampusOne
                </span>
              </div>
              <div className="mt-4 grid gap-1">
                {[
                  { icon: TrendingUp, label: "Home", active: true },
                  { icon: FileText, label: "Notes" },
                  { icon: MessageCircle, label: "Discussions" },
                  { icon: CalendarDays, label: "Events" },
                  { icon: Bot, label: "AI Assistant" },
                ].map((item) => (
                  <div
                    className={cn(
                      "flex items-center gap-2 rounded-lg px-2 py-2 text-[9px] font-medium",
                      item.active
                        ? "bg-brand-50 text-brand-700"
                        : "text-slate-400",
                    )}
                    key={item.label}
                  >
                    <item.icon className="size-3" />
                    {item.label}
                  </div>
                ))}
              </div>
            </aside>

          <div className="min-w-0 flex-1 p-3 sm:p-5">
              <p className="mb-3 text-[8px] font-semibold uppercase tracking-wider text-brand-600">
                Interface preview · illustrative content
              </p>
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-sm font-bold text-slate-950 sm:text-base">
                    Welcome to your campus workspace
                  </p>
                  <p className="mt-1 text-[8px] text-slate-400 sm:text-[10px]">
                    Here is what is happening at your campus.
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="grid size-7 place-items-center rounded-lg border border-slate-200 bg-white text-slate-400">
                    <Bell className="size-3" />
                  </span>
                  <Avatar name="CampusOne student" size="sm" />
                </div>
              </div>

              <div className="mt-4 grid grid-cols-2 gap-2 lg:grid-cols-4">
                {[
                  { value: "Browse", label: "Notes", icon: FileText },
                  { value: "Ask", label: "Discussions", icon: MessageCircle },
                  { value: "Join", label: "Events", icon: CalendarDays },
                  { value: "Apply", label: "Internships", icon: TrendingUp },
                ].map((stat) => (
                  <div
                    className="rounded-xl border border-slate-200 bg-white p-2.5 shadow-sm sm:p-3"
                    key={stat.label}
                  >
                    <span className="grid size-6 place-items-center rounded-lg bg-brand-50 text-brand-600">
                      <stat.icon className="size-3" />
                    </span>
                    <p className="mt-2 text-sm font-bold text-slate-900 sm:text-base">
                      {stat.value}
                    </p>
                    <p className="text-[8px] text-slate-400 sm:text-[9px]">
                      {stat.label}
                    </p>
                  </div>
                ))}
              </div>

              <div className="mt-3 grid gap-3 lg:grid-cols-[1.25fr_0.75fr]">
                <div className="rounded-xl border border-slate-200 bg-white p-3">
                  <div className="flex items-center justify-between">
                    <p className="text-[10px] font-bold text-slate-800">
                      Trending discussions
                    </p>
                    <span className="text-[8px] font-semibold text-brand-600">
                      View all
                    </span>
                  </div>
                  <div className="mt-3 grid gap-2">
                    {[
                      "Best way to prepare for the OOP final?",
                      "FAST Spring Career Fair updates",
                      "Anyone joining the AI society workshop?",
                    ].map((title, index) => (
                      <div
                        className="flex gap-2 rounded-lg bg-slate-50 p-2.5"
                        key={title}
                      >
                        <span className="grid size-6 shrink-0 place-items-center rounded-md bg-white text-[8px] font-bold text-brand-600 shadow-sm">
                          {index + 1}
                        </span>
                        <div className="min-w-0">
                          <p className="truncate text-[9px] font-semibold text-slate-700">
                            {title}
                          </p>
                          <p className="mt-1 text-[7px] text-slate-400">
                            Example discussion preview {index + 1}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="grid gap-3">
                  <div className="rounded-xl bg-gradient-to-br from-brand-600 to-brand-800 p-3 text-white shadow-lg shadow-brand-600/15">
                    <div className="flex items-center justify-between">
                      <span className="grid size-7 place-items-center rounded-lg bg-white/15">
                        <Bot className="size-3.5" />
                      </span>
                      <Sparkles className="size-3.5 text-brand-200" />
                    </div>
                    <p className="mt-3 text-[10px] font-bold">
                      Ask CampusOne AI
                    </p>
                    <p className="mt-1 text-[8px] leading-3 text-brand-100">
                      Summarize notes or create a study plan.
                    </p>
                    <div className="mt-3 rounded-lg bg-white px-2 py-1.5 text-[8px] font-semibold text-brand-700">
                      Start a conversation
                    </div>
                  </div>
                  <div className="rounded-xl border border-slate-200 bg-white p-3">
                    <div className="flex items-center gap-2">
                      <span className="grid size-7 place-items-center rounded-lg bg-emerald-50 text-emerald-600">
                        <Clock3 className="size-3.5" />
                      </span>
                      <div>
                        <p className="text-[9px] font-bold text-slate-700">
                          Next event
                        </p>
                        <p className="text-[7px] text-slate-400">
                          Hackathon · Friday
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="animate-campus-float absolute -bottom-5 -left-2 hidden items-center gap-2 rounded-2xl border border-slate-200 bg-white p-3 shadow-xl sm:flex">
        <span className="grid size-9 place-items-center rounded-xl bg-emerald-50 text-emerald-600">
          <Download className="size-4" />
        </span>
        <div>
          <p className="text-xs font-bold text-slate-900">Study resources</p>
          <p className="text-[10px] text-slate-400">Browse the notes library</p>
        </div>
      </div>

      <div className="animate-campus-float absolute -right-3 top-20 hidden items-center gap-2 rounded-2xl border border-slate-200 bg-white p-3 shadow-xl [animation-delay:1s] md:flex">
        <span className="grid size-9 place-items-center rounded-xl bg-amber-50 text-amber-600">
          <Sparkles className="size-4" />
        </span>
        <div>
          <p className="text-xs font-bold text-slate-900">Gamification</p>
          <p className="text-[10px] text-slate-400">Earn XP by contributing</p>
        </div>
      </div>
    </div>
  );
}

export function LandingPage() {
  const [openFaq, setOpenFaq] = useState<number | null>(0);

  useDocumentTitle("CampusOne — Everything your campus needs");

  return (
    <div className="overflow-hidden bg-white">
      <section className="relative isolate overflow-hidden px-4 pb-20 pt-16 sm:px-6 sm:pb-28 sm:pt-24 lg:px-8">
        <div
          className="absolute inset-0 -z-20"
          style={{
            backgroundImage:
              "radial-gradient(circle at 18% 15%, rgba(99,102,241,.13), transparent 30%), radial-gradient(circle at 82% 28%, rgba(16,185,129,.09), transparent 28%)",
          }}
        />
        <div className="absolute left-1/2 top-0 -z-10 h-px w-[80%] -translate-x-1/2 bg-gradient-to-r from-transparent via-brand-300 to-transparent" />

        <div className="mx-auto max-w-7xl">
          <Reveal className="mx-auto max-w-4xl text-center">
            <Badge
              className="mx-auto gap-2 border border-brand-200/70 bg-brand-50/80 px-3.5 py-1.5 text-brand-700 shadow-sm"
              variant="brand"
            >
              <Sparkles className="size-3.5" />
              The digital campus, reimagined
            </Badge>
            <h1 className="mx-auto mt-7 max-w-4xl text-4xl font-bold leading-[1.08] tracking-[-0.04em] text-slate-950 sm:text-6xl lg:text-7xl">
              Everything your campus needs,{" "}
              <span className="bg-gradient-to-r from-brand-600 to-campus-600 bg-clip-text text-transparent">
                in one place.
              </span>
            </h1>
            <p className="mx-auto mt-6 max-w-2xl text-base leading-7 text-slate-600 sm:text-lg sm:leading-8">
              Notes, answers, opportunities, events, student deals, and an AI
              study partner—organized around your university community.
            </p>
            <div className="mt-9 flex flex-col items-center justify-center gap-3 sm:flex-row">
              <Link
                className="group inline-flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-brand-600 px-6 text-sm font-semibold text-white shadow-lg shadow-brand-600/20 transition duration-200 hover:-translate-y-0.5 hover:bg-brand-700 hover:shadow-xl sm:w-auto"
                to={paths.signup}
              >
                Get started free
                <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" />
              </Link>
              <Link
                className="inline-flex h-12 w-full items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-6 text-sm font-semibold text-slate-700 shadow-sm transition duration-200 hover:-translate-y-0.5 hover:border-slate-400 hover:bg-slate-50 sm:w-auto"
                to={paths.dashboard}
              >
                Explore campus
              </Link>
            </div>
            <div className="mt-8 text-sm text-slate-500">
              Built for focused university communities.
            </div>
          </Reveal>

          <Reveal className="mt-16 sm:mt-20" delay={120}>
            <DashboardMockup />
          </Reveal>
        </div>
      </section>

      <section
        className="scroll-mt-24 border-y border-slate-200 bg-slate-50 px-4 py-20 sm:px-6 sm:py-28 lg:px-8"
        id="features"
      >
        <div className="mx-auto max-w-7xl">
          <Reveal className="mx-auto max-w-2xl text-center">
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-brand-600">
              One connected campus
            </p>
            <h2 className="mt-3 text-3xl font-bold tracking-tight text-slate-950 sm:text-5xl">
              Less searching. More student life.
            </h2>
            <p className="mt-4 text-base leading-7 text-slate-600">
              CampusOne brings the tools and communities students already need
              into a single, beautifully organized experience.
            </p>
          </Reveal>

          <div className="mt-12 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {landingFeatures.map((feature, index) => (
              <Reveal delay={index * 60} key={feature.title}>
                <Card className="group h-full border-slate-200 bg-white transition duration-300 hover:-translate-y-1.5 hover:border-brand-200 hover:shadow-xl">
                  <CardContent className="p-6 sm:p-7">
                    <span className="grid size-12 place-items-center rounded-2xl bg-brand-50 text-brand-600 transition duration-300 group-hover:bg-brand-600 group-hover:text-white">
                      <feature.icon className="size-5.5" />
                    </span>
                    <h3 className="mt-5 text-lg font-bold text-slate-950">
                      {feature.title}
                    </h3>
                    <p className="mt-2 text-sm leading-6 text-slate-500">
                      {feature.description}
                    </p>
                  </CardContent>
                </Card>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <section
        className="scroll-mt-24 px-4 py-20 sm:px-6 sm:py-28 lg:px-8"
        id="why-campusone"
      >
        <div className="mx-auto grid max-w-7xl items-center gap-14 lg:grid-cols-2 lg:gap-20">
          <Reveal>
            <div className="relative mx-auto max-w-xl rounded-[2rem] bg-slate-950 p-5 shadow-2xl shadow-slate-950/20 sm:p-8">
              <div className="absolute -left-12 -top-12 size-48 rounded-full bg-brand-500/20 blur-3xl" />
              <div className="absolute -bottom-16 -right-16 size-56 rounded-full bg-campus-500/15 blur-3xl" />
              <div className="relative rounded-2xl border border-white/10 bg-white/[0.06] p-5 backdrop-blur">
                <div className="flex items-center gap-3">
                  <Avatar name="CampusOne student" />
                  <div>
                    <p className="text-sm font-semibold text-white">
                      Your student profile
                    </p>
                    <p className="text-xs text-slate-400">
                      Real account data from CampusOne
                    </p>
                  </div>
                  <Badge className="ml-auto bg-brand-400/10 text-brand-200 ring-brand-300/20">
                    Live profile
                  </Badge>
                </div>
                <div className="mt-5 grid grid-cols-2 gap-3">
                  <div className="rounded-xl border border-white/10 bg-white/[0.05] p-4">
                    <FileText className="size-5 text-brand-300" />
                    <p className="mt-3 text-sm font-bold text-white">
                      Share resources
                    </p>
                    <p className="text-xs text-slate-400">Notes module</p>
                  </div>
                  <div className="rounded-xl border border-white/10 bg-white/[0.05] p-4">
                    <UsersRound className="size-5 text-emerald-300" />
                    <p className="mt-3 text-sm font-bold text-white">
                      Help students
                    </p>
                    <p className="text-xs text-slate-400">Discussion module</p>
                  </div>
                </div>
                <div className="mt-3 rounded-xl border border-white/10 bg-white/[0.05] p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="grid size-8 place-items-center rounded-lg bg-brand-500/20 text-brand-200">
                        <Sparkles className="size-4" />
                      </span>
                      <div>
                        <p className="text-xs font-semibold text-white">
                          Connected workspace
                        </p>
                        <p className="text-[10px] text-slate-400">
                          Notes · internships · events
                        </p>
                      </div>
                    </div>
                    <ArrowRight className="size-4 text-slate-500" />
                  </div>
                </div>
              </div>
            </div>
          </Reveal>

          <Reveal delay={100}>
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-brand-600">
              Why CampusOne
            </p>
            <h2 className="mt-3 text-3xl font-bold tracking-tight text-slate-950 sm:text-5xl">
              Designed around students, not algorithms.
            </h2>
            <p className="mt-5 text-base leading-7 text-slate-600">
              CampusOne gives university communities a focused home where
              useful contributions are easy to discover and every student can
              participate.
            </p>
            <div className="mt-8 grid gap-6 sm:grid-cols-2">
              {campusOneBenefits.map((benefit) => (
                <div className="group" key={benefit.title}>
                  <span className="grid size-10 place-items-center rounded-xl bg-slate-100 text-brand-600 transition group-hover:bg-brand-50">
                    <benefit.icon className="size-4.5" />
                  </span>
                  <h3 className="mt-3 font-bold text-slate-900">
                    {benefit.title}
                  </h3>
                  <p className="mt-1.5 text-sm leading-6 text-slate-500">
                    {benefit.description}
                  </p>
                </div>
              ))}
            </div>
          </Reveal>
        </div>
      </section>

      <section
        className="scroll-mt-24 border-y border-slate-200 bg-slate-50 px-4 py-20 sm:px-6 sm:py-28 lg:px-8"
        id="faq"
      >
        <div className="mx-auto grid max-w-6xl gap-12 lg:grid-cols-[0.75fr_1.25fr]">
          <Reveal>
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-brand-600">
              Questions, answered
            </p>
            <h2 className="mt-3 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">
              Everything you might want to know.
            </h2>
            <p className="mt-4 text-sm leading-6 text-slate-600">
              These answers describe the features available in the current
              CampusOne codebase.
            </p>
          </Reveal>

          <Reveal className="grid gap-3" delay={80}>
            {landingFaqs.map((faq, index) => {
              const isOpen = openFaq === index;
              const panelId = `faq-panel-${index}`;

              return (
                <div
                  className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
                  key={faq.question}
                >
                  <button
                    aria-controls={panelId}
                    aria-expanded={isOpen}
                    className="flex w-full items-center justify-between gap-4 p-5 text-left sm:p-6"
                    onClick={() => setOpenFaq(isOpen ? null : index)}
                    type="button"
                  >
                    <span className="font-semibold text-slate-900">
                      {faq.question}
                    </span>
                    <span
                      className={cn(
                        "grid size-8 shrink-0 place-items-center rounded-lg bg-slate-100 text-slate-500 transition-transform",
                        isOpen && "rotate-180 bg-brand-50 text-brand-600",
                      )}
                    >
                      <ChevronDown className="size-4" />
                    </span>
                  </button>
                  <div
                    className={cn(
                      "grid transition-[grid-template-rows] duration-300",
                      isOpen ? "grid-rows-[1fr]" : "grid-rows-[0fr]",
                    )}
                    id={panelId}
                  >
                    <div className="overflow-hidden">
                      <p className="px-5 pb-5 text-sm leading-6 text-slate-500 sm:px-6 sm:pb-6">
                        {faq.answer}
                      </p>
                    </div>
                  </div>
                </div>
              );
            })}
          </Reveal>
        </div>
      </section>

      <section className="px-4 py-20 sm:px-6 sm:py-24 lg:px-8">
        <Reveal className="relative mx-auto max-w-6xl overflow-hidden rounded-[2rem] bg-slate-950 px-6 py-14 text-center shadow-2xl sm:px-12 sm:py-16">
          <div className="absolute -left-24 top-0 size-72 rounded-full bg-brand-600/30 blur-3xl" />
          <div className="absolute -right-24 bottom-0 size-72 rounded-full bg-campus-500/20 blur-3xl" />
          <div className="relative">
            <span className="mx-auto grid size-12 place-items-center rounded-2xl bg-white/10 text-brand-200">
              <Sparkles className="size-5" />
            </span>
            <h2 className="mx-auto mt-6 max-w-2xl text-3xl font-bold tracking-tight text-white sm:text-5xl">
              Your campus is already connected. Make it useful.
            </h2>
            <p className="mx-auto mt-4 max-w-xl text-sm leading-6 text-slate-300 sm:text-base">
              Join CampusOne and spend less time searching for what your
              university community already knows.
            </p>
            <Link
              className="group mt-8 inline-flex h-12 items-center gap-2 rounded-xl bg-white px-6 text-sm font-bold text-slate-950 transition hover:-translate-y-0.5 hover:bg-brand-50"
              to={paths.signup}
            >
              Create your free account
              <ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" />
            </Link>
            <p className="mt-4 flex items-center justify-center gap-1.5 text-xs text-slate-400">
              <Check className="size-3.5 text-emerald-400" />
              No credit card. Just your campus.
            </p>
          </div>
        </Reveal>
      </section>

      <footer className="border-t border-slate-200 bg-slate-50 px-4 py-12 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-7xl">
          <div className="grid gap-10 md:grid-cols-[1.5fr_1fr_1fr_1fr]">
            <div>
              <CampusOneLogo />
              <p className="mt-4 max-w-sm text-sm leading-6 text-slate-500">
                The modern university community for Pakistani students.
                Everything your campus needs, in one place.
              </p>
            </div>
            <div>
              <p className="text-sm font-bold text-slate-900">Product</p>
              <div className="mt-4 grid gap-3 text-sm text-slate-500">
                <a className="hover:text-brand-700" href="/#features">
                  Features
                </a>
                <Link className="hover:text-brand-700" to={paths.dashboard}>
                  Explore campus
                </Link>
                <a className="hover:text-brand-700" href="/#faq">
                  FAQ
                </a>
              </div>
            </div>
            <div>
              <p className="text-sm font-bold text-slate-900">Community</p>
              <div className="mt-4 grid gap-3 text-sm text-slate-500">
                <Link className="hover:text-brand-700" to={paths.signup}>
                  Join CampusOne
                </Link>
                <Link className="hover:text-brand-700" to={paths.signup}>
                  Create an account
                </Link>
              </div>
            </div>
            <div>
              <p className="text-sm font-bold text-slate-900">Account</p>
              <div className="mt-4 grid gap-3 text-sm text-slate-500">
                <Link className="hover:text-brand-700" to={paths.login}>
                  Log in
                </Link>
                <Link className="hover:text-brand-700" to={paths.signup}>
                  Sign up
                </Link>
                <Link className="hover:text-brand-700" to={paths.login}>
                  Existing account
                </Link>
              </div>
            </div>
          </div>
          <div className="mt-10 flex flex-col gap-3 border-t border-slate-200 pt-6 text-xs text-slate-400 sm:flex-row sm:items-center sm:justify-between">
            <p>© 2026 CampusOne. Built for student communities.</p>
            <p>Pakistan · Made with care for campus life</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
