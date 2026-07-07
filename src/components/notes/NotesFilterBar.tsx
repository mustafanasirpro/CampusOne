import { RotateCcw, SlidersHorizontal } from "lucide-react";

import { noteSortOptions } from "@/api/notesApi";
import { Button, Dropdown, SearchBar } from "@/components/common";
import { FormField } from "@/components/forms";
import type { NoteSort } from "@/types/notes";

interface NotesFilterBarProps {
  course: string;
  disabled?: boolean;
  onApply: () => void;
  onClear: () => void;
  onCourseChange: (value: string) => void;
  onSortChange: (value: NoteSort) => void;
  onTagChange: (value: string) => void;
  sort: NoteSort;
  tag: string;
}

export function NotesFilterBar({
  course,
  disabled = false,
  onApply,
  onClear,
  onCourseChange,
  onSortChange,
  onTagChange,
  sort,
  tag,
}: NotesFilterBarProps) {
  return (
    <div className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-card lg:grid-cols-[1fr_1fr_auto] lg:items-end">
      <FormField
        disabled={disabled}
        hint={
          disabled
            ? "Course filtering is unavailable in My Notes."
            : "Search by course code or course name."
        }
        label="Course"
        onChange={(event) => onCourseChange(event.target.value)}
        placeholder="CSC275, OOP, DBMS..."
        value={course}
      />

      <div className="grid gap-1.5">
        <span className="text-sm font-semibold text-slate-700">
          Tag filter
        </span>
        <SearchBar
          disabled={disabled}
          onSearch={() => onApply()}
          onValueChange={onTagChange}
          placeholder="Filter by tag, for example java"
          value={tag}
        />
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Dropdown
          aria-label="Sort notes"
          onChange={(event) =>
            onSortChange(event.target.value as NoteSort)
          }
          options={noteSortOptions}
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
          Clear
        </Button>
      </div>
    </div>
  );
}
