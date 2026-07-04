import { ArrowLeft } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { createEvent } from "@/api/eventsApi";
import { ErrorMessage, PageHeader, useToast } from "@/components/common";
import { EventForm } from "@/components/events";
import { paths } from "@/routes/paths";
import type { CreateEventRequest } from "@/types/events";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function CreateEventPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string[]>>({});
  useDocumentTitle("Create event · CampusOne");

  const submit = async (request: CreateEventRequest) => {
    setIsSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      const event = await createEvent(request);
      showToast({ title: "Event created", message: event.title, variant: "success" });
      navigate(paths.eventDetail(event.id), { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        setError(requestError.message);
        setFieldErrors(requestError.fieldErrors);
      } else setError("The event could not be created.");
    } finally {
      setIsSubmitting(false);
    }
  };
  return (
    <div className="grid gap-6 pb-8">
      <Link className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600" to={paths.events}>
        <ArrowLeft className="size-4" /> Back to events
      </Link>
      <PageHeader description="Publish an event for your campus community." eyebrow="Events" title="Create an event" />
      {error ? <ErrorMessage message={error} /> : null}
      <EventForm backendFieldErrors={fieldErrors} isSubmitting={isSubmitting} mode="create" onSubmit={submit} submitLabel="Create event" />
    </div>
  );
}

