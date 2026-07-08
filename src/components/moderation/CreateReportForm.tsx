import { Flag } from "lucide-react";
import { useState, type FormEvent } from "react";

import { ApiError } from "@/api/apiClient";
import { createReport } from "@/api/moderationApi";
import {
  Button,
  Card,
  CardContent,
  ErrorMessage,
  useToast,
} from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import {
  isUuid,
  moderationTargetOptions,
  reportReasonOptions,
} from "@/components/moderation/moderationFormatting";
import type {
  ContentReportDetail,
  ModerationTargetType,
  ReportReason,
} from "@/types/moderation";

export function CreateReportForm({
  onCreated,
}: {
  onCreated: (report: ContentReportDetail) => void;
}) {
  const { showToast } = useToast();
  const [targetType, setTargetType] =
    useState<ModerationTargetType>("NOTE");
  const [targetReference, setTargetReference] = useState("");
  const [reason, setReason] = useState<ReportReason>("SPAM");
  const [details, setDetails] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const normalizedTargetId = extractContentId(targetReference);
    if (!isUuid(normalizedTargetId)) {
      setError("Enter a valid CampusOne content link or content ID.");
      return;
    }
    setIsSubmitting(true);
    setError(null);
    try {
      const report = await createReport({
        details: details.trim() || null,
        reason,
        targetId: normalizedTargetId,
        targetType,
      });
      setTargetReference("");
      setDetails("");
      onCreated(report);
      showToast({
        title: "Report submitted",
        message: "Your report is now waiting for moderation review.",
        variant: "success",
      });
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "The report could not be submitted.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card>
      <CardContent>
        <form className="grid gap-5" onSubmit={submit}>
          <div>
            <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-950">
              <Flag className="size-5 text-brand-600" />
              Report content
            </h2>
            <p className="mt-1 text-sm leading-6 text-slate-500">
              Submit a CampusOne content link for review. You can paste a
              note, listing, event, discussion, or internship URL here.
            </p>
          </div>
          <div className="grid gap-5 md:grid-cols-2">
            <SelectField
              label="Target type"
              onChange={(event) =>
                setTargetType(event.target.value as ModerationTargetType)
              }
              options={moderationTargetOptions}
              required
              value={targetType}
            />
            <FormField
              label="Content link or ID"
              onChange={(event) => setTargetReference(event.target.value)}
              placeholder="https://campusone.dev/notes/..."
              required
              value={targetReference}
            />
          </div>
          <SelectField
            label="Reason"
            onChange={(event) =>
              setReason(event.target.value as ReportReason)
            }
            options={reportReasonOptions}
            required
            value={reason}
          />
          <label className="grid gap-1.5 text-sm font-semibold text-slate-700">
            Additional details
            <textarea
              className="min-h-32 rounded-xl border border-slate-200 px-3.5 py-3 text-sm font-normal leading-6 outline-none focus:border-brand-400 focus:ring-4 focus:ring-brand-100"
              maxLength={1000}
              onChange={(event) => setDetails(event.target.value)}
              placeholder="Explain what should be reviewed."
              value={details}
            />
            <span className="text-xs font-normal text-slate-400">
              {details.length}/1000
            </span>
          </label>
          {error ? <ErrorMessage message={error} /> : null}
          <Button
            className="w-full sm:w-fit"
            loading={isSubmitting}
            type="submit"
          >
            Submit report
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}

function extractContentId(value: string) {
  const trimmed = value.trim();
  if (isUuid(trimmed)) return trimmed;
  const match = trimmed.match(
    /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i,
  );
  return match?.[0] ?? trimmed;
}
