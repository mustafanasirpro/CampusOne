import { CalendarDays, MapPin, UserRound } from "lucide-react";
import { Link } from "react-router-dom";

import { Badge, Card, CardContent } from "@/components/common";
import {
  formatLostFoundDate,
  lostFoundCategoryLabel,
  lostFoundStatusLabel,
  lostFoundTypeLabel,
} from "@/components/lost-found/lostFoundFormatting";
import { paths } from "@/routes/paths";
import type { LostFoundItemSummary } from "@/types/lostFound";

export function LostFoundItemCard({
  item,
  showStatus = false,
}: {
  item: LostFoundItemSummary;
  showStatus?: boolean;
}) {
  return (
    <Card className="overflow-hidden hover:border-brand-200 hover:shadow-lg">
      <div className="aspect-[16/10] bg-slate-100">
        {item.primaryImage ? (
          <img
            alt=""
            className="h-full w-full object-cover"
            src={item.primaryImage.imageUrl}
          />
        ) : (
          <div className="grid h-full place-items-center text-sm font-semibold text-slate-400">
            No photo
          </div>
        )}
      </div>
      <CardContent className="grid gap-4 p-5">
        <div className="flex flex-wrap gap-2">
          <Badge variant={item.type === "LOST" ? "danger" : "success"}>
            {lostFoundTypeLabel(item.type)}
          </Badge>
          <Badge>{lostFoundCategoryLabel(item.category)}</Badge>
          {showStatus ? <Badge>{lostFoundStatusLabel(item.status)}</Badge> : null}
        </div>

        <div>
          <h3 className="line-clamp-2 text-lg font-bold text-slate-950">
            {item.title}
          </h3>
          <p className="mt-2 line-clamp-3 text-sm leading-6 text-slate-500">
            {item.description}
          </p>
        </div>

        <div className="grid gap-2 text-sm text-slate-500">
          <span className="flex items-center gap-2">
            <MapPin className="size-4 text-slate-400" />
            {item.locationText}
          </span>
          <span className="flex items-center gap-2">
            <CalendarDays className="size-4 text-slate-400" />
            {formatLostFoundDate(item.itemDate)}
          </span>
          {item.reporter.fullName ? (
            <span className="flex items-center gap-2">
              <UserRound className="size-4 text-slate-400" />
              {item.reporter.fullName}
            </span>
          ) : null}
        </div>

        <Link
          className="inline-flex h-10 items-center justify-center rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-700"
          to={paths.lostFoundDetail(item.id)}
        >
          View details
        </Link>
      </CardContent>
    </Card>
  );
}
