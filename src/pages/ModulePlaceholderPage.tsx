import type { LucideIcon } from "lucide-react";
import { ArrowRight, Layers3 } from "lucide-react";
import { Link } from "react-router-dom";

import { Badge, Card, CardContent } from "@/components/common";
import { paths } from "@/routes/paths";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

interface ModulePlaceholderPageProps {
  description: string;
  icon: LucideIcon;
  title: string;
}

export function ModulePlaceholderPage({
  description,
  icon: Icon,
  title,
}: ModulePlaceholderPageProps) {
  useDocumentTitle(`${title} · CampusOne`);

  return (
    <div className="grid gap-6 pb-8">
      <div>
        <Badge className="gap-1.5">
          <Layers3 className="size-3.5" />
          Frontend foundation
        </Badge>
        <h1 className="mt-3 text-3xl font-bold tracking-tight text-slate-950">
          {title}
        </h1>
        <p className="mt-2 max-w-2xl leading-7 text-slate-500">
          {description}
        </p>
      </div>

      <Card className="overflow-hidden">
        <CardContent className="grid min-h-80 place-items-center p-8 text-center">
          <div className="max-w-md">
            <span className="mx-auto grid size-16 place-items-center rounded-2xl bg-brand-50 text-brand-600">
              <Icon className="size-7" />
            </span>
            <h2 className="mt-5 text-xl font-semibold text-slate-950">
              Foundation ready
            </h2>
            <p className="mt-2 leading-7 text-slate-500">
              Routing, authentication, and the shared app shell are in place.
              This module’s connected interface will be implemented in a
              future frontend phase.
            </p>
            <Link
              className="mt-6 inline-flex h-10 items-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white transition hover:bg-brand-700"
              to={paths.dashboard}
            >
              Return to dashboard
              <ArrowRight className="size-4" />
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
