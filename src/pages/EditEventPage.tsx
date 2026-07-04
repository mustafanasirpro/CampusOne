import { ArrowLeft, LockKeyhole } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { getEventById, updateEvent } from "@/api/eventsApi";
import { EmptyState, ErrorMessage, LoadingSpinner, PageHeader, useToast } from "@/components/common";
import { EventForm } from "@/components/events";
import { paths } from "@/routes/paths";
import type { EventDetail, UpdateEventRequest } from "@/types/events";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function EditEventPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [event, setEvent] = useState<EventDetail | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(eventId));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(() => eventId ? null : "The event ID is missing.");
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string[]>>({});
  useDocumentTitle(event ? `Edit ${event.title} · CampusOne` : "Edit event · CampusOne");

  useEffect(() => {
    if (!eventId) return;
    const controller = new AbortController();
    let active = true;
    void getEventById(eventId, controller.signal)
      .then((response) => { if (active) setEvent(response); })
      .catch((requestError: unknown) => {
        if (active) setLoadError(requestError instanceof ApiError ? requestError.message : "The event could not be loaded.");
      })
      .finally(() => { if (active) setIsLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [eventId]);

  const submit = async (request: UpdateEventRequest) => {
    if (!eventId) return;
    setIsSubmitting(true); setSubmitError(null); setFieldErrors({});
    try {
      const updated = await updateEvent(eventId, request);
      showToast({ title: "Event updated", message: updated.title, variant: "success" });
      navigate(paths.eventDetail(updated.id), { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        setSubmitError(requestError.message); setFieldErrors(requestError.fieldErrors);
      } else setSubmitError("The event could not be updated.");
    } finally { setIsSubmitting(false); }
  };
  if (isLoading) return <div className="grid min-h-[60vh] place-items-center"><LoadingSpinner label="Loading event" /></div>;
  if (!event || loadError) return <ErrorMessage message={loadError ?? "Event not found."} />;
  if (!event.ownedByCurrentUser) {
    return <EmptyState action={<Link className="rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white" to={paths.eventDetail(event.id)}>View event</Link>} description="Only the organizer can edit this event." icon={<LockKeyhole className="size-6" />} title="Organizer access required" />;
  }
  return (
    <div className="grid gap-6 pb-8">
      <Link className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600" to={paths.eventDetail(event.id)}><ArrowLeft className="size-4" /> Back to event</Link>
      <PageHeader description="Update event details, capacity, visibility, or status." eyebrow="Your events" title="Edit event" />
      {submitError ? <ErrorMessage message={submitError} /> : null}
      <EventForm backendFieldErrors={fieldErrors} initialEvent={event} isSubmitting={isSubmitting} mode="edit" onSubmit={submit} submitLabel="Save changes" />
    </div>
  );
}

