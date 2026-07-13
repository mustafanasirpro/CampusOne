import type { GlobalSearchType } from "@/types/search";
import { formatDate } from "@/utils/format";

export function searchTypeLabel(type: GlobalSearchType) {
  return {
    DISCUSSION: "Discussion",
    EVENT: "Event",
    INTERNSHIP: "Internship",
    LOST_FOUND: "Lost & Found",
    MARKETPLACE: "Marketplace",
    NOTE: "Note",
  }[type];
}

export function formatSearchDate(value: string) {
  return formatDate(value);
}
