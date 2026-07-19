import { UserRoundCheck } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/api/apiClient";
import {
  createAuraStudentRegistration,
  getAuraSetupReferences,
  listAuraOfferings,
  listAuraStudentRegistrations,
} from "@/api/auraApi";
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
} from "@/components/common";
import type {
  AuraOffering,
  AuraSetupReferences,
  AuraStudentRegistration,
} from "@/types/aura";

interface AuraRegistrationPanelProps {
  onChanged: () => Promise<void>;
  termId: string;
}

const inputClass =
  "w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-100";

export function AuraRegistrationPanel({
  onChanged,
  termId,
}: AuraRegistrationPanelProps) {
  const [references, setReferences] = useState<AuraSetupReferences | null>(null);
  const [offerings, setOfferings] = useState<AuraOffering[]>([]);
  const [registrations, setRegistrations] = useState<
    AuraStudentRegistration[]
  >([]);
  const [studentUserId, setStudentUserId] = useState("");
  const [offeringId, setOfferingId] = useState("");
  const [registrationType, setRegistrationType] = useState("PRIMARY_SECTION");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    async (signal?: AbortSignal) => {
      const [referenceResponse, offeringResponse, registrationResponse] =
        await Promise.all([
          getAuraSetupReferences(signal),
          listAuraOfferings(termId, signal),
          listAuraStudentRegistrations(termId, signal),
        ]);
      setReferences(referenceResponse);
      setOfferings(offeringResponse);
      setRegistrations(registrationResponse);
    },
    [termId],
  );

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    void Promise.resolve()
      .then(() => load(controller.signal))
      .catch((requestError: unknown) => {
        if (active) {
          setError(messageFor(requestError, "Registrations could not be loaded."));
        }
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [load]);

  const submit = async () => {
    if (!studentUserId || !offeringId || isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    try {
      await createAuraStudentRegistration({
        offeringId,
        registrationType,
        studentUserId,
        termId,
      });
      setStudentUserId("");
      setOfferingId("");
      await Promise.all([load(), onChanged()]);
    } catch (requestError) {
      setError(messageFor(requestError, "The registration could not be saved."));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="grid min-h-40 place-items-center">
          <LoadingSpinner label="Loading registrations" />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Student registrations</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-5">
        {error ? <ErrorMessage message={error} /> : null}
        <div className="grid gap-3 lg:grid-cols-[1fr_1fr_0.8fr_auto] lg:items-end">
          <SelectField
            label="Student"
            onChange={setStudentUserId}
            options={(references?.students ?? []).map((student) => ({
              label: `${student.name} · ${student.code}`,
              value: student.id,
            }))}
            value={studentUserId}
          />
          <SelectField
            label="Course offering"
            onChange={setOfferingId}
            options={offerings.map((offering) => ({
              label: `${offering.courseCode} · ${offering.sectionName}`,
              value: offering.id,
            }))}
            value={offeringId}
          />
          <SelectField
            label="Registration type"
            onChange={setRegistrationType}
            options={registrationTypes.map((type) => ({
              label: type.replaceAll("_", " "),
              value: type,
            }))}
            value={registrationType}
          />
          <Button
            disabled={!studentUserId || !offeringId}
            loading={isSubmitting}
            onClick={() => void submit()}
          >
            Add registration
          </Button>
        </div>

        {registrations.length ? (
          <div className="overflow-x-auto rounded-2xl border border-slate-200">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3">Student</th>
                  <th className="px-4 py-3">Course</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {registrations.map((registration) => (
                  <tr key={registration.id}>
                    <td className="px-4 py-3 font-medium text-slate-950">
                      {registration.studentName}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {registration.courseCode} · {registration.courseTitle}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {registration.registrationType.replaceAll("_", " ")}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {registration.status}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            description="Add individual registrations for repeaters, electives, and cross-section students."
            icon={<UserRoundCheck className="size-6" />}
            title="No student registrations"
          />
        )}
      </CardContent>
    </Card>
  );
}

function SelectField({
  label,
  onChange,
  options,
  value,
}: {
  label: string;
  onChange: (value: string) => void;
  options: Array<{ label: string; value: string }>;
  value: string;
}) {
  return (
    <label className="grid gap-1.5 text-sm font-medium text-slate-700">
      {label}
      <select
        className={inputClass}
        onChange={(event) => onChange(event.target.value)}
        value={value}
      >
        <option value="">Select {label.toLowerCase()}</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

function messageFor(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}

const registrationTypes = [
  "PRIMARY_SECTION",
  "REPEATER",
  "ELECTIVE",
  "CROSS_SECTION",
  "IMPROVEMENT",
  "MAKEUP",
  "MANUAL",
];
