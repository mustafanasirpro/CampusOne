import {
  Bot,
  BriefcaseBusiness,
  CalendarDays,
  FileText,
  Home,
  MessageSquareText,
  Settings,
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
  { label: "Leaderboard", path: paths.leaderboard, icon: Trophy },
  { label: "AI Assistant", path: paths.assistant, icon: Bot },
];

export const secondaryNavigation: NavigationItem[] = [
  { label: "Settings", path: paths.settings, icon: Settings },
];

