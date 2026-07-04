import {
  ArrowDownToLine,
  BookOpen,
  CalendarDays,
  Edit3,
  Eye,
  Star,
  UserRound,
} from "lucide-react";
import { Link } from "react-router-dom";

import { Badge, Card, CardContent } from "@/components/common";
import {
  formatNoteDate,
  moderationLabel,
  visibilityLabel,
} from "@/components/notes/noteFormatting";
import { paths } from "@/routes/paths";
import type { NoteSummary } from "@/types/notes";

const statusVariants = {
  APPROVED: "success",
  HIDDEN: "danger",
  PENDING: "warning",
  REJECTED: "danger",
} as const;

export function NoteCard({
  note,
  owned = false,
}: {
  note: NoteSummary;
  owned?: boolean;
}) {
  return (
    <Card className="group h-full overflow-hidden hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl">
      <CardContent className="flex h-full flex-col p-5">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="brand">{note.fileType}</Badge>
          <Badge>{visibilityLabel(note.visibility)}</Badge>
          {owned ? (
            <Badge variant={statusVariants[note.moderationStatus]}>
              {moderationLabel(note.moderationStatus)}
            </Badge>
          ) : null}
        </div>

        <h2 className="mt-4 line-clamp-2 text-lg font-semibold leading-7 text-slate-950">
          {note.title}
        </h2>

        <div className="mt-4 grid gap-2.5 text-sm text-slate-500">
          <p className="flex items-center gap-2">
            <BookOpen className="size-4 shrink-0 text-brand-500" />
            <span className="truncate">
              {note.course.courseCode} · {note.course.title}
            </span>
          </p>
          <p className="flex items-center gap-2">
            <UserRound className="size-4 shrink-0 text-slate-400" />
            <span className="truncate">
              {note.uploader.fullName} · {note.uploader.university}
            </span>
          </p>
          <p className="flex items-center gap-2">
            <CalendarDays className="size-4 shrink-0 text-slate-400" />
            <span>
              Semester {note.semester} · {formatNoteDate(note.createdAt)}
            </span>
          </p>
        </div>

        {note.tags.length > 0 ? (
          <div className="mt-4 flex flex-wrap gap-1.5">
            {note.tags.slice(0, 4).map((tag) => (
              <span
                className="rounded-lg bg-slate-100 px-2 py-1 text-[11px] font-medium text-slate-600"
                key={tag.id}
              >
                #{tag.name}
              </span>
            ))}
          </div>
        ) : null}

        <div className="mt-auto flex items-center gap-4 border-t border-slate-100 pt-5 text-xs font-semibold text-slate-500">
          <span className="flex items-center gap-1.5">
            <Star className="size-3.5 fill-amber-400 text-amber-400" />
            {note.averageRating.toFixed(1)} ({note.ratingCount})
          </span>
          <span className="flex items-center gap-1.5">
            <ArrowDownToLine className="size-3.5" />
            {note.downloadCount.toLocaleString()}
          </span>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-2">
          <Link
            className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:bg-slate-50"
            to={paths.noteDetail(note.id)}
          >
            <Eye className="size-4" />
            View note
          </Link>
          {owned ? (
            <Link
              className="inline-flex h-10 items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white transition hover:bg-brand-700"
              to={paths.noteEdit(note.id)}
            >
              <Edit3 className="size-4" />
              Edit
            </Link>
          ) : (
            <span className="inline-flex h-10 items-center justify-center rounded-xl bg-slate-50 px-3 text-xs font-medium text-slate-500">
              Taught by {note.teacherName}
            </span>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
