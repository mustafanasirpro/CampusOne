import {
  ArrowRight,
  Banknote,
  Bookmark,
  BriefcaseBusiness,
  CalendarClock,
  CheckCircle2,
  Clock3,
  ExternalLink,
  MapPin,
  SearchX,
  SlidersHorizontal,
  Sparkles,
} from "lucide-react";
import { useMemo, useState, type FormEvent } from "react";

import { InternshipCard, StatCard } from "@/components/cards";
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
  useToast,
} from "@/components/common";
import { FormField, SelectField } from "@/components/forms";
import {
  careerTips,
  internshipCityOptions,
  internshipDeadlineOptions,
  internshipOpportunities,
  internshipPaidOptions,
  internshipPostCityOptions,
  internshipPostPaidOptions,
  internshipPostWorkModeOptions,
  internshipSkillOptions,
  internshipSortOptions,
  internshipStats,
  internshipTypeOptions,
  internshipWorkModeOptions,
  type InternshipOpportunity,
  type InternshipType,
  type WorkMode,
} from "@/data/internships";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

type InternshipSort = "newest" | "deadline" | "paid";

interface InternshipFilters {
  city: string;
  deadline: string;
  paid: string;
  skill: string;
  type: string;
  workMode: string;
}

interface PostInternshipForm {
  city: string;
  company: string;
  deadline: string;
  description: string;
  duration: string;
  paid: string;
  role: string;
  skills: string;
  stipend: string;
  workMode: string;
}

type PostInternshipErrors = Partial<
  Record<keyof PostInternshipForm, string>
>;

const initialFilters: InternshipFilters = {
  city: "all",
  type: "all",
  workMode: "all",
  paid: "all",
  skill: "all",
  deadline: "all",
};

const initialPostForm: PostInternshipForm = {
  company: "",
  role: "",
  city: "",
  workMode: "",
  paid: "",
  stipend: "",
  duration: "",
  skills: "",
  description: "",
  deadline: "",
};

const companyToneClasses = {
  brand: "bg-brand-100 text-brand-700 ring-brand-200",
  emerald: "bg-emerald-100 text-emerald-700 ring-emerald-200",
  amber: "bg-amber-100 text-amber-700 ring-amber-200",
  sky: "bg-sky-100 text-sky-700 ring-sky-200",
  rose: "bg-rose-100 text-rose-700 ring-rose-200",
  violet: "bg-violet-100 text-violet-700 ring-violet-200",
};

const careerTipTones = {
  brand: "bg-brand-50 text-brand-600 group-hover:bg-brand-600",
  emerald: "bg-emerald-50 text-emerald-600 group-hover:bg-emerald-600",
  amber: "bg-amber-50 text-amber-600 group-hover:bg-amber-500",
  sky: "bg-sky-50 text-sky-600 group-hover:bg-sky-600",
};

const DEMO_TODAY = new Date("2026-06-30T00:00:00");

function companyInitials(company: string) {
  return company
    .split(/\s+/)
    .slice(0, 2)
    .map((word) => word[0])
    .join("")
    .toUpperCase();
}

function daysUntilDeadline(date: string) {
  const deadline = new Date(`${date}T00:00:00`);
  return Math.ceil(
    (deadline.getTime() - DEMO_TODAY.getTime()) / (1000 * 60 * 60 * 24),
  );
}

function matchesDeadline(date: string, range: string) {
  const days = daysUntilDeadline(date);
  if (range === "week") return days >= 0 && days <= 7;
  if (range === "two-weeks") return days >= 0 && days <= 14;
  if (range === "month") return days >= 0 && days <= 30;
  if (range === "later") return days > 30;
  return true;
}

function inferInternshipType(role: string): InternshipType {
  const normalized = role.toLowerCase();
  if (normalized.includes("backend")) return "Backend";
  if (normalized.includes("java")) return "Java";
  if (normalized.includes("design") || normalized.includes("ui")) {
    return "UI/UX";
  }
  if (normalized.includes("data")) return "Data Analyst";
  if (normalized.includes("ai") || normalized.includes("machine")) {
    return "AI/ML";
  }
  if (normalized.includes("marketing")) return "Marketing";
  return "Frontend";
}

interface FullInternshipCardProps {
  internship: InternshipOpportunity;
  isSaved: boolean;
  onApply: () => void;
  onSave: () => void;
  onView: () => void;
}

function FullInternshipCard({
  internship,
  isSaved,
  onApply,
  onSave,
  onView,
}: FullInternshipCardProps) {
  return (
    <Card className="group flex h-full flex-col transition duration-200 hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl">
      <CardContent className="flex flex-1 flex-col p-5">
        <div className="flex items-start gap-3">
          <span
            className={cn(
              "grid size-12 shrink-0 place-items-center rounded-2xl text-sm font-bold ring-1 ring-inset",
              companyToneClasses[internship.tone],
            )}
          >
            {companyInitials(internship.company)}
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-semibold text-slate-700">
              {internship.company}
            </p>
            <h3 className="mt-1 font-semibold leading-6 text-slate-950 transition group-hover:text-brand-700">
              {internship.role}
            </h3>
          </div>
          <Button
            aria-label={
              isSaved ? "Remove saved internship" : "Save internship"
            }
            onClick={onSave}
            size="icon"
            variant={isSaved ? "secondary" : "ghost"}
          >
            <Bookmark
              className={cn(
                "size-4.5",
                isSaved && "fill-brand-600 text-brand-600",
              )}
            />
          </Button>
        </div>

        <div className="mt-4 flex flex-wrap gap-2">
          <Badge variant="brand">{internship.type}</Badge>
          <Badge>{internship.workMode}</Badge>
          <Badge variant={internship.paid ? "success" : "neutral"}>
            {internship.paid ? "Paid" : "Unpaid"}
          </Badge>
        </div>

        <div className="mt-4 flex flex-wrap gap-1.5">
          {internship.skills.slice(0, 4).map((skill) => (
            <span
              className="rounded-lg bg-slate-100 px-2 py-1 text-[11px] font-medium text-slate-600"
              key={skill}
            >
              {skill}
            </span>
          ))}
        </div>

        <div className="mt-5 grid gap-2.5 text-xs text-slate-500">
          <p className="flex items-center gap-2">
            <MapPin className="size-3.5 text-slate-400" />
            {internship.city} · {internship.workMode}
          </p>
          <p className="flex items-center gap-2">
            <Banknote className="size-3.5 text-slate-400" />
            {internship.stipend}
          </p>
          <p className="flex items-center gap-2">
            <CalendarClock className="size-3.5 text-slate-400" />
            Apply by {internship.deadline}
          </p>
          <p className="flex items-center gap-2">
            <Clock3 className="size-3.5 text-slate-400" />
            Posted {internship.postedTime}
          </p>
        </div>

        <div className="mt-auto grid grid-cols-2 gap-2 pt-5">
          <Button onClick={onView} variant="outline">
            View details
          </Button>
          <Button onClick={onApply}>
            Apply
            <ExternalLink className="size-3.5" />
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

interface InternshipFilterControlsProps {
  filters: InternshipFilters;
  onChange: (field: keyof InternshipFilters, value: string) => void;
  onClear: () => void;
  showClear: boolean;
}

function InternshipFilterControls({
  filters,
  onChange,
  onClear,
  showClear,
}: InternshipFilterControlsProps) {
  const fields = [
    {
      key: "city" as const,
      label: "City",
      options: internshipCityOptions,
    },
    {
      key: "type" as const,
      label: "Internship type",
      options: internshipTypeOptions,
    },
    {
      key: "workMode" as const,
      label: "Work mode",
      options: internshipWorkModeOptions,
    },
    {
      key: "paid" as const,
      label: "Paid or unpaid",
      options: internshipPaidOptions,
    },
    {
      key: "skill" as const,
      label: "Skills",
      options: internshipSkillOptions,
    },
    {
      key: "deadline" as const,
      label: "Deadline",
      options: internshipDeadlineOptions,
    },
  ];

  return (
    <FilterBar onClear={onClear} showClear={showClear}>
      {fields.map((field) => (
        <div className="w-full min-w-40 flex-1 sm:w-auto" key={field.key}>
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

export function InternshipsPage() {
  const [opportunities, setOpportunities] = useState(
    internshipOpportunities,
  );
  const [searchValue, setSearchValue] = useState("");
  const [filters, setFilters] =
    useState<InternshipFilters>(initialFilters);
  const [sortBy, setSortBy] = useState<InternshipSort>("newest");
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [savedIds, setSavedIds] = useState<Set<string>>(
    new Set(["arbisoft-frontend", "motive-frontend"]),
  );
  const [selectedInternship, setSelectedInternship] =
    useState<InternshipOpportunity | null>(null);
  const [isPostOpen, setIsPostOpen] = useState(false);
  const [postForm, setPostForm] =
    useState<PostInternshipForm>(initialPostForm);
  const [postErrors, setPostErrors] =
    useState<PostInternshipErrors>({});
  const { showToast } = useToast();

  useDocumentTitle("Internship Hub · CampusOne");

  const activeFilterCount =
    Object.values(filters).filter((value) => value !== "all").length +
    (searchValue.trim() ? 1 : 0);

  const filteredOpportunities = useMemo(() => {
    const query = searchValue.trim().toLowerCase();
    const matches = opportunities.filter((internship) => {
      const searchableText = [
        internship.company,
        internship.role,
        internship.type,
        internship.city,
        internship.description,
        ...internship.skills,
      ]
        .join(" ")
        .toLowerCase();

      return (
        (!query || searchableText.includes(query)) &&
        (filters.city === "all" || internship.city === filters.city) &&
        (filters.type === "all" || internship.type === filters.type) &&
        (filters.workMode === "all" ||
          internship.workMode === filters.workMode) &&
        (filters.paid === "all" ||
          (filters.paid === "paid" && internship.paid) ||
          (filters.paid === "unpaid" && !internship.paid)) &&
        (filters.skill === "all" ||
          internship.skills.includes(filters.skill)) &&
        matchesDeadline(internship.deadlineDate, filters.deadline)
      );
    });

    return [...matches].sort((first, second) => {
      if (sortBy === "deadline") {
        return first.deadlineDate.localeCompare(second.deadlineDate);
      }
      if (sortBy === "paid") return Number(second.paid) - Number(first.paid);
      return second.postedAt.localeCompare(first.postedAt);
    });
  }, [filters, opportunities, searchValue, sortBy]);

  const featuredOpportunities = useMemo(
    () =>
      opportunities
        .filter((internship) => internship.featured)
        .slice(0, 3),
    [opportunities],
  );

  const clearFilters = () => {
    setFilters(initialFilters);
    setSearchValue("");
  };

  const updateFilter = (
    field: keyof InternshipFilters,
    value: string,
  ) => {
    setFilters((current) => ({ ...current, [field]: value }));
  };

  const toggleSaved = (internship: InternshipOpportunity) => {
    const isSaved = savedIds.has(internship.id);
    setSavedIds((current) => {
      const next = new Set(current);
      if (next.has(internship.id)) next.delete(internship.id);
      else next.add(internship.id);
      return next;
    });
    showToast({
      title: isSaved ? "Removed from saved" : "Internship saved",
      message: `${internship.role} at ${internship.company}`,
      variant: isSaved ? "info" : "success",
    });
  };

  const applyToInternship = (internship: InternshipOpportunity) => {
    showToast({
      title: "Application page ready",
      message: `${internship.company} applications are a frontend demo.`,
      variant: "success",
    });
  };

  const updatePostField = (
    field: keyof PostInternshipForm,
    value: string,
  ) => {
    setPostForm((current) => ({ ...current, [field]: value }));
    setPostErrors((current) => ({ ...current, [field]: undefined }));
  };

  const resetPostForm = () => {
    setPostForm(initialPostForm);
    setPostErrors({});
  };

  const handlePostInternship = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors: PostInternshipErrors = {};
    const skills = postForm.skills
      .split(",")
      .map((skill) => skill.trim())
      .filter(Boolean);

    if (postForm.company.trim().length < 2) {
      nextErrors.company = "Enter the company name.";
    }
    if (postForm.role.trim().length < 5) {
      nextErrors.role = "Enter a clear role title.";
    }
    if (!postForm.city) nextErrors.city = "Select a city.";
    if (!postForm.workMode) nextErrors.workMode = "Select a work mode.";
    if (!postForm.paid) nextErrors.paid = "Select compensation.";
    if (postForm.stipend.trim().length < 2) {
      nextErrors.stipend = "Enter the stipend or unpaid details.";
    }
    if (postForm.duration.trim().length < 2) {
      nextErrors.duration = "Enter the internship duration.";
    }
    if (skills.length === 0) {
      nextErrors.skills = "Add at least one required skill.";
    }
    if (postForm.description.trim().length < 30) {
      nextErrors.description = "Add at least 30 characters.";
    }
    if (!postForm.deadline) nextErrors.deadline = "Select a deadline.";

    if (Object.keys(nextErrors).length > 0) {
      setPostErrors(nextErrors);
      return;
    }

    const deadlineDate = new Date(`${postForm.deadline}T00:00:00`);
    const newInternship: InternshipOpportunity = {
      id: `internship-${Date.now()}`,
      company: postForm.company.trim(),
      role: postForm.role.trim(),
      type: inferInternshipType(postForm.role),
      city: postForm.city,
      workMode: postForm.workMode as WorkMode,
      paid: postForm.paid === "paid",
      stipend: postForm.stipend.trim(),
      duration: postForm.duration.trim(),
      skills,
      deadline: new Intl.DateTimeFormat("en-PK", {
        month: "long",
        day: "numeric",
        year: "numeric",
      }).format(deadlineDate),
      deadlineDate: postForm.deadline,
      postedAt: new Date().toISOString(),
      postedTime: "Just now",
      featured: false,
      tone: "brand",
      description: postForm.description.trim(),
      responsibilities: [
        "Complete role-specific tasks with guidance from the team.",
        "Communicate progress and document completed work.",
        "Participate in feedback sessions and team collaboration.",
      ],
      requirements: [
        `Working knowledge of ${skills.slice(0, 2).join(" and ")}.`,
        "Strong communication and willingness to learn.",
        "Current university enrollment or recent graduation.",
      ],
    };

    setOpportunities((current) => [newInternship, ...current]);
    setIsPostOpen(false);
    clearFilters();
    resetPostForm();
    showToast({
      title: "Internship posted",
      message: "The new opportunity now appears in the hub demo.",
      variant: "success",
    });
  };

  return (
    <div className="grid gap-8 pb-8">
      <PageHeader
        actions={
          <Button onClick={() => setIsPostOpen(true)}>
            <BriefcaseBusiness className="size-4" />
            Post internship
          </Button>
        }
        description="Discover student-friendly roles, build practical experience, and take the next step toward your career."
        eyebrow="Career opportunities"
        title="Internship Hub"
      />

      <SearchBar
        className="max-w-3xl"
        onSearch={setSearchValue}
        onValueChange={setSearchValue}
        placeholder="Search internships, companies, skills, or cities..."
        value={searchValue}
      />

      <section aria-labelledby="internship-stats">
        <h2 className="sr-only" id="internship-stats">
          Internship statistics
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {internshipStats.map((stat) => (
            <div
              className="transition duration-200 hover:-translate-y-1 [&>div]:h-full [&>div]:transition-shadow [&>div]:hover:shadow-xl"
              key={stat.label}
            >
              <StatCard
                change={stat.change}
                icon={stat.icon}
                label={stat.label}
                value={stat.value}
              />
            </div>
          ))}
        </div>
      </section>

      <section>
        <div className="flex items-center gap-2">
          <span className="grid size-9 place-items-center rounded-xl bg-amber-50 text-amber-600">
            <Sparkles className="size-4" />
          </span>
          <SectionTitle
            description="High-value roles with strong mentorship and practical learning."
            title="Featured internships"
          />
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {featuredOpportunities.map((internship) => (
            <div
              className="transition duration-200 hover:-translate-y-1 [&>div]:h-full [&>div]:transition-shadow [&>div]:hover:shadow-xl"
              key={internship.id}
            >
              <InternshipCard
                internship={{
                  company: internship.company,
                  role: internship.role,
                  location: internship.city,
                  deadline: internship.deadline,
                  paid: internship.paid,
                  remote: internship.workMode === "Remote",
                  saved: savedIds.has(internship.id),
                }}
                onApply={() => applyToInternship(internship)}
                onSave={() => toggleSaved(internship)}
              />
            </div>
          ))}
        </div>
      </section>

      <section>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <SectionTitle
            description={`${filteredOpportunities.length} ${filteredOpportunities.length === 1 ? "opportunity" : "opportunities"} match your career view.`}
            title="Explore opportunities"
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
              aria-label="Sort internships"
              onChange={(event) =>
                setSortBy(event.target.value as InternshipSort)
              }
              options={internshipSortOptions}
              value={sortBy}
            />
          </div>
        </div>

        {filtersOpen ? (
          <div className="mt-4 md:hidden">
            <InternshipFilterControls
              filters={filters}
              onChange={updateFilter}
              onClear={clearFilters}
              showClear={activeFilterCount > 0}
            />
          </div>
        ) : null}
        <div className="mt-4 hidden md:block">
          <InternshipFilterControls
            filters={filters}
            onChange={updateFilter}
            onClear={clearFilters}
            showClear={activeFilterCount > 0}
          />
        </div>

        {filteredOpportunities.length > 0 ? (
          <div className="mt-5 grid gap-4 md:grid-cols-2 2xl:grid-cols-3">
            {filteredOpportunities.map((internship) => (
              <FullInternshipCard
                internship={internship}
                isSaved={savedIds.has(internship.id)}
                key={internship.id}
                onApply={() => applyToInternship(internship)}
                onSave={() => toggleSaved(internship)}
                onView={() => setSelectedInternship(internship)}
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
            description="Try another city, role, skill, work mode, or deadline range to discover more opportunities."
            icon={<SearchX className="size-6" />}
            title="No internships found."
          />
        )}
      </section>

      <section>
        <SectionTitle
          description="Practical guidance for landing and succeeding in your first role."
          title="Career tips"
        />
        <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {careerTips.map((tip) => (
            <Card
              className="group h-full transition duration-200 hover:-translate-y-1 hover:border-brand-200 hover:shadow-lg"
              key={tip.title}
            >
              <CardContent className="flex h-full flex-col p-5">
                <span
                  className={cn(
                    "grid size-11 place-items-center rounded-xl transition group-hover:text-white",
                    careerTipTones[tip.tone],
                  )}
                >
                  <tip.icon className="size-5" />
                </span>
                <h3 className="mt-4 font-semibold text-slate-900">
                  {tip.title}
                </h3>
                <p className="mt-2 flex-1 text-sm leading-6 text-slate-500">
                  {tip.description}
                </p>
                <Button
                  className="mt-4 justify-start px-0"
                  onClick={() =>
                    showToast({
                      title: tip.title,
                      message: tip.description,
                    })
                  }
                  size="sm"
                  variant="ghost"
                >
                  Read tip
                  <ArrowRight className="size-3.5" />
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      </section>

      <Modal
        description={
          selectedInternship
            ? `${selectedInternship.company} · ${selectedInternship.city}`
            : undefined
        }
        footer={
          selectedInternship ? (
            <>
              <Button
                onClick={() => toggleSaved(selectedInternship)}
                variant={
                  savedIds.has(selectedInternship.id)
                    ? "secondary"
                    : "outline"
                }
              >
                <Bookmark
                  className={cn(
                    "size-4",
                    savedIds.has(selectedInternship.id) &&
                      "fill-brand-600",
                  )}
                />
                {savedIds.has(selectedInternship.id) ? "Saved" : "Save"}
              </Button>
              <Button onClick={() => applyToInternship(selectedInternship)}>
                Apply now
                <ExternalLink className="size-4" />
              </Button>
            </>
          ) : undefined
        }
        isOpen={Boolean(selectedInternship)}
        onClose={() => setSelectedInternship(null)}
        size="xl"
        title="Internship details"
      >
        {selectedInternship ? (
          <article>
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
              <span
                className={cn(
                  "grid size-16 shrink-0 place-items-center rounded-2xl text-lg font-bold ring-1 ring-inset",
                  companyToneClasses[selectedInternship.tone],
                )}
              >
                {companyInitials(selectedInternship.company)}
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-brand-700">
                  {selectedInternship.company}
                </p>
                <h2 className="mt-1 text-2xl font-bold tracking-tight text-slate-950">
                  {selectedInternship.role}
                </h2>
                <div className="mt-3 flex flex-wrap gap-2">
                  <Badge variant="brand">{selectedInternship.type}</Badge>
                  <Badge>{selectedInternship.workMode}</Badge>
                  <Badge
                    variant={
                      selectedInternship.paid ? "success" : "neutral"
                    }
                  >
                    {selectedInternship.paid ? "Paid" : "Unpaid"}
                  </Badge>
                </div>
              </div>
            </div>

            <dl className="mt-6 grid gap-4 rounded-2xl bg-slate-50 p-4 text-sm sm:grid-cols-2 lg:grid-cols-4">
              <div>
                <dt className="text-xs font-medium text-slate-400">
                  Location
                </dt>
                <dd className="mt-1 font-semibold text-slate-800">
                  {selectedInternship.city} ·{" "}
                  {selectedInternship.workMode}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-400">
                  Stipend
                </dt>
                <dd className="mt-1 font-semibold text-slate-800">
                  {selectedInternship.stipend}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-400">
                  Duration
                </dt>
                <dd className="mt-1 font-semibold text-slate-800">
                  {selectedInternship.duration}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-400">
                  Deadline
                </dt>
                <dd className="mt-1 font-semibold text-slate-800">
                  {selectedInternship.deadline}
                </dd>
              </div>
            </dl>

            <div className="mt-6">
              <h3 className="font-semibold text-slate-950">About the role</h3>
              <p className="mt-2 text-sm leading-7 text-slate-600">
                {selectedInternship.description}
              </p>
            </div>

            <div className="mt-6">
              <h3 className="font-semibold text-slate-950">
                Required skills
              </h3>
              <div className="mt-3 flex flex-wrap gap-2">
                {selectedInternship.skills.map((skill) => (
                  <Badge key={skill} variant="brand">
                    {skill}
                  </Badge>
                ))}
              </div>
            </div>

            <div className="mt-6 grid gap-6 lg:grid-cols-2">
              <div>
                <h3 className="font-semibold text-slate-950">
                  Responsibilities
                </h3>
                <ul className="mt-3 grid gap-2.5">
                  {selectedInternship.responsibilities.map((item) => (
                    <li
                      className="flex gap-2.5 text-sm leading-6 text-slate-600"
                      key={item}
                    >
                      <CheckCircle2 className="mt-1 size-4 shrink-0 text-emerald-500" />
                      {item}
                    </li>
                  ))}
                </ul>
              </div>
              <div>
                <h3 className="font-semibold text-slate-950">Requirements</h3>
                <ul className="mt-3 grid gap-2.5">
                  {selectedInternship.requirements.map((item) => (
                    <li
                      className="flex gap-2.5 text-sm leading-6 text-slate-600"
                      key={item}
                    >
                      <CheckCircle2 className="mt-1 size-4 shrink-0 text-brand-500" />
                      {item}
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </article>
        ) : null}
      </Modal>

      <Modal
        description="Share a student-friendly opportunity with the CampusOne community."
        footer={
          <>
            <Button
              onClick={() => {
                setIsPostOpen(false);
                resetPostForm();
              }}
              type="button"
              variant="ghost"
            >
              Cancel
            </Button>
            <Button form="post-internship-form" type="submit">
              <BriefcaseBusiness className="size-4" />
              Post internship
            </Button>
          </>
        }
        isOpen={isPostOpen}
        onClose={() => setIsPostOpen(false)}
        size="xl"
        title="Post an internship"
      >
        <form
          className="grid gap-5"
          id="post-internship-form"
          noValidate
          onSubmit={handlePostInternship}
        >
          <div className="grid gap-5 sm:grid-cols-2">
            <FormField
              error={postErrors.company}
              label="Company name"
              onChange={(event) =>
                updatePostField("company", event.target.value)
              }
              placeholder="e.g. Arbisoft"
              required
              value={postForm.company}
            />
            <FormField
              error={postErrors.role}
              label="Role title"
              onChange={(event) =>
                updatePostField("role", event.target.value)
              }
              placeholder="e.g. Frontend Engineering Intern"
              required
              value={postForm.role}
            />
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              error={postErrors.city}
              label="City"
              onChange={(event) =>
                updatePostField("city", event.target.value)
              }
              options={internshipPostCityOptions}
              required
              value={postForm.city}
            />
            <SelectField
              error={postErrors.workMode}
              label="Work mode"
              onChange={(event) =>
                updatePostField("workMode", event.target.value)
              }
              options={internshipPostWorkModeOptions}
              required
              value={postForm.workMode}
            />
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              error={postErrors.paid}
              label="Paid or unpaid"
              onChange={(event) =>
                updatePostField("paid", event.target.value)
              }
              options={internshipPostPaidOptions}
              required
              value={postForm.paid}
            />
            <FormField
              error={postErrors.stipend}
              label="Stipend"
              onChange={(event) =>
                updatePostField("stipend", event.target.value)
              }
              placeholder="e.g. Rs. 40,000 / month"
              required
              value={postForm.stipend}
            />
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <FormField
              error={postErrors.duration}
              label="Duration"
              onChange={(event) =>
                updatePostField("duration", event.target.value)
              }
              placeholder="e.g. 3 months"
              required
              value={postForm.duration}
            />
            <FormField
              error={postErrors.deadline}
              label="Application deadline"
              min="2026-06-30"
              onChange={(event) =>
                updatePostField("deadline", event.target.value)
              }
              required
              type="date"
              value={postForm.deadline}
            />
          </div>

          <FormField
            error={postErrors.skills}
            hint="Separate skills with commas."
            label="Skills required"
            onChange={(event) =>
              updatePostField("skills", event.target.value)
            }
            placeholder="React, TypeScript, Git"
            required
            value={postForm.skills}
          />

          <div className="grid gap-1.5">
            <label
              className="text-sm font-semibold text-slate-700"
              htmlFor="internship-description"
            >
              Description
              <span aria-hidden="true" className="ml-1 text-red-500">
                *
              </span>
            </label>
            <textarea
              aria-describedby={
                postErrors.description
                  ? "internship-description-error"
                  : undefined
              }
              aria-invalid={Boolean(postErrors.description)}
              className={cn(
                "min-h-36 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm leading-6 text-slate-950 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:ring-4",
                postErrors.description
                  ? "border-red-300 focus:border-red-400 focus:ring-red-100"
                  : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
              )}
              id="internship-description"
              onChange={(event) =>
                updatePostField("description", event.target.value)
              }
              placeholder="Describe the opportunity, learning outcomes, and ideal student candidate."
              value={postForm.description}
            />
            {postErrors.description ? (
              <p
                className="text-xs font-medium text-red-600"
                id="internship-description-error"
              >
                {postErrors.description}
              </p>
            ) : null}
          </div>
        </form>
      </Modal>
    </div>
  );
}
