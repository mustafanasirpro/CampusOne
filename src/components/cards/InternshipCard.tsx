import { Bookmark, BriefcaseBusiness, CalendarClock, MapPin } from "lucide-react";

import { Badge, Button, Card, CardContent } from "@/components/common";
import type { InternshipSummary } from "@/types/content";
import { cn } from "@/utils/cn";

export interface InternshipCardProps {
  internship: InternshipSummary;
  onApply?: () => void;
  onSave?: () => void;
}

export function InternshipCard({
  internship,
  onApply,
  onSave,
}: InternshipCardProps) {
  return (
    <Card>
      <CardContent>
        <div className="flex items-start gap-3">
          <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600">
            <BriefcaseBusiness className="size-5" />
          </span>
          <div className="min-w-0 flex-1">
            <h3 className="font-semibold text-slate-950">{internship.role}</h3>
            <p className="mt-1 text-sm text-slate-500">{internship.company}</p>
          </div>
          <Button
            aria-label={internship.saved ? "Remove saved internship" : "Save internship"}
            onClick={onSave}
            size="icon"
            variant="ghost"
          >
            <Bookmark
              className={cn(
                "size-5",
                internship.saved && "fill-brand-600 text-brand-600",
              )}
            />
          </Button>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <Badge variant={internship.paid ? "success" : "neutral"}>
            {internship.paid ? "Paid" : "Unpaid"}
          </Badge>
          <Badge>{internship.remote ? "Remote" : "Onsite"}</Badge>
        </div>
        <div className="mt-4 grid gap-2 text-sm text-slate-500">
          <p className="flex items-center gap-2">
            <MapPin className="size-4" />
            {internship.location}
          </p>
          <p className="flex items-center gap-2">
            <CalendarClock className="size-4" />
            Deadline: {internship.deadline}
          </p>
        </div>
        <Button className="mt-5 w-full" onClick={onApply}>
          Apply now
        </Button>
      </CardContent>
    </Card>
  );
}

