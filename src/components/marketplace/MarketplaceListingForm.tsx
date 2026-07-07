import {
  ImagePlus,
  Save,
  Trash2,
  UploadCloud,
} from "lucide-react";
import {
  useEffect,
  useId,
  useRef,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react";

import { Button, Card, CardContent } from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import {
  marketplaceCategoryOptions,
  marketplaceConditionOptions,
} from "@/components/marketplace/marketplaceFormatting";
import type {
  CreateMarketplaceListingRequest,
  MarketplaceCategory,
  MarketplaceItemCondition,
  MarketplaceListingDetail,
  MarketplaceListingUpdateStatus,
  UpdateMarketplaceListingRequest,
} from "@/types/marketplace";
import { cn } from "@/utils/cn";
import { formatFileSize } from "@/components/notes";

interface MarketplaceFormState {
  category: MarketplaceCategory;
  condition: MarketplaceItemCondition;
  currency: string;
  description: string;
  price: string;
  status: MarketplaceListingUpdateStatus;
  title: string;
}

interface SelectedImage {
  file: File;
  id: string;
  previewUrl: string;
}

type MarketplaceFormErrors = Partial<
  Record<keyof MarketplaceFormState | "images", string>
>;

interface CommonMarketplaceFormProps {
  backendFieldErrors?: Record<string, string[]>;
  isSubmitting: boolean;
  submitLabel: string;
}

type MarketplaceListingFormProps =
  | (CommonMarketplaceFormProps & {
      mode: "create";
      onSubmit: (
        request: CreateMarketplaceListingRequest,
        imageFiles: File[],
      ) => Promise<void>;
    })
  | (CommonMarketplaceFormProps & {
      initialListing: MarketplaceListingDetail;
      mode: "edit";
      onSubmit: (
        request: UpdateMarketplaceListingRequest,
        imageFiles?: File[],
      ) => Promise<void>;
    });

const MAX_IMAGES = 5;
const MAX_IMAGE_SIZE_MB = 5;
const MAX_IMAGE_SIZE_BYTES = MAX_IMAGE_SIZE_MB * 1024 * 1024;
const allowedImageTypes = ["image/jpeg", "image/png", "image/webp"];
const allowedImageExtensions = [".jpg", ".jpeg", ".png", ".webp"];

function initialState(
  initialListing?: MarketplaceListingDetail,
): MarketplaceFormState {
  return {
    category: initialListing?.category ?? "BOOKS",
    condition: initialListing?.condition ?? "USED",
    currency: initialListing?.currency ?? "PKR",
    description: initialListing?.description ?? "",
    price: initialListing ? String(initialListing.price) : "",
    status: initialListing?.status === "SOLD" ? "SOLD" : "ACTIVE",
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

export function MarketplaceListingForm(
  props: MarketplaceListingFormProps,
) {
  const imageInputId = useId();
  const imageInputRef = useRef<HTMLInputElement>(null);
  const initialListing =
    props.mode === "edit" ? props.initialListing : undefined;
  const [form, setForm] = useState<MarketplaceFormState>(() =>
    initialState(initialListing),
  );
  const [selectedImages, setSelectedImages] = useState<SelectedImage[]>([]);
  const selectedImagesRef = useRef<SelectedImage[]>([]);
  const [errors, setErrors] = useState<MarketplaceFormErrors>({});

  useEffect(() => {
    selectedImagesRef.current = selectedImages;
  }, [selectedImages]);

  useEffect(() => () => {
    selectedImagesRef.current.forEach((image) =>
      URL.revokeObjectURL(image.previewUrl),
    );
  }, []);

  const update = <Key extends keyof MarketplaceFormState>(
    key: Key,
    value: MarketplaceFormState[Key],
  ) => {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: undefined }));
  };

  const backendError = (key: string) =>
    props.backendFieldErrors?.[key]?.[0] ??
    props.backendFieldErrors?.[`listing.${key}`]?.[0];

  const validateImage = (file: File) => {
    const lowerName = file.name.toLowerCase();
    if (file.size === 0) {
      return "Select a non-empty image file.";
    }
    if (
      !allowedImageTypes.includes(file.type) ||
      !allowedImageExtensions.some((extension) =>
        lowerName.endsWith(extension),
      )
    ) {
      return "Only JPG, PNG, or WebP images are allowed.";
    }
    if (file.size > MAX_IMAGE_SIZE_BYTES) {
      return `Each image must be ${MAX_IMAGE_SIZE_MB} MB or less.`;
    }
    return null;
  };

  const selectImages = (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []);
    setErrors((current) => ({ ...current, images: undefined }));
    if (files.length === 0) return;

    if (selectedImages.length + files.length > MAX_IMAGES) {
      setErrors((current) => ({
        ...current,
        images: `You can upload up to ${MAX_IMAGES} images.`,
      }));
      event.target.value = "";
      return;
    }

    const invalidMessage = files
      .map(validateImage)
      .find((message) => Boolean(message));
    if (invalidMessage) {
      setErrors((current) => ({ ...current, images: invalidMessage }));
      event.target.value = "";
      return;
    }

    setSelectedImages((current) => [
      ...current,
      ...files.map((file) => ({
        file,
        id: `${file.name}-${file.lastModified}-${crypto.randomUUID()}`,
        previewUrl: URL.createObjectURL(file),
      })),
    ]);
    event.target.value = "";
  };

  const removeSelectedImage = (imageId: string) => {
    setSelectedImages((current) => {
      const removed = current.find((image) => image.id === imageId);
      if (removed) URL.revokeObjectURL(removed.previewUrl);
      return current.filter((image) => image.id !== imageId);
    });
    if (imageInputRef.current) {
      imageInputRef.current.value = "";
    }
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
    if (selectedImages.length > MAX_IMAGES) {
      nextErrors.images = `You can upload up to ${MAX_IMAGES} images.`;
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (props.isSubmitting) return;
    if (!validate()) return;

    const request: CreateMarketplaceListingRequest = {
      category: form.category,
      condition: form.condition,
      currency: form.currency.trim().toUpperCase(),
      description: form.description.trim(),
      price: Number(form.price),
      title: form.title.trim(),
    };
    const imageFiles = selectedImages.map((image) => image.file);

    if (props.mode === "create") {
      await props.onSubmit(request, imageFiles);
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
    if (form.status !== props.initialListing.status) {
      updateRequest.status = form.status;
    }
    await props.onSubmit(
      updateRequest,
      imageFiles.length > 0 ? imageFiles : undefined,
    );
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
                Upload up to {MAX_IMAGES} JPG, PNG, or WebP images. Each image
                must be {MAX_IMAGE_SIZE_MB} MB or less.
              </p>
            </div>
          </div>

          <label
            className={cn(
              "grid cursor-pointer place-items-center gap-3 rounded-2xl border-2 border-dashed px-5 py-8 text-center transition",
              errors.images || backendError("images")
                ? "border-red-300 bg-red-50 hover:border-red-400"
                : "border-slate-300 bg-slate-50 hover:border-brand-400 hover:bg-brand-50/40",
              props.isSubmitting ? "cursor-not-allowed opacity-70" : "",
            )}
            htmlFor={imageInputId}
          >
            <UploadCloud className="size-8 text-brand-600" />
            <span>
              <span className="block text-sm font-semibold text-slate-800">
                Select images
              </span>
              <span className="mt-1 block text-xs text-slate-500">
                JPG, PNG, or WebP · up to {MAX_IMAGE_SIZE_MB} MB each
              </span>
            </span>
            <input
              accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
              className="sr-only"
              disabled={
                props.isSubmitting || selectedImages.length >= MAX_IMAGES
              }
              id={imageInputId}
              multiple
              onChange={selectImages}
              ref={imageInputRef}
              type="file"
            />
          </label>

          {errors.images || backendError("images") ? (
            <p className="text-sm font-medium text-red-600" role="alert">
              {errors.images ?? backendError("images")}
            </p>
          ) : null}

          {props.mode === "edit" &&
          selectedImages.length === 0 &&
          props.initialListing.images.length > 0 ? (
            <div className="grid gap-3">
              <p className="text-sm font-semibold text-slate-700">
                Current images
              </p>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {props.initialListing.images.map((image) => (
                  <div
                    className="overflow-hidden rounded-2xl border border-slate-200 bg-white"
                    key={image.id}
                  >
                    <img
                      alt={image.altText ?? props.initialListing.title}
                      className="aspect-[4/3] w-full object-cover"
                      src={image.imageUrl}
                    />
                    <p className="truncate p-3 text-xs text-slate-500">
                      Upload new images to replace the current gallery.
                    </p>
                  </div>
                ))}
              </div>
            </div>
          ) : null}

          {selectedImages.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50 p-6 text-center text-sm text-slate-500">
              Images are optional. Listings without images receive a clean
              placeholder.
            </div>
          ) : (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {selectedImages.map((image, index) => (
                <div
                  className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
                  key={image.id}
                >
                  <img
                    alt={`Selected listing image ${index + 1}`}
                    className="aspect-[4/3] w-full object-cover"
                    src={image.previewUrl}
                  />
                  <div className="grid gap-3 p-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-slate-800">
                        {image.file.name}
                      </p>
                      <p className="mt-0.5 text-xs text-slate-500">
                        {formatFileSize(image.file.size)}
                      </p>
                    </div>
                    <Button
                      disabled={props.isSubmitting}
                      onClick={() => removeSelectedImage(image.id)}
                      size="sm"
                      variant="outline"
                    >
                      <Trash2 className="size-4" />
                      Remove image
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {selectedImages.length < MAX_IMAGES ? (
            <Button
              className="w-fit"
              disabled={props.isSubmitting}
              onClick={() => imageInputRef.current?.click()}
              variant="outline"
            >
              <ImagePlus className="size-4" />
              Add more images
            </Button>
          ) : null}
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
