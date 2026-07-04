import { ArrowLeft } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { createListing } from "@/api/marketplaceApi";
import { ErrorMessage, PageHeader, useToast } from "@/components/common";
import { MarketplaceListingForm } from "@/components/marketplace";
import { paths } from "@/routes/paths";
import type { CreateMarketplaceListingRequest } from "@/types/marketplace";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function CreateMarketplaceListingPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<
    Record<string, string[]>
  >({});

  useDocumentTitle("Create listing · CampusOne");

  const handleSubmit = async (
    request: CreateMarketplaceListingRequest,
  ) => {
    setIsSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      const listing = await createListing(request);
      showToast({
        title: "Listing published",
        message: "Your item is now visible in the campus marketplace.",
        variant: "success",
      });
      navigate(paths.marketplaceDetail(listing.id), { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        setError(requestError.message);
        setFieldErrors(requestError.fieldErrors);
      } else {
        setError("The listing could not be created. Please try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="grid gap-6 pb-8">
      <Link
        className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand-700"
        to={paths.marketplace}
      >
        <ArrowLeft className="size-4" />
        Back to marketplace
      </Link>

      <PageHeader
        description="Publish an item with clear details, price, condition, and optional image links."
        eyebrow="Marketplace"
        title="Create a listing"
      />

      {error ? <ErrorMessage message={error} /> : null}

      <MarketplaceListingForm
        backendFieldErrors={fieldErrors}
        isSubmitting={isSubmitting}
        mode="create"
        onSubmit={handleSubmit}
        submitLabel="Publish listing"
      />
    </div>
  );
}
