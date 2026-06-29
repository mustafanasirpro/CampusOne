import {
  Armchair,
  ArrowRight,
  BedDouble,
  Bike,
  BookOpen,
  Calculator,
  Camera,
  CheckCircle2,
  Eye,
  Headphones,
  Heart,
  ImagePlus,
  Laptop,
  MapPin,
  Package,
  SearchX,
  ShoppingBag,
  SlidersHorizontal,
  Star,
  UploadCloud,
  UserRound,
} from "lucide-react";
import { useMemo, useState, type FormEvent } from "react";

import { ProductCard, StatCard } from "@/components/cards";
import {
  Avatar,
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
  marketplaceCategories,
  marketplaceCategoryOptions,
  marketplaceConditionOptions,
  marketplaceContactOptions,
  marketplaceDepartmentOptions,
  marketplaceListings,
  marketplaceLocationOptions,
  marketplacePriceOptions,
  marketplaceSortOptions,
  marketplaceStats,
  marketplaceUniversityOptions,
  type MarketplaceCategory,
  type MarketplaceCondition,
  type MarketplaceListing,
} from "@/data/marketplace";
import { cn } from "@/utils/cn";
import { useDocumentTitle } from "@/utils/useDocumentTitle";

type MarketplaceSort = "newest" | "price-low" | "price-high";

interface MarketplaceFilters {
  category: string;
  condition: string;
  department: string;
  location: string;
  price: string;
  university: string;
}

interface ListingForm {
  category: string;
  condition: string;
  contactMethod: string;
  description: string;
  location: string;
  price: string;
  title: string;
  university: string;
}

type ListingErrors = Partial<Record<keyof ListingForm | "image", string>>;

const initialFilters: MarketplaceFilters = {
  university: "all",
  department: "all",
  category: "all",
  condition: "all",
  price: "all",
  location: "all",
};

const initialListingForm: ListingForm = {
  title: "",
  category: "",
  price: "",
  condition: "",
  description: "",
  university: "",
  location: "",
  contactMethod: "",
};

const categoryIcons = {
  Books: BookOpen,
  Electronics: Laptop,
  Calculators: Calculator,
  "Hostel Items": BedDouble,
  Furniture: Armchair,
  Bikes: Bike,
  Accessories: Headphones,
  Others: Package,
};

const categoryToneClasses = {
  Books: "bg-brand-50 text-brand-600",
  Electronics: "bg-sky-50 text-sky-600",
  Calculators: "bg-emerald-50 text-emerald-600",
  "Hostel Items": "bg-violet-50 text-violet-600",
  Furniture: "bg-amber-50 text-amber-600",
  Bikes: "bg-rose-50 text-rose-600",
  Accessories: "bg-fuchsia-50 text-fuchsia-600",
  Others: "bg-slate-100 text-slate-600",
};

const imageToneClasses = {
  brand: "from-brand-50 to-brand-100 text-brand-500",
  emerald: "from-emerald-50 to-emerald-100 text-emerald-500",
  amber: "from-amber-50 to-amber-100 text-amber-500",
  sky: "from-sky-50 to-sky-100 text-sky-500",
  rose: "from-rose-50 to-rose-100 text-rose-500",
  violet: "from-violet-50 to-violet-100 text-violet-500",
};

function formatPrice(price: number) {
  return `Rs. ${price.toLocaleString("en-PK")}`;
}

function matchesPriceRange(price: number, range: string) {
  if (range === "under-1000") return price < 1000;
  if (range === "1000-5000") return price >= 1000 && price <= 5000;
  if (range === "5000-20000") return price > 5000 && price <= 20000;
  if (range === "above-20000") return price > 20000;
  return true;
}

interface MarketplaceProductCardProps {
  isSaved: boolean;
  listing: MarketplaceListing;
  onContact: () => void;
  onSave: () => void;
  onView: () => void;
}

function MarketplaceProductCard({
  isSaved,
  listing,
  onContact,
  onSave,
  onView,
}: MarketplaceProductCardProps) {
  const CategoryIcon = categoryIcons[listing.category];

  return (
    <Card className="group flex h-full flex-col overflow-hidden transition duration-200 hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl">
      <div
        className={cn(
          "relative grid aspect-[4/3] place-items-center overflow-hidden bg-gradient-to-br",
          imageToneClasses[listing.tone],
        )}
      >
        <div className="absolute -right-10 -top-10 size-40 rounded-full bg-white/45" />
        <div className="absolute -bottom-16 -left-12 size-48 rounded-full bg-white/35" />
        <CategoryIcon className="relative size-16 opacity-80 transition duration-300 group-hover:scale-110" />
        <Badge className="absolute left-3 top-3 bg-white/85 text-slate-700 ring-white/80 backdrop-blur">
          {listing.category}
        </Badge>
        <span className="absolute bottom-3 left-3 rounded-lg bg-slate-950/70 px-2 py-1 text-[11px] font-medium text-white backdrop-blur">
          {listing.postedTime}
        </span>
        <Button
          aria-label={isSaved ? "Remove from wishlist" : "Add to wishlist"}
          className="absolute right-3 top-3 bg-white/90 shadow-sm hover:bg-white"
          onClick={onSave}
          size="icon"
          variant="ghost"
        >
          <Heart
            className={cn(
              "size-4.5",
              isSaved && "fill-rose-500 text-rose-500",
            )}
          />
        </Button>
      </div>

      <CardContent className="flex flex-1 flex-col p-5">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h3 className="line-clamp-2 font-semibold leading-6 text-slate-950">
              {listing.title}
            </h3>
            <p className="mt-2 text-xl font-bold tracking-tight text-brand-700">
              {formatPrice(listing.price)}
            </p>
          </div>
          <Badge variant={listing.condition === "New" ? "success" : "neutral"}>
            {listing.condition}
          </Badge>
        </div>

        <div className="mt-4 flex items-center gap-3">
          <Avatar name={listing.seller.name} size="sm" />
          <div className="min-w-0">
            <p className="flex items-center gap-1 truncate text-sm font-medium text-slate-800">
              {listing.seller.name}
              {listing.seller.verified ? (
                <CheckCircle2 className="size-3.5 shrink-0 text-brand-500" />
              ) : null}
            </p>
            <p className="truncate text-xs text-slate-400">
              {listing.university}
            </p>
          </div>
        </div>

        <div className="mt-4 flex items-center gap-1.5 text-xs text-slate-500">
          <MapPin className="size-3.5" />
          {listing.location}
          <span className="mx-1 text-slate-300">•</span>
          {listing.department}
        </div>

        <div className="mt-auto grid grid-cols-2 gap-2 pt-5">
          <Button onClick={onView} variant="outline">
            <Eye className="size-4" />
            Details
          </Button>
          <Button onClick={onContact}>
            <UserRound className="size-4" />
            Contact
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

interface MarketplaceFilterControlsProps {
  filters: MarketplaceFilters;
  onChange: (field: keyof MarketplaceFilters, value: string) => void;
  onClear: () => void;
  showClear: boolean;
}

function MarketplaceFilterControls({
  filters,
  onChange,
  onClear,
  showClear,
}: MarketplaceFilterControlsProps) {
  const fields = [
    {
      key: "university" as const,
      label: "University",
      options: marketplaceUniversityOptions,
    },
    {
      key: "department" as const,
      label: "Department",
      options: marketplaceDepartmentOptions,
    },
    {
      key: "category" as const,
      label: "Category",
      options: marketplaceCategoryOptions,
    },
    {
      key: "condition" as const,
      label: "Condition",
      options: marketplaceConditionOptions,
    },
    {
      key: "price" as const,
      label: "Price range",
      options: marketplacePriceOptions,
    },
    {
      key: "location" as const,
      label: "Location",
      options: marketplaceLocationOptions,
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

export function MarketplacePage() {
  const [listings, setListings] = useState(marketplaceListings);
  const [searchValue, setSearchValue] = useState("");
  const [filters, setFilters] =
    useState<MarketplaceFilters>(initialFilters);
  const [sortBy, setSortBy] = useState<MarketplaceSort>("newest");
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [savedIds, setSavedIds] = useState<Set<string>>(
    new Set(["hp-elitebook"]),
  );
  const [selectedListing, setSelectedListing] =
    useState<MarketplaceListing | null>(null);
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [listingForm, setListingForm] =
    useState<ListingForm>(initialListingForm);
  const [listingErrors, setListingErrors] = useState<ListingErrors>({});
  const [selectedImage, setSelectedImage] = useState("");
  const { showToast } = useToast();

  useDocumentTitle("Student Marketplace · CampusOne");

  const activeFilterCount =
    Object.values(filters).filter((value) => value !== "all").length +
    (searchValue.trim() ? 1 : 0);

  const filteredListings = useMemo(() => {
    const query = searchValue.trim().toLowerCase();
    const matches = listings.filter((listing) => {
      const searchableText = [
        listing.title,
        listing.category,
        listing.description,
        listing.seller.name,
        listing.university,
        listing.department,
        listing.location,
      ]
        .join(" ")
        .toLowerCase();

      return (
        (!query || searchableText.includes(query)) &&
        (filters.university === "all" ||
          listing.university === filters.university) &&
        (filters.department === "all" ||
          listing.department === filters.department) &&
        (filters.category === "all" ||
          listing.category === filters.category) &&
        (filters.condition === "all" ||
          listing.condition === filters.condition) &&
        matchesPriceRange(listing.price, filters.price) &&
        (filters.location === "all" ||
          listing.location === filters.location)
      );
    });

    return [...matches].sort((first, second) => {
      if (sortBy === "price-low") return first.price - second.price;
      if (sortBy === "price-high") return second.price - first.price;
      return second.postedAt.localeCompare(first.postedAt);
    });
  }, [filters, listings, searchValue, sortBy]);

  const featuredListings = useMemo(
    () => listings.filter((listing) => listing.featured).slice(0, 3),
    [listings],
  );
  const recentListings = useMemo(
    () =>
      [...listings]
        .sort((a, b) => b.postedAt.localeCompare(a.postedAt))
        .slice(0, 4),
    [listings],
  );

  const updateFilter = (
    field: keyof MarketplaceFilters,
    value: string,
  ) => {
    setFilters((current) => ({ ...current, [field]: value }));
  };

  const clearFilters = () => {
    setFilters(initialFilters);
    setSearchValue("");
  };

  const toggleSaved = (listing: MarketplaceListing) => {
    const isSaved = savedIds.has(listing.id);
    setSavedIds((current) => {
      const next = new Set(current);
      if (next.has(listing.id)) next.delete(listing.id);
      else next.add(listing.id);
      return next;
    });
    showToast({
      title: isSaved ? "Removed from wishlist" : "Added to wishlist",
      message: listing.title,
      variant: isSaved ? "info" : "success",
    });
  };

  const contactSeller = (listing: MarketplaceListing) => {
    showToast({
      title: `Contact ${listing.seller.name}`,
      message: `${listing.contactMethod} will open when messaging is connected.`,
      variant: "success",
    });
  };

  const updateListingField = (field: keyof ListingForm, value: string) => {
    setListingForm((current) => ({ ...current, [field]: value }));
    setListingErrors((current) => ({ ...current, [field]: undefined }));
  };

  const resetListingForm = () => {
    setListingForm(initialListingForm);
    setListingErrors({});
    setSelectedImage("");
  };

  const handlePublishListing = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors: ListingErrors = {};
    const numericPrice = Number(listingForm.price);

    if (listingForm.title.trim().length < 5) {
      nextErrors.title = "Enter a clear product title.";
    }
    if (!listingForm.category) nextErrors.category = "Select a category.";
    if (!numericPrice || numericPrice < 1) {
      nextErrors.price = "Enter a valid price.";
    }
    if (!listingForm.condition) {
      nextErrors.condition = "Select the item condition.";
    }
    if (listingForm.description.trim().length < 20) {
      nextErrors.description = "Add at least 20 characters.";
    }
    if (!listingForm.university) {
      nextErrors.university = "Select a university.";
    }
    if (!listingForm.location) {
      nextErrors.location = "Select a location.";
    }
    if (!selectedImage) nextErrors.image = "Choose a demo image.";
    if (!listingForm.contactMethod) {
      nextErrors.contactMethod = "Select a contact method.";
    }

    if (Object.keys(nextErrors).length > 0) {
      setListingErrors(nextErrors);
      return;
    }

    const newListing: MarketplaceListing = {
      id: `listing-${Date.now()}`,
      title: listingForm.title.trim(),
      category: listingForm.category as MarketplaceCategory,
      price: numericPrice,
      condition: listingForm.condition as MarketplaceCondition,
      description: listingForm.description.trim(),
      university: listingForm.university,
      department: "Computer Science",
      location: listingForm.location,
      postedAt: new Date().toISOString(),
      postedDate: "Just now",
      postedTime: "Just now",
      contactMethod: listingForm.contactMethod,
      featured: false,
      tone: "brand",
      seller: {
        name: "Ali Khan",
        department: "Computer Science",
        university: listingForm.university,
        rating: 5,
        listingsCount: 1,
        verified: true,
      },
    };

    setListings((current) => [newListing, ...current]);
    setIsAddOpen(false);
    clearFilters();
    resetListingForm();
    showToast({
      title: "Listing published",
      message: "Your item now appears in the marketplace demo.",
      variant: "success",
    });
  };

  const addFormOptions = {
    category: [
      { label: "Select category", value: "", disabled: true },
      ...marketplaceCategoryOptions.slice(1),
    ],
    condition: [
      { label: "Select condition", value: "", disabled: true },
      ...marketplaceConditionOptions.slice(1),
    ],
    university: [
      { label: "Select university", value: "", disabled: true },
      ...marketplaceUniversityOptions.slice(1),
    ],
    location: [
      { label: "Select location", value: "", disabled: true },
      ...marketplaceLocationOptions.slice(1),
    ],
  };

  return (
    <div className="grid gap-8 pb-8">
      <PageHeader
        actions={
          <Button onClick={() => setIsAddOpen(true)}>
            <ShoppingBag className="size-4" />
            Add listing
          </Button>
        }
        description="Buy and sell useful items safely within trusted university communities."
        eyebrow="Campus commerce"
        title="Student Marketplace"
      />

      <SearchBar
        className="max-w-3xl"
        onSearch={setSearchValue}
        onValueChange={setSearchValue}
        placeholder="Search books, calculators, laptops, hostel items..."
        value={searchValue}
      />

      <section aria-labelledby="marketplace-stats">
        <h2 className="sr-only" id="marketplace-stats">
          Marketplace statistics
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {marketplaceStats.map((stat) => (
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

      <Card>
        <CardContent className="p-5">
          <SectionTitle
            description="Browse the items students trade most often."
            title="Shop by category"
          />
          <div className="mt-4 flex gap-2 overflow-x-auto pb-1">
            {marketplaceCategories.map((category) => {
              const isAll = category === "All";
              const value = isAll ? "all" : category;
              const isActive = filters.category === value;
              const Icon = isAll ? ShoppingBag : categoryIcons[category];

              return (
                <button
                  aria-pressed={isActive}
                  className={cn(
                    "flex shrink-0 items-center gap-2 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition",
                    isActive
                      ? "bg-brand-600 text-white shadow-sm"
                      : "bg-slate-50 text-slate-600 hover:-translate-y-0.5 hover:bg-brand-50 hover:text-brand-700",
                  )}
                  key={category}
                  onClick={() => updateFilter("category", value)}
                  type="button"
                >
                  <Icon className="size-4" />
                  {category}
                </button>
              );
            })}
          </div>
        </CardContent>
      </Card>

      <section>
        <SectionTitle
          description="Popular items from trusted student sellers."
          title="Featured listings"
        />
        <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {featuredListings.map((listing) => (
            <div
              className="transition duration-200 hover:-translate-y-1 [&>div]:h-full [&>div]:transition-shadow [&>div]:hover:shadow-xl"
              key={listing.id}
            >
              <ProductCard
                onContact={() => contactSeller(listing)}
                product={{
                  title: listing.title,
                  price: formatPrice(listing.price),
                  condition: listing.condition,
                  location: listing.location,
                  seller: listing.seller.name,
                }}
              />
            </div>
          ))}
        </div>
      </section>

      <section>
        <SectionTitle
          description="Fresh items posted by students today."
          title="Recently added"
        />
        <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {recentListings.map((listing) => {
            const CategoryIcon = categoryIcons[listing.category];

            return (
              <Card
                className="group transition duration-200 hover:-translate-y-1 hover:border-brand-200 hover:shadow-lg"
                key={listing.id}
              >
                <CardContent className="flex items-center gap-3 p-4">
                  <span
                    className={cn(
                      "grid size-11 shrink-0 place-items-center rounded-xl",
                      categoryToneClasses[listing.category],
                    )}
                  >
                    <CategoryIcon className="size-5" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <h3 className="truncate text-sm font-semibold text-slate-900">
                      {listing.title}
                    </h3>
                    <p className="mt-0.5 text-sm font-bold text-brand-700">
                      {formatPrice(listing.price)}
                    </p>
                    <p className="mt-0.5 text-[11px] text-slate-400">
                      {listing.postedTime}
                    </p>
                  </div>
                  <Button
                    aria-label={`View ${listing.title}`}
                    onClick={() => setSelectedListing(listing)}
                    size="icon"
                    variant="ghost"
                  >
                    <ArrowRight className="size-4" />
                  </Button>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </section>

      <section>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <SectionTitle
            description={`${filteredListings.length} ${filteredListings.length === 1 ? "listing" : "listings"} match your marketplace view.`}
            title="Explore marketplace"
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
              aria-label="Sort marketplace"
              onChange={(event) =>
                setSortBy(event.target.value as MarketplaceSort)
              }
              options={marketplaceSortOptions}
              value={sortBy}
            />
          </div>
        </div>

        {filtersOpen ? (
          <div className="mt-4 md:hidden">
            <MarketplaceFilterControls
              filters={filters}
              onChange={updateFilter}
              onClear={clearFilters}
              showClear={activeFilterCount > 0}
            />
          </div>
        ) : null}
        <div className="mt-4 hidden md:block">
          <MarketplaceFilterControls
            filters={filters}
            onChange={updateFilter}
            onClear={clearFilters}
            showClear={activeFilterCount > 0}
          />
        </div>

        {filteredListings.length > 0 ? (
          <div className="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
            {filteredListings.map((listing) => (
              <MarketplaceProductCard
                isSaved={savedIds.has(listing.id)}
                key={listing.id}
                listing={listing}
                onContact={() => contactSeller(listing)}
                onSave={() => toggleSaved(listing)}
                onView={() => setSelectedListing(listing)}
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
            description="Try another search, category, university, or price range to discover more student listings."
            icon={<SearchX className="size-6" />}
            title="No listings found."
          />
        )}
      </section>

      <Modal
        description={
          selectedListing
            ? `${selectedListing.category} · ${selectedListing.location}`
            : undefined
        }
        isOpen={Boolean(selectedListing)}
        onClose={() => setSelectedListing(null)}
        size="xl"
        title="Product details"
      >
        {selectedListing ? (
          <div className="grid gap-6 lg:grid-cols-[1fr_1.05fr]">
            <div
              className={cn(
                "relative grid min-h-72 place-items-center overflow-hidden rounded-2xl bg-gradient-to-br",
                imageToneClasses[selectedListing.tone],
              )}
            >
              {(() => {
                const ProductIcon = categoryIcons[selectedListing.category];
                return <ProductIcon className="size-24 opacity-80" />;
              })()}
              <Badge className="absolute left-4 top-4 bg-white/90 text-slate-700 ring-white">
                {selectedListing.category}
              </Badge>
            </div>

            <div>
              <div className="flex flex-wrap items-center gap-2">
                <Badge
                  variant={
                    selectedListing.condition === "New"
                      ? "success"
                      : "neutral"
                  }
                >
                  {selectedListing.condition}
                </Badge>
                <span className="text-xs text-slate-400">
                  Posted {selectedListing.postedDate}
                </span>
              </div>
              <h2 className="mt-4 text-2xl font-bold tracking-tight text-slate-950">
                {selectedListing.title}
              </h2>
              <p className="mt-3 text-3xl font-bold text-brand-700">
                {formatPrice(selectedListing.price)}
              </p>
              <p className="mt-4 text-sm leading-7 text-slate-600">
                {selectedListing.description}
              </p>
              <p className="mt-4 flex items-center gap-2 text-sm text-slate-500">
                <MapPin className="size-4 text-brand-500" />
                {selectedListing.location} · {selectedListing.university}
              </p>

              <Card className="mt-5 bg-slate-50 shadow-none">
                <CardContent className="p-4">
                  <div className="flex items-start gap-3">
                    <Avatar name={selectedListing.seller.name} size="lg" />
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-1.5">
                        <h3 className="font-semibold text-slate-900">
                          {selectedListing.seller.name}
                        </h3>
                        {selectedListing.seller.verified ? (
                          <Badge variant="brand">Verified seller</Badge>
                        ) : null}
                      </div>
                      <p className="mt-1 text-xs text-slate-500">
                        {selectedListing.seller.department} ·{" "}
                        {selectedListing.seller.university}
                      </p>
                      <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-slate-500">
                        <span className="flex items-center gap-1 font-semibold text-amber-600">
                          <Star className="size-3.5 fill-amber-400 text-amber-400" />
                          {selectedListing.seller.rating}
                        </span>
                        <span>
                          {selectedListing.seller.listingsCount} listings
                        </span>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <div className="mt-5 grid grid-cols-2 gap-3">
                <Button
                  onClick={() => toggleSaved(selectedListing)}
                  variant={
                    savedIds.has(selectedListing.id)
                      ? "secondary"
                      : "outline"
                  }
                >
                  <Heart
                    className={cn(
                      "size-4",
                      savedIds.has(selectedListing.id) &&
                        "fill-rose-500 text-rose-500",
                    )}
                  />
                  {savedIds.has(selectedListing.id) ? "Saved" : "Save"}
                </Button>
                <Button onClick={() => contactSeller(selectedListing)}>
                  <UserRound className="size-4" />
                  Contact seller
                </Button>
              </div>
            </div>
          </div>
        ) : null}
      </Modal>

      <Modal
        description="Create a trusted listing for students in your university community."
        footer={
          <>
            <Button
              onClick={() => {
                setIsAddOpen(false);
                resetListingForm();
              }}
              type="button"
              variant="ghost"
            >
              Cancel
            </Button>
            <Button form="add-marketplace-listing" type="submit">
              <ShoppingBag className="size-4" />
              Publish listing
            </Button>
          </>
        }
        isOpen={isAddOpen}
        onClose={() => setIsAddOpen(false)}
        size="xl"
        title="Add marketplace listing"
      >
        <form
          className="grid gap-5"
          id="add-marketplace-listing"
          noValidate
          onSubmit={handlePublishListing}
        >
          <FormField
            error={listingErrors.title}
            label="Product title"
            onChange={(event) =>
              updateListingField("title", event.target.value)
            }
            placeholder="e.g. Casio Scientific Calculator"
            required
            value={listingForm.title}
          />

          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              error={listingErrors.category}
              label="Category"
              onChange={(event) =>
                updateListingField("category", event.target.value)
              }
              options={addFormOptions.category}
              required
              value={listingForm.category}
            />
            <FormField
              error={listingErrors.price}
              label="Price (PKR)"
              min="1"
              onChange={(event) =>
                updateListingField("price", event.target.value)
              }
              placeholder="4500"
              required
              type="number"
              value={listingForm.price}
            />
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              error={listingErrors.condition}
              label="Condition"
              onChange={(event) =>
                updateListingField("condition", event.target.value)
              }
              options={addFormOptions.condition}
              required
              value={listingForm.condition}
            />
            <SelectField
              error={listingErrors.university}
              label="University"
              onChange={(event) =>
                updateListingField("university", event.target.value)
              }
              options={addFormOptions.university}
              required
              value={listingForm.university}
            />
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <SelectField
              error={listingErrors.location}
              label="Location"
              onChange={(event) =>
                updateListingField("location", event.target.value)
              }
              options={addFormOptions.location}
              required
              value={listingForm.location}
            />
            <SelectField
              error={listingErrors.contactMethod}
              label="Contact method"
              onChange={(event) =>
                updateListingField("contactMethod", event.target.value)
              }
              options={marketplaceContactOptions}
              required
              value={listingForm.contactMethod}
            />
          </div>

          <div className="grid gap-1.5">
            <label
              className="text-sm font-semibold text-slate-700"
              htmlFor="listing-description"
            >
              Description
              <span aria-hidden="true" className="ml-1 text-red-500">
                *
              </span>
            </label>
            <textarea
              aria-describedby={
                listingErrors.description
                  ? "listing-description-error"
                  : undefined
              }
              aria-invalid={Boolean(listingErrors.description)}
              className={cn(
                "min-h-32 w-full resize-y rounded-xl border bg-white px-3.5 py-3 text-sm leading-6 text-slate-950 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:ring-4",
                listingErrors.description
                  ? "border-red-300 focus:border-red-400 focus:ring-red-100"
                  : "border-slate-200 focus:border-brand-400 focus:ring-brand-100",
              )}
              id="listing-description"
              onChange={(event) =>
                updateListingField("description", event.target.value)
              }
              placeholder="Describe the item, its condition, and anything a buyer should know."
              value={listingForm.description}
            />
            {listingErrors.description ? (
              <p
                className="text-xs font-medium text-red-600"
                id="listing-description-error"
              >
                {listingErrors.description}
              </p>
            ) : null}
          </div>

          <div className="grid gap-1.5">
            <span className="text-sm font-semibold text-slate-700">
              Product image
              <span aria-hidden="true" className="ml-1 text-red-500">
                *
              </span>
            </span>
            <div
              className={cn(
                "flex flex-col items-center justify-center rounded-2xl border border-dashed p-6 text-center transition",
                listingErrors.image
                  ? "border-red-300 bg-red-50/40"
                  : selectedImage
                    ? "border-emerald-300 bg-emerald-50/50"
                    : "border-slate-300 bg-slate-50 hover:border-brand-300 hover:bg-brand-50/30",
              )}
            >
              <span
                className={cn(
                  "grid size-12 place-items-center rounded-2xl",
                  selectedImage
                    ? "bg-emerald-100 text-emerald-700"
                    : "bg-white text-brand-600 shadow-sm",
                )}
              >
                {selectedImage ? (
                  <Camera className="size-5" />
                ) : (
                  <ImagePlus className="size-5" />
                )}
              </span>
              <p className="mt-3 text-sm font-semibold text-slate-800">
                {selectedImage || "Add a product image for this demo"}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                No image is uploaded or stored.
              </p>
              <Button
                className="mt-4"
                onClick={() => {
                  setSelectedImage("campus-marketplace-item.jpg");
                  setListingErrors((current) => ({
                    ...current,
                    image: undefined,
                  }));
                }}
                size="sm"
                type="button"
                variant="outline"
              >
                <UploadCloud className="size-3.5" />
                Select demo image
              </Button>
            </div>
            {listingErrors.image ? (
              <p className="text-xs font-medium text-red-600">
                {listingErrors.image}
              </p>
            ) : null}
          </div>
        </form>
      </Modal>
    </div>
  );
}
