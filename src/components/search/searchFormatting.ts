import type { GlobalSearchType } from "@/types/search";
import { formatDate } from "@/utils/format";

export function searchTypeLabel(type: GlobalSearchType) {
  return {
    DISCUSSION: "Discussion",
    EVENT: "Event",
    INTERNSHIP: "Internship",
    MARKETPLACE: "Marketplace",
    NOTE: "Note",
  }[type];
}

export function formatSearchDate(value: string) {
  return formatDate(value);
}
