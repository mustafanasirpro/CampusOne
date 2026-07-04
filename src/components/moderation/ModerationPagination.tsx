import { ArrowLeft, ArrowRight } from "lucide-react";

import { Button } from "@/components/common";

export function ModerationPagination({
  first,
  last,
  onPageChange,
  page,
  totalPages,
}: {
  first: boolean;
  last: boolean;
  onPageChange: (page: number) => void;
  page: number;
  totalPages: number;
}) {
  return (
    <nav
      aria-label="Moderation results pagination"
      className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white p-3"
    >
      <Button
        disabled={first}
        onClick={() => onPageChange(Math.max(0, page - 1))}
        variant="outline"
      >
        <ArrowLeft className="size-4" />
        Previous
      </Button>
      <span className="text-sm font-semibold text-slate-700">
        {page + 1} / {Math.max(1, totalPages)}
      </span>
      <Button
        disabled={last}
        onClick={() => onPageChange(page + 1)}
        variant="outline"
      >
        Next
        <ArrowRight className="size-4" />
      </Button>
    </nav>
  );
}
