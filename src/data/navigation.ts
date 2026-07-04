import {
  Bot,
  Bell,
  BriefcaseBusiness,
  CalendarDays,
  FileText,
  Home,
  MessageSquareText,
  Search,
  Settings,
  ShieldCheck,
  ShoppingBag,
  Trophy,
  UserRound,
} from "lucide-react";

import { paths } from "@/routes/paths";
import type { NavigationItem } from "@/types/navigation";

export const primaryNavigation: NavigationItem[] = [
  { label: "Home", path: paths.dashboard, icon: Home },
  { label: "Profile", path: paths.profile, icon: UserRound },
  { label: "Notes", path: paths.notes, icon: FileText },
  {
    label: "Discussions",
    path: paths.discussions,
    icon: MessageSquareText,
  },
  { label: "Marketplace", path: paths.marketplace, icon: ShoppingBag },
  {
    label: "Internships",
    path: paths.internships,
    icon: BriefcaseBusiness,
  },
  { label: "Events", path: paths.events, icon: CalendarDays },
  { label: "Notifications", path: paths.notifications, icon: Bell },
  { label: "Search", path: paths.search, icon: Search },
  { label: "Leaderboard", path: paths.leaderboard, icon: Trophy },
  { label: "AI Assistant", path: paths.assistant, icon: Bot },
];

export const secondaryNavigation: NavigationItem[] = [
  { label: "Admin", path: paths.admin, icon: ShieldCheck },
  { label: "Settings", path: paths.settings, icon: Settings },
];
