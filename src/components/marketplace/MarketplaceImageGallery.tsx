import { ImageOff, Images } from "lucide-react";
import { useState } from "react";

import type { MarketplaceImage } from "@/types/marketplace";
import { cn } from "@/utils/cn";

function ListingImage({
  alt,
  className,
  src,
}: {
  alt: string;
  className?: string;
  src: string;
}) {
  const [failedSrc, setFailedSrc] = useState<string | null>(null);
  const failed = failedSrc === src;

  if (failed) {
    return (
      <div
        className={cn(
          "grid place-items-center bg-slate-100 text-slate-400",
          className,
        )}
        role="img"
        aria-label={`${alt} could not be loaded`}
      >
        <ImageOff className="size-8" />
      </div>
    );
  }

  return (
    <img
      alt={alt}
      className={cn("object-cover", className)}
      loading="lazy"
      onError={() => setFailedSrc(src)}
      src={src}
    />
  );
}

export function MarketplaceImagePlaceholder({
  className,
}: {
  className?: string;
}) {
  return (
    <div
      aria-label="No listing image"
      className={cn(
        "grid place-items-center bg-gradient-to-br from-slate-100 to-slate-200 text-slate-400",
        className,
      )}
      role="img"
    >
      <Images className="size-10" />
    </div>
  );
}

export function MarketplaceImageGallery({
  images,
  title,
}: {
  images: MarketplaceImage[];
  title: string;
}) {
  const sortedImages = [...images].sort(
    (first, second) => first.displayOrder - second.displayOrder,
  );
  const [selectedId, setSelectedId] = useState<string | null>(
    sortedImages[0]?.id ?? null,
  );
  const selected =
    sortedImages.find((image) => image.id === selectedId) ?? sortedImages[0];

  if (!selected) {
    return <MarketplaceImagePlaceholder className="aspect-[4/3] rounded-2xl" />;
  }

  return (
    <div className="grid gap-3">
      <ListingImage
        alt={selected.altText || title}
        className="aspect-[4/3] w-full rounded-2xl"
        src={selected.imageUrl}
      />
      {sortedImages.length > 1 ? (
        <div
          aria-label="Listing image gallery"
          className="grid grid-cols-4 gap-2 sm:grid-cols-6"
        >
          {sortedImages.map((image) => (
            <button
              aria-label={`View ${image.altText || title}`}
              className={cn(
                "overflow-hidden rounded-xl border-2 transition",
                image.id === selected.id
                  ? "border-brand-500"
                  : "border-transparent hover:border-slate-300",
              )}
              key={image.id}
              onClick={() => setSelectedId(image.id)}
              type="button"
            >
              <ListingImage
                alt=""
                className="aspect-square w-full"
                src={image.imageUrl}
              />
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}

export { ListingImage };
