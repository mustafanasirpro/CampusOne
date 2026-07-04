import { Save } from "lucide-react";
import { useState, type FormEvent } from "react";

import { Button, Card, CardContent } from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import {
  internshipTypeOptions,
  internshipWorkModeOptions,
} from "@/components/internships/internshipFormatting";
import { toDateTimeInput } from "@/components/events";
import type {
  CreateInternshipRequest,
  InternshipDetail,
  InternshipStatus,
  InternshipType,
  InternshipWorkMode,
  UpdateInternshipRequest,
} from "@/types/internships";
import { cn } from "@/utils/cn";

interface InternshipFormState {
  applyUrl: string;
  companyName: string;
  currency: string;
  deadline: string;
  description: string;
  internshipType: InternshipType;
  location: string;
  paid: "false" | "true";
  status: InternshipStatus;
  stipendAmount: string;
  title: string;
  workMode: InternshipWorkMode;
}

type InternshipFormErrors = Partial<Record<keyof InternshipFormState, string>>;

interface CommonProps {
  backendFieldErrors?: Record<string, string[]>;
  isSubmitting: boolean;
  submitLabel: string;
}

type InternshipFormProps =
  | (CommonProps & {
      mode: "create";
      onSubmit: (request: CreateInternshipRequest) => Promise<void>;
    })
  | (CommonProps & {
      initialInternship: InternshipDetail;
      mode: "edit";
      onSubmit: (request: UpdateInternshipRequest) => Promise<void>;
    });

function initialState(internship?: InternshipDetail): InternshipFormState {
  return {
    applyUrl: internship?.applyUrl ?? "",
    companyName: internship?.companyName ?? "",
    currency: internship?.currency ?? "",
    deadline: internship ? toDateTimeInput(internship.deadline) : "",
    description: internship?.description ?? "",
    internshipType: internship?.internshipType ?? "SUMMER",
    location: internship?.location ?? "",
    paid: internship?.paid ? "true" : "false",
    status: internship?.status ?? "OPEN",
    stipendAmount:
      internship?.stipendAmount === null ||
      internship?.stipendAmount === undefined
        ? ""
        : String(internship.stipendAmount),
    title: internship?.title ?? "",
    workMode: internship?.workMode ?? "ONSITE",
  };
}

export function InternshipForm(props: InternshipFormProps) {
  const initialInternship =
    props.mode === "edit" ? props.initialInternship : undefined;
  const [form, setForm] = useState<InternshipFormState>(() =>
    initialState(initialInternship),
  );
  const [errors, setErrors] = useState<InternshipFormErrors>({});
  const update = <Key extends keyof InternshipFormState>(
    key: Key,
    value: InternshipFormState[Key],
  ) => {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: undefined }));
  };
  const backendError = (key: string) =>
    props.backendFieldErrors?.[key]?.[0];

  const validate = () => {
    const next: InternshipFormErrors = {};
    if (form.title.trim().length < 5) next.title = "Use at least 5 characters.";
    if (form.companyName.trim().length < 2) {
      next.companyName = "Use at least 2 characters.";
    }
    if (form.description.trim().length < 20) {
      next.description = "Use at least 20 characters.";
    }
    if (form.location.trim().length < 2) {
      next.location = "Use at least 2 characters.";
    }
    if (!/^https?:\/\/\S+$/i.test(form.applyUrl.trim())) {
      next.applyUrl = "Enter a complete HTTP or HTTPS application URL.";
    }
    const deadline = new Date(form.deadline);
    if (!form.deadline || Number.isNaN(deadline.getTime())) {
      next.deadline = "Choose a valid application deadline.";
    } else {
      const changedDeadline =
        props.mode === "create" ||
        new Date(form.deadline).getTime() !==
          new Date(props.initialInternship.deadline).getTime();
      if (changedDeadline && deadline <= new Date()) {
        next.deadline = "The application deadline must be in the future.";
      }
    }
    if (
      form.stipendAmount &&
      (!Number.isFinite(Number(form.stipendAmount)) ||
        Number(form.stipendAmount) < 0)
    ) {
      next.stipendAmount = "Stipend cannot be negative.";
    }
    if (
      form.currency &&
      !/^[A-Z]{3,10}$/.test(form.currency.trim().toUpperCase())
    ) {
      next.currency = "Use a 3 to 10 letter currency code.";
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!validate()) return;
    const isPaid = form.paid === "true";
    const request: CreateInternshipRequest = {
      applyUrl: form.applyUrl.trim(),
      companyName: form.companyName.trim(),
      currency: isPaid ? form.currency.trim().toUpperCase() || null : null,
      deadline: new Date(form.deadline).toISOString(),
      description: form.description.trim(),
      internshipType: form.internshipType,
      location: form.location.trim(),
      paid: isPaid,
      stipendAmount:
        isPaid && form.stipendAmount ? Number(form.stipendAmount) : null,
      title: form.title.trim(),
      workMode: form.workMode,
    };
    if (props.mode === "create") {
      await props.onSubmit(request);
      return;
    }
    const updateRequest: UpdateInternshipRequest = {};
    const initial = props.initialInternship;
    if (request.title !== initial.title) updateRequest.title = request.title;
    if (request.companyName !== initial.companyName) {
      updateRequest.companyName = request.companyName;
    }
    if (request.description !== initial.description) {
      updateRequest.description = request.description;
    }
    if (request.location !== initial.location) updateRequest.location = request.location;
    if (request.internshipType !== initial.internshipType) {
      updateRequest.internshipType = request.internshipType;
    }
    if (request.workMode !== initial.workMode) updateRequest.workMode = request.workMode;
    if (request.paid !== initial.paid) updateRequest.paid = request.paid;
    if (request.stipendAmount !== initial.stipendAmount) {
      updateRequest.stipendAmount = request.stipendAmount;
    }
    if (request.currency !== initial.currency) updateRequest.currency = request.currency;
    if (request.applyUrl !== initial.applyUrl) updateRequest.applyUrl = request.applyUrl;
    if (
      new Date(request.deadline).getTime() !==
      new Date(initial.deadline).getTime()
    ) {
      updateRequest.deadline = request.deadline;
    }
    if (form.status !== initial.status) updateRequest.status = form.status;
    await props.onSubmit(updateRequest);
  };

  return (
    <form className="grid gap-6" noValidate onSubmit={handleSubmit}>
      <Card>
        <CardContent className="grid gap-5 p-5 sm:p-6">
          <div className="grid gap-5 sm:grid-cols-2">
            <FormField
              error={errors.title ?? backendError("title")}
              label="Role title"
              maxLength={180}
              onChange={(event) => update("title", event.target.value)}
              required
              value={form.title}
            />
            <FormField
              error={errors.companyName ?? backendError("companyName")}
              label="Company name"
              maxLength={160}
              onChange={(event) => update("companyName", event.target.value)}
              required
              value={form.companyName}
            />
          </div>
          <label className="grid gap-1.5">
            <span className="text-sm font-semibold text-slate-700">
              Description <span className="text-red-500">*</span>
            </span>
            <textarea
              aria-invalid={Boolean(errors.description ?? backendError("description"))}
              className={cn(
                "min-h-44 rounded-xl border px-3.5 py-3 text-sm leading-6 outline-none focus:ring-4",
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
            required
            value={form.location}
          />
          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              label="Internship type"
              onChange={(event) =>
                update("internshipType", event.target.value as InternshipType)
              }
              options={internshipTypeOptions}
              required
              value={form.internshipType}
            />
            <SelectField
              label="Work mode"
              onChange={(event) =>
                update("workMode", event.target.value as InternshipWorkMode)
              }
              options={internshipWorkModeOptions}
              required
              value={form.workMode}
            />
          </div>
          <div className="grid gap-5 sm:grid-cols-3">
            <SelectField
              label="Compensation"
              onChange={(event) =>
                update("paid", event.target.value as "false" | "true")
              }
              options={[
                { label: "Unpaid", value: "false" },
                { label: "Paid", value: "true" },
              ]}
              value={form.paid}
            />
            <FormField
              disabled={form.paid === "false"}
              error={errors.stipendAmount ?? backendError("stipendAmount")}
              label="Stipend amount"
              min={0}
              onChange={(event) => update("stipendAmount", event.target.value)}
              step="0.01"
              type="number"
              value={form.stipendAmount}
            />
            <FormField
              disabled={form.paid === "false"}
              error={errors.currency ?? backendError("currency")}
              label="Currency"
              maxLength={10}
              onChange={(event) =>
                update("currency", event.target.value.toUpperCase())
              }
              placeholder="PKR"
              value={form.currency}
            />
          </div>
          <FormField
            error={errors.applyUrl ?? backendError("applyUrl")}
            label="Application URL"
            maxLength={1000}
            onChange={(event) => update("applyUrl", event.target.value)}
            placeholder="https://company.example/careers/apply"
            required
            type="url"
            value={form.applyUrl}
          />
          <FormField
            error={errors.deadline ?? backendError("deadline")}
            label="Application deadline"
            onChange={(event) => update("deadline", event.target.value)}
            required
            type="datetime-local"
            value={form.deadline}
          />
          {props.mode === "edit" ? (
            <SelectField
              label="Status"
              onChange={(event) =>
                update("status", event.target.value as InternshipStatus)
              }
              options={[
                { label: "Open", value: "OPEN" },
                { label: "Closed", value: "CLOSED" },
                { label: "Expired", value: "EXPIRED" },
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
          {props.isSubmitting ? "Saving internship" : props.submitLabel}
        </Button>
      </div>
    </form>
  );
}
