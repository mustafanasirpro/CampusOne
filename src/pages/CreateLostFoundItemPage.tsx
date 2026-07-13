import { ArrowLeft } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import { createLostFoundItem } from "@/api/lostFoundApi";
import { ErrorMessage, PageHeader, useToast } from "@/components/common";
import { LostFoundItemForm } from "@/components/lost-found";
import { paths } from "@/routes/paths";
import type { CreateLostFoundItemRequest } from "@/types/lostFound";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function CreateLostFoundItemPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string[]>>({});

  useDocumentTitle("Submit Lost & Found item · CampusOne");

  const handleSubmit = async (
    request: CreateLostFoundItemRequest,
    images: File[],
  ) => {
    setIsSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      const item = await createLostFoundItem(request, images);
      showToast({
        title: "Submitted for review",
        message: "Your item will appear after admin approval.",
        variant: "success",
      });
      navigate(paths.lostFoundDetail(item.id), { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        setError(requestError.message);
        setFieldErrors(requestError.fieldErrors);
      } else {
        setError("Your item could not be submitted. Please try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="grid gap-6 pb-8">
      <Link
        className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand-700"
        to={paths.lostFound}
      >
        <ArrowLeft className="size-4" />
        Back to Lost & Found
      </Link>

      <PageHeader
        description="Share what was lost or found. CampusOne reviews submissions before they go public."
        eyebrow="Lost & Found"
        title="Submit an item"
      />

      {error ? <ErrorMessage message={error} /> : null}

      <LostFoundItemForm
        backendFieldErrors={fieldErrors}
        isSubmitting={isSubmitting}
        mode="create"
        onSubmit={handleSubmit}
        submitLabel="Submit for review"
      />
    </div>
  );
}
