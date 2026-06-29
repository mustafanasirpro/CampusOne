import {
  BadgeCheck,
  CircleDollarSign,
  PackageCheck,
  ShoppingBag,
  type LucideIcon,
} from "lucide-react";

export type MarketplaceCategory =
  | "Books"
  | "Electronics"
  | "Calculators"
  | "Hostel Items"
  | "Furniture"
  | "Bikes"
  | "Accessories"
  | "Others";

export type MarketplaceCondition = "New" | "Like New" | "Used" | "Fair";

export interface MarketplaceSeller {
  department: string;
  listingsCount: number;
  name: string;
  rating: number;
  university: string;
  verified: boolean;
}

export interface MarketplaceListing {
  category: MarketplaceCategory;
  condition: MarketplaceCondition;
  contactMethod: string;
  department: string;
  description: string;
  featured: boolean;
  id: string;
  location: string;
  postedAt: string;
  postedDate: string;
  postedTime: string;
  price: number;
  seller: MarketplaceSeller;
  title: string;
  tone: "brand" | "emerald" | "amber" | "sky" | "rose" | "violet";
  university: string;
}

export const marketplaceStats: Array<{
  change: number;
  icon: LucideIcon;
  label: string;
  value: string;
}> = [
  {
    label: "Active Listings",
    value: "1,284",
    change: 14,
    icon: ShoppingBag,
  },
  {
    label: "Items Sold",
    value: "3,642",
    change: 11,
    icon: PackageCheck,
  },
  {
    label: "New Listings Today",
    value: "86",
    change: 23,
    icon: CircleDollarSign,
  },
  {
    label: "Verified Sellers",
    value: "918",
    change: 8,
    icon: BadgeCheck,
  },
];

export const marketplaceCategories = [
  "All",
  "Books",
  "Electronics",
  "Calculators",
  "Hostel Items",
  "Furniture",
  "Bikes",
  "Accessories",
  "Others",
] as const;

export const marketplaceListings: MarketplaceListing[] = [
  {
    id: "oop-book",
    title: "Object Oriented Programming in Java",
    category: "Books",
    price: 850,
    condition: "Used",
    description:
      "Clean copy of the OOP course textbook with a few useful highlights and handwritten notes in the design-pattern chapters. No missing pages.",
    university: "COMSATS",
    department: "Computer Science",
    location: "Islamabad",
    postedAt: "2026-06-29T14:30:00",
    postedDate: "June 29, 2026",
    postedTime: "35 min ago",
    contactMethod: "CampusOne Chat",
    featured: true,
    tone: "brand",
    seller: {
      name: "Sara Ahmed",
      department: "Software Engineering",
      university: "COMSATS Islamabad",
      rating: 4.9,
      listingsCount: 12,
      verified: true,
    },
  },
  {
    id: "casio-calculator",
    title: "Casio fx-991EX Scientific Calculator",
    category: "Calculators",
    price: 4500,
    condition: "Like New",
    description:
      "Original Casio calculator used for one semester. All functions and buttons work perfectly. Includes protective cover and original box.",
    university: "FAST",
    department: "Computer Science",
    location: "Islamabad",
    postedAt: "2026-06-29T13:55:00",
    postedDate: "June 29, 2026",
    postedTime: "1 hour ago",
    contactMethod: "WhatsApp",
    featured: true,
    tone: "emerald",
    seller: {
      name: "Hamza Raza",
      department: "Computer Science",
      university: "FAST Islamabad",
      rating: 4.8,
      listingsCount: 8,
      verified: true,
    },
  },
  {
    id: "hp-elitebook",
    title: "HP EliteBook 840 G6 — Core i5",
    category: "Electronics",
    price: 98000,
    condition: "Like New",
    description:
      "Core i5 8th generation, 16GB RAM, 512GB SSD, and excellent battery health. Ideal for programming, FYP work, and everyday university use.",
    university: "NUST",
    department: "Software Engineering",
    location: "Rawalpindi",
    postedAt: "2026-06-29T12:40:00",
    postedDate: "June 29, 2026",
    postedTime: "2 hours ago",
    contactMethod: "Phone",
    featured: true,
    tone: "sky",
    seller: {
      name: "Ayesha Malik",
      department: "Data Science",
      university: "NUST",
      rating: 5,
      listingsCount: 5,
      verified: true,
    },
  },
  {
    id: "study-table",
    title: "Compact Hostel Study Table",
    category: "Furniture",
    price: 3500,
    condition: "Used",
    description:
      "Solid wooden study table sized for a hostel room. Stable, clean, and easy to move. Pickup available near UET hostels.",
    university: "UET",
    department: "Computer Science",
    location: "Lahore",
    postedAt: "2026-06-29T10:20:00",
    postedDate: "June 29, 2026",
    postedTime: "4 hours ago",
    contactMethod: "CampusOne Chat",
    featured: false,
    tone: "amber",
    seller: {
      name: "Bilal Ahmed",
      department: "Computer Science",
      university: "UET Lahore",
      rating: 4.7,
      listingsCount: 9,
      verified: true,
    },
  },
  {
    id: "roadmaster-bicycle",
    title: "Roadmaster Campus Bicycle",
    category: "Bikes",
    price: 18500,
    condition: "Used",
    description:
      "Well-maintained bicycle with recently serviced brakes and new rear tyre. Comfortable for daily travel around campus and nearby hostels.",
    university: "LUMS",
    department: "Economics",
    location: "Lahore",
    postedAt: "2026-06-28T18:15:00",
    postedDate: "June 28, 2026",
    postedTime: "Yesterday",
    contactMethod: "WhatsApp",
    featured: false,
    tone: "rose",
    seller: {
      name: "Maham Iqbal",
      department: "Economics",
      university: "LUMS",
      rating: 4.8,
      listingsCount: 6,
      verified: true,
    },
  },
  {
    id: "headphones",
    title: "Anker Soundcore Headphones",
    category: "Accessories",
    price: 5200,
    condition: "Like New",
    description:
      "Comfortable over-ear headphones with clear sound and strong battery life. Used lightly for online lectures and study sessions.",
    university: "PU",
    department: "Data Science",
    location: "Lahore",
    postedAt: "2026-06-28T15:45:00",
    postedDate: "June 28, 2026",
    postedTime: "Yesterday",
    contactMethod: "CampusOne Chat",
    featured: false,
    tone: "violet",
    seller: {
      name: "Noor Fatima",
      department: "Data Science",
      university: "Punjab University",
      rating: 4.6,
      listingsCount: 4,
      verified: false,
    },
  },
  {
    id: "mini-fridge",
    title: "Dawlance Mini Fridge for Hostel",
    category: "Hostel Items",
    price: 16000,
    condition: "Used",
    description:
      "Energy-efficient mini fridge in good working condition. Fits under a hostel desk and has a small freezer compartment.",
    university: "FAST",
    department: "Artificial Intelligence",
    location: "Islamabad",
    postedAt: "2026-06-27T12:30:00",
    postedDate: "June 27, 2026",
    postedTime: "2 days ago",
    contactMethod: "Phone",
    featured: false,
    tone: "sky",
    seller: {
      name: "Hira Javed",
      department: "Artificial Intelligence",
      university: "FAST Islamabad",
      rating: 4.9,
      listingsCount: 11,
      verified: true,
    },
  },
  {
    id: "drawing-set",
    title: "Engineering Drawing Instrument Set",
    category: "Others",
    price: 900,
    condition: "Like New",
    description:
      "Complete engineering drawing set with compass, divider, scale, protractor, and carrying case. Used for one course only.",
    university: "UET",
    department: "Software Engineering",
    location: "Lahore",
    postedAt: "2026-06-26T09:10:00",
    postedDate: "June 26, 2026",
    postedTime: "3 days ago",
    contactMethod: "CampusOne Chat",
    featured: false,
    tone: "emerald",
    seller: {
      name: "Zain Hassan",
      department: "Software Engineering",
      university: "UET Lahore",
      rating: 4.5,
      listingsCount: 3,
      verified: false,
    },
  },
  {
    id: "dsa-book",
    title: "Data Structures & Algorithms Book",
    category: "Books",
    price: 1200,
    condition: "Used",
    description:
      "International edition covering arrays, linked lists, trees, graphs, algorithms, and complexity with many solved examples.",
    university: "COMSATS",
    department: "Computer Science",
    location: "Rawalpindi",
    postedAt: "2026-06-25T16:00:00",
    postedDate: "June 25, 2026",
    postedTime: "4 days ago",
    contactMethod: "WhatsApp",
    featured: false,
    tone: "brand",
    seller: {
      name: "Ali Raza",
      department: "Computer Science",
      university: "COMSATS Islamabad",
      rating: 4.7,
      listingsCount: 7,
      verified: true,
    },
  },
  {
    id: "laptop-stand",
    title: "Adjustable Aluminium Laptop Stand",
    category: "Accessories",
    price: 1800,
    condition: "New",
    description:
      "Foldable aluminium laptop stand with six height levels, anti-slip pads, and a storage pouch. Sealed and unused.",
    university: "NUST",
    department: "Computer Science",
    location: "Islamabad",
    postedAt: "2026-06-24T11:20:00",
    postedDate: "June 24, 2026",
    postedTime: "5 days ago",
    contactMethod: "CampusOne Chat",
    featured: false,
    tone: "amber",
    seller: {
      name: "Usman Tariq",
      department: "Computer Science",
      university: "NUST",
      rating: 4.9,
      listingsCount: 18,
      verified: true,
    },
  },
  {
    id: "hostel-chair",
    title: "Comfortable Mesh Study Chair",
    category: "Furniture",
    price: 7000,
    condition: "Fair",
    description:
      "Breathable mesh chair with adjustable height. The armrests show light wear, but the lift and wheels work well.",
    university: "PU",
    department: "Computer Science",
    location: "Lahore",
    postedAt: "2026-06-22T14:45:00",
    postedDate: "June 22, 2026",
    postedTime: "1 week ago",
    contactMethod: "Phone",
    featured: false,
    tone: "rose",
    seller: {
      name: "Maryam Noor",
      department: "Computer Science",
      university: "Punjab University",
      rating: 4.4,
      listingsCount: 2,
      verified: false,
    },
  },
  {
    id: "power-bank",
    title: "Baseus 20,000mAh Power Bank",
    category: "Electronics",
    price: 3000,
    condition: "Used",
    description:
      "Reliable fast-charging power bank with USB-C input and two USB outputs. Battery capacity remains strong.",
    university: "LUMS",
    department: "Software Engineering",
    location: "Lahore",
    postedAt: "2026-06-20T17:30:00",
    postedDate: "June 20, 2026",
    postedTime: "1 week ago",
    contactMethod: "WhatsApp",
    featured: false,
    tone: "violet",
    seller: {
      name: "Daniyal Khan",
      department: "Software Engineering",
      university: "LUMS",
      rating: 4.6,
      listingsCount: 5,
      verified: true,
    },
  },
];

export const marketplaceUniversityOptions = [
  { label: "All universities", value: "all" },
  { label: "COMSATS", value: "COMSATS" },
  { label: "FAST", value: "FAST" },
  { label: "NUST", value: "NUST" },
  { label: "UET", value: "UET" },
  { label: "PU", value: "PU" },
  { label: "LUMS", value: "LUMS" },
];

export const marketplaceDepartmentOptions = [
  { label: "All departments", value: "all" },
  { label: "Computer Science", value: "Computer Science" },
  { label: "Software Engineering", value: "Software Engineering" },
  { label: "Artificial Intelligence", value: "Artificial Intelligence" },
  { label: "Data Science", value: "Data Science" },
  { label: "Economics", value: "Economics" },
];

export const marketplaceCategoryOptions = [
  { label: "All categories", value: "all" },
  ...marketplaceCategories
    .filter((category) => category !== "All")
    .map((category) => ({ label: category, value: category })),
];

export const marketplaceConditionOptions = [
  { label: "All conditions", value: "all" },
  { label: "New", value: "New" },
  { label: "Like New", value: "Like New" },
  { label: "Used", value: "Used" },
  { label: "Fair", value: "Fair" },
];

export const marketplacePriceOptions = [
  { label: "Any price", value: "all" },
  { label: "Under Rs. 1,000", value: "under-1000" },
  { label: "Rs. 1,000–5,000", value: "1000-5000" },
  { label: "Rs. 5,000–20,000", value: "5000-20000" },
  { label: "Above Rs. 20,000", value: "above-20000" },
];

export const marketplaceLocationOptions = [
  { label: "All locations", value: "all" },
  { label: "Islamabad", value: "Islamabad" },
  { label: "Rawalpindi", value: "Rawalpindi" },
  { label: "Lahore", value: "Lahore" },
];

export const marketplaceSortOptions = [
  { label: "Newest first", value: "newest" },
  { label: "Price: low to high", value: "price-low" },
  { label: "Price: high to low", value: "price-high" },
];

export const marketplaceContactOptions = [
  { label: "Select contact method", value: "", disabled: true },
  { label: "CampusOne Chat", value: "CampusOne Chat" },
  { label: "WhatsApp", value: "WhatsApp" },
  { label: "Phone", value: "Phone" },
  { label: "Email", value: "Email" },
];

