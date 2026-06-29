import { BookOpenCheck, ShieldCheck, Sparkles, UsersRound } from "lucide-react";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";

import { Avatar, Badge } from "@/components/common";

export interface AuthPageShellProps {
  alternateAction: string;
  alternatePrompt: string;
  alternateTo: string;
  children: ReactNode;
  description: string;
  eyebrow: string;
  title: string;
}

export function AuthPageShell({
  alternateAction,
  alternatePrompt,
  alternateTo,
  children,
  description,
  eyebrow,
  title,
}: AuthPageShellProps) {
  return (
    <div className="mx-auto grid min-h-[calc(100vh-4.5rem)] max-w-[1440px] lg:grid-cols-[0.9fr_1.1fr]">
      <aside className="relative hidden overflow-hidden bg-slate-950 px-10 py-12 text-white lg:flex lg:flex-col xl:px-16">
        <div className="absolute -left-32 top-20 size-80 rounded-full bg-brand-600/25 blur-3xl" />
        <div className="absolute -right-28 bottom-0 size-96 rounded-full bg-campus-500/15 blur-3xl" />
        <div className="relative">
          <Badge className="border-white/10 bg-white/10 text-brand-100 ring-white/10">
            Built for student life
          </Badge>
          <h2 className="mt-8 max-w-lg text-4xl font-bold leading-tight tracking-tight">
            Your campus gets easier when everything connects.
          </h2>
          <p className="mt-4 max-w-lg leading-7 text-slate-300">
            Resources, conversations, opportunities, and the people who make
            university life better—all in one calm, organized space.
          </p>
        </div>

        <div className="relative mt-10 grid gap-3">
          {[
            { icon: BookOpenCheck, text: "Course notes and past papers" },
            { icon: UsersRound, text: "Your real campus community" },
            { icon: Sparkles, text: "Smarter AI study tools" },
            { icon: ShieldCheck, text: "Student-first, trusted experience" },
          ].map((item) => (
            <div
              className="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/[0.06] p-3.5 backdrop-blur"
              key={item.text}
            >
              <span className="grid size-9 place-items-center rounded-xl bg-brand-500/20 text-brand-200">
                <item.icon className="size-4.5" />
              </span>
              <span className="text-sm font-medium text-slate-200">
                {item.text}
              </span>
            </div>
          ))}
        </div>

        <div className="relative mt-auto rounded-2xl border border-white/10 bg-white/[0.06] p-5 backdrop-blur">
          <p className="text-sm leading-6 text-slate-200">
            “One place for the things students usually spend hours finding
            across groups, drives, and scattered links.”
          </p>
          <div className="mt-4 flex items-center gap-3">
            <Avatar name="Sara Ahmed" size="sm" />
            <div>
              <p className="text-sm font-semibold">Sara Ahmed</p>
              <p className="text-xs text-slate-400">FAST Islamabad</p>
            </div>
          </div>
        </div>
      </aside>

      <section className="flex items-center justify-center px-4 py-10 sm:px-8 lg:px-12">
        <div className="w-full max-w-xl">
          <p className="text-xs font-bold uppercase tracking-[0.18em] text-brand-600">
            {eyebrow}
          </p>
          <h1 className="mt-3 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">
            {title}
          </h1>
          <p className="mt-3 text-sm leading-6 text-slate-500 sm:text-base">
            {description}
          </p>
          <div className="mt-8 rounded-3xl border border-slate-200 bg-white p-5 shadow-card sm:p-7">
            {children}
          </div>
          <p className="mt-6 text-center text-sm text-slate-500">
            {alternatePrompt}{" "}
            <Link
              className="font-semibold text-brand-700 transition hover:text-brand-800 hover:underline"
              to={alternateTo}
            >
              {alternateAction}
            </Link>
          </p>
        </div>
      </section>
    </div>
  );
}

