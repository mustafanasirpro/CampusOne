import { RotateCcw, SlidersHorizontal } from "lucide-react";

import { Button, Dropdown, SearchBar } from "@/components/common";
import type { EventSort, EventStatus } from "@/types/events";

export function EventFilterBar({
  filtersDisabled,
  onApply,
  onClear,
  onSearchChange,
  onSortChange,
  onStatusChange,
  search,
  sort,
  status,
}: {
  filtersDisabled: boolean;
  onApply: () => void;
  onClear: () => void;
  onSearchChange: (value: string) => void;
  onSortChange: (value: EventSort) => void;
  onStatusChange: (value: EventStatus | "") => void;
  search: string;
  sort: EventSort;
  status: EventStatus | "";
}) {
  return (
    <div className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-card xl:grid-cols-[1fr_13rem_13rem_auto] xl:items-end">
      <div className="grid gap-1.5">
        <span className="text-sm font-semibold text-slate-700">
          Search events
        </span>
        <SearchBar
          disabled={filtersDisabled}
          maxLength={200}
          onSearch={onApply}
          onValueChange={onSearchChange}
          placeholder="Search by title or location..."
          value={search}
        />
      </div>
      <Dropdown
        disabled={filtersDisabled}
        label="Status"
        onChange={(event) =>
          onStatusChange(event.target.value as EventStatus | "")
        }
        options={[
          { label: "All statuses", value: "" },
          { label: "Upcoming", value: "UPCOMING" },
          { label: "Cancelled", value: "CANCELLED" },
          { label: "Completed", value: "COMPLETED" },
        ]}
        value={status}
      />
      <Dropdown
        label="Sort"
        onChange={(event) => onSortChange(event.target.value as EventSort)}
        options={[
          { label: "Upcoming first", value: "UPCOMING" },
          { label: "Newest", value: "NEWEST" },
          { label: "Oldest", value: "OLDEST" },
        ]}
        value={sort}
      />
      <div className="flex gap-2">
        {!filtersDisabled ? (
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

