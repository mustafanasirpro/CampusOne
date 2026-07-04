import { ImagePlus, Save, Trash2 } from "lucide-react";
import { useState, type FormEvent } from "react";

import { Button, Card, CardContent } from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import {
  marketplaceCategoryOptions,
  marketplaceConditionOptions,
} from "@/components/marketplace/marketplaceFormatting";
import type {
  CreateMarketplaceListingRequest,
  MarketplaceCategory,
  MarketplaceImageRequest,
  MarketplaceItemCondition,
  MarketplaceListingDetail,
  MarketplaceListingUpdateStatus,
  UpdateMarketplaceListingRequest,
} from "@/types/marketplace";
import { cn } from "@/utils/cn";

interface MarketplaceFormState {
  category: MarketplaceCategory;
  condition: MarketplaceItemCondition;
  currency: string;
  description: string;
  images: MarketplaceImageRequest[];
  price: string;
  status: MarketplaceListingUpdateStatus;
  title: string;
}

type MarketplaceFormErrors = Partial<
  Record<keyof MarketplaceFormState | `image-${number}`, string>
>;

interface CommonMarketplaceFormProps {
  backendFieldErrors?: Record<string, string[]>;
  isSubmitting: boolean;
  submitLabel: string;
}

type MarketplaceListingFormProps =
  | (CommonMarketplaceFormProps & {
      mode: "create";
      onSubmit: (request: CreateMarketplaceListingRequest) => Promise<void>;
    })
  | (CommonMarketplaceFormProps & {
      initialListing: MarketplaceListingDetail;
      mode: "edit";
      onSubmit: (request: UpdateMarketplaceListingRequest) => Promise<void>;
    });

const httpUrlPattern = /^https?:\/\/\S+$/i;

function initialState(
  initialListing?: MarketplaceListingDetail,
): MarketplaceFormState {
  return {
    category: initialListing?.category ?? "BOOKS",
    condition: initialListing?.condition ?? "USED",
    currency: initialListing?.currency ?? "PKR",
    description: initialListing?.description ?? "",
    images:
      initialListing?.images.map((image) => ({
        altText: image.altText,
        imageUrl: image.imageUrl,
      })) ?? [],
    price: initialListing ? String(initialListing.price) : "",
    status:
      initialListing?.status === "SOLD" ? "SOLD" : "ACTIVE",
    title: initialListing?.title ?? "",
  };
}

function TextAreaField({
  error,
  onChange,
  value,
}: {
  error?: string;
  onChange: (value: string) => void;
  value: string;
}) {
  return (
    <label className="grid gap-1.5">
      <span className="text-sm font-semibold text-slate-700">
        Description
        <span aria-hidden="true" className="ml-1 text-red-500">
          *
        </span>
      </span>
      <textarea
        aria-invalid={Boolean(error)}
        className={cn(
          "min-h-40 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:ring-4",
          error
            ? "border-red-300 focus:border-red-400 focus:ring-red-100"
            : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
        )}
        maxLength={5000}
        onChange={(event) => onChange(event.target.value)}
        placeholder="Describe the item, its condition, and anything a buyer should know."
        required
        value={value}
      />
      <span className="flex justify-between gap-3 text-xs">
        <span className={error ? "font-medium text-red-600" : "text-slate-500"}>
          {error ?? "Include honest, useful details for campus buyers."}
        </span>
        <span className="shrink-0 text-slate-400">
          {value.length}/5000
        </span>
      </span>
    </label>
  );
}

function sameImages(
  first: MarketplaceImageRequest[],
  second: MarketplaceImageRequest[],
) {
  return (
    first.length === second.length &&
    first.every(
      (image, index) =>
        image.imageUrl === second[index]?.imageUrl &&
        (image.altText ?? null) === (second[index]?.altText ?? null),
    )
  );
}

export function MarketplaceListingForm(
  props: MarketplaceListingFormProps,
) {
  const initialListing =
    props.mode === "edit" ? props.initialListing : undefined;
  const [form, setForm] = useState<MarketplaceFormState>(() =>
    initialState(initialListing),
  );
  const [errors, setErrors] = useState<MarketplaceFormErrors>({});

  const update = <Key extends keyof MarketplaceFormState>(
    key: Key,
    value: MarketplaceFormState[Key],
  ) => {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: undefined }));
  };

  const backendError = (key: string) =>
    props.backendFieldErrors?.[key]?.[0];

  const updateImage = (
    index: number,
    key: keyof MarketplaceImageRequest,
    value: string,
  ) => {
    update(
      "images",
      form.images.map((image, imageIndex) =>
        imageIndex === index ? { ...image, [key]: value } : image,
      ),
    );
    setErrors((current) => ({
      ...current,
      [`image-${index}`]: undefined,
    }));
  };

  const validate = () => {
    const nextErrors: MarketplaceFormErrors = {};
    const price = Number(form.price);

    if (form.title.trim().length < 5) {
      nextErrors.title = "Title must contain at least 5 characters.";
    }
    if (form.description.trim().length < 10) {
      nextErrors.description =
        "Description must contain at least 10 characters.";
    }
    if (!Number.isFinite(price) || price <= 0 || price > 10_000_000) {
      nextErrors.price = "Price must be between 0.01 and 10,000,000.";
    }
    if (!/^[A-Z]{3}$/.test(form.currency.trim().toUpperCase())) {
      nextErrors.currency = "Use a three-letter currency code such as PKR.";
    }
    if (form.images.length > 6) {
      nextErrors.images = "A listing can have at most six images.";
    }
    form.images.forEach((image, index) => {
      if (!httpUrlPattern.test(image.imageUrl.trim())) {
        nextErrors[`image-${index}`] =
          "Enter a complete HTTP or HTTPS image URL.";
      } else if ((image.altText?.trim().length ?? 0) > 160) {
        nextErrors[`image-${index}`] =
          "Image description cannot exceed 160 characters.";
      }
    });

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!validate()) return;

    const request: CreateMarketplaceListingRequest = {
      category: form.category,
      condition: form.condition,
      currency: form.currency.trim().toUpperCase(),
      description: form.description.trim(),
      images: form.images.map((image) => ({
        altText: image.altText?.trim() || null,
        imageUrl: image.imageUrl.trim(),
      })),
      price: Number(form.price),
      title: form.title.trim(),
    };

    if (props.mode === "create") {
      await props.onSubmit(request);
      return;
    }

    const updateRequest: UpdateMarketplaceListingRequest = {};
    if (request.title !== props.initialListing.title) {
      updateRequest.title = request.title;
    }
    if (request.description !== props.initialListing.description) {
      updateRequest.description = request.description;
    }
    if (request.category !== props.initialListing.category) {
      updateRequest.category = request.category;
    }
    if (request.price !== props.initialListing.price) {
      updateRequest.price = request.price;
    }
    if (request.currency !== props.initialListing.currency) {
      updateRequest.currency = request.currency;
    }
    if (request.condition !== props.initialListing.condition) {
      updateRequest.condition = request.condition;
    }
    const initialImages = props.initialListing.images.map((image) => ({
      altText: image.altText,
      imageUrl: image.imageUrl,
    }));
    if (!sameImages(request.images, initialImages)) {
      updateRequest.images = request.images;
    }
    if (form.status !== props.initialListing.status) {
      updateRequest.status = form.status;
    }
    await props.onSubmit(updateRequest);
  };

  return (
    <form className="grid gap-6" noValidate onSubmit={handleSubmit}>
      <Card>
        <CardContent className="grid gap-5 p-5 sm:p-6">
          <div>
            <h2 className="text-lg font-semibold text-slate-950">
              Listing information
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              Add clear details so students know exactly what is available.
            </p>
          </div>

          <FormField
            error={errors.title ?? backendError("title")}
            label="Listing title"
            maxLength={160}
            onChange={(event) => update("title", event.target.value)}
            placeholder="Data Structures textbook, 4th edition"
            required
            value={form.title}
          />

          <TextAreaField
            error={errors.description ?? backendError("description")}
            onChange={(value) => update("description", value)}
            value={form.description}
          />

          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              error={errors.category ?? backendError("category")}
              label="Category"
              onChange={(event) =>
                update(
                  "category",
                  event.target.value as MarketplaceCategory,
                )
              }
              options={marketplaceCategoryOptions}
              required
              value={form.category}
            />
            <SelectField
              error={errors.condition ?? backendError("condition")}
              label="Condition"
              onChange={(event) =>
                update(
                  "condition",
                  event.target.value as MarketplaceItemCondition,
                )
              }
              options={marketplaceConditionOptions}
              required
              value={form.condition}
            />
          </div>

          <div className="grid gap-5 sm:grid-cols-[1fr_10rem]">
            <FormField
              error={errors.price ?? backendError("price")}
              label="Price"
              max={10_000_000}
              min={0.01}
              onChange={(event) => update("price", event.target.value)}
              placeholder="2500"
              required
              step="0.01"
              type="number"
              value={form.price}
            />
            <FormField
              error={errors.currency ?? backendError("currency")}
              label="Currency"
              maxLength={3}
              onChange={(event) =>
                update("currency", event.target.value.toUpperCase())
              }
              placeholder="PKR"
              required
              value={form.currency}
            />
          </div>

          {props.mode === "edit" ? (
            <SelectField
              error={errors.status ?? backendError("status")}
              label="Listing status"
              onChange={(event) =>
                update(
                  "status",
                  event.target.value as MarketplaceListingUpdateStatus,
                )
              }
              options={[
                { label: "Active", value: "ACTIVE" },
                { label: "Sold", value: "SOLD" },
              ]}
              required
              value={form.status}
            />
          ) : null}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="grid gap-5 p-5 sm:p-6">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-950">
                Listing images
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                Add up to six public HTTP or HTTPS image URLs.
              </p>
            </div>
            <Button
              disabled={form.images.length >= 6}
              onClick={() =>
                update("images", [
                  ...form.images,
                  { altText: "", imageUrl: "" },
                ])
              }
              variant="outline"
            >
              <ImagePlus className="size-4" />
              Add image
            </Button>
          </div>

          {errors.images || backendError("images") ? (
            <p className="text-sm font-medium text-red-600">
              {errors.images ?? backendError("images")}
            </p>
          ) : null}

          {form.images.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50 p-6 text-center text-sm text-slate-500">
              Images are optional. Listings without images receive a clean
              placeholder.
            </div>
          ) : (
            <div className="grid gap-4">
              {form.images.map((image, index) => (
                <div
                  className="grid gap-4 rounded-xl border border-slate-200 p-4 md:grid-cols-[1fr_18rem_auto] md:items-start"
                  key={index}
                >
                  <FormField
                    error={
                      errors[`image-${index}`] ??
                      backendError(`images[${index}].imageUrl`)
                    }
                    label={`Image ${index + 1} URL`}
                    maxLength={2048}
                    onChange={(event) =>
                      updateImage(index, "imageUrl", event.target.value)
                    }
                    placeholder="https://example.com/item.jpg"
                    required
                    type="url"
                    value={image.imageUrl}
                  />
                  <FormField
                    error={backendError(`images[${index}].altText`)}
                    label="Image description"
                    maxLength={160}
                    onChange={(event) =>
                      updateImage(index, "altText", event.target.value)
                    }
                    placeholder="Front cover of the textbook"
                    value={image.altText ?? ""}
                  />
                  <Button
                    aria-label={`Remove image ${index + 1}`}
                    className="md:mt-7"
                    onClick={() =>
                      update(
                        "images",
                        form.images.filter(
                          (_, imageIndex) => imageIndex !== index,
                        ),
                      )
                    }
                    size="icon"
                    variant="ghost"
                  >
                    <Trash2 className="size-4 text-red-600" />
                  </Button>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <div className="flex justify-end">
        <Button
          className="w-full sm:w-auto"
          loading={props.isSubmitting}
          size="lg"
          type="submit"
        >
          <Save className="size-4" />
          {props.isSubmitting ? "Saving listing" : props.submitLabel}
        </Button>
      </div>
    </form>
  );
}
