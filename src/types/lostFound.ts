export type LostFoundItemType = "LOST" | "FOUND";

export type LostFoundCategory =
  | "ID_CARD"
  | "WALLET_PURSE"
  | "ELECTRONICS"
  | "KEYS"
  | "BAG"
  | "BOOKS_STATIONERY"
  | "CLOTHING_ACCESSORIES"
  | "DOCUMENTS"
  | "JEWELRY"
  | "BOTTLE_UMBRELLA"
  | "OTHER";

export type LostFoundItemStatus =
  | "PENDING_REVIEW"
  | "PUBLISHED"
  | "CLAIM_IN_PROGRESS"
  | "RESOLVED"
  | "CLOSED"
  | "REJECTED"
  | "ARCHIVED"
  | "DELETED";

export type LostFoundClaimStatus =
  | "PENDING"
  | "APPROVED"
  | "REJECTED"
  | "CANCELLED"
  | "COMPLETED";

export type LostFoundMatchStatus =
  | "SUGGESTED"
  | "CONFIRMED"
  | "REJECTED";

export interface LostFoundReporter {
  avatarUrl: string | null;
  fullName: string | null;
  userId: string;
}

export interface LostFoundImage {
  displayOrder: number;
  fileSizeBytes: number;
  id: string;
  imageUrl: string;
  mimeType: string;
  originalFilename: string;
}

export interface LostFoundItemSummary {
  brand: string | null;
  category: LostFoundCategory;
  color: string | null;
  createdAt: string;
  description: string;
  expiresAt: string | null;
  id: string;
  itemDate: string;
  locationText: string;
  primaryImage: LostFoundImage | null;
  publishedAt: string | null;
  reporter: LostFoundReporter;
  status: LostFoundItemStatus;
  title: string;
  type: LostFoundItemType;
  updatedAt: string;
}

export interface LostFoundItemDetail extends LostFoundItemSummary {
  claimable: boolean;
  images: LostFoundImage[];
  moderationReason: string | null;
  ownedByCurrentUser: boolean;
  resolvedAt: string | null;
}

export interface LostFoundItemPage {
  content: LostFoundItemSummary[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateLostFoundItemRequest {
  brand?: string | null;
  category: LostFoundCategory;
  color?: string | null;
  description: string;
  itemDate: string;
  locationText: string;
  title: string;
  type: LostFoundItemType;
}

export type UpdateLostFoundItemRequest =
  Partial<CreateLostFoundItemRequest>;

export interface LostFoundListParameters {
  category?: LostFoundCategory;
  page?: number;
  search?: string;
  signal?: AbortSignal;
  size?: number;
  type?: LostFoundItemType;
}

export interface LostFoundClaim {
  claimant: LostFoundReporter;
  createdAt: string;
  claimantHandoverConfirmedAt: string | null;
  claimantIsCurrentUser: boolean;
  handoverCompletedAt: string | null;
  handoverNote: string | null;
  id: string;
  itemId: string;
  itemTitle: string;
  proofText: string | null;
  reporterHandoverConfirmedAt: string | null;
  reporterIsCurrentUser: boolean;
  reviewedAt: string | null;
  reviewerNote: string | null;
  status: LostFoundClaimStatus;
}

export interface LostFoundClaimPage {
  content: LostFoundClaim[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LostFoundMatch {
  createdAt: string;
  foundItem: LostFoundItemSummary;
  id: string;
  lostItem: LostFoundItemSummary;
  reasons: unknown;
  score: number;
  status: LostFoundMatchStatus;
  updatedAt: string;
}

export interface LostFoundMatchPage {
  content: LostFoundMatch[];
  first: boolean;
  last: boolean;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LostFoundStats {
  statusCounts: Record<string, number>;
}
