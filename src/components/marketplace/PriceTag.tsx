import { formatMarketplacePrice } from "@/components/marketplace/marketplaceFormatting";

export function PriceTag({
  currency,
  price,
}: {
  currency: string;
  price: number;
}) {
  return (
    <p className="text-2xl font-bold tracking-tight text-brand-700">
      {formatMarketplacePrice(price, currency)}
    </p>
  );
}
