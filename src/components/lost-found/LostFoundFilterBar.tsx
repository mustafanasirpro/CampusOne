import { Button, Card, CardContent } from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import {
  lostFoundCategoryOptions,
  lostFoundTypeOptions,
} from "@/components/lost-found/lostFoundFormatting";
import type {
  LostFoundCategory,
  LostFoundItemType,
} from "@/types/lostFound";

export function LostFoundFilterBar({
  category,
  onApply,
  onCategoryChange,
  onClear,
  onSearchChange,
  onTypeChange,
  search,
  type,
}: {
  category: LostFoundCategory | "";
  onApply: () => void;
  onCategoryChange: (value: LostFoundCategory | "") => void;
  onClear: () => void;
  onSearchChange: (value: string) => void;
  onTypeChange: (value: LostFoundItemType | "") => void;
  search: string;
  type: LostFoundItemType | "";
}) {
  return (
    <Card>
      <CardContent className="grid gap-4 p-4 md:grid-cols-[1.5fr_1fr_1fr_auto_auto] md:items-end">
        <FormField
          label="Search"
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder="Laptop, keys, wallet, library..."
          type="search"
          value={search}
        />
        <SelectField
          label="Type"
          onChange={(event) =>
            onTypeChange(event.target.value as LostFoundItemType | "")
          }
          options={lostFoundTypeOptions}
          value={type}
        />
        <SelectField
          label="Category"
          onChange={(event) =>
            onCategoryChange(event.target.value as LostFoundCategory | "")
          }
          options={lostFoundCategoryOptions}
          value={category}
        />
        <Button onClick={onApply}>Apply</Button>
        <Button onClick={onClear} variant="outline">
          Clear
        </Button>
      </CardContent>
    </Card>
  );
}
