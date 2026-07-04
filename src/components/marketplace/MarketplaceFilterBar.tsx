import { RotateCcw, SlidersHorizontal } from "lucide-react";

import { Button, Dropdown, SearchBar } from "@/components/common";
import { marketplaceCategoryOptions } from "@/components/marketplace/marketplaceFormatting";
import type { MarketplaceCategory } from "@/types/marketplace";

interface MarketplaceFilterBarProps {
  category: MarketplaceCategory | "";
  disabled?: boolean;
  onApply: () => void;
  onCategoryChange: (value: MarketplaceCategory | "") => void;
  onClear: () => void;
  onSearchChange: (value: string) => void;
  search: string;
}

export function MarketplaceFilterBar({
  category,
  disabled = false,
  onApply,
  onCategoryChange,
  onClear,
  onSearchChange,
  search,
}: MarketplaceFilterBarProps) {
  return (
    <div className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-card lg:grid-cols-[1fr_15rem_auto] lg:items-end">
      <div className="grid gap-1.5">
        <span className="text-sm font-semibold text-slate-700">
          Search title
        </span>
        <SearchBar
          disabled={disabled}
          maxLength={100}
          onSearch={() => onApply()}
          onValueChange={onSearchChange}
          placeholder="Search listing titles..."
          value={search}
        />
      </div>

      <Dropdown
        disabled={disabled}
        label="Category"
        onChange={(event) =>
          onCategoryChange(
            event.target.value as MarketplaceCategory | "",
          )
        }
        options={[
          { label: "All categories", value: "" },
          ...marketplaceCategoryOptions,
        ]}
        value={category}
      />

      <div className="flex flex-wrap gap-2">
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
