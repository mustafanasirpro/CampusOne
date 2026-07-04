import type { GlobalSearchType } from "@/types/search";

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
  return new Intl.DateTimeFormat("en-PK", {
    dateStyle: "medium",
  }).format(new Date(value));
}

