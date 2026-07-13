import { ImagePlus, Save, Trash2, UploadCloud } from "lucide-react";
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
  lostFoundCategoryOptions,
  lostFoundTypeOptions,
} from "@/components/lost-found/lostFoundFormatting";
import { formatFileSize } from "@/components/notes";
import type {
  CreateLostFoundItemRequest,
  LostFoundCategory,
  LostFoundItemDetail,
  LostFoundItemType,
  UpdateLostFoundItemRequest,
} from "@/types/lostFound";
import { cn } from "@/utils/cn";

interface LostFoundFormState {
  brand: string;
  category: LostFoundCategory;
  color: string;
  description: string;
  itemDate: string;
  locationText: string;
  title: string;
  type: LostFoundItemType;
}

interface SelectedImage {
  file: File;
  id: string;
  previewUrl: string;
}

type LostFoundFormErrors = Partial<
  Record<keyof LostFoundFormState | "images", string>
>;

interface CommonProps {
  backendFieldErrors?: Record<string, string[]>;
  isSubmitting: boolean;
  submitLabel: string;
}

type LostFoundItemFormProps =
  | (CommonProps & {
      mode: "create";
      onSubmit: (
        request: CreateLostFoundItemRequest,
        images: File[],
      ) => Promise<void>;
    })
  | (CommonProps & {
      initialItem: LostFoundItemDetail;
      mode: "edit";
      onSubmit: (
        request: UpdateLostFoundItemRequest,
        images?: File[],
      ) => Promise<void>;
    });

const MAX_IMAGES = 3;
const MAX_IMAGE_SIZE_MB = 5;
const MAX_IMAGE_SIZE_BYTES = MAX_IMAGE_SIZE_MB * 1024 * 1024;
const allowedImageTypes = ["image/jpeg", "image/png", "image/webp"];
const allowedImageExtensions = [".jpg", ".jpeg", ".png", ".webp"];

function initialState(initialItem?: LostFoundItemDetail): LostFoundFormState {
  return {
    brand: initialItem?.brand ?? "",
    category: initialItem?.category ?? "OTHER",
    color: initialItem?.color ?? "",
    description: initialItem?.description ?? "",
    itemDate: initialItem?.itemDate ?? new Date().toISOString().slice(0, 10),
    locationText: initialItem?.locationText ?? "",
    title: initialItem?.title ?? "",
    type: initialItem?.type ?? "LOST",
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
          "min-h-36 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:ring-4",
          error
            ? "border-red-300 focus:border-red-400 focus:ring-red-100"
            : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
        )}
        maxLength={2000}
        onChange={(event) => onChange(event.target.value)}
        placeholder="Describe distinctive details, marks, case color, stickers, or anything that helps identify the item."
        required
        value={value}
      />
      <span className="flex justify-between gap-3 text-xs">
        <span className={error ? "font-medium text-red-600" : "text-slate-500"}>
          {error ?? "Avoid sharing private contact details in the description."}
        </span>
        <span className="shrink-0 text-slate-400">
          {value.length}/2000
        </span>
      </span>
    </label>
  );
}

export function LostFoundItemForm(props: LostFoundItemFormProps) {
  const imageInputId = useId();
  const imageInputRef = useRef<HTMLInputElement>(null);
  const initialItem = props.mode === "edit" ? props.initialItem : undefined;
  const [form, setForm] = useState<LostFoundFormState>(() =>
    initialState(initialItem),
  );
  const [selectedImages, setSelectedImages] = useState<SelectedImage[]>([]);
  const selectedImagesRef = useRef<SelectedImage[]>([]);
  const [errors, setErrors] = useState<LostFoundFormErrors>({});

  useEffect(() => {
    selectedImagesRef.current = selectedImages;
  }, [selectedImages]);

  useEffect(
    () => () => {
      selectedImagesRef.current.forEach((image) =>
        URL.revokeObjectURL(image.previewUrl),
      );
    },
    [],
  );

  const backendError = (key: string) =>
    props.backendFieldErrors?.[key]?.[0] ??
    props.backendFieldErrors?.[`item.${key}`]?.[0];

  const update = <Key extends keyof LostFoundFormState>(
    key: Key,
    value: LostFoundFormState[Key],
  ) => {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: undefined }));
  };

  const validateImage = (file: File) => {
    const lowerName = file.name.toLowerCase();
    if (file.size === 0) return "Select a non-empty image file.";
    if (
      !allowedImageTypes.includes(file.type) ||
      !allowedImageExtensions.some((extension) => lowerName.endsWith(extension))
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
        images: `You can upload up to ${MAX_IMAGES} photos.`,
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
  };

  const validate = () => {
    const nextErrors: LostFoundFormErrors = {};
    if (form.title.trim().length < 5) {
      nextErrors.title = "Title must contain at least 5 characters.";
    }
    if (form.description.trim().length < 10) {
      nextErrors.description =
        "Description must contain at least 10 characters.";
    }
    if (form.locationText.trim().length < 2) {
      nextErrors.locationText = "Location must contain at least 2 characters.";
    }
    if (!form.itemDate) {
      nextErrors.itemDate = "Select the date the item was lost or found.";
    }
    if (selectedImages.length > MAX_IMAGES) {
      nextErrors.images = `You can upload up to ${MAX_IMAGES} photos.`;
    }
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (props.isSubmitting) return;
    if (!validate()) return;

    const request: CreateLostFoundItemRequest = {
      brand: form.brand.trim() || undefined,
      category: form.category,
      color: form.color.trim() || undefined,
      description: form.description.trim(),
      itemDate: form.itemDate,
      locationText: form.locationText.trim(),
      title: form.title.trim(),
      type: form.type,
    };
    const imageFiles = selectedImages.map((image) => image.file);

    if (props.mode === "create") {
      await props.onSubmit(request, imageFiles);
      return;
    }

    const updateRequest: UpdateLostFoundItemRequest = {};
    if (request.type !== props.initialItem.type) updateRequest.type = request.type;
    if (request.category !== props.initialItem.category) {
      updateRequest.category = request.category;
    }
    if (request.title !== props.initialItem.title) updateRequest.title = request.title;
    if (request.description !== props.initialItem.description) {
      updateRequest.description = request.description;
    }
    if (request.locationText !== props.initialItem.locationText) {
      updateRequest.locationText = request.locationText;
    }
    if (request.itemDate !== props.initialItem.itemDate) {
      updateRequest.itemDate = request.itemDate;
    }
    if ((request.brand ?? "") !== (props.initialItem.brand ?? "")) {
      updateRequest.brand = request.brand;
    }
    if ((request.color ?? "") !== (props.initialItem.color ?? "")) {
      updateRequest.color = request.color;
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
              Item details
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              Submissions are reviewed before they appear to students at your university.
            </p>
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              disabled={props.mode === "edit"}
              error={errors.type ?? backendError("type")}
              label="Type"
              onChange={(event) =>
                update("type", event.target.value as LostFoundItemType)
              }
              options={lostFoundTypeOptions.filter((option) => option.value)}
              required
              value={form.type}
            />
            <SelectField
              error={errors.category ?? backendError("category")}
              label="Category"
              onChange={(event) =>
                update("category", event.target.value as LostFoundCategory)
              }
              options={lostFoundCategoryOptions.filter((option) => option.value)}
              required
              value={form.category}
            />
          </div>

          <FormField
            error={errors.title ?? backendError("title")}
            label="Title"
            maxLength={160}
            onChange={(event) => update("title", event.target.value)}
            placeholder="Black backpack near library"
            required
            value={form.title}
          />

          <TextAreaField
            error={errors.description ?? backendError("description")}
            onChange={(value) => update("description", value)}
            value={form.description}
          />

          <div className="grid gap-5 sm:grid-cols-2">
            <FormField
              error={errors.locationText ?? backendError("locationText")}
              label="Location"
              maxLength={255}
              onChange={(event) =>
                update("locationText", event.target.value)
              }
              placeholder="Library entrance, cafeteria, lab block..."
              required
              value={form.locationText}
            />
            <FormField
              error={errors.itemDate ?? backendError("itemDate")}
              label="Date"
              max={new Date().toISOString().slice(0, 10)}
              onChange={(event) => update("itemDate", event.target.value)}
              required
              type="date"
              value={form.itemDate}
            />
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <FormField
              error={errors.brand ?? backendError("brand")}
              label="Brand"
              maxLength={80}
              onChange={(event) => update("brand", event.target.value)}
              placeholder="Dell, Casio, Nike..."
              value={form.brand}
            />
            <FormField
              error={errors.color ?? backendError("color")}
              label="Color"
              maxLength={60}
              onChange={(event) => update("color", event.target.value)}
              placeholder="Black, blue, silver..."
              value={form.color}
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="grid gap-4 p-5 sm:p-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-950">
                Photos
              </h2>
              <p className="mt-1 text-sm text-slate-500">
                JPG, PNG, or WebP. Up to {MAX_IMAGES} photos, {MAX_IMAGE_SIZE_MB} MB each.
              </p>
            </div>
            <Button
              onClick={() => imageInputRef.current?.click()}
              type="button"
              variant="outline"
            >
              <ImagePlus className="size-4" />
              Select photos
            </Button>
          </div>
          <input
            accept="image/jpeg,image/png,image/webp"
            className="sr-only"
            id={imageInputId}
            multiple
            onChange={selectImages}
            ref={imageInputRef}
            type="file"
          />
          {errors.images ?? backendError("images") ? (
            <p className="text-sm font-medium text-red-600">
              {errors.images ?? backendError("images")}
            </p>
          ) : null}

          {selectedImages.length > 0 ? (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {selectedImages.map((image) => (
                <div
                  className="overflow-hidden rounded-2xl border border-slate-200 bg-white"
                  key={image.id}
                >
                  <img
                    alt=""
                    className="aspect-video w-full object-cover"
                    src={image.previewUrl}
                  />
                  <div className="flex items-center justify-between gap-3 p-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-slate-800">
                        {image.file.name}
                      </p>
                      <p className="text-xs text-slate-500">
                        {formatFileSize(image.file.size)}
                      </p>
                    </div>
                    <Button
                      aria-label={`Remove ${image.file.name}`}
                      onClick={() => removeSelectedImage(image.id)}
                      type="button"
                      variant="ghost"
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="grid min-h-40 place-items-center rounded-2xl border border-dashed border-slate-300 bg-slate-50 text-center">
              <div>
                <UploadCloud className="mx-auto size-8 text-slate-400" />
                <p className="mt-2 text-sm font-medium text-slate-600">
                  Photos are optional, but they help students recognize the item.
                </p>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="flex justify-end">
        <Button loading={props.isSubmitting} type="submit">
          <Save className="size-4" />
          {props.submitLabel}
        </Button>
      </div>
    </form>
  );
}
