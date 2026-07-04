import { RotateCcw, SlidersHorizontal } from "lucide-react";

import { Button, Dropdown, SearchBar } from "@/components/common";
import { discussionCategoryOptions } from "@/components/discussion/discussionFormatting";
import type {
  DiscussionCategory,
  DiscussionQuestionSort,
} from "@/types/discussion";

const sortOptions = [
  { label: "Newest", value: "NEWEST" },
  { label: "Oldest", value: "OLDEST" },
  { label: "Most voted", value: "MOST_VOTED" },
  { label: "Most answered", value: "MOST_ANSWERED" },
];

interface QuestionFilterBarProps {
  category: DiscussionCategory | "";
  filtersDisabled?: boolean;
  onApply: () => void;
  onCategoryChange: (value: DiscussionCategory | "") => void;
  onClear: () => void;
  onSearchChange: (value: string) => void;
  onSortChange: (value: DiscussionQuestionSort) => void;
  search: string;
  sort: DiscussionQuestionSort;
}

export function QuestionFilterBar({
  category,
  filtersDisabled = false,
  onApply,
  onCategoryChange,
  onClear,
  onSearchChange,
  onSortChange,
  search,
  sort,
}: QuestionFilterBarProps) {
  return (
    <div className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-card xl:grid-cols-[1fr_13rem_13rem_auto] xl:items-end">
      <div className="grid gap-1.5">
        <span className="text-sm font-semibold text-slate-700">
          Search discussions
        </span>
        <SearchBar
          disabled={filtersDisabled}
          maxLength={200}
          onSearch={() => onApply()}
          onValueChange={onSearchChange}
          placeholder="Search titles and question details..."
          value={search}
        />
      </div>
      <Dropdown
        disabled={filtersDisabled}
        label="Category"
        onChange={(event) =>
          onCategoryChange(event.target.value as DiscussionCategory | "")
        }
        options={[
          { label: "All categories", value: "" },
          ...discussionCategoryOptions,
        ]}
        value={category}
      />
      <Dropdown
        label="Sort"
        onChange={(event) =>
          onSortChange(event.target.value as DiscussionQuestionSort)
        }
        options={sortOptions}
        value={sort}
      />
      <div className="flex flex-wrap gap-2">
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

