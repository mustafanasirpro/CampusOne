import {
  ArrowLeft,
  CalendarDays,
  Edit3,
  GraduationCap,
  PackageCheck,
  Tags,
  Trash2,
  UserRound,
} from "lucide-react";
import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  deleteListing,
  getListingById,
} from "@/api/marketplaceApi";
import { getCurrentUserIdentity } from "@/api/userApi";
import {
  Avatar,
  Badge,
  Button,
  Card,
  CardContent,
  ErrorMessage,
  LoadingSpinner,
  useToast,
} from "@/components/common";
import {
  categoryLabel,
  conditionLabel,
  formatMarketplaceDate,
  MarketplaceImageGallery,
  PriceTag,
  statusLabel,
} from "@/components/marketplace";
import { paths } from "@/routes/paths";
import type { MarketplaceListingDetail } from "@/types/marketplace";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

function requestErrorMessage(error: unknown) {
  return error instanceof ApiError
    ? error.message
    : "The marketplace action could not be completed.";
}

export function MarketplaceListingDetailPage() {
  const { listingId } = useParams<{ listingId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [listing, setListing] =
    useState<MarketplaceListingDetail | null>(null);
  const [currentUserId, setCurrentUserId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(() =>
    listingId ? null : "The listing ID is missing.",
  );
  const [actionError, setActionError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isLoading, setIsLoading] = useState(Boolean(listingId));

  useDocumentTitle(
    listing ? `${listing.title} · CampusOne` : "Marketplace · CampusOne",
  );

  useEffect(() => {
    if (!listingId) return;

    const controller = new AbortController();
    let active = true;

    void Promise.all([
      getListingById(listingId, controller.signal),
      getCurrentUserIdentity(controller.signal).catch(() => null),
    ])
      .then(([listingResponse, identity]) => {
        if (!active) return;
        setListing(listingResponse);
        setCurrentUserId(identity?.userId ?? null);
      })
      .catch((requestError: unknown) => {
        if (active) setError(requestErrorMessage(requestError));
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [listingId]);

  const isOwner =
    listing !== null &&
    currentUserId !== null &&
    listing.seller.userId === currentUserId;

  const removeListing = async () => {
    if (!listing) return;
    const confirmed = window.confirm(
      `Delete "${listing.title}"? It will be removed from the marketplace.`,
    );
    if (!confirmed) return;

    setIsDeleting(true);
    setActionError(null);
    try {
      await deleteListing(listing.id);
      showToast({
        title: "Listing deleted",
        message: "The listing was removed from the marketplace.",
        variant: "success",
      });
      navigate(paths.marketplace, { replace: true });
    } catch (requestError) {
      setActionError(requestErrorMessage(requestError));
      setIsDeleting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="grid min-h-[60vh] place-items-center">
        <LoadingSpinner label="Loading listing details" />
      </div>
    );
  }

  if (error || !listing) {
    return (
      <div className="grid gap-4">
        <ErrorMessage
          message={error ?? "The marketplace listing could not be found."}
        />
        <Link
          className="inline-flex h-10 w-fit items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          to={paths.marketplace}
        >
          <ArrowLeft className="size-4" />
          Back to marketplace
        </Link>
      </div>
    );
  }

  return (
    <div className="grid gap-6 pb-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link
          className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600 transition hover:text-brand-700"
          to={paths.marketplace}
        >
          <ArrowLeft className="size-4" />
          Back to marketplace
        </Link>
        {isOwner ? (
          <div className="flex flex-wrap gap-2">
            <Link
              className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 hover:bg-slate-50"
              to={paths.marketplaceEdit(listing.id)}
            >
              <Edit3 className="size-4" />
              Edit listing
            </Link>
            <Button
              loading={isDeleting}
              onClick={() => void removeListing()}
              variant="danger"
            >
              <Trash2 className="size-4" />
              Delete
            </Button>
          </div>
        ) : null}
      </div>

      {actionError ? <ErrorMessage message={actionError} /> : null}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.25fr)_minmax(20rem,0.75fr)]">
        <Card className="overflow-hidden">
          <CardContent className="p-4 sm:p-5">
            <MarketplaceImageGallery
              images={listing.images}
              title={listing.title}
            />
          </CardContent>
        </Card>

        <div className="grid content-start gap-5">
          <Card>
            <CardContent className="grid gap-5 p-5 sm:p-6">
              <div className="flex flex-wrap gap-2">
                <Badge variant="brand">
                  {categoryLabel(listing.category)}
                </Badge>
                <Badge>{conditionLabel(listing.condition)}</Badge>
                <Badge
                  variant={
                    listing.status === "ACTIVE" ? "success" : "neutral"
                  }
                >
                  {statusLabel(listing.status)}
                </Badge>
              </div>

              <div>
                <h1 className="text-3xl font-bold tracking-tight text-slate-950">
                  {listing.title}
                </h1>
                <div className="mt-3">
                  <PriceTag
                    currency={listing.currency}
                    price={listing.price}
                  />
                </div>
              </div>

              <dl className="grid gap-3 border-t border-slate-100 pt-5 text-sm">
                <DetailRow
                  icon={<Tags className="size-4" />}
                  label="Category"
                  value={categoryLabel(listing.category)}
                />
                <DetailRow
                  icon={<PackageCheck className="size-4" />}
                  label="Condition"
                  value={conditionLabel(listing.condition)}
                />
                <DetailRow
                  icon={<CalendarDays className="size-4" />}
                  label="Published"
                  value={formatMarketplaceDate(listing.createdAt)}
                />
              </dl>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="flex items-center gap-4 p-5">
              <Avatar
                name={listing.seller.fullName}
                size="lg"
                src={listing.seller.avatarUrl ?? undefined}
              />
              <div className="min-w-0">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                  Seller
                </p>
                <p className="truncate font-semibold text-slate-950">
                  {listing.seller.fullName}
                </p>
                <p className="mt-1 flex items-center gap-1.5 text-sm text-slate-500">
                  <GraduationCap className="size-4 shrink-0" />
                  <span className="truncate">
                    {listing.seller.university}
                  </span>
                </p>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      <Card>
        <CardContent className="grid gap-3 p-5 sm:p-6">
          <div className="flex items-center gap-2">
            <UserRound className="size-5 text-brand-600" />
            <h2 className="text-lg font-semibold text-slate-950">
              About this item
            </h2>
          </div>
          <p className="whitespace-pre-wrap text-sm leading-7 text-slate-600 sm:text-base">
            {listing.description}
          </p>
          {listing.updatedAt !== listing.createdAt ? (
            <p className="border-t border-slate-100 pt-4 text-xs text-slate-400">
              Last updated {formatMarketplaceDate(listing.updatedAt)}
            </p>
          ) : null}
        </CardContent>
      </Card>
    </div>
  );
}

function DetailRow({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-center gap-3">
      <span className="text-slate-400">{icon}</span>
      <dt className="text-slate-500">{label}</dt>
      <dd className="ml-auto font-semibold text-slate-800">{value}</dd>
    </div>
  );
}
