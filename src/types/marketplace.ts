export type MarketplaceCategory =
  | "BOOKS"
  | "ELECTRONICS"
  | "CALCULATORS"
  | "HOSTEL_ITEMS"
  | "FURNITURE"
  | "BIKES"
  | "ACCESSORIES"
  | "OTHER";

export type MarketplaceItemCondition =
  | "NEW"
  | "LIKE_NEW"
  | "USED"
  | "FAIR";

export type MarketplaceListingStatus =
  | "PENDING_REVIEW"
  | "ACTIVE"
  | "SOLD"
  | "REJECTED"
  | "DELETED";
export type MarketplaceListingUpdateStatus = "ACTIVE" | "SOLD";

export interface MarketplaceImage {
  altText: string | null;
  displayOrder: number;
  id: string;
  imageUrl: string;
  mimeType?: string | null;
  originalFilename?: string | null;
  sizeBytes?: number | null;
}

export interface MarketplaceSeller {
  avatarUrl: string | null;
  fullName: string;
  university: string;
  userId: string;
}

export interface MarketplaceListingSummary {
  category: MarketplaceCategory;
  condition: MarketplaceItemCondition;
  createdAt: string;
  currency: string;
  id: string;
  price: number;
  primaryImage: MarketplaceImage | null;
  seller: MarketplaceSeller;
  status: MarketplaceListingStatus;
  title: string;
  updatedAt: string;
}

export interface MarketplaceListingDetail
  extends Omit<MarketplaceListingSummary, "primaryImage"> {
  description: string;
  images: MarketplaceImage[];
}

export interface MarketplaceListingPage {
  content: MarketplaceListingSummary[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MarketplaceImageRequest {
  altText?: string | null;
  imageUrl: string;
}

export interface CreateMarketplaceListingRequest {
  category: MarketplaceCategory;
  condition: MarketplaceItemCondition;
  currency: string;
  description: string;
  images?: MarketplaceImageRequest[];
  price: number;
  title: string;
}

export interface UpdateMarketplaceListingRequest {
  category?: MarketplaceCategory;
  condition?: MarketplaceItemCondition;
  currency?: string;
  description?: string;
  images?: MarketplaceImageRequest[];
  price?: number;
  status?: MarketplaceListingUpdateStatus;
  title?: string;
}

export interface MarketplaceListParameters {
  category?: MarketplaceCategory;
  page?: number;
  search?: string;
  signal?: AbortSignal;
  size?: number;
}
