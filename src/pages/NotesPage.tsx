import {
  ArrowDownToLine,
  Bookmark,
  CalendarDays,
  Eye,
  FileText,
  GraduationCap,
  SearchX,
  SlidersHorizontal,
  Star,
  UploadCloud,
  UserRound,
} from "lucide-react";
import {
  useMemo,
  useState,
  type FormEvent,
} from "react";

import { NoteCard, StatCard } from "@/components/cards";
import {
  Badge,
  Button,
  Card,
  CardContent,
  Dropdown,
  EmptyState,
  FilterBar,
  Modal,
  PageHeader,
  SearchBar,
  SectionTitle,
  Tabs,
  useToast,
} from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import {
  noteUploadCourseOptions,
  noteUploadDepartmentOptions,
  noteUploadFileTypeOptions,
  noteUploadSemesterOptions,
  noteUploadUniversityOptions,
  notesCourseOptions,
  notesDepartmentOptions,
  notesFeaturedTabs,
  notesFileTypeOptions,
  notesLibrary,
  notesLibraryStats,
  notesSemesterOptions,
  notesSortOptions,
  notesTeacherOptions,
  notesUniversityOptions,
  type NoteFileType,
  type NotesLibraryItem,
} from "@/data/notes";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

type FeaturedTab = (typeof notesFeaturedTabs)[number]["value"];
type NotesSort = "newest" | "rating" | "downloads";

interface NotesFilters {
  course: string;
  department: string;
  fileType: string;
  semester: string;
  teacher: string;
  university: string;
}

interface UploadForm {
  course: string;
  department: string;
  description: string;
  fileType: string;
  semester: string;
  tags: string;
  teacher: string;
  title: string;
  university: string;
}

type UploadErrors = Partial<Record<keyof UploadForm | "file", string>>;

const initialFilters: NotesFilters = {
  university: "all",
  department: "all",
  semester: "all",
  course: "all",
  teacher: "all",
  fileType: "all",
};

const initialUploadForm: UploadForm = {
  title: "",
  university: "",
  department: "",
  semester: "",
  course: "",
  teacher: "",
  fileType: "",
  description: "",
  tags: "",
};

const fileTypeTones: Record<NoteFileType, string> = {
  PDF: "bg-red-50 text-red-700 ring-red-600/10",
  PPT: "bg-orange-50 text-orange-700 ring-orange-600/10",
  DOCX: "bg-blue-50 text-blue-700 ring-blue-600/10",
  Images: "bg-violet-50 text-violet-700 ring-violet-600/10",
};

interface LibraryNoteCardProps {
  isBookmarked: boolean;
  isRated: boolean;
  note: NotesLibraryItem;
  onBookmark: () => void;
  onDownload: () => void;
  onPreview: () => void;
  onRate: () => void;
}

function LibraryNoteCard({
  isBookmarked,
  isRated,
  note,
  onBookmark,
  onDownload,
  onPreview,
  onRate,
}: LibraryNoteCardProps) {
  return (
    <Card className="group h-full overflow-hidden transition duration-200 hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl">
      <CardContent className="flex h-full flex-col p-5">
        <div className="flex items-start gap-3">
          <span className="grid size-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-600 transition group-hover:bg-brand-600 group-hover:text-white">
            <FileText className="size-5" />
          </span>
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <Badge className={fileTypeTones[note.fileType]}>
                {note.fileType}
              </Badge>
              <Badge variant="brand">{note.course}</Badge>
            </div>
          </div>
          <Button
            aria-label={isBookmarked ? "Remove saved note" : "Save note"}
            onClick={onBookmark}
            size="sm"
            variant={isBookmarked ? "secondary" : "ghost"}
          >
            <Bookmark
              className={cn(
                "size-4",
                isBookmarked && "fill-brand-600 text-brand-600",
              )}
            />
            <span className="hidden sm:inline">
              {isBookmarked ? "Saved" : "Save"}
            </span>
          </Button>
        </div>

        <h3 className="mt-4 text-base font-semibold leading-6 text-slate-950">
          {note.title}
        </h3>
        <p className="mt-2 line-clamp-2 text-sm leading-6 text-slate-500">
          {note.description}
        </p>

        <div className="mt-4 grid gap-2.5 text-xs text-slate-500">
          <div className="flex items-center gap-2">
            <GraduationCap className="size-3.5 shrink-0 text-slate-400" />
            <span className="truncate">
              {note.teacher} · {note.university} · Semester {note.semester}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <UserRound className="size-3.5 shrink-0 text-slate-400" />
            <span className="truncate">Uploaded by {note.uploader}</span>
          </div>
          <div className="flex items-center gap-2">
            <CalendarDays className="size-3.5 shrink-0 text-slate-400" />
            <span>{note.uploadDate}</span>
          </div>
        </div>

        <div className="mt-5 flex items-center gap-4 border-t border-slate-100 pt-4 text-xs font-medium text-slate-500">
          <button
            aria-label={isRated ? "You rated this note five stars" : "Rate this note five stars"}
            className={cn(
              "flex items-center gap-1.5 rounded-md transition hover:text-amber-700",
              isRated && "font-semibold text-amber-700",
            )}
            onClick={onRate}
            type="button"
          >
            <Star
              className={cn(
                "size-3.5 text-amber-400",
                (note.rating > 0 || isRated) && "fill-amber-400",
              )}
            />
            {isRated ? "Rated 5" : note.rating > 0 ? note.rating : "Rate"}
          </button>
          <span className="flex items-center gap-1.5">
            <ArrowDownToLine className="size-3.5" />
            {note.downloads.toLocaleString()}
          </span>
          <span className="ml-auto">{note.tags.slice(0, 2).join(" · ")}</span>
        </div>

        <div className="mt-4 grid grid-cols-2 gap-2">
          <Button onClick={onPreview} variant="outline">
            <Eye className="size-4" />
            Preview
          </Button>
          <Button onClick={onDownload}>
            <ArrowDownToLine className="size-4" />
            Download
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

interface NotesFilterControlsProps {
  filters: NotesFilters;
  onChange: (field: keyof NotesFilters, value: string) => void;
  onClear: () => void;
  showClear: boolean;
}

function NotesFilterControls({
  filters,
  onChange,
  onClear,
  showClear,
}: NotesFilterControlsProps) {
  const filterFields = [
    {
      key: "university" as const,
      label: "University",
      options: notesUniversityOptions,
    },
    {
      key: "department" as const,
      label: "Department",
      options: notesDepartmentOptions,
    },
    {
      key: "semester" as const,
      label: "Semester",
      options: notesSemesterOptions,
    },
    {
      key: "course" as const,
      label: "Course",
      options: notesCourseOptions,
    },
    {
      key: "teacher" as const,
      label: "Teacher",
      options: notesTeacherOptions,
    },
    {
      key: "fileType" as const,
      label: "File type",
      options: notesFileTypeOptions,
    },
  ];

  return (
    <FilterBar onClear={onClear} showClear={showClear}>
      {filterFields.map((field) => (
        <div
          className="w-full min-w-40 flex-1 sm:w-auto"
          key={field.key}
        >
          <Dropdown
            aria-label={field.label}
            onChange={(event) => onChange(field.key, event.target.value)}
            options={field.options}
            value={filters[field.key]}
          />
        </div>
      ))}
    </FilterBar>
  );
}

export function NotesPage() {
  const [library, setLibrary] = useState(notesLibrary);
  const [searchValue, setSearchValue] = useState("");
  const [filters, setFilters] = useState<NotesFilters>(initialFilters);
  const [sortBy, setSortBy] = useState<NotesSort>("newest");
  const [featuredTab, setFeaturedTab] =
    useState<FeaturedTab>("top-rated");
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [bookmarkedIds, setBookmarkedIds] = useState<Set<string>>(
    new Set(["oop-final-uml", "database-sql-normalization"]),
  );
  const [ratedIds, setRatedIds] = useState<Set<string>>(new Set());
  const [previewNote, setPreviewNote] = useState<NotesLibraryItem | null>(
    null,
  );
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [uploadForm, setUploadForm] =
    useState<UploadForm>(initialUploadForm);
  const [uploadErrors, setUploadErrors] = useState<UploadErrors>({});
  const [selectedFile, setSelectedFile] = useState("");
  const { showToast } = useToast();

  useDocumentTitle("Notes Library · CampusOne");

  const activeFilterCount =
    Object.values(filters).filter((value) => value !== "all").length +
    (searchValue.trim() ? 1 : 0);

  const filteredNotes = useMemo(() => {
    const query = searchValue.trim().toLowerCase();
    const matches = library.filter((note) => {
      const searchableText = [
        note.title,
        note.course,
        note.teacher,
        note.uploader,
        note.description,
        note.university,
        ...note.tags,
      ]
        .join(" ")
        .toLowerCase();

      return (
        (!query || searchableText.includes(query)) &&
        (filters.university === "all" ||
          note.university === filters.university) &&
        (filters.department === "all" ||
          note.department === filters.department) &&
        (filters.semester === "all" ||
          note.semester === filters.semester) &&
        (filters.course === "all" || note.course === filters.course) &&
        (filters.teacher === "all" || note.teacher === filters.teacher) &&
        (filters.fileType === "all" ||
          note.fileType === filters.fileType)
      );
    });

    return [...matches].sort((first, second) => {
      if (sortBy === "rating") return second.rating - first.rating;
      if (sortBy === "downloads") {
        return second.downloads - first.downloads;
      }
      return second.uploadedAt.localeCompare(first.uploadedAt);
    });
  }, [filters, library, searchValue, sortBy]);

  const featuredNotes = useMemo(() => {
    const sorted = [...library].sort((first, second) => {
      if (featuredTab === "top-rated") {
        return second.rating - first.rating;
      }
      if (featuredTab === "most-downloaded") {
        return second.downloads - first.downloads;
      }
      return second.uploadedAt.localeCompare(first.uploadedAt);
    });

    return sorted.slice(0, 3);
  }, [featuredTab, library]);

  const clearFilters = () => {
    setFilters(initialFilters);
    setSearchValue("");
  };

  const updateFilter = (field: keyof NotesFilters, value: string) => {
    setFilters((current) => ({ ...current, [field]: value }));
  };

  const toggleBookmark = (note: NotesLibraryItem) => {
    const isBookmarked = bookmarkedIds.has(note.id);
    setBookmarkedIds((current) => {
      const next = new Set(current);
      if (next.has(note.id)) next.delete(note.id);
      else next.add(note.id);
      return next;
    });
    showToast({
      title: isBookmarked ? "Removed from bookmarks" : "Note saved",
      message: note.title,
      variant: isBookmarked ? "info" : "success",
    });
  };

  const downloadNote = (note: NotesLibraryItem) => {
    showToast({
      title: "Download started",
      message: `${note.title} is a demo file.`,
      variant: "success",
    });
  };

  const rateNote = (note: NotesLibraryItem) => {
    setRatedIds((current) => new Set([...current, note.id]));
    showToast({
      title: "Five-star rating saved",
      message: `Thanks for rating ${note.title}.`,
      variant: "success",
    });
  };

  const updateUploadField = (field: keyof UploadForm, value: string) => {
    setUploadForm((current) => ({ ...current, [field]: value }));
    setUploadErrors((current) => ({ ...current, [field]: undefined }));
  };

  const resetUploadForm = () => {
    setUploadForm(initialUploadForm);
    setUploadErrors({});
    setSelectedFile("");
  };

  const handleUploadSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors: UploadErrors = {};

    if (uploadForm.title.trim().length < 5) {
      nextErrors.title = "Enter a descriptive note title.";
    }
    if (!uploadForm.university) {
      nextErrors.university = "Select a university.";
    }
    if (!uploadForm.department) {
      nextErrors.department = "Select a department.";
    }
    if (!uploadForm.semester) {
      nextErrors.semester = "Select a semester.";
    }
    if (!uploadForm.course) nextErrors.course = "Select a course.";
    if (uploadForm.teacher.trim().length < 2) {
      nextErrors.teacher = "Enter the teacher’s name.";
    }
    if (!uploadForm.fileType) {
      nextErrors.fileType = "Select a file type.";
    }
    if (uploadForm.description.trim().length < 20) {
      nextErrors.description = "Add at least 20 characters.";
    }
    if (!selectedFile) nextErrors.file = "Choose a demo file.";

    if (Object.keys(nextErrors).length > 0) {
      setUploadErrors(nextErrors);
      return;
    }

    const newNote: NotesLibraryItem = {
      id: `uploaded-${Date.now()}`,
      title: uploadForm.title.trim(),
      university: uploadForm.university,
      department: uploadForm.department,
      semester: uploadForm.semester,
      course: uploadForm.course,
      teacher: uploadForm.teacher.trim(),
      fileType: uploadForm.fileType as NoteFileType,
      description: uploadForm.description.trim(),
      tags: uploadForm.tags
        .split(",")
        .map((tag) => tag.trim())
        .filter(Boolean),
      uploader: "Ali Khan",
      uploadedAt: new Date().toISOString().slice(0, 10),
      uploadDate: "Just now",
      rating: 0,
      downloads: 0,
    };

    setLibrary((current) => [newNote, ...current]);
    setIsUploadOpen(false);
    setFeaturedTab("recent");
    clearFilters();
    resetUploadForm();
    showToast({
      title: "Notes uploaded",
      message: "Your new resource now appears in the library demo.",
      variant: "success",
    });
  };

  const uploadSelectOptions = {
    university: [
      { label: "Select university", value: "", disabled: true },
      ...noteUploadUniversityOptions,
    ],
    department: [
      { label: "Select department", value: "", disabled: true },
      ...noteUploadDepartmentOptions,
    ],
    semester: [
      { label: "Select semester", value: "", disabled: true },
      ...noteUploadSemesterOptions,
    ],
    course: [
      { label: "Select course", value: "", disabled: true },
      ...noteUploadCourseOptions,
    ],
    fileType: [
      { label: "Select file type", value: "", disabled: true },
      ...noteUploadFileTypeOptions,
    ],
  };

  return (
    <div className="grid gap-8 pb-8">
      <PageHeader
        actions={
          <Button onClick={() => setIsUploadOpen(true)}>
            <UploadCloud className="size-4" />
            Upload notes
          </Button>
        }
        description="Discover trusted course resources shared by students across Pakistan’s leading universities."
        eyebrow="Study resources"
        title="Notes Library"
      />

      <SearchBar
        className="max-w-3xl"
        onSearch={setSearchValue}
        onValueChange={setSearchValue}
        placeholder="Search by course, teacher, topic, or uploader..."
        value={searchValue}
      />

      <section aria-labelledby="notes-stats">
        <h2 className="sr-only" id="notes-stats">
          Notes library statistics
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {notesLibraryStats.map((stat) => (
            <div
              className="transition duration-200 hover:-translate-y-1 [&>div]:h-full [&>div]:transition-shadow [&>div]:hover:shadow-xl"
              key={stat.label}
            >
              <StatCard
                description={stat.description}
                icon={stat.icon}
                label={stat.label}
                value={stat.value}
              />
            </div>
          ))}
        </div>
      </section>

      <section>
        <SectionTitle
          description="Standout resources students are using this week."
          title="Featured notes"
        />
        <Tabs
          activeTab={featuredTab}
          className="mt-4"
          onChange={setFeaturedTab}
          tabs={notesFeaturedTabs}
        />
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {featuredNotes.map((note) => (
            <div
              className="transition duration-200 hover:-translate-y-1 [&>div]:h-full [&>div]:transition-shadow [&>div]:hover:shadow-xl"
              key={`${featuredTab}-${note.id}`}
            >
              <NoteCard
                note={{
                  bookmarked: bookmarkedIds.has(note.id),
                  course: note.course,
                  downloads: note.downloads,
                  rating: note.rating,
                  title: note.title,
                  uploader: `By ${note.uploader} · ${note.university}`,
                }}
                onBookmark={() => toggleBookmark(note)}
                onDownload={() => downloadNote(note)}
              />
            </div>
          ))}
        </div>
      </section>

      <section>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <SectionTitle
            description={`${filteredNotes.length} ${filteredNotes.length === 1 ? "resource" : "resources"} match your library view.`}
            title="Browse all notes"
          />
          <div className="flex items-center gap-2">
            <Button
              className="md:hidden"
              onClick={() => setFiltersOpen((current) => !current)}
              variant="outline"
            >
              <SlidersHorizontal className="size-4" />
              Filters
              {activeFilterCount > 0 ? (
                <Badge variant="brand">{activeFilterCount}</Badge>
              ) : null}
            </Button>
            <Dropdown
              aria-label="Sort notes"
              onChange={(event) =>
                setSortBy(event.target.value as NotesSort)
              }
              options={notesSortOptions}
              value={sortBy}
            />
          </div>
        </div>

        {filtersOpen ? (
          <div className="mt-4 md:hidden">
            <NotesFilterControls
              filters={filters}
              onChange={updateFilter}
              onClear={clearFilters}
              showClear={activeFilterCount > 0}
            />
          </div>
        ) : null}

        <div className="mt-4 hidden md:block">
          <NotesFilterControls
            filters={filters}
            onChange={updateFilter}
            onClear={clearFilters}
            showClear={activeFilterCount > 0}
          />
        </div>

        {filteredNotes.length > 0 ? (
          <div className="mt-5 grid gap-4 lg:grid-cols-2 2xl:grid-cols-3">
            {filteredNotes.map((note) => (
              <LibraryNoteCard
                isBookmarked={bookmarkedIds.has(note.id)}
                isRated={ratedIds.has(note.id)}
                key={note.id}
                note={note}
                onBookmark={() => toggleBookmark(note)}
                onDownload={() => downloadNote(note)}
                onPreview={() => setPreviewNote(note)}
                onRate={() => rateNote(note)}
              />
            ))}
          </div>
        ) : (
          <EmptyState
            action={
              <Button onClick={clearFilters} variant="outline">
                Clear filters
              </Button>
            }
            className="mt-5"
            description="Try a different keyword or clear the active filters to see more student resources."
            icon={<SearchX className="size-6" />}
            title="No notes match your search"
          />
        )}
      </section>

      <Modal
        description="Share a useful course resource with your campus community."
        footer={
          <>
            <Button
              onClick={() => {
                setIsUploadOpen(false);
                resetUploadForm();
              }}
              type="button"
              variant="ghost"
            >
              Cancel
            </Button>
            <Button form="upload-notes-form" type="submit">
              <UploadCloud className="size-4" />
              Upload notes
            </Button>
          </>
        }
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        size="xl"
        title="Upload notes"
      >
        <form
          className="grid gap-5"
          id="upload-notes-form"
          noValidate
          onSubmit={handleUploadSubmit}
        >
          <FormField
            error={uploadErrors.title}
            label="Note title"
            onChange={(event) =>
              updateUploadField("title", event.target.value)
            }
            placeholder="e.g. OOP Final Revision Notes"
            required
            value={uploadForm.title}
          />

          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              error={uploadErrors.university}
              label="University"
              onChange={(event) =>
                updateUploadField("university", event.target.value)
              }
              options={uploadSelectOptions.university}
              required
              value={uploadForm.university}
            />
            <SelectField
              error={uploadErrors.department}
              label="Department"
              onChange={(event) =>
                updateUploadField("department", event.target.value)
              }
              options={uploadSelectOptions.department}
              required
              value={uploadForm.department}
            />
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              error={uploadErrors.semester}
              label="Semester"
              onChange={(event) =>
                updateUploadField("semester", event.target.value)
              }
              options={uploadSelectOptions.semester}
              required
              value={uploadForm.semester}
            />
            <SelectField
              error={uploadErrors.course}
              label="Course"
              onChange={(event) =>
                updateUploadField("course", event.target.value)
              }
              options={uploadSelectOptions.course}
              required
              value={uploadForm.course}
            />
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <FormField
              error={uploadErrors.teacher}
              label="Teacher"
              onChange={(event) =>
                updateUploadField("teacher", event.target.value)
              }
              placeholder="Teacher or instructor name"
              required
              value={uploadForm.teacher}
            />
            <SelectField
              error={uploadErrors.fileType}
              label="File type"
              onChange={(event) => {
                updateUploadField("fileType", event.target.value);
                setSelectedFile("");
                setUploadErrors((current) => ({
                  ...current,
                  file: undefined,
                }));
              }}
              options={uploadSelectOptions.fileType}
              required
              value={uploadForm.fileType}
            />
          </div>

          <div className="grid gap-1.5">
            <label
              className="text-sm font-semibold text-slate-700"
              htmlFor="upload-note-description"
            >
              Description
              <span aria-hidden="true" className="ml-1 text-red-500">
                *
              </span>
            </label>
            <textarea
              aria-describedby={
                uploadErrors.description
                  ? "upload-note-description-error"
                  : undefined
              }
              aria-invalid={Boolean(uploadErrors.description)}
              className={cn(
                "min-h-28 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm leading-6 text-slate-950 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:ring-4",
                uploadErrors.description
                  ? "border-red-300 focus:border-red-400 focus:ring-red-100"
                  : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
              )}
              id="upload-note-description"
              onChange={(event) =>
                updateUploadField("description", event.target.value)
              }
              placeholder="What topics do these notes cover?"
              value={uploadForm.description}
            />
            {uploadErrors.description ? (
              <p
                className="text-xs font-medium text-red-600"
                id="upload-note-description-error"
              >
                {uploadErrors.description}
              </p>
            ) : null}
          </div>

          <FormField
            hint="Separate tags with commas."
            label="Tags"
            onChange={(event) =>
              updateUploadField("tags", event.target.value)
            }
            placeholder="oop, finals, java, uml"
            value={uploadForm.tags}
          />

          <div className="grid gap-1.5">
            <span className="text-sm font-semibold text-slate-700">
              Notes file
              <span aria-hidden="true" className="ml-1 text-red-500">
                *
              </span>
            </span>
            <div
              className={cn(
                "flex flex-col items-center justify-center rounded-2xl border border-dashed p-6 text-center transition",
                uploadErrors.file
                  ? "border-red-300 bg-red-50/40"
                  : selectedFile
                    ? "border-emerald-300 bg-emerald-50/50"
                    : "border-slate-300 bg-slate-50 hover:border-brand-300 hover:bg-brand-50/30",
              )}
            >
              <span
                className={cn(
                  "grid size-12 place-items-center rounded-2xl",
                  selectedFile
                    ? "bg-emerald-100 text-emerald-700"
                    : "bg-white text-brand-600 shadow-sm",
                )}
              >
                {selectedFile ? (
                  <FileText className="size-5" />
                ) : (
                  <UploadCloud className="size-5" />
                )}
              </span>
              <p className="mt-3 text-sm font-semibold text-slate-800">
                {selectedFile || "Choose a file for this demo"}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                No file leaves your device or is uploaded to a server.
              </p>
              <Button
                className="mt-4"
                disabled={!uploadForm.fileType}
                onClick={() => {
                  const extension =
                    uploadForm.fileType === "Images"
                      ? "zip"
                      : uploadForm.fileType.toLowerCase();
                  setSelectedFile(`campusone-notes.${extension}`);
                  setUploadErrors((current) => ({
                    ...current,
                    file: undefined,
                  }));
                }}
                size="sm"
                type="button"
                variant="outline"
              >
                Select demo file
              </Button>
            </div>
            {uploadErrors.file ? (
              <p className="text-xs font-medium text-red-600">
                {uploadErrors.file}
              </p>
            ) : null}
          </div>
        </form>
      </Modal>

      <Modal
        description={previewNote?.description}
        footer={
          <>
            <Button
              onClick={() => setPreviewNote(null)}
              type="button"
              variant="ghost"
            >
              Close
            </Button>
            <Button
              onClick={() => {
                if (previewNote) downloadNote(previewNote);
              }}
              type="button"
            >
              <ArrowDownToLine className="size-4" />
              Download
            </Button>
          </>
        }
        isOpen={Boolean(previewNote)}
        onClose={() => setPreviewNote(null)}
        size="lg"
        title={previewNote?.title ?? "Note preview"}
      >
        {previewNote ? (
          <div>
            <div className="flex flex-wrap gap-2">
              <Badge variant="brand">{previewNote.course}</Badge>
              <Badge className={fileTypeTones[previewNote.fileType]}>
                {previewNote.fileType}
              </Badge>
            </div>
            <dl className="mt-5 grid gap-4 rounded-2xl bg-slate-50 p-4 text-sm sm:grid-cols-3">
              <div>
                <dt className="text-xs font-medium text-slate-400">Teacher</dt>
                <dd className="mt-1 font-semibold text-slate-800">
                  {previewNote.teacher}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-400">
                  Uploaded by
                </dt>
                <dd className="mt-1 font-semibold text-slate-800">
                  {previewNote.uploader}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-400">
                  University
                </dt>
                <dd className="mt-1 font-semibold text-slate-800">
                  {previewNote.university}
                </dd>
              </div>
            </dl>

            <div className="mt-5 overflow-hidden rounded-2xl border border-slate-200 bg-slate-100 p-3 sm:p-5">
              <div className="mx-auto min-h-80 max-w-md rounded-xl bg-white p-6 shadow-lg">
                <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                  <div className="flex items-center gap-2">
                    <span className="grid size-8 place-items-center rounded-lg bg-brand-50 text-brand-600">
                      <FileText className="size-4" />
                    </span>
                    <span className="text-xs font-bold text-slate-700">
                      CampusOne Notes
                    </span>
                  </div>
                  <Badge>{previewNote.fileType}</Badge>
                </div>
                <h3 className="mt-6 text-lg font-bold text-slate-900">
                  {previewNote.title}
                </h3>
                <p className="mt-2 text-xs text-slate-500">
                  {previewNote.course} · {previewNote.teacher}
                </p>
                <div className="mt-6 grid gap-3">
                  {[100, 92, 96, 75, 88, 62].map((width, index) => (
                    <span
                      className={cn(
                        "h-2 rounded-full",
                        index === 0 ? "bg-brand-100" : "bg-slate-100",
                      )}
                      key={`${width}-${index}`}
                      style={{ width: `${width}%` }}
                    />
                  ))}
                </div>
                <div className="mt-7 rounded-xl border border-brand-100 bg-brand-50 p-4">
                  <p className="text-xs font-semibold text-brand-800">
                    Preview only
                  </p>
                  <p className="mt-1 text-[11px] leading-5 text-brand-700">
                    This area represents the first page of the selected study
                    resource. No real document is loaded.
                  </p>
                </div>
              </div>
            </div>
          </div>
        ) : null}
      </Modal>
    </div>
  );
}
