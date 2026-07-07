import { apiRequest } from "@/api/apiClient";
import type {
  CreateMarketplaceListingRequest,
  MarketplaceListingDetail,
  MarketplaceListingPage,
  MarketplaceListParameters,
  UpdateMarketplaceListingRequest,
} from "@/types/marketplace";

function queryString(parameters: Record<string, string | number | undefined>) {
  const query = new URLSearchParams();
  Object.entries(parameters).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      query.set(key, String(value));
    }
  });
  const value = query.toString();
  return value ? `?${value}` : "";
}

const listingsPath = "/marketplace/listings";

export function listListings({
  category,
  page = 0,
  search,
  signal,
  size = 12,
}: MarketplaceListParameters = {}) {
  return apiRequest<MarketplaceListingPage>(
    `${listingsPath}${queryString({ category, page, search, size })}`,
    { signal },
  );
}

export function getMyListings({
  page = 0,
  signal,
  size = 12,
}: Pick<MarketplaceListParameters, "page" | "signal" | "size"> = {}) {
  return apiRequest<MarketplaceListingPage>(
    `${listingsPath}/my${queryString({ page, size })}`,
    { signal },
  );
}

export function getListingById(
  listingId: string,
  signal?: AbortSignal,
) {
  return apiRequest<MarketplaceListingDetail>(
    `${listingsPath}/${listingId}`,
    { signal },
  );
}

export function createListing(
  request: CreateMarketplaceListingRequest,
  imageFiles: File[] = [],
) {
  if (imageFiles.length > 0) {
    const formData = new FormData();
    formData.append(
      "listing",
      new Blob([JSON.stringify({ ...request, images: [] })], {
        type: "application/json",
      }),
    );
    imageFiles.forEach((file) => formData.append("images", file));
    return apiRequest<MarketplaceListingDetail>(`${listingsPath}/upload`, {
      body: formData,
      method: "POST",
    });
  }

  return apiRequest<MarketplaceListingDetail>(listingsPath, {
    body: JSON.stringify(request),
    method: "POST",
  });
}

export function updateListing(
  listingId: string,
  request: UpdateMarketplaceListingRequest,
  imageFiles?: File[],
) {
  if (imageFiles !== undefined) {
    const formData = new FormData();
    formData.append(
      "listing",
      new Blob([JSON.stringify({ ...request, images: undefined })], {
        type: "application/json",
      }),
    );
    imageFiles.forEach((file) => formData.append("images", file));
    return apiRequest<MarketplaceListingDetail>(
      `${listingsPath}/${listingId}/upload`,
      {
        body: formData,
        method: "PATCH",
      },
    );
  }

  return apiRequest<MarketplaceListingDetail>(
    `${listingsPath}/${listingId}`,
    {
      body: JSON.stringify(request),
      method: "PATCH",
    },
  );
}

export function deleteListing(listingId: string) {
  return apiRequest<void>(`${listingsPath}/${listingId}`, {
    method: "DELETE",
  });
}
