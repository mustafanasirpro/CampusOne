import { Bookmark, Download, FileText, Star } from "lucide-react";

import { Badge, Button, Card, CardContent } from "@/components/common";
import type { NoteSummary } from "@/types/content";
import { cn } from "@/utils/cn";

export interface NoteCardProps {
  note: NoteSummary;
  onBookmark?: () => void;
  onDownload?: () => void;
}

export function NoteCard({
  note,
  onBookmark,
  onDownload,
}: NoteCardProps) {
  return (
    <Card>
      <CardContent>
        <div className="flex items-start gap-3">
          <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600">
            <FileText className="size-5" />
          </span>
          <div className="min-w-0 flex-1">
            <h3 className="truncate font-semibold text-slate-950">
              {note.title}
            </h3>
            <p className="mt-1 text-sm text-slate-500">{note.uploader}</p>
          </div>
          <Button
            aria-label={note.bookmarked ? "Remove bookmark" : "Bookmark note"}
            onClick={onBookmark}
            size="icon"
            variant="ghost"
          >
            <Bookmark
              className={cn(
                "size-5",
                note.bookmarked && "fill-brand-600 text-brand-600",
              )}
            />
          </Button>
        </div>
        <Badge className="mt-4" variant="brand">
          {note.course}
        </Badge>
        <div className="mt-4 flex items-center gap-4 text-xs text-slate-500">
          <span className="flex items-center gap-1">
            <Star className="size-3.5 fill-amber-400 text-amber-400" />
            {note.rating}
          </span>
          <span>{note.downloads.toLocaleString()} downloads</span>
        </div>
        <Button className="mt-4 w-full" onClick={onDownload} variant="outline">
          <Download className="size-4" />
          Download
        </Button>
      </CardContent>
    </Card>
  );
}

