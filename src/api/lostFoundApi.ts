import { apiRequest } from "@/api/apiClient";
import type {
  CreateLostFoundItemRequest,
  CreateLostFoundClaimRequest,
  LostFoundCategory,
  LostFoundClaim,
  LostFoundClaimPage,
  LostFoundItemDetail,
  LostFoundItemPage,
  LostFoundListParameters,
  LostFoundMatch,
  LostFoundMatchPage,
  LostFoundStats,
  UpdateLostFoundClaimContactPhoneRequest,
  UpdateLostFoundItemRequest,
} from "@/types/lostFound";

function queryString(parameters: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(parameters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") query.set(key, String(value));
  });
  const value = query.toString();
  return value ? `?${value}` : "";
}

const basePath = "/lost-found";

export function listLostFoundItems({
  category,
  page = 0,
  search,
  signal,
  size = 12,
  type,
}: LostFoundListParameters = {}) {
  return apiRequest<LostFoundItemPage>(
    `${basePath}/items${queryString({
      category,
      page,
      search,
      size,
      type,
    })}`,
    { signal },
  );
}

export function listMyLostFoundItems({
  page = 0,
  signal,
  size = 12,
}: Pick<LostFoundListParameters, "page" | "signal" | "size"> = {}) {
  return apiRequest<LostFoundItemPage>(
    `${basePath}/items/my${queryString({ page, size })}`,
    { signal },
  );
}

export function getLostFoundItem(itemId: string, signal?: AbortSignal) {
  return apiRequest<LostFoundItemDetail>(`${basePath}/items/${itemId}`, {
    signal,
  });
}

export function createLostFoundItem(
  request: CreateLostFoundItemRequest,
  images: File[] = [],
) {
  const formData = new FormData();
  formData.append(
    "item",
    new Blob([JSON.stringify(request)], { type: "application/json" }),
  );
  images.forEach((image) => formData.append("images", image));
  return apiRequest<LostFoundItemDetail>(`${basePath}/items`, {
    body: formData,
    method: "POST",
  });
}

export function updateLostFoundItem(
  itemId: string,
  request: UpdateLostFoundItemRequest,
  images?: File[],
) {
  const formData = new FormData();
  formData.append(
    "item",
    new Blob([JSON.stringify(request)], { type: "application/json" }),
  );
  images?.forEach((image) => formData.append("images", image));
  return apiRequest<LostFoundItemDetail>(`${basePath}/items/${itemId}`, {
    body: formData,
    method: "PATCH",
  });
}

export function deleteLostFoundItem(itemId: string) {
  return apiRequest<void>(`${basePath}/items/${itemId}`, {
    method: "DELETE",
  });
}

export function closeLostFoundItem(itemId: string) {
  return apiRequest<LostFoundItemDetail>(`${basePath}/items/${itemId}/close`, {
    method: "PATCH",
  });
}

export function archiveLostFoundItem(itemId: string) {
  return apiRequest<LostFoundItemDetail>(
    `${basePath}/items/${itemId}/archive`,
    { method: "PATCH" },
  );
}

export function renewLostFoundItem(itemId: string) {
  return apiRequest<LostFoundItemDetail>(`${basePath}/items/${itemId}/renew`, {
    method: "PATCH",
  });
}

export function reopenLostFoundItem(itemId: string) {
  return apiRequest<LostFoundItemDetail>(`${basePath}/items/${itemId}/reopen`, {
    method: "PATCH",
  });
}

export function resolveLostFoundItem(itemId: string) {
  return apiRequest<LostFoundItemDetail>(
    `${basePath}/items/${itemId}/resolve`,
    { method: "PATCH" },
  );
}

export function createLostFoundClaim(
  itemId: string,
  request: CreateLostFoundClaimRequest,
) {
  return apiRequest<LostFoundClaim>(`${basePath}/items/${itemId}/claims`, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function updateLostFoundClaimContactPhone(
  claimId: string,
  request: UpdateLostFoundClaimContactPhoneRequest,
) {
  return apiRequest<LostFoundClaim>(
    `${basePath}/claims/${claimId}/contact-phone`,
    {
      body: JSON.stringify(request),
      method: "PATCH",
    },
  );
}

export function listLostFoundClaims({
  page = 0,
  signal,
  size = 12,
}: {
  page?: number;
  signal?: AbortSignal;
  size?: number;
} = {}) {
  return apiRequest<LostFoundClaimPage>(
    `${basePath}/claims/my${queryString({ page, size })}`,
    { signal },
  );
}

export function listLostFoundItemClaims(
  itemId: string,
  {
    page = 0,
    signal,
    size = 12,
  }: { page?: number; signal?: AbortSignal; size?: number } = {},
) {
  return apiRequest<LostFoundClaimPage>(
    `${basePath}/items/${itemId}/claims${queryString({ page, size })}`,
    { signal },
  );
}

export function approveLostFoundClaim(claimId: string, note?: string) {
  return apiRequest<LostFoundClaim>(`${basePath}/claims/${claimId}/approve`, {
    body: JSON.stringify({ note }),
    method: "PATCH",
  });
}

export function rejectLostFoundClaim(claimId: string, note?: string) {
  return apiRequest<LostFoundClaim>(`${basePath}/claims/${claimId}/reject`, {
    body: JSON.stringify({ note }),
    method: "PATCH",
  });
}

export function cancelLostFoundClaim(claimId: string) {
  return apiRequest<LostFoundClaim>(`${basePath}/claims/${claimId}/cancel`, {
    method: "PATCH",
  });
}

export function confirmLostFoundClaimantHandover(
  claimId: string,
  handoverNote?: string,
) {
  return apiRequest<LostFoundClaim>(
    `${basePath}/claims/${claimId}/handover/claimant`,
    {
      body: JSON.stringify({ handoverNote }),
      method: "PATCH",
    },
  );
}

export function confirmLostFoundReporterHandover(
  claimId: string,
  handoverNote?: string,
) {
  return apiRequest<LostFoundClaim>(
    `${basePath}/claims/${claimId}/handover/reporter`,
    {
      body: JSON.stringify({ handoverNote }),
      method: "PATCH",
    },
  );
}

export function completeLostFoundClaim(
  claimId: string,
  handoverNote?: string,
) {
  return apiRequest<LostFoundClaim>(`${basePath}/claims/${claimId}/complete`, {
    body: JSON.stringify({ handoverNote }),
    method: "PATCH",
  });
}

export function listLostFoundMatches({
  page = 0,
  signal,
  size = 12,
}: {
  page?: number;
  signal?: AbortSignal;
  size?: number;
} = {}) {
  return apiRequest<LostFoundMatchPage>(
    `${basePath}/matches/my${queryString({ page, size })}`,
    { signal },
  );
}

export function listLostFoundItemMatches(
  itemId: string,
  {
    page = 0,
    signal,
    size = 12,
  }: { page?: number; signal?: AbortSignal; size?: number } = {},
) {
  return apiRequest<LostFoundMatchPage>(
    `${basePath}/items/${itemId}/matches${queryString({ page, size })}`,
    { signal },
  );
}

export function confirmLostFoundMatch(matchId: string) {
  return apiRequest<LostFoundMatch>(`${basePath}/matches/${matchId}/confirm`, {
    method: "PATCH",
  });
}

export function rejectLostFoundMatch(matchId: string) {
  return apiRequest<LostFoundMatch>(`${basePath}/matches/${matchId}/reject`, {
    method: "PATCH",
  });
}

export function getLostFoundStats(signal?: AbortSignal) {
  return apiRequest<LostFoundStats>(`${basePath}/admin/stats`, { signal });
}

export const lostFoundCategories: LostFoundCategory[] = [
  "ID_CARD",
  "WALLET_PURSE",
  "ELECTRONICS",
  "KEYS",
  "BAG",
  "BOOKS_STATIONERY",
  "CLOTHING_ACCESSORIES",
  "DOCUMENTS",
  "JEWELRY",
  "BOTTLE_UMBRELLA",
  "OTHER",
];
