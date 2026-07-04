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
  const [targetId, setTargetId] = useState("");
  const [reason, setReason] = useState<ReportReason>("SPAM");
  const [details, setDetails] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const normalizedTargetId = targetId.trim();
    if (!isUuid(normalizedTargetId)) {
      setError("Enter a valid target UUID.");
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
      setTargetId("");
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
              Submit a content reference for review. You can find the UUID in
              the relevant CampusOne URL or API response.
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
              label="Target UUID"
              onChange={(event) => setTargetId(event.target.value)}
              placeholder="00000000-0000-0000-0000-000000000000"
              required
              value={targetId}
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
