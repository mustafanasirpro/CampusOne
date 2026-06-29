import { CalendarDays, MapPin, UsersRound } from "lucide-react";

import { Badge, Button, Card, CardContent } from "@/components/common";
import type { EventSummary } from "@/types/content";

export interface EventCardProps {
  event: EventSummary;
  onRsvp?: () => void;
}

export function EventCard({ event, onRsvp }: EventCardProps) {
  return (
    <Card>
      <CardContent>
        <div className="flex items-start justify-between gap-3">
          <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600">
            <CalendarDays className="size-5" />
          </span>
          <Badge variant="brand">{event.category}</Badge>
        </div>
        <h3 className="mt-4 font-semibold text-slate-950">{event.title}</h3>
        <div className="mt-3 grid gap-2 text-sm text-slate-500">
          <p className="flex items-center gap-2">
            <CalendarDays className="size-4" />
            {event.date}
          </p>
          <p className="flex items-center gap-2">
            <MapPin className="size-4" />
            {event.venue}
          </p>
          <p className="flex items-center gap-2">
            <UsersRound className="size-4" />
            {event.organizer}
          </p>
        </div>
        <Button className="mt-5 w-full" onClick={onRsvp}>
          RSVP
        </Button>
      </CardContent>
    </Card>
  );
}

