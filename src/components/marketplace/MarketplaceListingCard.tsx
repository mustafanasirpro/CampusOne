import { CalendarDays, Edit3, Eye, UserRound } from "lucide-react";
import { Link } from "react-router-dom";

import { Badge, Card, CardContent } from "@/components/common";
import {
  ListingImage,
  MarketplaceImagePlaceholder,
} from "@/components/marketplace/MarketplaceImageGallery";
import {
  categoryLabel,
  conditionLabel,
  formatMarketplaceDate,
  statusLabel,
} from "@/components/marketplace/marketplaceFormatting";
import { PriceTag } from "@/components/marketplace/PriceTag";
import { paths } from "@/routes/paths";
import type { MarketplaceListingSummary } from "@/types/marketplace";

export function MarketplaceListingCard({
  listing,
  owned = false,
}: {
  listing: MarketplaceListingSummary;
  owned?: boolean;
}) {
  return (
    <Card className="group flex h-full flex-col overflow-hidden hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl">
      <div className="relative overflow-hidden">
        {listing.primaryImage ? (
          <ListingImage
            alt={listing.primaryImage.altText || listing.title}
            className="aspect-[4/3] w-full transition duration-300 group-hover:scale-[1.03]"
            src={listing.primaryImage.imageUrl}
          />
        ) : (
          <MarketplaceImagePlaceholder className="aspect-[4/3] w-full" />
        )}
        <Badge className="absolute left-3 top-3 bg-white/90 text-slate-700 ring-white backdrop-blur">
          {categoryLabel(listing.category)}
        </Badge>
        {owned ? (
          <Badge
            className="absolute right-3 top-3 bg-white/90 ring-white backdrop-blur"
            variant={listing.status === "ACTIVE" ? "success" : "neutral"}
          >
            {statusLabel(listing.status)}
          </Badge>
        ) : null}
      </div>

      <CardContent className="flex flex-1 flex-col p-5">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h2 className="line-clamp-2 font-semibold leading-6 text-slate-950">
              {listing.title}
            </h2>
            <div className="mt-2">
              <PriceTag
                currency={listing.currency}
                price={listing.price}
              />
            </div>
          </div>
          <Badge>{conditionLabel(listing.condition)}</Badge>
        </div>

        <div className="mt-5 grid gap-2.5 text-sm text-slate-500">
          <p className="flex items-center gap-2">
            <UserRound className="size-4 shrink-0 text-slate-400" />
            <span className="truncate">
              {listing.seller.fullName} · {listing.seller.university}
            </span>
          </p>
          <p className="flex items-center gap-2">
            <CalendarDays className="size-4 shrink-0 text-slate-400" />
            {formatMarketplaceDate(listing.createdAt)}
          </p>
        </div>

        <div className="mt-auto grid grid-cols-1 gap-2 pt-5 sm:grid-cols-2">
          <Link
            className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
            to={paths.marketplaceDetail(listing.id)}
          >
            <Eye className="size-4" />
            Details
          </Link>
          {owned ? (
            <Link
              className="inline-flex h-10 items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white transition hover:bg-brand-700"
              to={paths.marketplaceEdit(listing.id)}
            >
              <Edit3 className="size-4" />
              Edit
            </Link>
          ) : (
            <span className="inline-flex h-10 items-center justify-center rounded-xl bg-slate-50 px-3 text-xs font-medium text-slate-500">
              {statusLabel(listing.status)}
            </span>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
