import { ArrowLeft, RefreshCw } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { ApiError } from "@/api/apiClient";
import {
  getLostFoundItem,
  updateLostFoundItem,
} from "@/api/lostFoundApi";
import {
  Button,
  ErrorMessage,
  LoadingSpinner,
  PageHeader,
  useToast,
} from "@/components/common";
import { LostFoundItemForm } from "@/components/lost-found";
import { paths } from "@/routes/paths";
import type {
  LostFoundItemDetail,
  UpdateLostFoundItemRequest,
} from "@/types/lostFound";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

export function EditLostFoundItemPage() {
  const { itemId } = useParams<{ itemId: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [item, setItem] = useState<LostFoundItemDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string[]>>({});
  const [refreshKey, setRefreshKey] = useState(0);

  useDocumentTitle(item ? `Edit ${item.title} · CampusOne` : "Edit Lost & Found item");

  useEffect(() => {
    if (!itemId) return;
    const controller = new AbortController();
    let active = true;
    void getLostFoundItem(itemId, controller.signal)
      .then((response) => {
        if (!active) return;
        setItem(response);
        setError(null);
      })
      .catch((requestError: unknown) => {
        if (!active) return;
        setError(
          requestError instanceof ApiError
            ? requestError.message
            : "This item could not be loaded.",
        );
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [itemId, refreshKey]);

  const handleSubmit = async (
    request: UpdateLostFoundItemRequest,
    images?: File[],
  ) => {
    if (!itemId) return;
    setIsSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      const updated = await updateLostFoundItem(itemId, request, images);
      showToast({
        title: "Changes submitted",
        message: "Your item will be reviewed before it appears publicly again.",
        variant: "success",
      });
      navigate(paths.lostFoundDetail(updated.id), { replace: true });
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        setError(requestError.message);
        setFieldErrors(requestError.fieldErrors);
      } else {
        setError("This item could not be updated. Please try again.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="grid min-h-96 place-items-center">
        <LoadingSpinner label="Loading item" />
      </div>
    );
  }

  if (error && !item) {
    return (
      <div className="grid gap-4">
        <ErrorMessage message={error} />
        <Button
          onClick={() => {
            setIsLoading(true);
            setRefreshKey((current) => current + 1);
          }}
        >
          <RefreshCw className="size-4" />
          Try again
        </Button>
      </div>
    );
  }

  if (!item) return null;

  return (
    <div className="grid gap-6 pb-8">
      <Link
        className="inline-flex w-fit items-center gap-2 text-sm font-semibold text-slate-600 hover:text-brand-700"
        to={paths.lostFoundDetail(item.id)}
      >
        <ArrowLeft className="size-4" />
        Back to item
      </Link>

      <PageHeader
        description="Editing an item sends it back to review so public posts stay safe and accurate."
        eyebrow="Lost & Found"
        title="Edit item"
      />

      {error ? <ErrorMessage message={error} /> : null}

      <LostFoundItemForm
        backendFieldErrors={fieldErrors}
        initialItem={item}
        isSubmitting={isSubmitting}
        mode="edit"
        onSubmit={handleSubmit}
        submitLabel="Submit changes"
      />
    </div>
  );
}
