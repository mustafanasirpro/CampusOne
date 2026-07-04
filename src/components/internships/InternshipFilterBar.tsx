import { RotateCcw, SlidersHorizontal } from "lucide-react";

import { Button, Dropdown, SearchBar } from "@/components/common";
import {
  internshipTypeOptions,
  internshipWorkModeOptions,
} from "@/components/internships/internshipFormatting";
import type {
  InternshipSort,
  InternshipStatus,
  InternshipType,
  InternshipWorkMode,
} from "@/types/internships";

export function InternshipFilterBar({
  disabled,
  internshipType,
  onApply,
  onClear,
  onInternshipTypeChange,
  onPaidChange,
  onSearchChange,
  onSortChange,
  onStatusChange,
  onWorkModeChange,
  paid,
  search,
  sort,
  status,
  workMode,
}: {
  disabled: boolean;
  internshipType: InternshipType | "";
  onApply: () => void;
  onClear: () => void;
  onInternshipTypeChange: (value: InternshipType | "") => void;
  onPaidChange: (value: "" | "false" | "true") => void;
  onSearchChange: (value: string) => void;
  onSortChange: (value: InternshipSort) => void;
  onStatusChange: (value: InternshipStatus | "") => void;
  onWorkModeChange: (value: InternshipWorkMode | "") => void;
  paid: "" | "false" | "true";
  search: string;
  sort: InternshipSort;
  status: InternshipStatus | "";
  workMode: InternshipWorkMode | "";
}) {
  return (
    <div className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-card">
      <div className="grid gap-4 xl:grid-cols-[1fr_repeat(3,12rem)]">
        <div className="grid gap-1.5">
          <span className="text-sm font-semibold text-slate-700">Search</span>
          <SearchBar
            disabled={disabled}
            maxLength={200}
            onSearch={onApply}
            onValueChange={onSearchChange}
            placeholder="Title, company, or location..."
            value={search}
          />
        </div>
        <Dropdown
          disabled={disabled}
          label="Status"
          onChange={(event) =>
            onStatusChange(event.target.value as InternshipStatus | "")
          }
          options={[
            { label: "All statuses", value: "" },
            { label: "Open", value: "OPEN" },
            { label: "Closed", value: "CLOSED" },
            { label: "Expired", value: "EXPIRED" },
          ]}
          value={status}
        />
        <Dropdown
          disabled={disabled}
          label="Type"
          onChange={(event) =>
            onInternshipTypeChange(event.target.value as InternshipType | "")
          }
          options={[{ label: "All types", value: "" }, ...internshipTypeOptions]}
          value={internshipType}
        />
        <Dropdown
          disabled={disabled}
          label="Work mode"
          onChange={(event) =>
            onWorkModeChange(event.target.value as InternshipWorkMode | "")
          }
          options={[{ label: "All modes", value: "" }, ...internshipWorkModeOptions]}
          value={workMode}
        />
      </div>
      <div className="flex flex-wrap items-end gap-4">
        <Dropdown
          className="min-w-40"
          disabled={disabled}
          label="Compensation"
          onChange={(event) =>
            onPaidChange(event.target.value as "" | "false" | "true")
          }
          options={[
            { label: "Paid and unpaid", value: "" },
            { label: "Paid", value: "true" },
            { label: "Unpaid", value: "false" },
          ]}
          value={paid}
        />
        <Dropdown
          className="min-w-44"
          label="Sort"
          onChange={(event) =>
            onSortChange(event.target.value as InternshipSort)
          }
          options={[
            { label: "Newest", value: "NEWEST" },
            { label: "Oldest", value: "OLDEST" },
            { label: "Deadline soonest", value: "DEADLINE_ASC" },
            { label: "Deadline latest", value: "DEADLINE_DESC" },
          ]}
          value={sort}
        />
        {!disabled ? (
          <Button onClick={onApply}>
            <SlidersHorizontal className="size-4" />
            Apply
          </Button>
        ) : null}
        <Button onClick={onClear} variant="ghost">
          <RotateCcw className="size-4" />
          Reset
        </Button>
      </div>
    </div>
  );
}

