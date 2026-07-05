import { ArrowLeft, CalendarDays, Edit3, MapPin, Trash2, UsersRound } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { deleteEvent, getEventById, joinEvent, leaveEvent } from "@/api/eventsApi";
import { Avatar, Badge, Button, Card, CardContent, ErrorMessage, LoadingSpinner, useToast } from "@/components/common";
import { EventCapacityMeter, eventStatusLabel, formatEventDateTime } from "@/components/events";
import { paths } from "@/routes/paths";
import type { EventDetail } from "@/types/events";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function EventDetailPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [event, setEvent] = useState<EventDetail | null>(null);
  const [error, setError] = useState<string | null>(() => eventId ? null : "The event ID is missing.");
  const [actionError, setActionError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(eventId));
  const [busy, setBusy] = useState<string | null>(null);
  useDocumentTitle(event ? `${event.title} · CampusOne` : "Event · CampusOne");

  useEffect(() => {
    if (!eventId) return;
    const controller = new AbortController();
    let active = true;
    void getEventById(eventId, controller.signal).then((detail) => {
      if (active) setEvent(detail);
    }).catch((requestError: unknown) => {
      if (active) setError(requestError instanceof ApiError ? requestError.message : "The event could not be loaded.");
    }).finally(() => { if (active) setIsLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [eventId]);

  const toggleJoin = async () => {
    if (!event) return;
    setBusy("join"); setActionError(null);
    try {
      if (event.joinedByCurrentUser) {
        await leaveEvent(event.id);
        setEvent({ ...event, joinedByCurrentUser: false, participantCount: Math.max(0, event.participantCount - 1) });
        showToast({ title: "Event left", message: event.title });
      } else {
        const state = await joinEvent(event.id);
        setEvent({ ...event, joinedByCurrentUser: true, participantCount: state.participantCount });
        showToast({ title: "Event joined", message: event.title, variant: "success" });
      }
    } catch (requestError) {
      setActionError(requestError instanceof ApiError && requestError.status === 409 ? requestError.message : requestError instanceof ApiError ? requestError.message : "Participation could not be updated.");
    } finally { setBusy(null); }
  };
  const remove = async () => {
    if (!event || !window.confirm(`Delete "${event.title}"?`)) return;
    setBusy("delete"); setActionError(null);
    try {
      await deleteEvent(event.id);
      showToast({ title: "Event deleted", message: event.title, variant: "success" });
      navigate(paths.events, { replace: true });
    } catch (requestError) {
      setActionError(requestError instanceof ApiError ? requestError.message : "The event could not be deleted.");
      setBusy(null);
    }
  };
  if (isLoading) return <div className="grid min-h-[60vh] place-items-center"><LoadingSpinner label="Loading event details" /></div>;
  if (!event || error) return <div className="grid gap-4"><ErrorMessage message={error ?? "Event not found."} /><Link to={paths.events}>Back to events</Link></div>;
  const full = event.participantCount >= event.capacity;
  const canManageParticipation =
    event.joinedByCurrentUser ||
    (!event.ownedByCurrentUser &&
      event.status === "UPCOMING" &&
      event.visibility === "PUBLIC");
  return (
    <div className="grid gap-6 pb-8">
      <div className="flex flex-wrap justify-between gap-3">
        <Link className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600" to={paths.events}><ArrowLeft className="size-4" /> Back to events</Link>
        {event.ownedByCurrentUser ? <div className="flex gap-2"><Link className="inline-flex h-10 items-center gap-2 rounded-xl border px-4 text-sm font-semibold" to={paths.eventEdit(event.id)}><Edit3 className="size-4" /> Edit</Link><Button loading={busy === "delete"} onClick={() => void remove()} variant="danger"><Trash2 className="size-4" /> Delete</Button></div> : null}
      </div>
      {actionError ? <ErrorMessage message={actionError} /> : null}
      <section className="rounded-3xl bg-gradient-to-br from-brand-700 to-slate-950 p-6 text-white sm:p-8">
        <div className="flex gap-2"><Badge className="bg-white/15 text-white ring-white/20">{eventStatusLabel(event.status)}</Badge><Badge className="bg-white/15 text-white ring-white/20">{event.visibility}</Badge></div>
        <h1 className="mt-5 text-3xl font-bold sm:text-4xl">{event.title}</h1>
        <div className="mt-5 flex flex-wrap gap-5 text-sm text-slate-200"><span className="flex gap-2"><CalendarDays className="size-4" />{formatEventDateTime(event.startTime)}</span><span className="flex gap-2"><MapPin className="size-4" />{event.location}</span></div>
      </section>
      <div className="grid gap-6 xl:grid-cols-[1fr_22rem]">
        <Card><CardContent className="grid gap-4 p-6"><h2 className="text-lg font-semibold">About this event</h2><p className="whitespace-pre-wrap text-sm leading-7 text-slate-600">{event.description}</p><dl className="grid gap-4 border-t pt-5 sm:grid-cols-2"><div><dt className="text-xs text-slate-400">Starts</dt><dd className="font-semibold">{formatEventDateTime(event.startTime)}</dd></div><div><dt className="text-xs text-slate-400">Ends</dt><dd className="font-semibold">{formatEventDateTime(event.endTime)}</dd></div></dl></CardContent></Card>
        <aside className="grid content-start gap-4">
          <Card><CardContent className="grid gap-4 p-5"><div className="flex items-center gap-3"><Avatar name={event.organizer.fullName} src={event.organizer.avatarUrl ?? undefined} /><div><p className="font-semibold">{event.organizer.fullName}</p><p className="text-xs text-slate-400">{event.organizer.university}</p></div></div><EventCapacityMeter capacity={event.capacity} participantCount={event.participantCount} />{canManageParticipation ? <Button disabled={!event.joinedByCurrentUser && full} loading={busy === "join"} onClick={() => void toggleJoin()} variant={event.joinedByCurrentUser ? "outline" : "primary"}><UsersRound className="size-4" />{event.joinedByCurrentUser ? "Leave event" : full ? "Event full" : "Join event"}</Button> : null}{event.ownedByCurrentUser ? <p className="text-xs text-slate-500">Organizers cannot join their own events.</p> : null}</CardContent></Card>
        </aside>
      </div>
    </div>
  );
}
