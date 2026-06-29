import {
  Award,
  BookOpenCheck,
  BriefcaseBusiness,
  CalendarCheck2,
  CalendarDays,
  FileCheck2,
  FileUp,
  HeartHandshake,
  HelpCircle,
  MessageCircleQuestion,
  MessageSquareText,
  ShoppingBag,
  Store,
  Trophy,
  type LucideIcon,
} from "lucide-react";

import type { DiscussionSummary } from "@/types/content";

export interface StudentProfile {
  bio: string;
  department: string;
  fullName: string;
  location: string;
  semester: string;
  skills: string[];
  university: string;
}

export const initialStudentProfile: StudentProfile = {
  fullName: "Ali Khan",
  university: "COMSATS Islamabad",
  department: "Computer Science",
  semester: "6th Semester",
  location: "Islamabad, Pakistan",
  bio: "Computer Science student who enjoys building useful web products, helping classmates understand difficult concepts, and turning ambitious ideas into clean interfaces.",
  skills: [
    "Java",
    "React",
    "OOP",
    "Database Systems",
    "UI/UX",
    "Problem Solving",
  ],
};

export const profileStats: Array<{
  description: string;
  icon: LucideIcon;
  label: string;
  value: number;
}> = [
  {
    label: "XP Points",
    value: 2480,
    description: "Top 8% this month",
    icon: Trophy,
  },
  {
    label: "Notes Uploaded",
    value: 38,
    description: "12 this semester",
    icon: FileUp,
  },
  {
    label: "Questions Answered",
    value: 126,
    description: "92% helpful votes",
    icon: MessageCircleQuestion,
  },
  {
    label: "Marketplace Listings",
    value: 5,
    description: "4 successful sales",
    icon: ShoppingBag,
  },
  {
    label: "Events Attended",
    value: 12,
    description: "3 workshops",
    icon: CalendarCheck2,
  },
  {
    label: "Badges Earned",
    value: 6,
    description: "2 close to unlock",
    icon: Award,
  },
];

export const profileAchievements: Array<{
  description: string;
  earned?: string;
  icon: LucideIcon;
  name: string;
  progress?: number;
  tone: "amber" | "brand" | "emerald" | "sky" | "rose" | "violet";
}> = [
  {
    name: "Top Contributor",
    description: "Ranked among the most helpful students on campus.",
    earned: "Earned April 2026",
    icon: Trophy,
    tone: "amber",
  },
  {
    name: "Notes Champion",
    description: "Shared 25 high-quality course resources.",
    earned: "Earned June 2026",
    icon: BookOpenCheck,
    tone: "brand",
  },
  {
    name: "Helpful Student",
    description: "Received 100 helpful votes on answers.",
    earned: "Earned May 2026",
    icon: HeartHandshake,
    tone: "emerald",
  },
  {
    name: "Discussion Starter",
    description: "Start 20 conversations that help the community.",
    progress: 72,
    icon: MessageSquareText,
    tone: "sky",
  },
  {
    name: "Campus Seller",
    description: "Complete five trusted marketplace sales.",
    progress: 60,
    icon: Store,
    tone: "rose",
  },
  {
    name: "Internship Seeker",
    description: "Build a focused list of ten saved opportunities.",
    progress: 85,
    icon: BriefcaseBusiness,
    tone: "violet",
  },
];

export const profileActivity: Array<{
  description: string;
  icon: LucideIcon;
  id: string;
  time: string;
  title: string;
  tone: "brand" | "emerald" | "amber" | "sky" | "rose";
}> = [
  {
    id: "activity-notes",
    title: "Uploaded OOP final notes",
    description: "Object Oriented Programming · 36 pages",
    time: "Today, 10:24 AM",
    icon: FileCheck2,
    tone: "brand",
  },
  {
    id: "activity-answer",
    title: "Answered a Java question",
    description: "Explained interfaces versus abstract classes",
    time: "Yesterday, 6:42 PM",
    icon: HelpCircle,
    tone: "emerald",
  },
  {
    id: "activity-event",
    title: "RSVP’d to AI Builders Workshop",
    description: "ACM Student Chapter · Main Auditorium",
    time: "June 27",
    icon: CalendarDays,
    tone: "amber",
  },
  {
    id: "activity-internship",
    title: "Saved a frontend internship",
    description: "Frontend Engineering Intern · Arbisoft",
    time: "June 26",
    icon: BriefcaseBusiness,
    tone: "sky",
  },
  {
    id: "activity-listing",
    title: "Listed a scientific calculator",
    description: "Marketplace · Like new · Islamabad",
    time: "June 24",
    icon: Store,
    tone: "rose",
  },
];

export const profileNotes = [
  {
    id: "oop-final-notes",
    title: "OOP Final Revision Notes",
    course: "Object Oriented Programming",
    downloads: 384,
    rating: 4.9,
  },
  {
    id: "database-cheatsheet",
    title: "SQL & Normalization Cheatsheet",
    course: "Database Systems",
    downloads: 276,
    rating: 4.8,
  },
  {
    id: "dsa-graphs",
    title: "Graphs and Trees — Visual Guide",
    course: "Data Structures",
    downloads: 219,
    rating: 4.7,
  },
];

export const profileDiscussions: DiscussionSummary[] = [
  {
    title: "How do Java interfaces differ from abstract classes?",
    category: "Answered",
    tags: ["java", "oop"],
    upvotes: 84,
    comments: 18,
    author: "Ali Khan",
    time: "Yesterday",
  },
  {
    title: "Which database indexing strategy fits this query?",
    category: "Question",
    tags: ["database", "sql"],
    upvotes: 47,
    comments: 23,
    author: "Ali Khan",
    time: "3 days ago",
  },
  {
    title: "Sharing my checklist for technical internship interviews",
    category: "Career advice",
    tags: ["internships", "careers"],
    upvotes: 112,
    comments: 31,
    author: "Ali Khan",
    time: "1 week ago",
  },
];

export const profileUniversityOptions = [
  { label: "COMSATS Islamabad", value: "COMSATS Islamabad" },
  { label: "FAST Islamabad", value: "FAST Islamabad" },
  { label: "NUST", value: "NUST" },
  { label: "UET Lahore", value: "UET Lahore" },
  { label: "Punjab University", value: "Punjab University" },
  { label: "LUMS", value: "LUMS" },
];

export const profileDepartmentOptions = [
  { label: "Computer Science", value: "Computer Science" },
  { label: "Software Engineering", value: "Software Engineering" },
  { label: "Artificial Intelligence", value: "Artificial Intelligence" },
  { label: "Data Science", value: "Data Science" },
];

export const profileSemesterOptions = Array.from(
  { length: 8 },
  (_, index) => {
    const semester = index + 1;
    const suffix =
      semester === 1
        ? "st"
        : semester === 2
          ? "nd"
          : semester === 3
            ? "rd"
            : "th";
    const label = `${semester}${suffix} Semester`;

    return { label, value: label };
  },
);

