import { MapPin, ShoppingBag } from "lucide-react";

import {
  Badge,
  Button,
  Card,
  CardContent,
} from "@/components/common";
import type { ProductSummary } from "@/types/content";

export interface ProductCardProps {
  onContact?: () => void;
  product: ProductSummary;
}

export function ProductCard({ onContact, product }: ProductCardProps) {
  return (
    <Card className="overflow-hidden">
      <div className="grid aspect-[4/3] place-items-center bg-slate-100">
        {product.imageUrl ? (
          <img
            alt={product.title}
            className="size-full object-cover"
            src={product.imageUrl}
          />
        ) : (
          <ShoppingBag className="size-10 text-slate-300" />
        )}
      </div>
      <CardContent>
        <div className="flex items-start justify-between gap-3">
          <h3 className="font-semibold text-slate-950">{product.title}</h3>
          <Badge>{product.condition}</Badge>
        </div>
        <p className="mt-2 text-lg font-bold text-brand-700">{product.price}</p>
        <p className="mt-2 flex items-center gap-1.5 text-sm text-slate-500">
          <MapPin className="size-4" />
          {product.location}
        </p>
        <p className="mt-1 text-xs text-slate-400">Seller: {product.seller}</p>
        <Button className="mt-4 w-full" onClick={onContact} variant="outline">
          Contact seller
        </Button>
      </CardContent>
    </Card>
  );
}

