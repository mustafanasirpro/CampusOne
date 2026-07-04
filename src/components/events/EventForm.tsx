import { Save } from "lucide-react";
import { useState, type FormEvent } from "react";

import { Button, Card, CardContent } from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import { toDateTimeInput } from "@/components/events/eventFormatting";
import type {
  CreateEventRequest,
  EventDetail,
  EventStatus,
  EventVisibility,
  UpdateEventRequest,
} from "@/types/events";
import { cn } from "@/utils/cn";

interface EventFormState {
  capacity: string;
  description: string;
  endTime: string;
  location: string;
  startTime: string;
  status: EventStatus;
  title: string;
  visibility: EventVisibility;
}

type EventFormErrors = Partial<Record<keyof EventFormState, string>>;

interface CommonProps {
  backendFieldErrors?: Record<string, string[]>;
  isSubmitting: boolean;
  submitLabel: string;
}

type EventFormProps =
  | (CommonProps & {
      mode: "create";
      onSubmit: (request: CreateEventRequest) => Promise<void>;
    })
  | (CommonProps & {
      initialEvent: EventDetail;
      mode: "edit";
      onSubmit: (request: UpdateEventRequest) => Promise<void>;
    });

function initialState(event?: EventDetail): EventFormState {
  return {
    capacity: String(event?.capacity ?? 50),
    description: event?.description ?? "",
    endTime: event ? toDateTimeInput(event.endTime) : "",
    location: event?.location ?? "",
    startTime: event ? toDateTimeInput(event.startTime) : "",
    status: event?.status ?? "UPCOMING",
    title: event?.title ?? "",
    visibility: event?.visibility ?? "PUBLIC",
  };
}

export function EventForm(props: EventFormProps) {
  const initialEvent = props.mode === "edit" ? props.initialEvent : undefined;
  const [form, setForm] = useState<EventFormState>(() =>
    initialState(initialEvent),
  );
  const [errors, setErrors] = useState<EventFormErrors>({});

  const update = <Key extends keyof EventFormState>(
    key: Key,
    value: EventFormState[Key],
  ) => {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: undefined }));
  };
  const backendError = (key: string) =>
    props.backendFieldErrors?.[key]?.[0];

  const validate = () => {
    const next: EventFormErrors = {};
    if (form.title.trim().length < 5) next.title = "Use at least 5 characters.";
    if (form.description.trim().length < 10) {
      next.description = "Use at least 10 characters.";
    }
    if (form.location.trim().length < 2) {
      next.location = "Use at least 2 characters.";
    }
    const startTime = new Date(form.startTime);
    const endTime = new Date(form.endTime);
    if (!form.startTime || Number.isNaN(startTime.getTime())) {
      next.startTime = "Choose a valid start time.";
    }
    if (!form.endTime || Number.isNaN(endTime.getTime())) {
      next.endTime = "Choose a valid end time.";
    }
    if (
      !Number.isNaN(startTime.getTime()) &&
      !Number.isNaN(endTime.getTime()) &&
      endTime <= startTime
    ) {
      next.endTime = "End time must be after the start time.";
    }
    const capacity = Number(form.capacity);
    const minimumCapacity =
      props.mode === "edit" ? props.initialEvent.participantCount : 1;
    if (
      !Number.isInteger(capacity) ||
      capacity < Math.max(1, minimumCapacity)
    ) {
      next.capacity =
        minimumCapacity > 1
          ? `Capacity cannot be below ${minimumCapacity} current participants.`
          : "Capacity must be at least 1.";
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!validate()) return;

    const request: CreateEventRequest = {
      capacity: Number(form.capacity),
      description: form.description.trim(),
      endTime: new Date(form.endTime).toISOString(),
      location: form.location.trim(),
      startTime: new Date(form.startTime).toISOString(),
      title: form.title.trim(),
      visibility: form.visibility,
    };
    if (props.mode === "create") {
      await props.onSubmit(request);
      return;
    }
    const updateRequest: UpdateEventRequest = {};
    if (request.title !== props.initialEvent.title) updateRequest.title = request.title;
    if (request.description !== props.initialEvent.description) {
      updateRequest.description = request.description;
    }
    if (request.location !== props.initialEvent.location) {
      updateRequest.location = request.location;
    }
    if (
      new Date(request.startTime).getTime() !==
      new Date(props.initialEvent.startTime).getTime()
    ) {
      updateRequest.startTime = request.startTime;
    }
    if (
      new Date(request.endTime).getTime() !==
      new Date(props.initialEvent.endTime).getTime()
    ) {
      updateRequest.endTime = request.endTime;
    }
    if (request.capacity !== props.initialEvent.capacity) {
      updateRequest.capacity = request.capacity;
    }
    if (request.visibility !== props.initialEvent.visibility) {
      updateRequest.visibility = request.visibility;
    }
    if (form.status !== props.initialEvent.status) {
      updateRequest.status = form.status;
    }
    await props.onSubmit(updateRequest);
  };

  return (
    <form className="grid gap-6" noValidate onSubmit={handleSubmit}>
      <Card>
        <CardContent className="grid gap-5 p-5 sm:p-6">
          <FormField
            error={errors.title ?? backendError("title")}
            label="Event title"
            maxLength={160}
            onChange={(event) => update("title", event.target.value)}
            placeholder="Campus Web Development Workshop"
            required
            value={form.title}
          />
          <label className="grid gap-1.5">
            <span className="text-sm font-semibold text-slate-700">
              Description <span className="text-red-500">*</span>
            </span>
            <textarea
              aria-invalid={Boolean(errors.description ?? backendError("description"))}
              className={cn(
                "min-h-40 rounded-xl border px-3.5 py-3 text-sm leading-6 outline-none focus:ring-4",
                errors.description ?? backendError("description")
                  ? "border-red-300 focus:ring-red-100"
                  : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
              )}
              maxLength={5000}
              onChange={(event) => update("description", event.target.value)}
              required
              value={form.description}
            />
            <p className="text-xs text-red-600">
              {errors.description ?? backendError("description")}
            </p>
          </label>
          <FormField
            error={errors.location ?? backendError("location")}
            label="Location"
            maxLength={255}
            onChange={(event) => update("location", event.target.value)}
            placeholder="Main Auditorium, Islamabad Campus"
            required
            value={form.location}
          />
          <div className="grid gap-5 sm:grid-cols-2">
            <FormField
              error={errors.startTime ?? backendError("startTime")}
              label="Start time"
              onChange={(event) => update("startTime", event.target.value)}
              required
              type="datetime-local"
              value={form.startTime}
            />
            <FormField
              error={errors.endTime ?? backendError("endTime")}
              label="End time"
              onChange={(event) => update("endTime", event.target.value)}
              required
              type="datetime-local"
              value={form.endTime}
            />
          </div>
          <div className="grid gap-5 sm:grid-cols-2">
            <FormField
              error={errors.capacity ?? backendError("capacity")}
              label="Capacity"
              min={props.mode === "edit" ? Math.max(1, props.initialEvent.participantCount) : 1}
              onChange={(event) => update("capacity", event.target.value)}
              required
              type="number"
              value={form.capacity}
            />
            <SelectField
              error={errors.visibility ?? backendError("visibility")}
              label="Visibility"
              onChange={(event) =>
                update("visibility", event.target.value as EventVisibility)
              }
              options={[
                { label: "Public", value: "PUBLIC" },
                { label: "Private", value: "PRIVATE" },
              ]}
              required
              value={form.visibility}
            />
          </div>
          {props.mode === "edit" ? (
            <SelectField
              error={errors.status ?? backendError("status")}
              label="Status"
              onChange={(event) =>
                update("status", event.target.value as EventStatus)
              }
              options={[
                { label: "Upcoming", value: "UPCOMING" },
                { label: "Cancelled", value: "CANCELLED" },
                { label: "Completed", value: "COMPLETED" },
              ]}
              value={form.status}
            />
          ) : null}
        </CardContent>
      </Card>
      <div className="flex justify-end">
        <Button
          className="w-full sm:w-auto"
          loading={props.isSubmitting}
          size="lg"
          type="submit"
        >
          <Save className="size-4" />
          {props.isSubmitting ? "Saving event" : props.submitLabel}
        </Button>
      </div>
    </form>
  );
}
