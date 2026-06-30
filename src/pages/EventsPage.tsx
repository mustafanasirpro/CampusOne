import {
  ArrowRight,
  Bookmark,
  BriefcaseBusiness,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Code2,
  Dumbbell,
  Eye,
  GraduationCap,
  MapPin,
  Medal,
  Radio,
  SearchX,
  Sparkles,
  Trophy,
  UploadCloud,
  UsersRound,
} from "lucide-react";
import { useMemo, useState, type FormEvent } from "react";

import { EventCard, StatCard } from "@/components/cards";
import {
  Avatar,
  Badge,
  Button,
  Card,
  CardContent,
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
  campusEvents,
  createEventCategoryOptions,
  eventCategories,
  eventStats,
  initiallyRegisteredEventIds,
  type CampusEvent,
  type EventCategory,
} from "@/data/events";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

interface CreateEventForm {
  category: string;
  date: string;
  description: string;
  organizer: string;
  time: string;
  title: string;
  venue: string;
}

type CreateEventErrors = Partial<
  Record<keyof CreateEventForm | "banner", string>
>;

const initialCreateEvent: CreateEventForm = {
  title: "",
  category: "",
  date: "",
  time: "",
  venue: "",
  organizer: "",
  description: "",
};

const eventCategoryIcons = {
  Workshops: GraduationCap,
  Seminars: UsersRound,
  Hackathons: Code2,
  Competitions: Trophy,
  Sports: Dumbbell,
  "Society Events": Sparkles,
  "Career Fair": BriefcaseBusiness,
  Bootcamps: Medal,
};

const bannerToneClasses = {
  brand: "from-brand-600 to-brand-900",
  emerald: "from-emerald-600 to-emerald-900",
  amber: "from-amber-500 to-orange-700",
  sky: "from-sky-500 to-blue-800",
  rose: "from-rose-500 to-pink-800",
  violet: "from-violet-600 to-purple-900",
};

const TODAY_KEY = "2026-06-30";
const dayLabels = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

function dateKey(year: number, month: number, day: number) {
  return `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

function calendarCells(year: number, month: number) {
  const firstWeekday = (new Date(year, month, 1).getDay() + 6) % 7;
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  return [
    ...Array.from({ length: firstWeekday }, () => null),
    ...Array.from({ length: daysInMonth }, (_, index) => index + 1),
  ];
}

interface FullEventCardProps {
  event: CampusEvent;
  isRegistered: boolean;
  isSaved: boolean;
  onRsvp: () => void;
  onSave: () => void;
  onView: () => void;
}

function FullEventCard({
  event,
  isRegistered,
  isSaved,
  onRsvp,
  onSave,
  onView,
}: FullEventCardProps) {
  const EventIcon = eventCategoryIcons[event.category];

  return (
    <Card className="group flex h-full flex-col overflow-hidden transition duration-200 hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl">
      <div
        className={cn(
          "relative flex min-h-44 items-center justify-center overflow-hidden bg-gradient-to-br text-white",
          bannerToneClasses[event.tone],
        )}
      >
        <div className="absolute -right-12 -top-12 size-48 rounded-full bg-white/10" />
        <div className="absolute -bottom-16 -left-10 size-52 rounded-full bg-white/10" />
        <EventIcon className="relative size-16 opacity-75 transition duration-300 group-hover:scale-110" />
        <Badge className="absolute left-3 top-3 bg-white/15 text-white ring-white/20 backdrop-blur">
          {event.category}
        </Badge>
        {event.live ? (
          <span className="absolute bottom-3 left-3 inline-flex items-center gap-1.5 rounded-full bg-red-500 px-2.5 py-1 text-xs font-bold text-white shadow-lg">
            <Radio className="size-3" />
            Live now
          </span>
        ) : null}
        <Button
          aria-label={isSaved ? "Remove saved event" : "Save event"}
          className="absolute right-3 top-3 bg-white/15 text-white hover:bg-white/25"
          onClick={onSave}
          size="icon"
          variant="ghost"
        >
          <Bookmark
            className={cn("size-4.5", isSaved && "fill-white")}
          />
        </Button>
      </div>

      <CardContent className="flex flex-1 flex-col p-5">
        <p className="text-xs font-bold uppercase tracking-[0.12em] text-brand-600">
          {event.dateLabel} · {event.time}
        </p>
        <h3 className="mt-2 text-lg font-semibold leading-7 text-slate-950 transition group-hover:text-brand-700">
          {event.title}
        </h3>
        <p className="mt-1 text-sm text-slate-500">
          {event.organizer} · {event.university}
        </p>

        <div className="mt-4 grid gap-2 text-xs text-slate-500">
          <p className="flex items-center gap-2">
            <MapPin className="size-3.5 text-slate-400" />
            {event.venue}
          </p>
          <p className="flex items-center gap-2">
            <UsersRound className="size-3.5 text-slate-400" />
            {event.attendees.toLocaleString()} students attending
          </p>
        </div>

        <div className="mt-auto grid grid-cols-2 gap-2 pt-5">
          <Button onClick={onView} variant="outline">
            <Eye className="size-4" />
            Details
          </Button>
          <Button
            onClick={onRsvp}
            variant={isRegistered ? "secondary" : "primary"}
          >
            {isRegistered ? "Registered" : "RSVP"}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

export function EventsPage() {
  const [events, setEvents] = useState(campusEvents);
  const [searchValue, setSearchValue] = useState("");
  const [activeCategory, setActiveCategory] = useState("All");
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [calendarMonth, setCalendarMonth] = useState({
    year: 2026,
    month: 5,
  });
  const [registeredIds, setRegisteredIds] = useState<Set<string>>(
    new Set(initiallyRegisteredEventIds),
  );
  const [savedIds, setSavedIds] = useState<Set<string>>(
    new Set(["pakistan-ai-hackathon", "react-bootcamp"]),
  );
  const [selectedEvent, setSelectedEvent] = useState<CampusEvent | null>(
    null,
  );
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [createForm, setCreateForm] =
    useState<CreateEventForm>(initialCreateEvent);
  const [createErrors, setCreateErrors] =
    useState<CreateEventErrors>({});
  const [selectedBanner, setSelectedBanner] = useState("");
  const { showToast } = useToast();

  useDocumentTitle("Campus Events · CampusOne");

  const featuredEvent =
    events.find((event) => event.featured) ?? events[0];

  const filteredEvents = useMemo(() => {
    const query = searchValue.trim().toLowerCase();

    return events.filter((event) => {
      const searchableText = [
        event.title,
        event.description,
        event.organizer,
        event.university,
        event.venue,
        event.category,
        ...event.speakers.map((speaker) => speaker.name),
      ]
        .join(" ")
        .toLowerCase();

      return (
        (!query || searchableText.includes(query)) &&
        (activeCategory === "All" || event.category === activeCategory) &&
        (!selectedDate || event.date === selectedDate)
      );
    });
  }, [activeCategory, events, searchValue, selectedDate]);

  const registeredEvents = useMemo(
    () =>
      events
        .filter((event) => registeredIds.has(event.id))
        .sort((first, second) => first.date.localeCompare(second.date)),
    [events, registeredIds],
  );

  const calendarEventDates = useMemo(
    () => new Set(events.map((event) => event.date)),
    [events],
  );
  const previewDate = selectedDate ?? TODAY_KEY;
  const previewEvents = events.filter((event) => event.date === previewDate);
  const cells = calendarCells(calendarMonth.year, calendarMonth.month);
  const calendarLabel = new Intl.DateTimeFormat("en-PK", {
    month: "long",
    year: "numeric",
  }).format(new Date(calendarMonth.year, calendarMonth.month, 1));

  const clearFilters = () => {
    setSearchValue("");
    setActiveCategory("All");
    setSelectedDate(null);
  };

  const moveCalendar = (direction: number) => {
    const next = new Date(
      calendarMonth.year,
      calendarMonth.month + direction,
      1,
    );
    setCalendarMonth({ year: next.getFullYear(), month: next.getMonth() });
    setSelectedDate(null);
  };

  const toggleRegistration = (event: CampusEvent) => {
    const isRegistered = registeredIds.has(event.id);
    setRegisteredIds((current) => {
      const next = new Set(current);
      if (next.has(event.id)) next.delete(event.id);
      else next.add(event.id);
      return next;
    });
    showToast({
      title: isRegistered ? "Registration cancelled" : "You’re registered",
      message: event.title,
      variant: isRegistered ? "info" : "success",
    });
  };

  const toggleSaved = (event: CampusEvent) => {
    const isSaved = savedIds.has(event.id);
    setSavedIds((current) => {
      const next = new Set(current);
      if (next.has(event.id)) next.delete(event.id);
      else next.add(event.id);
      return next;
    });
    showToast({
      title: isSaved ? "Removed from saved" : "Event saved",
      message: event.title,
      variant: isSaved ? "info" : "success",
    });
  };

  const updateCreateField = (
    field: keyof CreateEventForm,
    value: string,
  ) => {
    setCreateForm((current) => ({ ...current, [field]: value }));
    setCreateErrors((current) => ({ ...current, [field]: undefined }));
  };

  const resetCreateForm = () => {
    setCreateForm(initialCreateEvent);
    setCreateErrors({});
    setSelectedBanner("");
  };

  const handleCreateEvent = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors: CreateEventErrors = {};

    if (createForm.title.trim().length < 5) {
      nextErrors.title = "Enter a clear event title.";
    }
    if (!createForm.category) nextErrors.category = "Select a category.";
    if (!createForm.date) nextErrors.date = "Select an event date.";
    if (!createForm.time) nextErrors.time = "Select a start time.";
    if (createForm.venue.trim().length < 3) {
      nextErrors.venue = "Enter the event venue.";
    }
    if (createForm.organizer.trim().length < 2) {
      nextErrors.organizer = "Enter the organizer name.";
    }
    if (createForm.description.trim().length < 30) {
      nextErrors.description = "Add at least 30 characters.";
    }
    if (!selectedBanner) nextErrors.banner = "Choose a demo banner.";

    if (Object.keys(nextErrors).length > 0) {
      setCreateErrors(nextErrors);
      return;
    }

    const eventDate = new Date(`${createForm.date}T00:00:00`);
    const newEvent: CampusEvent = {
      id: `event-${Date.now()}`,
      title: createForm.title.trim(),
      category: createForm.category as EventCategory,
      date: createForm.date,
      dateLabel: new Intl.DateTimeFormat("en-PK", {
        month: "long",
        day: "numeric",
        year: "numeric",
      }).format(eventDate),
      time: createForm.time,
      venue: createForm.venue.trim(),
      organizer: createForm.organizer.trim(),
      university: "COMSATS Islamabad",
      attendees: 1,
      seats: 100,
      live: false,
      featured: false,
      tone: "brand",
      description: createForm.description.trim(),
      schedule: [
        { time: createForm.time, title: "Event welcome and opening" },
        { time: "After opening", title: "Main event programme" },
        { time: "Closing", title: "Networking and wrap-up" },
      ],
      speakers: [
        {
          name: createForm.organizer.trim(),
          role: "Event organizer",
        },
      ],
    };

    setEvents((current) => [newEvent, ...current]);
    setRegisteredIds((current) => new Set([...current, newEvent.id]));
    setIsCreateOpen(false);
    clearFilters();
    resetCreateForm();
    showToast({
      title: "Event created",
      message: "Your event now appears in the Campus Events demo.",
      variant: "success",
    });
  };

  return (
    <div className="grid gap-8 pb-8">
      <PageHeader
        actions={
          <Button onClick={() => setIsCreateOpen(true)}>
            <CalendarDays className="size-4" />
            Create event
          </Button>
        }
        description="Discover workshops, competitions, society experiences, and career events across university communities."
        eyebrow="Campus calendar"
        title="Campus Events"
      />

      <SearchBar
        className="max-w-3xl"
        onSearch={setSearchValue}
        onValueChange={setSearchValue}
        placeholder="Search workshops, hackathons, seminars, sports..."
        value={searchValue}
      />

      <section aria-labelledby="event-stats">
        <h2 className="sr-only" id="event-stats">
          Event statistics
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {eventStats.map((stat) => (
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
        <FilterBar
          onClear={clearFilters}
          showClear={
            activeCategory !== "All" ||
            Boolean(selectedDate) ||
            Boolean(searchValue.trim())
          }
        >
          <div className="flex flex-wrap gap-2">
            {eventCategories.map((category) => (
              <button
                aria-pressed={activeCategory === category}
                className={cn(
                  "rounded-xl px-3 py-2 text-xs font-semibold transition",
                  activeCategory === category
                    ? "bg-brand-600 text-white shadow-sm"
                    : "bg-slate-50 text-slate-600 hover:bg-brand-50 hover:text-brand-700",
                )}
                key={category}
                onClick={() => setActiveCategory(category)}
                type="button"
              >
                {category}
              </button>
            ))}
          </div>
        </FilterBar>
      </section>

      <section>
        <Card className="overflow-hidden border-0 bg-slate-950 shadow-xl shadow-slate-950/15">
          <div className="relative grid gap-8 p-6 sm:p-8 lg:grid-cols-[1.2fr_0.8fr] lg:items-center">
            <div className="absolute -left-24 top-0 size-80 rounded-full bg-brand-600/30 blur-3xl" />
            <div className="absolute -bottom-24 right-0 size-80 rounded-full bg-violet-500/20 blur-3xl" />
            <div className="relative">
              <Badge className="gap-1.5 bg-white/10 text-brand-100 ring-white/10">
                <Sparkles className="size-3.5" />
                Featured event
              </Badge>
              <h2 className="mt-5 max-w-3xl text-3xl font-bold tracking-tight text-white sm:text-4xl">
                {featuredEvent.title}
              </h2>
              <p className="mt-4 max-w-2xl text-sm leading-7 text-slate-300 sm:text-base">
                {featuredEvent.description}
              </p>
              <div className="mt-5 flex flex-wrap gap-x-5 gap-y-2 text-sm text-slate-300">
                <span className="flex items-center gap-2">
                  <CalendarDays className="size-4 text-brand-300" />
                  {featuredEvent.dateLabel}
                </span>
                <span className="flex items-center gap-2">
                  <MapPin className="size-4 text-brand-300" />
                  {featuredEvent.venue}
                </span>
                <span className="flex items-center gap-2">
                  <UsersRound className="size-4 text-brand-300" />
                  {featuredEvent.organizer}
                </span>
              </div>
              <Button
                className="mt-7 bg-white text-slate-950 hover:bg-brand-50"
                onClick={() => toggleRegistration(featuredEvent)}
              >
                {registeredIds.has(featuredEvent.id)
                  ? "Registration confirmed"
                  : "Register now"}
                <ArrowRight className="size-4" />
              </Button>
            </div>
            <div className="relative rounded-2xl border border-white/10 bg-white/[0.07] p-5 backdrop-blur">
              <p className="text-xs font-bold uppercase tracking-[0.18em] text-brand-200">
                Event starts in
              </p>
              <div className="mt-4 grid grid-cols-4 gap-2 text-center">
                {[
                  { value: "03", label: "Days" },
                  { value: "08", label: "Hours" },
                  { value: "24", label: "Mins" },
                  { value: "16", label: "Secs" },
                ].map((item) => (
                  <div
                    className="rounded-xl bg-white/10 px-2 py-3"
                    key={item.label}
                  >
                    <p className="text-xl font-bold text-white sm:text-2xl">
                      {item.value}
                    </p>
                    <p className="mt-1 text-[10px] uppercase tracking-wide text-slate-400">
                      {item.label}
                    </p>
                  </div>
                ))}
              </div>
              <div className="mt-5 flex items-center justify-between border-t border-white/10 pt-4">
                <div>
                  <p className="text-xs text-slate-400">Registered</p>
                  <p className="mt-1 font-semibold text-white">
                    {featuredEvent.attendees} students
                  </p>
                </div>
                <Button
                  onClick={() => setSelectedEvent(featuredEvent)}
                  size="sm"
                  variant="secondary"
                >
                  View details
                </Button>
              </div>
            </div>
          </div>
        </Card>
      </section>

      <section>
        <SectionTitle
          description="Select an event date to focus the upcoming-event feed."
          title="Monthly calendar"
        />
        <div className="mt-4 grid items-start gap-4 xl:grid-cols-[1fr_22rem]">
          <Card>
            <CardContent className="p-4 sm:p-6">
              <div className="flex items-center justify-between">
                <Button
                  aria-label="Previous month"
                  onClick={() => moveCalendar(-1)}
                  size="icon"
                  variant="ghost"
                >
                  <ChevronLeft className="size-5" />
                </Button>
                <div className="text-center">
                  <h3 className="font-semibold text-slate-950">
                    {calendarLabel}
                  </h3>
                  <button
                    className="mt-1 text-xs font-semibold text-brand-600 hover:underline"
                    onClick={() => {
                      setCalendarMonth({ year: 2026, month: 5 });
                      setSelectedDate(TODAY_KEY);
                    }}
                    type="button"
                  >
                    Go to today
                  </button>
                </div>
                <Button
                  aria-label="Next month"
                  onClick={() => moveCalendar(1)}
                  size="icon"
                  variant="ghost"
                >
                  <ChevronRight className="size-5" />
                </Button>
              </div>

              <div className="mt-5 grid grid-cols-7 gap-1 sm:gap-2">
                {dayLabels.map((day) => (
                  <div
                    className="py-2 text-center text-[10px] font-bold uppercase tracking-wider text-slate-400 sm:text-xs"
                    key={day}
                  >
                    {day}
                  </div>
                ))}
                {cells.map((day, index) => {
                  if (!day) {
                    return (
                      <span
                        aria-hidden="true"
                        className="aspect-square"
                        key={`blank-${index}`}
                      />
                    );
                  }

                  const key = dateKey(
                    calendarMonth.year,
                    calendarMonth.month,
                    day,
                  );
                  const hasEvent = calendarEventDates.has(key);
                  const isToday = key === TODAY_KEY;
                  const isSelected = key === selectedDate;

                  return (
                    <button
                      aria-label={`${key}${hasEvent ? ", has events" : ""}`}
                      aria-pressed={isSelected}
                      className={cn(
                        "relative aspect-square rounded-xl text-xs font-semibold transition sm:text-sm",
                        isSelected
                          ? "bg-brand-600 text-white shadow-md"
                          : isToday
                            ? "bg-brand-50 text-brand-700 ring-1 ring-brand-300"
                            : "text-slate-600 hover:bg-slate-100",
                      )}
                      key={key}
                      onClick={() => setSelectedDate(key)}
                      type="button"
                    >
                      {day}
                      {hasEvent ? (
                        <span
                          className={cn(
                            "absolute bottom-1.5 left-1/2 size-1 -translate-x-1/2 rounded-full sm:bottom-2",
                            isSelected ? "bg-white" : "bg-brand-500",
                          )}
                        />
                      ) : null}
                    </button>
                  );
                })}
              </div>
            </CardContent>
          </Card>

          <div>
            <div className="mb-3 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-semibold text-slate-900">
                  {selectedDate ? "Selected date" : "Today’s events"}
                </h3>
                <p className="text-xs text-slate-500">{previewDate}</p>
              </div>
              {selectedDate ? (
                <button
                  className="text-xs font-semibold text-brand-700 hover:underline"
                  onClick={() => setSelectedDate(null)}
                  type="button"
                >
                  Clear date
                </button>
              ) : null}
            </div>
            {previewEvents.length > 0 ? (
              <div className="grid gap-3">
                {previewEvents.slice(0, 2).map((event) => (
                  <EventCard
                    event={{
                      title: event.title,
                      date: `${event.dateLabel} · ${event.time}`,
                      venue: event.venue,
                      organizer: event.organizer,
                      category: event.category,
                    }}
                    key={event.id}
                    onRsvp={() => toggleRegistration(event)}
                  />
                ))}
              </div>
            ) : (
              <EmptyState
                className="min-h-64"
                description="Choose a highlighted date or browse the full event feed below."
                icon={<CalendarDays className="size-6" />}
                title="No event on this date"
              />
            )}
          </div>
        </div>
      </section>

      <section>
        <SectionTitle
          description={`${filteredEvents.length} ${filteredEvents.length === 1 ? "event" : "events"} match your calendar view.`}
          title="Upcoming events"
        />
        {filteredEvents.length > 0 ? (
          <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {filteredEvents.map((event) => (
              <FullEventCard
                event={event}
                isRegistered={registeredIds.has(event.id)}
                isSaved={savedIds.has(event.id)}
                key={event.id}
                onRsvp={() => toggleRegistration(event)}
                onSave={() => toggleSaved(event)}
                onView={() => setSelectedEvent(event)}
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
            className="mt-4"
            description="Try another keyword, category, or calendar date to discover more campus experiences."
            icon={<SearchX className="size-6" />}
            title="No events found."
          />
        )}
      </section>

      <section>
        <SectionTitle
          description="Events currently reserved on your student calendar."
          title="My registered events"
        />
        {registeredEvents.length > 0 ? (
          <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {registeredEvents.map((event) => (
              <Card
                className="transition duration-200 hover:border-brand-200 hover:shadow-lg"
                key={event.id}
              >
                <CardContent className="flex items-center gap-4 p-4">
                  <span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-brand-50 text-center text-brand-700">
                    <span>
                      <span className="block text-[10px] font-bold uppercase">
                        {new Date(`${event.date}T00:00:00`).toLocaleString(
                          "en",
                          { month: "short" },
                        )}
                      </span>
                      <span className="block text-lg font-bold leading-5">
                        {new Date(`${event.date}T00:00:00`).getDate()}
                      </span>
                    </span>
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <Badge variant="success">Registered</Badge>
                      <span className="text-[11px] text-slate-400">
                        {event.time}
                      </span>
                    </div>
                    <h3 className="mt-2 truncate text-sm font-semibold text-slate-900">
                      {event.title}
                    </h3>
                    <p className="mt-1 truncate text-xs text-slate-500">
                      {event.venue}
                    </p>
                  </div>
                  <Button
                    onClick={() => toggleRegistration(event)}
                    size="sm"
                    variant="ghost"
                  >
                    Cancel
                  </Button>
                </CardContent>
              </Card>
            ))}
          </div>
        ) : (
          <EmptyState
            className="mt-4"
            description="RSVP to an upcoming event and it will appear on your student calendar."
            icon={<CalendarDays className="size-6" />}
            title="No registered events"
          />
        )}
      </section>

      <Modal
        description={
          selectedEvent
            ? `${selectedEvent.category} · ${selectedEvent.university}`
            : undefined
        }
        footer={
          selectedEvent ? (
            <>
              <Button
                onClick={() => toggleSaved(selectedEvent)}
                variant={
                  savedIds.has(selectedEvent.id) ? "secondary" : "outline"
                }
              >
                <Bookmark
                  className={cn(
                    "size-4",
                    savedIds.has(selectedEvent.id) && "fill-brand-600",
                  )}
                />
                {savedIds.has(selectedEvent.id) ? "Saved" : "Save"}
              </Button>
              <Button onClick={() => toggleRegistration(selectedEvent)}>
                {registeredIds.has(selectedEvent.id)
                  ? "Cancel RSVP"
                  : "RSVP now"}
              </Button>
            </>
          ) : undefined
        }
        isOpen={Boolean(selectedEvent)}
        onClose={() => setSelectedEvent(null)}
        size="xl"
        title="Event details"
      >
        {selectedEvent ? (
          <article>
            <div
              className={cn(
                "relative flex min-h-56 items-center justify-center overflow-hidden rounded-2xl bg-gradient-to-br text-white",
                bannerToneClasses[selectedEvent.tone],
              )}
            >
              {(() => {
                const EventIcon = eventCategoryIcons[selectedEvent.category];
                return <EventIcon className="size-24 opacity-70" />;
              })()}
              <Badge className="absolute left-4 top-4 bg-white/15 text-white ring-white/20">
                {selectedEvent.category}
              </Badge>
            </div>

            <h2 className="mt-6 text-2xl font-bold tracking-tight text-slate-950">
              {selectedEvent.title}
            </h2>
            <p className="mt-3 text-sm leading-7 text-slate-600">
              {selectedEvent.description}
            </p>

            <dl className="mt-6 grid gap-4 rounded-2xl bg-slate-50 p-4 text-sm sm:grid-cols-2 lg:grid-cols-4">
              <div>
                <dt className="text-xs font-medium text-slate-400">
                  Date and time
                </dt>
                <dd className="mt-1 font-semibold text-slate-800">
                  {selectedEvent.dateLabel}
                  <span className="block">{selectedEvent.time}</span>
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-400">Venue</dt>
                <dd className="mt-1 font-semibold text-slate-800">
                  {selectedEvent.venue}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-400">
                  Organizer
                </dt>
                <dd className="mt-1 font-semibold text-slate-800">
                  {selectedEvent.organizer}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-400">
                  Seats available
                </dt>
                <dd className="mt-1 font-semibold text-slate-800">
                  {Math.max(
                    selectedEvent.seats - selectedEvent.attendees,
                    0,
                  )}{" "}
                  of {selectedEvent.seats}
                </dd>
              </div>
            </dl>

            <div className="mt-6 grid gap-6 lg:grid-cols-2">
              <div>
                <h3 className="font-semibold text-slate-950">
                  Event schedule
                </h3>
                <ol className="mt-4 grid gap-3">
                  {selectedEvent.schedule.map((item, index) => (
                    <li className="flex gap-3" key={`${item.time}-${item.title}`}>
                      <span className="grid size-8 shrink-0 place-items-center rounded-lg bg-brand-50 text-xs font-bold text-brand-700">
                        {index + 1}
                      </span>
                      <div>
                        <p className="text-xs font-semibold text-brand-600">
                          {item.time}
                        </p>
                        <p className="mt-0.5 text-sm text-slate-700">
                          {item.title}
                        </p>
                      </div>
                    </li>
                  ))}
                </ol>
              </div>
              <div>
                <h3 className="font-semibold text-slate-950">
                  Speakers & hosts
                </h3>
                <div className="mt-4 grid gap-3">
                  {selectedEvent.speakers.map((speaker) => (
                    <div
                      className="flex items-center gap-3 rounded-xl border border-slate-200 p-3"
                      key={speaker.name}
                    >
                      <Avatar name={speaker.name} size="sm" />
                      <div>
                        <p className="text-sm font-semibold text-slate-800">
                          {speaker.name}
                        </p>
                        <p className="text-xs text-slate-500">
                          {speaker.role}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </article>
        ) : null}
      </Modal>

      <Modal
        description="Publish a campus experience for students to discover and join."
        footer={
          <>
            <Button
              onClick={() => {
                setIsCreateOpen(false);
                resetCreateForm();
              }}
              type="button"
              variant="ghost"
            >
              Cancel
            </Button>
            <Button form="create-event-form" type="submit">
              <CalendarDays className="size-4" />
              Create event
            </Button>
          </>
        }
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        size="xl"
        title="Create an event"
      >
        <form
          className="grid gap-5"
          id="create-event-form"
          noValidate
          onSubmit={handleCreateEvent}
        >
          <FormField
            error={createErrors.title}
            label="Event title"
            onChange={(event) =>
              updateCreateField("title", event.target.value)
            }
            placeholder="e.g. Campus Web Development Workshop"
            required
            value={createForm.title}
          />

          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              error={createErrors.category}
              label="Category"
              onChange={(event) =>
                updateCreateField("category", event.target.value)
              }
              options={createEventCategoryOptions}
              required
              value={createForm.category}
            />
            <FormField
              error={createErrors.organizer}
              label="Organizer"
              onChange={(event) =>
                updateCreateField("organizer", event.target.value)
              }
              placeholder="Society, department, or organizer"
              required
              value={createForm.organizer}
            />
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <FormField
              error={createErrors.date}
              label="Date"
              min="2026-06-30"
              onChange={(event) =>
                updateCreateField("date", event.target.value)
              }
              required
              type="date"
              value={createForm.date}
            />
            <FormField
              error={createErrors.time}
              label="Time"
              onChange={(event) =>
                updateCreateField("time", event.target.value)
              }
              required
              type="time"
              value={createForm.time}
            />
          </div>

          <FormField
            error={createErrors.venue}
            label="Venue"
            onChange={(event) =>
              updateCreateField("venue", event.target.value)
            }
            placeholder="Building, room, or campus location"
            required
            value={createForm.venue}
          />

          <div className="grid gap-1.5">
            <label
              className="text-sm font-semibold text-slate-700"
              htmlFor="event-description"
            >
              Description
              <span aria-hidden="true" className="ml-1 text-red-500">
                *
              </span>
            </label>
            <textarea
              aria-describedby={
                createErrors.description
                  ? "event-description-error"
                  : undefined
              }
              aria-invalid={Boolean(createErrors.description)}
              className={cn(
                "min-h-36 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm leading-6 text-slate-950 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:ring-4",
                createErrors.description
                  ? "border-red-300 focus:border-red-400 focus:ring-red-100"
                  : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
              )}
              id="event-description"
              onChange={(event) =>
                updateCreateField("description", event.target.value)
              }
              placeholder="Describe the event, what students will experience, and who should attend."
              value={createForm.description}
            />
            {createErrors.description ? (
              <p
                className="text-xs font-medium text-red-600"
                id="event-description-error"
              >
                {createErrors.description}
              </p>
            ) : null}
          </div>

          <div className="grid gap-1.5">
            <span className="text-sm font-semibold text-slate-700">
              Event banner
              <span aria-hidden="true" className="ml-1 text-red-500">
                *
              </span>
            </span>
            <div
              className={cn(
                "flex flex-col items-center justify-center rounded-2xl border border-dashed p-6 text-center transition",
                createErrors.banner
                  ? "border-red-300 bg-red-50/40"
                  : selectedBanner
                    ? "border-emerald-300 bg-emerald-50/50"
                    : "border-slate-300 bg-slate-50 hover:border-brand-300 hover:bg-brand-50/30",
              )}
            >
              <span className="grid size-12 place-items-center rounded-2xl bg-white text-brand-600 shadow-sm">
                <UploadCloud className="size-5" />
              </span>
              <p className="mt-3 text-sm font-semibold text-slate-800">
                {selectedBanner || "Choose a banner for this demo"}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                No image is uploaded or stored.
              </p>
              <Button
                className="mt-4"
                onClick={() => {
                  setSelectedBanner("campus-event-banner.jpg");
                  setCreateErrors((current) => ({
                    ...current,
                    banner: undefined,
                  }));
                }}
                size="sm"
                type="button"
                variant="outline"
              >
                Select demo banner
              </Button>
            </div>
            {createErrors.banner ? (
              <p className="text-xs font-medium text-red-600">
                {createErrors.banner}
              </p>
            ) : null}
          </div>
        </form>
      </Modal>
    </div>
  );
}
