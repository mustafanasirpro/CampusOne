import { ArrowLeft } from "lucide-react";
import { Link } from "react-router-dom";

import { paths } from "@/routes/paths";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function NotFoundPage() {
  useDocumentTitle("Page not found · CampusOne");

  return (
    <main className="grid min-h-screen place-items-center bg-slate-50 p-6 text-center">
      <div>
        <p className="text-sm font-bold uppercase tracking-[0.2em] text-brand-600">
          404
        </p>
        <h1 className="mt-3 text-3xl font-bold tracking-tight text-slate-950">
          This page left campus
        </h1>
        <p className="mt-2 text-slate-500">
          The route you requested does not exist.
        </p>
        <Link
          className="mt-6 inline-flex h-10 items-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700"
          to={paths.landing}
        >
          <ArrowLeft className="size-4" />
          Back to CampusOne
        </Link>
      </div>
    </main>
  );
}
