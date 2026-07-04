import {
  CalendarDays,
  Edit3,
  MapPin,
  UsersRound,
} from "lucide-react";
import { Link } from "react-router-dom";

import { Avatar, Badge, Card, CardContent } from "@/components/common";
import { EventCapacityMeter } from "@/components/events/EventCapacityMeter";
import {
  eventStatusLabel,
  formatEventDateTime,
} from "@/components/events/eventFormatting";
import { paths } from "@/routes/paths";
import type { EventSummary } from "@/types/events";

export function EventCard({ event }: { event: EventSummary }) {
  return (
    <Card className="group flex h-full flex-col overflow-hidden hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl">
      <div className="bg-gradient-to-br from-brand-600 via-brand-700 to-slate-950 p-5 text-white">
        <div className="flex flex-wrap gap-2">
          <Badge className="bg-white/15 text-white ring-white/20">
            {eventStatusLabel(event.status)}
          </Badge>
          <Badge className="bg-white/15 text-white ring-white/20">
            {event.visibility === "PUBLIC" ? "Public" : "Private"}
          </Badge>
          {event.joinedByCurrentUser ? (
            <Badge className="ml-auto bg-emerald-400/20 text-emerald-100 ring-emerald-300/20">
              Joined
            </Badge>
          ) : null}
        </div>
        <Link
          className="mt-5 block line-clamp-2 text-xl font-bold leading-7 hover:text-brand-100"
          to={paths.eventDetail(event.id)}
        >
          {event.title}
        </Link>
        <p className="mt-3 flex items-center gap-2 text-sm text-brand-100">
          <CalendarDays className="size-4" />
          {formatEventDateTime(event.startTime)}
        </p>
      </div>
      <CardContent className="flex flex-1 flex-col gap-4 p-5">
        <p className="line-clamp-3 text-sm leading-6 text-slate-500">
          {event.description}
        </p>
        <p className="flex items-center gap-2 text-sm text-slate-600">
          <MapPin className="size-4 shrink-0 text-slate-400" />
          <span className="truncate">{event.location}</span>
        </p>
        <EventCapacityMeter
          capacity={event.capacity}
          participantCount={event.participantCount}
        />
        <div className="mt-auto flex items-center gap-3 border-t border-slate-100 pt-4">
          <Avatar
            name={event.organizer.fullName}
            size="sm"
            src={event.organizer.avatarUrl ?? undefined}
          />
          <div className="min-w-0">
            <p className="truncate text-xs font-semibold text-slate-700">
              {event.organizer.fullName}
            </p>
            <p className="flex items-center gap-1 text-xs text-slate-400">
              <UsersRound className="size-3" />
              Organizer
            </p>
          </div>
          {event.ownedByCurrentUser ? (
            <Link
              className="ml-auto inline-flex h-9 items-center gap-1.5 rounded-lg border border-slate-200 px-3 text-xs font-semibold text-slate-600 hover:bg-slate-50"
              to={paths.eventEdit(event.id)}
            >
              <Edit3 className="size-3.5" />
              Edit
            </Link>
          ) : (
            <Link
              className="ml-auto inline-flex h-9 items-center rounded-lg bg-brand-600 px-3 text-xs font-semibold text-white hover:bg-brand-700"
              to={paths.eventDetail(event.id)}
            >
              Details
            </Link>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
