import type {
  LostFoundCategory,
  LostFoundClaimStatus,
  LostFoundItemStatus,
  LostFoundItemType,
} from "@/types/lostFound";
import { formatDate, formatDateTime } from "@/utils/format";

export const lostFoundTypeOptions: Array<{
  label: string;
  value: LostFoundItemType | "";
}> = [
  { label: "All items", value: "" },
  { label: "Lost", value: "LOST" },
  { label: "Found", value: "FOUND" },
];

export const lostFoundCategoryOptions: Array<{
  label: string;
  value: LostFoundCategory | "";
}> = [
  { label: "All categories", value: "" },
  { label: "ID card", value: "ID_CARD" },
  { label: "Wallet or purse", value: "WALLET_PURSE" },
  { label: "Electronics", value: "ELECTRONICS" },
  { label: "Keys", value: "KEYS" },
  { label: "Bag", value: "BAG" },
  { label: "Books and stationery", value: "BOOKS_STATIONERY" },
  { label: "Clothing and accessories", value: "CLOTHING_ACCESSORIES" },
  { label: "Documents", value: "DOCUMENTS" },
  { label: "Jewelry", value: "JEWELRY" },
  { label: "Bottle or umbrella", value: "BOTTLE_UMBRELLA" },
  { label: "Other", value: "OTHER" },
];

export function lostFoundTypeLabel(type: LostFoundItemType) {
  return type === "LOST" ? "Lost" : "Found";
}

export function lostFoundCategoryLabel(category: LostFoundCategory) {
  return (
    lostFoundCategoryOptions.find((option) => option.value === category)
      ?.label ?? category
  );
}

export function lostFoundStatusLabel(status: LostFoundItemStatus) {
  return {
    ARCHIVED: "Archived",
    CLAIM_IN_PROGRESS: "Claim in progress",
    CLOSED: "Closed",
    DELETED: "Deleted",
    PENDING_REVIEW: "Pending review",
    PUBLISHED: "Published",
    REJECTED: "Rejected",
    RESOLVED: "Resolved",
  }[status];
}

export function lostFoundClaimStatusLabel(status: LostFoundClaimStatus) {
  return {
    APPROVED: "Approved",
    CANCELLED: "Cancelled",
    COMPLETED: "Completed",
    PENDING: "Pending",
    REJECTED: "Rejected",
  }[status];
}

export function formatLostFoundDate(value: string | null) {
  return formatDate(value, "Not available");
}

export function formatLostFoundDateTime(value: string | null) {
  return formatDateTime(value, "Not available");
}
