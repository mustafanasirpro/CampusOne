import {
  BookOpenCheck,
  BriefcaseBusiness,
  CalendarDays,
  ClipboardPlus,
  FileText,
  GraduationCap,
  Megaphone,
  MessageSquareText,
  PartyPopper,
  ShoppingBag,
  type LucideIcon,
} from "lucide-react";

import { paths } from "@/routes/paths";
import type { EventSummary, InternshipSummary } from "@/types/content";

export const dashboardStudent = {
  name: "Ali Khan",
  university: "COMSATS Islamabad",
  department: "Computer Science",
  semester: "6th Semester",
  focus: "Database Systems final",
  weeklyGoal: 72,
};

export const dashboardStats: Array<{
  change: number;
  icon: LucideIcon;
  label: string;
  value: number;
}> = [
  { label: "Notes Available", value: 1248, change: 12, icon: FileText },
  {
    label: "Active Discussions",
    value: 286,
    change: 8,
    icon: MessageSquareText,
  },
  { label: "Upcoming Events", value: 18, change: 5, icon: CalendarDays },
  {
    label: "Internship Opportunities",
    value: 42,
    change: 16,
    icon: BriefcaseBusiness,
  },
];

export const dashboardAnnouncements: Array<{
  category: string;
  description: string;
  icon: LucideIcon;
  id: string;
  posted: string;
  title: string;
  tone: "brand" | "emerald" | "amber" | "rose";
}> = [
  {
    id: "exam-schedule",
    title: "Final exam schedule published",
    description:
      "Spring 2026 final examination dates and room allocations are now available.",
    category: "Examinations",
    posted: "2 hours ago",
    icon: GraduationCap,
    tone: "brand",
  },
  {
    id: "society-week",
    title: "Student society week",
    description:
      "Meet campus societies in the central courtyard and register for the fall term.",
    category: "Campus life",
    posted: "Today",
    icon: PartyPopper,
    tone: "emerald",
  },
  {
    id: "registration",
    title: "Course registration update",
    description:
      "The add/drop window for summer courses remains open through Wednesday.",
    category: "Registration",
    posted: "Yesterday",
    icon: ClipboardPlus,
    tone: "amber",
  },
  {
    id: "holiday",
    title: "Campus closed on Monday",
    description:
      "All academic and administrative offices will observe the public holiday.",
    category: "Holiday notice",
    posted: "Jun 27",
    icon: Megaphone,
    tone: "rose",
  },
];

export const dashboardNotes = [
  {
    id: "dsa-final",
    title: "Data Structures — Final Revision Notes",
    course: "Data Structures",
    teacher: "Dr. Hina Tariq",
    uploadedBy: "Sara Ahmed",
    rating: 4.9,
    pages: 42,
  },
  {
    id: "db-normalization",
    title: "Database Normalization & SQL Cheatsheet",
    course: "Database Systems",
    teacher: "Sir Usman Khalid",
    uploadedBy: "Hamza Raza",
    rating: 4.8,
    pages: 28,
  },
  {
    id: "networks-mid",
    title: "Computer Networks Complete Handouts",
    course: "Computer Networks",
    teacher: "Dr. Areeba Noor",
    uploadedBy: "Ayesha Malik",
    rating: 4.7,
    pages: 65,
  },
];

export const dashboardDiscussions = [
  {
    id: "oop-final",
    title: "What is the best way to prepare for the OOP final?",
    category: "Study help",
    author: "Hamza Raza",
    time: "18 min ago",
    upvotes: 84,
    comments: 26,
  },
  {
    id: "career-fair",
    title: "Companies confirmed for the Spring Career Fair",
    category: "Careers",
    author: "Maham Iqbal",
    time: "1 hour ago",
    upvotes: 61,
    comments: 19,
  },
  {
    id: "fyp-ideas",
    title: "Looking for one teammate for an AI-based FYP",
    category: "Projects",
    author: "Bilal Ahmed",
    time: "3 hours ago",
    upvotes: 47,
    comments: 31,
  },
];

export const dashboardEvents: EventSummary[] = [
  {
    title: "AI Builders Hackathon",
    date: "July 4 · 9:00 AM",
    venue: "Main Auditorium",
    organizer: "ACM Student Chapter",
    category: "Hackathon",
  },
  {
    title: "Designing Your First Portfolio",
    date: "July 7 · 2:30 PM",
    venue: "CS Seminar Hall",
    organizer: "Google Developer Group",
    category: "Workshop",
  },
  {
    title: "Inter-University Futsal Cup",
    date: "July 12 · 4:00 PM",
    venue: "Sports Complex",
    organizer: "Sports Society",
    category: "Sports",
  },
];

export const dashboardInternships: Array<InternshipSummary & { id: string }> = [
  {
    id: "arbisoft-frontend",
    company: "Arbisoft",
    role: "Frontend Engineering Intern",
    location: "Lahore",
    deadline: "July 8",
    paid: true,
    remote: false,
  },
  {
    id: "systems-data",
    company: "Systems Limited",
    role: "Data Analyst Intern",
    location: "Islamabad",
    deadline: "July 11",
    paid: true,
    remote: false,
  },
  {
    id: "devsinc-ui",
    company: "Devsinc",
    role: "UI/UX Design Intern",
    location: "Remote",
    deadline: "July 15",
    paid: true,
    remote: true,
  },
];

export const dashboardQuickActions: Array<{
  description: string;
  icon: LucideIcon;
  label: string;
  path: string;
  tone: "brand" | "emerald" | "amber" | "sky";
}> = [
  {
    label: "Upload notes",
    description: "Share a resource",
    icon: FileText,
    path: paths.notes,
    tone: "brand",
  },
  {
    label: "Ask a question",
    description: "Get campus answers",
    icon: MessageSquareText,
    path: paths.discussions,
    tone: "emerald",
  },
  {
    label: "Add a listing",
    description: "Sell to students",
    icon: ShoppingBag,
    path: paths.marketplace,
    tone: "amber",
  },
  {
    label: "Browse internships",
    description: "Find your next role",
    icon: BriefcaseBusiness,
    path: paths.internships,
    tone: "sky",
  },
];

export const dashboardNotifications = [
  {
    id: "note-approved",
    kind: "note" as const,
    title: "Your notes were approved",
    message: "Database Systems notes are now visible to students.",
    time: "8 min ago",
    read: false,
  },
  {
    id: "discussion-reply",
    kind: "discussion" as const,
    title: "New reply to your question",
    message: "Sara shared a helpful explanation in OOP Help.",
    time: "24 min ago",
    read: false,
  },
  {
    id: "event-reminder",
    kind: "event" as const,
    title: "Event starts tomorrow",
    message: "AI Builders Hackathon begins at 9:00 AM.",
    time: "2 hours ago",
    read: false,
  },
  {
    id: "internship-deadline",
    kind: "internship" as const,
    title: "Saved internship deadline",
    message: "Arbisoft applications close in 9 days.",
    time: "Yesterday",
    read: true,
  },
];

export const dashboardStudyPrompt = {
  title: "Turn today’s revision into a plan",
  description:
    "Ask CampusOne AI to create a focused study schedule for your upcoming Database Systems final.",
  icon: BookOpenCheck,
};

