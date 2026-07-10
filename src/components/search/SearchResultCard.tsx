import { ArrowUpRight, CalendarDays, UserRound } from "lucide-react";
import { Link } from "react-router-dom";

import { Badge, Card, CardContent } from "@/components/common";
import {
  formatSearchDate,
  searchTypeLabel,
} from "@/components/search/searchFormatting";
import type { GlobalSearchResult } from "@/types/search";

function displayMetadata(metadata: Record<string, unknown>) {
  return Object.entries(metadata)
    .filter(([, value]) =>
      ["boolean", "number", "string"].includes(typeof value),
    )
    .slice(0, 3);
}

function escapeRegex(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function highlightedText(text: string, query?: string) {
  const terms = Array.from(
    new Set(
      (query ?? "")
        .split(/[^\p{L}\p{N}]+/u)
        .map((term) => term.trim())
        .filter((term) => term.length >= 2),
    ),
  ).sort((left, right) => right.length - left.length);

  if (terms.length === 0) return text;

  const matcher = new RegExp(`(${terms.map(escapeRegex).join("|")})`, "giu");
  return text.split(matcher).map((part, index) =>
    terms.some((term) => term.toLocaleLowerCase() === part.toLocaleLowerCase())
      ? (
          <mark
            className="rounded bg-amber-100 px-0.5 text-inherit"
            key={`${part}-${index}`}
          >
            {part}
          </mark>
        )
      : part,
  );
}

export function SearchResultCard({
  query,
  result,
}: {
  query?: string;
  result: GlobalSearchResult;
}) {
  const isExternal = /^https?:\/\//i.test(result.targetUrl);
  const linkClasses =
    "inline-flex h-9 items-center gap-1.5 rounded-lg bg-brand-600 px-3 text-xs font-semibold text-white hover:bg-brand-700";

  return (
    <Card className="hover:border-brand-200 hover:shadow-lg">
      <CardContent className="grid gap-4 p-5 sm:p-6">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="brand">{searchTypeLabel(result.type)}</Badge>
          <span className="ml-auto flex items-center gap-1.5 text-xs text-slate-400">
            <CalendarDays className="size-3.5" />
            {formatSearchDate(result.createdAt)}
          </span>
        </div>
        <div>
          <h2 className="text-lg font-semibold text-slate-950">
            {highlightedText(result.title, query)}
          </h2>
          <p className="mt-2 text-sm leading-6 text-slate-500">
            {highlightedText(result.snippet, query)}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {displayMetadata(result.metadata).map(([key, value]) => (
            <Badge key={key}>
              {key.replace(/([A-Z])/g, " $1")}: {String(value)}
            </Badge>
          ))}
        </div>
        <div className="flex flex-wrap items-center gap-3 border-t border-slate-100 pt-4">
          {result.ownerOrAuthorName ? (
            <span className="flex items-center gap-1.5 text-xs text-slate-500">
              <UserRound className="size-3.5" />
              {result.ownerOrAuthorName}
            </span>
          ) : null}
          {isExternal ? (
            <a
              className={`${linkClasses} ml-auto`}
              href={result.targetUrl}
              rel="noopener noreferrer"
              target="_blank"
            >
              Open
              <ArrowUpRight className="size-3.5" />
            </a>
          ) : (
            <Link className={`${linkClasses} ml-auto`} to={result.targetUrl}>
              Open
              <ArrowUpRight className="size-3.5" />
            </Link>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
