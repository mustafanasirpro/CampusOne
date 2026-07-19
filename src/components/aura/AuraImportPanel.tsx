import { CheckCircle2, FileSpreadsheet, Upload } from "lucide-react";
import { useRef, useState } from "react";

import { ApiError } from "@/api/apiClient";
import {
  applyAuraImport,
  previewAuraImport,
  validateAuraImport,
} from "@/api/auraApi";
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  EmptyState,
  ErrorMessage,
} from "@/components/common";
import type {
  AuraImportApplyResult,
  AuraImportPreview,
  AuraImportValidation,
} from "@/types/aura";

export function AuraImportPanel({ termId }: { termId: string }) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [importType, setImportType] = useState("TIMETABLE");
  const [source, setSource] = useState("");
  const [preview, setPreview] = useState<AuraImportPreview | null>(null);
  const [mapping, setMapping] = useState<Record<string, string>>({});
  const [validation, setValidation] = useState<AuraImportValidation | null>(null);
  const [applied, setApplied] = useState<AuraImportApplyResult | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const runPreview = async (selectedSource?: string) => {
    if (!file || isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    try {
      const response = await previewAuraImport(
        termId,
        importType,
        file,
        selectedSource || undefined,
      );
      setPreview(response);
      setMapping(response.suggestedMapping);
      setValidation(null);
      setApplied(null);
      setSource(response.selectedSource);
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "The file could not be previewed.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const applyImport = async () => {
    if (!preview || validation?.status !== "VALIDATED" || isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    try {
      setApplied(await applyAuraImport(preview.id));
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "The import could not be applied.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const validateMapping = async () => {
    if (!preview || isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    try {
      setValidation(await validateAuraImport(preview.id, mapping));
    } catch (requestError) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : "The import could not be validated.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Import scheduling data</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-5">
        {error ? <ErrorMessage message={error} /> : null}
        <div className="grid gap-3 md:grid-cols-[0.8fr_1fr_auto] md:items-end">
          <label className="grid gap-1.5 text-sm font-medium text-slate-700">
            Import type
            <select
              className={inputClass}
              onChange={(event) => {
                setImportType(event.target.value);
                setPreview(null);
                setValidation(null);
                setApplied(null);
              }}
              value={importType}
            >
              {importTypes.map((type) => (
                <option key={type} value={type}>
                  {type.replaceAll("_", " ")}
                </option>
              ))}
            </select>
          </label>
          <div className="grid gap-1.5 text-sm font-medium text-slate-700">
            Source file
            <input
              accept=".csv,.xlsx,.xls,.pdf"
              className="sr-only"
              onChange={(event) => {
                setFile(event.target.files?.item(0) ?? null);
                setPreview(null);
                setValidation(null);
                setApplied(null);
                setSource("");
              }}
              ref={inputRef}
              type="file"
            />
            <Button onClick={() => inputRef.current?.click()} variant="outline">
              <FileSpreadsheet className="size-4" />
              {file ? file.name : "Choose CSV, Excel, or PDF"}
            </Button>
          </div>
          <Button
            disabled={!file}
            loading={isSubmitting}
            onClick={() => void runPreview(source)}
          >
            <Upload className="size-4" /> Preview import
          </Button>
        </div>
        <p className="text-sm text-slate-500">
          Files are processed privately in CampusOne. The limit is 10 MB and
          scanned PDFs are flagged for manual correction instead of being guessed.
        </p>

        {preview ? (
          <div className="grid gap-4">
            <div className="grid gap-3 rounded-2xl bg-slate-50 p-4 sm:grid-cols-3">
              <Summary label="Format" value={preview.fileFormat} />
              <Summary label="Rows found" value={String(preview.totalRows)} />
              <Summary
                label="Mapping"
                value={`${Object.keys(preview.suggestedMapping).length} fields suggested`}
              />
            </div>
            {preview.sources.length > 1 ? (
              <label className="grid max-w-sm gap-1.5 text-sm font-medium text-slate-700">
                Workbook sheet
                <select
                  className={inputClass}
                  disabled={isSubmitting}
                  onChange={(event) => {
                    setSource(event.target.value);
                    void runPreview(event.target.value);
                  }}
                  value={source}
                >
                  {preview.sources.map((candidate) => (
                    <option key={candidate} value={candidate}>
                      {candidate}
                    </option>
                  ))}
                </select>
              </label>
            ) : null}
            {preview.warnings.map((warning) => (
              <div
                className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
                key={warning}
              >
                {warning}
              </div>
            ))}
            {preview.headers.length ? (
              <div className="grid gap-4 rounded-2xl border border-slate-200 p-4">
                <div>
                  <h3 className="font-semibold text-slate-950">Map columns</h3>
                  <p className="mt-1 text-sm text-slate-500">
                    Match each CampusOne field to the corresponding source column.
                  </p>
                </div>
                <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                  {(importTargets[importType] ?? []).map((target) => (
                    <label
                      className="grid gap-1.5 text-sm font-medium text-slate-700"
                      key={target.key}
                    >
                      {target.label}{target.required ? " *" : ""}
                      <select
                        className={inputClass}
                        onChange={(event) => {
                          setMapping((current) => {
                            const next = { ...current };
                            if (event.target.value) next[target.key] = event.target.value;
                            else delete next[target.key];
                            return next;
                          });
                          setValidation(null);
                          setApplied(null);
                        }}
                        value={mapping[target.key] ?? ""}
                      >
                        <option value="">Not mapped</option>
                        {preview.headers.map((header) => (
                          <option key={header} value={header}>{header}</option>
                        ))}
                      </select>
                    </label>
                  ))}
                </div>
                <Button
                  disabled={(importTargets[importType] ?? [])
                    .some((target) => target.required && !mapping[target.key])}
                  loading={isSubmitting}
                  onClick={() => void validateMapping()}
                >
                  <CheckCircle2 className="size-4" /> Validate every row
                </Button>
                {validation ? (
                  <div className={`rounded-xl border px-4 py-3 text-sm ${
                    validation.rejectedRows
                      ? "border-rose-200 bg-rose-50 text-rose-900"
                      : "border-emerald-200 bg-emerald-50 text-emerald-900"
                  }`}>
                    <p className="font-semibold">
                      {validation.rejectedRows
                        ? `${validation.rejectedRows} row${validation.rejectedRows === 1 ? "" : "s"} need attention.`
                        : `${validation.acceptedRows} row${validation.acceptedRows === 1 ? "" : "s"} passed validation.`}
                    </p>
                    {validation.issues.length ? (
                      <ul className="mt-2 list-disc space-y-1 pl-5">
                        {validation.issues.slice(0, 20).map((issue) => (
                          <li key={`${issue.rowNumber}-${issue.field}-${issue.code}`}>
                            Row {issue.rowNumber}: {issue.message}
                          </li>
                        ))}
                      </ul>
                    ) : null}
                  </div>
                ) : null}
                {validation?.status === "VALIDATED" && !applied ? (
                  <Button
                    loading={isSubmitting}
                    onClick={() => void applyImport()}
                  >
                    <Upload className="size-4" /> Apply validated import
                  </Button>
                ) : null}
                {applied ? (
                  <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">
                    <p className="font-semibold">Import applied successfully.</p>
                    <p className="mt-1">
                      {applied.acceptedRows} row{applied.acceptedRows === 1 ? "" : "s"} applied.
                      {applied.resultVersionId
                        ? " A new draft timetable is ready for clash review."
                        : ""}
                    </p>
                  </div>
                ) : null}
              </div>
            ) : null}
            {preview.rows.length ? (
              <div className="overflow-x-auto rounded-2xl border border-slate-200">
                <table className="min-w-full divide-y divide-slate-200 text-xs">
                  <thead className="bg-slate-50 text-left uppercase tracking-wide text-slate-500">
                    <tr>
                      {preview.headers.map((header) => (
                        <th className="whitespace-nowrap px-3 py-2" key={header}>
                          {header}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {preview.rows.slice(0, 20).map((row, rowIndex) => (
                      <tr key={`${preview.id}-${rowIndex}`}>
                        {preview.headers.map((header) => (
                          <td className="max-w-64 truncate px-3 py-2" key={header}>
                            {row[header] || "—"}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <EmptyState
                description="Choose a text-based PDF or use a spreadsheet for a structured preview."
                icon={<FileSpreadsheet className="size-6" />}
                title="No readable rows"
              />
            )}
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}

function Summary({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
        {label}
      </p>
      <p className="mt-1 font-semibold text-slate-950">{value}</p>
    </div>
  );
}

const inputClass =
  "w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm text-slate-950 outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-100";

const importTypes = [
  "TIMETABLE",
  "PROGRAMS",
  "BATCHES",
  "SECTIONS",
  "INSTRUCTORS",
  "ROOMS",
  "TIMESLOTS",
  "AVAILABILITY",
  "OFFERINGS",
  "REQUIREMENTS",
  "CONFLICTS",
  "REGISTRATIONS",
  "EXCEPTIONS",
  "TRAVEL_RULES",
];

type ImportTarget = { key: string; label: string; required?: boolean };

const targets = (...values: Array<[string, string, boolean?]>): ImportTarget[] =>
  values.map(([key, label, required]) => ({ key, label, required }));

const importTargets: Record<string, ImportTarget[]> = {
  PROGRAMS: targets(["CODE", "Program code", true], ["NAME", "Program name", true], ["DEPARTMENT_CODE", "Department code", true], ["SEMESTERS", "Semesters"]),
  BATCHES: targets(["PROGRAM_CODE", "Program code", true], ["CODE", "Batch code", true], ["ADMISSION_YEAR", "Admission year", true], ["GRADUATION_YEAR", "Graduation year"]),
  SECTIONS: targets(["PROGRAM_CODE", "Program code", true], ["BATCH_CODE", "Batch code", true], ["CODE", "Section code", true], ["NAME", "Section name", true], ["STUDENTS", "Students", true], ["SEMESTER", "Semester"]),
  INSTRUCTORS: targets(["EMPLOYEE_CODE", "Employee code", true], ["NAME", "Instructor name", true], ["EMAIL", "Email"], ["DEPARTMENT_CODE", "Department code"], ["WEEKLY_LOAD", "Weekly load"]),
  ROOMS: targets(["CODE", "Room code", true], ["NAME", "Room name", true], ["BUILDING", "Building"], ["CAPACITY", "Capacity", true], ["ROOM_TYPE", "Room type", true], ["FACILITIES", "Facilities"]),
  TIMESLOTS: targets(["DAY", "Day", true], ["ORDER", "Order", true], ["LABEL", "Label", true], ["START", "Start time", true], ["END", "End time", true], ["TYPE", "Slot type", true]),
  AVAILABILITY: targets(["RESOURCE_TYPE", "Resource type", true], ["RESOURCE_CODE", "Resource code", true], ["DAY", "Day", true], ["START", "Start time", true], ["END", "End time"], ["AVAILABILITY", "Availability", true], ["REASON", "Reason"]),
  OFFERINGS: targets(["CODE", "Offering code", true], ["COURSE_CODE", "Course code", true], ["SECTION_CODE", "Section code", true], ["INSTRUCTOR_CODE", "Instructor code", true], ["ENROLLMENT", "Enrollment", true], ["MAX_ENROLLMENT", "Maximum enrollment"], ["PARALLEL_GROUP", "Parallel group"], ["ELECTIVE_GROUP", "Elective group"]),
  REQUIREMENTS: targets(["OFFERING_CODE", "Offering code", true], ["TYPE", "Meeting type", true], ["OCCURRENCES", "Weekly occurrences", true], ["DURATION", "Duration in slots", true], ["ROOM_TYPE", "Room type", true], ["CAPACITY", "Required capacity", true], ["FACILITIES", "Facilities"], ["TEACHING_GROUP", "Teaching group"]),
  CONFLICTS: targets(["LEFT_OFFERING", "First offering", true], ["RIGHT_OFFERING", "Second offering", true], ["SOURCE", "Source", true], ["SEVERITY", "Severity", true], ["REASON", "Reason", true]),
  REGISTRATIONS: targets(["STUDENT_EMAIL", "Student email", true], ["OFFERING_CODE", "Offering code", true], ["REGISTRATION_TYPE", "Registration type", true], ["STATUS", "Status"], ["HOME_SECTION", "Home section"], ["TEACHING_SECTION", "Teaching section"], ["LECTURE_GROUP", "Lecture group"], ["LAB_GROUP", "Lab group"], ["TUTORIAL_GROUP", "Tutorial group"]),
  EXCEPTIONS: targets(["TYPE", "Exception type", true], ["START_DATE", "Start date", true], ["END_DATE", "End date", true], ["TARGET_CODE", "Affected resource"], ["FACILITY", "Facility"], ["REASON", "Reason", true]),
  TRAVEL_RULES: targets(["FROM_BUILDING", "From building", true], ["TO_BUILDING", "To building", true], ["MINUTES", "Travel minutes", true], ["DIFFICULTY", "Difficulty", true]),
  TIMETABLE: targets(["COURSE", "Course", true], ["SECTION", "Section", true], ["INSTRUCTOR", "Instructor", true], ["ROOM", "Room", true], ["DAY", "Day", true], ["START", "Start time", true], ["END", "End time"], ["TYPE", "Meeting type"], ["OCCURRENCE", "Occurrence"]),
};
