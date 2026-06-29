import { Link, Outlet } from "react-router-dom";

import { CampusOneLogo } from "@/components/layout/CampusOneLogo";
import { paths } from "@/routes/paths";

export function PublicLayout() {
  return (
    <div className="min-h-screen bg-slate-50">
      <header className="sticky top-0 z-50 border-b border-slate-200/80 bg-white/90 backdrop-blur-xl">
        <div className="mx-auto flex h-18 max-w-7xl items-center px-4 sm:px-6 lg:px-8">
          <CampusOneLogo className="sm:hidden" compact />
          <CampusOneLogo className="hidden sm:inline-flex" />
          <nav
            aria-label="Landing page sections"
            className="ml-10 hidden items-center gap-1 lg:flex"
          >
            <a
              className="rounded-lg px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-950"
              href="/#features"
            >
              Features
            </a>
            <a
              className="rounded-lg px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-950"
              href="/#why-campusone"
            >
              Why CampusOne
            </a>
            <a
              className="rounded-lg px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-slate-950"
              href="/#faq"
            >
              FAQ
            </a>
          </nav>
          <nav
            aria-label="Public navigation"
            className="ml-auto flex items-center gap-2"
          >
            <Link
              className="rounded-xl px-3 py-2 text-sm font-semibold text-slate-600 transition hover:bg-slate-100 hover:text-slate-950"
              to={paths.login}
            >
              Log in
            </Link>
            <Link
              className="rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-brand-700"
              to={paths.signup}
            >
              Get started
            </Link>
          </nav>
        </div>
      </header>
      <main>
        <Outlet />
      </main>
    </div>
  );
}
