import type {
  MarketplaceCategory,
  MarketplaceItemCondition,
  MarketplaceListingStatus,
} from "@/types/marketplace";
import {
  formatCurrency,
  formatDate,
} from "@/utils/format";

export const marketplaceCategoryOptions: Array<{
  label: string;
  value: MarketplaceCategory;
}> = [
  { label: "Books", value: "BOOKS" },
  { label: "Electronics", value: "ELECTRONICS" },
  { label: "Calculators", value: "CALCULATORS" },
  { label: "Hostel items", value: "HOSTEL_ITEMS" },
  { label: "Furniture", value: "FURNITURE" },
  { label: "Bikes", value: "BIKES" },
  { label: "Accessories", value: "ACCESSORIES" },
  { label: "Other", value: "OTHER" },
];

export const marketplaceConditionOptions: Array<{
  label: string;
  value: MarketplaceItemCondition;
}> = [
  { label: "New", value: "NEW" },
  { label: "Like new", value: "LIKE_NEW" },
  { label: "Used", value: "USED" },
  { label: "Fair", value: "FAIR" },
];

export function categoryLabel(category: MarketplaceCategory) {
  return marketplaceCategoryOptions.find((option) => option.value === category)
    ?.label ?? category;
}

export function conditionLabel(condition: MarketplaceItemCondition) {
  return marketplaceConditionOptions.find(
    (option) => option.value === condition,
  )?.label ?? condition;
}

export function statusLabel(status: MarketplaceListingStatus) {
  return {
    ACTIVE: "Active",
    DELETED: "Deleted",
    SOLD: "Sold",
  }[status];
}

export function formatMarketplaceDate(value: string) {
  return formatDate(value);
}

export function formatMarketplacePrice(price: number, currency: string) {
  return formatCurrency(price, currency);
}
