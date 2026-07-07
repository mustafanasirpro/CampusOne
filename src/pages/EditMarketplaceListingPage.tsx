import { ArrowLeft, LockKeyhole } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  getListingById,
  updateListing,
} from "@/api/marketplaceApi";
import { getCurrentUserIdentity } from "@/api/userApi";
import {
  EmptyState,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  useToast,
} from "@/components/common";
import { MarketplaceListingForm } from "@/components/marketplace";
import { paths } from "@/routes/paths";
import type {
  MarketplaceListingDetail,
  UpdateMarketplaceListingRequest,
} from "@/types/marketplace";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function EditMarketplaceListingPage() {
  const { listingId } = useParams<{ listingId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [listing, setListing] =
    useState<MarketplaceListingDetail | null>(null);
  const [isOwner, setIsOwner] = useState(false);
  const [isLoading, setIsLoading] = useState(Boolean(listingId));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(() =>
    listingId ? null : "The listing ID is missing.",
  );
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<
    Record<string, string[]>
  >({});

  useDocumentTitle(
    listing
      ? `Edit ${listing.title} · CampusOne`
      : "Edit listing · CampusOne",
  );

  useEffect(() => {
    if (!listingId) return;

    const controller = new AbortController();
    let active = true;

    void Promise.all([
      getListingById(listingId, controller.signal),
      getCurrentUserIdentity(controller.signal),
    ])
      .then(([listingResponse, identity]) => {
        if (!active) return;
        setListing(listingResponse);
        setIsOwner(listingResponse.seller.userId === identity.userId);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setLoadError(
          requestError instanceof ApiError
            ? requestError.message
            : "The listing could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [listingId]);

  const handleSubmit = async (
    request: UpdateMarketplaceListingRequest,
    imageFiles?: File[],
  ) => {
    if (!listingId) return;
    setIsSubmitting(true);
    setSubmitError(null);
    setFieldErrors({});
    try {
      const updated = await updateListing(listingId, request, imageFiles);
      showToast({
        title: "Listing updated",
        message: "Your marketplace listing changes were saved.",
        variant: "success",
      });
      navigate(paths.marketplaceDetail(updated.id), { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        setSubmitError(requestError.message);
        setFieldErrors(requestError.fieldErrors);
      } else {
        setSubmitError("The listing could not be updated. Please try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="grid min-h-[60vh] place-items-center">
        <LoadingSpinner label="Loading your listing" />
      </div>
    );
  }

  if (!listing || loadError) {
    return (
      <div className="grid gap-4">
        <ErrorMessage
          message={loadError ?? "The listing could not be found."}
        />
        <Link
          className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand-700"
          to={paths.marketplace}
        >
          <ArrowLeft className="size-4" />
          Back to marketplace
        </Link>
      </div>
    );
  }

  if (!isOwner) {
    return (
      <EmptyState
        action={
          <Link
            className="inline-flex h-10 items-center rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700"
            to={paths.marketplaceDetail(listing.id)}
          >
            View listing
          </Link>
        }
        description="Only the student who created this listing can edit it."
        icon={<LockKeyhole className="size-6" />}
        title="Owner access required"
      />
    );
  }

  return (
    <div className="grid gap-6 pb-8">
      <Link
        className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand-700"
        to={paths.marketplaceDetail(listing.id)}
      >
        <ArrowLeft className="size-4" />
        Back to listing
      </Link>

      <PageHeader
        description="Update listing details, replace uploaded images, or mark the item as sold."
        eyebrow="Your listings"
        title="Edit listing"
      />

      {submitError ? <ErrorMessage message={submitError} /> : null}

      <MarketplaceListingForm
        backendFieldErrors={fieldErrors}
        initialListing={listing}
        isSubmitting={isSubmitting}
        mode="edit"
        onSubmit={handleSubmit}
        submitLabel="Save changes"
      />
    </div>
  );
}
