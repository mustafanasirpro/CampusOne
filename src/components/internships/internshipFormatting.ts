import type {
  InternshipStatus,
  InternshipType,
  InternshipWorkMode,
} from "@/types/internships";

export const internshipTypeOptions: Array<{
  label: string;
  value: InternshipType;
}> = [
  { label: "Full time", value: "FULL_TIME" },
  { label: "Part time", value: "PART_TIME" },
  { label: "Summer", value: "SUMMER" },
  { label: "Winter", value: "WINTER" },
  { label: "Remote internship", value: "REMOTE_INTERNSHIP" },
];

export const internshipWorkModeOptions: Array<{
  label: string;
  value: InternshipWorkMode;
}> = [
  { label: "On-site", value: "ONSITE" },
  { label: "Remote", value: "REMOTE" },
  { label: "Hybrid", value: "HYBRID" },
];

export function internshipTypeLabel(value: InternshipType) {
  return internshipTypeOptions.find((option) => option.value === value)?.label ?? value;
}

export function internshipWorkModeLabel(value: InternshipWorkMode) {
  return internshipWorkModeOptions.find((option) => option.value === value)?.label ?? value;
}

export function internshipStatusLabel(value: InternshipStatus) {
  return { CLOSED: "Closed", EXPIRED: "Expired", OPEN: "Open" }[value];
}

export function formatInternshipDeadline(value: string) {
  return new Intl.DateTimeFormat("en-PK", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function formatInternshipPay(
  paid: boolean,
  stipendAmount: number | null,
  currency: string | null,
) {
  if (!paid) return "Unpaid";
  if (stipendAmount === null || !currency) return "Paid";
  try {
    return new Intl.NumberFormat("en-PK", {
      currency,
      maximumFractionDigits: 2,
      style: "currency",
    }).format(stipendAmount);
  } catch {
    return `${currency} ${stipendAmount.toLocaleString("en-PK")}`;
  }
}

