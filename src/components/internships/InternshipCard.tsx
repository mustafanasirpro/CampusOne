import {
  Bookmark,
  Building2,
  CalendarClock,
  Edit3,
  MapPin,
} from "lucide-react";
import { Link } from "react-router-dom";

import { Avatar, Badge, Card, CardContent } from "@/components/common";
import {
  formatInternshipDeadline,
  formatInternshipPay,
  internshipStatusLabel,
  internshipTypeLabel,
  internshipWorkModeLabel,
} from "@/components/internships/internshipFormatting";
import { paths } from "@/routes/paths";
import type { InternshipSummary } from "@/types/internships";

export function InternshipCard({
  internship,
}: {
  internship: InternshipSummary;
}) {
  return (
    <Card className="group flex h-full flex-col hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl">
      <CardContent className="flex flex-1 flex-col gap-4 p-5 sm:p-6">
        <div className="flex flex-wrap gap-2">
          <Badge variant="brand">
            {internshipTypeLabel(internship.internshipType)}
          </Badge>
          <Badge>{internshipWorkModeLabel(internship.workMode)}</Badge>
          <Badge variant={internship.status === "OPEN" ? "success" : "neutral"}>
            {internshipStatusLabel(internship.status)}
          </Badge>
          {internship.savedByCurrentUser ? (
            <Badge className="ml-auto" variant="warning">
              <Bookmark className="mr-1 size-3 fill-current" />
              Saved
            </Badge>
          ) : null}
        </div>
        <div>
          <p className="flex items-center gap-2 text-sm font-semibold text-brand-700">
            <Building2 className="size-4" />
            {internship.companyName}
          </p>
          <Link
            className="mt-2 block text-xl font-bold leading-7 text-slate-950 transition group-hover:text-brand-700"
            to={paths.internshipDetail(internship.id)}
          >
            {internship.title}
          </Link>
          <p className="mt-2 line-clamp-3 text-sm leading-6 text-slate-500">
            {internship.description}
          </p>
        </div>
        <div className="grid gap-2 text-sm text-slate-500">
          <p className="flex items-center gap-2">
            <MapPin className="size-4 shrink-0 text-slate-400" />
            {internship.location}
          </p>
          <p className="flex items-center gap-2">
            <CalendarClock className="size-4 shrink-0 text-slate-400" />
            Apply by {formatInternshipDeadline(internship.deadline)}
          </p>
        </div>
        <p className="text-lg font-bold text-slate-900">
          {formatInternshipPay(
            internship.paid,
            internship.stipendAmount,
            internship.currency,
          )}
        </p>
        <div className="mt-auto flex items-center gap-3 border-t border-slate-100 pt-4">
          <Avatar
            name={internship.poster.fullName}
            size="sm"
            src={internship.poster.avatarUrl ?? undefined}
          />
          <p className="min-w-0 truncate text-xs font-semibold text-slate-600">
            {internship.poster.fullName}
          </p>
          {internship.ownedByCurrentUser ? (
            <Link
              className="ml-auto inline-flex h-9 items-center gap-1.5 rounded-lg border border-slate-200 px-3 text-xs font-semibold text-slate-600 hover:bg-slate-50"
              to={paths.internshipEdit(internship.id)}
            >
              <Edit3 className="size-3.5" />
              Edit
            </Link>
          ) : (
            <Link
              className="ml-auto inline-flex h-9 items-center rounded-lg bg-brand-600 px-3 text-xs font-semibold text-white hover:bg-brand-700"
              to={paths.internshipDetail(internship.id)}
            >
              Details
            </Link>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

