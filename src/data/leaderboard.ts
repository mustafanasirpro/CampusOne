import {
  Award,
  BadgeCheck,
  BookOpenCheck,
  BriefcaseBusiness,
  CalendarCheck2,
  CheckCircle2,
  CircleDollarSign,
  Flame,
  HelpCircle,
  MessageSquareText,
  NotebookTabs,
  Rocket,
  ShoppingBag,
  Sparkles,
  Star,
  Target,
  Trophy,
  UsersRound,
  type LucideIcon,
} from "lucide-react";

export type LeaderboardPeriod = "weekly" | "monthly" | "all-time";

export interface RankedStudent {
  badgeNames: string[];
  badges: number;
  contribution: string;
  department: string;
  id: string;
  name: string;
  rank: number;
  university: string;
  xp: number;
}

interface StudentScoreProfile
  extends Omit<RankedStudent, "rank" | "xp"> {
  scores: Record<LeaderboardPeriod, number>;
}

const studentScores: StudentScoreProfile[] = [
  {
    id: "sara-ahmed",
    name: "Sara Ahmed",
    university: "COMSATS Islamabad",
    department: "Software Engineering",
    badges: 14,
    badgeNames: ["Top Contributor", "Notes Champion", "Streak Master"],
    contribution: "Notes & answers",
    scores: { weekly: 2840, monthly: 9220, "all-time": 28450 },
  },
  {
    id: "hamza-raza",
    name: "Hamza Raza",
    university: "FAST Islamabad",
    department: "Computer Science",
    badges: 12,
    badgeNames: ["Helpful Student", "Discussion Starter"],
    contribution: "Discussion answers",
    scores: { weekly: 2680, monthly: 9480, "all-time": 26180 },
  },
  {
    id: "ayesha-malik",
    name: "Ayesha Malik",
    university: "NUST",
    department: "Data Science",
    badges: 13,
    badgeNames: ["Event Explorer", "Internship Hunter"],
    contribution: "Events & opportunities",
    scores: { weekly: 2540, monthly: 8750, "all-time": 29120 },
  },
  {
    id: "bilal-ahmed",
    name: "Bilal Ahmed",
    university: "UET Lahore",
    department: "Computer Science",
    badges: 10,
    badgeNames: ["Notes Champion", "Campus Seller"],
    contribution: "Study resources",
    scores: { weekly: 2290, monthly: 7840, "all-time": 22110 },
  },
  {
    id: "maham-iqbal",
    name: "Maham Iqbal",
    university: "LUMS",
    department: "Software Engineering",
    badges: 11,
    badgeNames: ["Top Contributor", "Helpful Student"],
    contribution: "Design mentoring",
    scores: { weekly: 2180, monthly: 8120, "all-time": 23840 },
  },
  {
    id: "usman-tariq",
    name: "Usman Tariq",
    university: "NUST",
    department: "Artificial Intelligence",
    badges: 9,
    badgeNames: ["Internship Hunter", "Discussion Starter"],
    contribution: "AI discussions",
    scores: { weekly: 1960, monthly: 7290, "all-time": 20460 },
  },
  {
    id: "noor-fatima",
    name: "Noor Fatima",
    university: "Punjab University",
    department: "Data Science",
    badges: 8,
    badgeNames: ["Campus Seller", "Event Explorer"],
    contribution: "Marketplace & events",
    scores: { weekly: 1830, monthly: 6680, "all-time": 18520 },
  },
  {
    id: "zain-hassan",
    name: "Zain Hassan",
    university: "UET Lahore",
    department: "Electrical Engineering",
    badges: 8,
    badgeNames: ["Streak Master", "Helpful Student"],
    contribution: "Campus Q&A",
    scores: { weekly: 1710, monthly: 6950, "all-time": 19670 },
  },
  {
    id: "hira-javed",
    name: "Hira Javed",
    university: "FAST Islamabad",
    department: "Artificial Intelligence",
    badges: 7,
    badgeNames: ["Notes Champion", "Internship Hunter"],
    contribution: "AI notes",
    scores: { weekly: 1590, monthly: 6140, "all-time": 17380 },
  },
  {
    id: "ali-raza",
    name: "Ali Raza",
    university: "COMSATS Islamabad",
    department: "Computer Science",
    badges: 7,
    badgeNames: ["Discussion Starter", "Campus Seller"],
    contribution: "Questions & listings",
    scores: { weekly: 1480, monthly: 5790, "all-time": 16110 },
  },
];

function buildRanking(period: LeaderboardPeriod): RankedStudent[] {
  return [...studentScores]
    .sort((first, second) => second.scores[period] - first.scores[period])
    .map(({ scores, ...student }, index) => ({
      ...student,
      rank: index + 1,
      xp: scores[period],
    }));
}

export const leaderboardByPeriod: Record<
  LeaderboardPeriod,
  RankedStudent[]
> = {
  weekly: buildRanking("weekly"),
  monthly: buildRanking("monthly"),
  "all-time": buildRanking("all-time"),
};

export const leaderboardTabs = [
  { label: "Weekly", value: "weekly" as const },
  { label: "Monthly", value: "monthly" as const },
  { label: "All Time", value: "all-time" as const },
];

export const leaderboardStats: Array<{
  change: number;
  icon: LucideIcon;
  label: string;
  value: string;
}> = [
  {
    label: "Total XP Earned",
    value: "1.28M",
    change: 16,
    icon: Trophy,
  },
  {
    label: "Active Contributors",
    value: "2,846",
    change: 12,
    icon: UsersRound,
  },
  {
    label: "Weekly Challenges",
    value: "18",
    change: 8,
    icon: Target,
  },
  {
    label: "Badges Awarded",
    value: "9,420",
    change: 21,
    icon: Award,
  },
];

export const myGamificationProgress = {
  name: "Ali Khan",
  rank: 14,
  xp: 2480,
  level: 12,
  levelName: "Campus Collaborator",
  nextLevelXp: 2600,
  progress: 68,
  dailyStreak: 9,
  bestStreak: 17,
  contributions: [
    { label: "Notes uploaded", value: 38, icon: NotebookTabs },
    { label: "Questions answered", value: 126, icon: HelpCircle },
    { label: "Events joined", value: 12, icon: CalendarCheck2 },
    { label: "Listings posted", value: 5, icon: ShoppingBag },
  ],
};

export const gamificationBadges: Array<{
  description: string;
  earned: boolean;
  icon: LucideIcon;
  name: string;
  progress: number;
  tone: "amber" | "brand" | "emerald" | "sky" | "rose" | "violet";
}> = [
  {
    name: "Top Contributor",
    description: "Finish a month among the campus’s top 20 contributors.",
    earned: true,
    progress: 100,
    icon: Trophy,
    tone: "amber",
  },
  {
    name: "Notes Champion",
    description: "Upload 25 notes rated helpful by the community.",
    earned: true,
    progress: 100,
    icon: BookOpenCheck,
    tone: "brand",
  },
  {
    name: "Helpful Student",
    description: "Receive 100 helpful votes across your answers.",
    earned: true,
    progress: 100,
    icon: BadgeCheck,
    tone: "emerald",
  },
  {
    name: "Campus Seller",
    description: "Complete five trusted marketplace exchanges.",
    earned: false,
    progress: 80,
    icon: CircleDollarSign,
    tone: "rose",
  },
  {
    name: "Event Explorer",
    description: "Attend 15 campus events across three categories.",
    earned: false,
    progress: 80,
    icon: CalendarCheck2,
    tone: "sky",
  },
  {
    name: "Internship Hunter",
    description: "Save and review ten relevant opportunities.",
    earned: true,
    progress: 100,
    icon: BriefcaseBusiness,
    tone: "violet",
  },
  {
    name: "Discussion Starter",
    description: "Start 20 discussions with meaningful engagement.",
    earned: false,
    progress: 72,
    icon: MessageSquareText,
    tone: "brand",
  },
  {
    name: "Streak Master",
    description: "Contribute to CampusOne for 14 consecutive days.",
    earned: false,
    progress: 64,
    icon: Flame,
    tone: "amber",
  },
];

export const weeklyChallenges = [
  {
    id: "challenge-notes",
    title: "Upload 2 notes",
    description: "Share two useful course resources.",
    current: 2,
    target: 2,
    reward: 120,
    deadline: "Sunday, 11:59 PM",
    icon: NotebookTabs,
  },
  {
    id: "challenge-answers",
    title: "Answer 5 questions",
    description: "Help students solve academic problems.",
    current: 3,
    target: 5,
    reward: 150,
    deadline: "Sunday, 11:59 PM",
    icon: HelpCircle,
  },
  {
    id: "challenge-event",
    title: "RSVP to 1 event",
    description: "Join one new campus experience.",
    current: 1,
    target: 1,
    reward: 60,
    deadline: "Sunday, 11:59 PM",
    icon: CalendarCheck2,
  },
  {
    id: "challenge-internships",
    title: "Save 3 internships",
    description: "Build a focused opportunity shortlist.",
    current: 2,
    target: 3,
    reward: 80,
    deadline: "Sunday, 11:59 PM",
    icon: BriefcaseBusiness,
  },
  {
    id: "challenge-listing",
    title: "Post 1 marketplace listing",
    description: "Offer a useful item to your campus.",
    current: 1,
    target: 1,
    reward: 75,
    deadline: "Sunday, 11:59 PM",
    icon: ShoppingBag,
  },
];

export const contributionActivity: Array<{
  description: string;
  icon: LucideIcon;
  id: string;
  time: string;
  title: string;
  tone: "brand" | "emerald" | "amber" | "sky" | "violet";
}> = [
  {
    id: "activity-badge",
    title: "Sara Ahmed earned Notes Champion",
    description: "Her 25th highly rated resource unlocked the badge.",
    time: "12 min ago",
    icon: Award,
    tone: "amber",
  },
  {
    id: "activity-notes",
    title: "Hamza Raza uploaded Java revision notes",
    description: "OOP Final Revision Pack · FAST Islamabad",
    time: "34 min ago",
    icon: NotebookTabs,
    tone: "brand",
  },
  {
    id: "activity-answer",
    title: "Ayesha Malik answered a database question",
    description: "The answer received 18 helpful votes.",
    time: "1 hour ago",
    icon: CheckCircle2,
    tone: "emerald",
  },
  {
    id: "activity-event",
    title: "Bilal Ahmed joined the AI Hackathon",
    description: "His third event registration this month.",
    time: "2 hours ago",
    icon: CalendarCheck2,
    tone: "sky",
  },
  {
    id: "activity-level",
    title: "Maham Iqbal reached Level 18",
    description: "Unlocked the Community Mentor title.",
    time: "3 hours ago",
    icon: Rocket,
    tone: "violet",
  },
];

export const departmentRankings = [
  {
    rank: 1,
    department: "Computer Science",
    totalXp: 284500,
    activeStudents: 846,
  },
  {
    rank: 2,
    department: "Software Engineering",
    totalXp: 248200,
    activeStudents: 692,
  },
  {
    rank: 3,
    department: "Artificial Intelligence",
    totalXp: 196800,
    activeStudents: 488,
  },
  {
    rank: 4,
    department: "Data Science",
    totalXp: 172400,
    activeStudents: 416,
  },
  {
    rank: 5,
    department: "Electrical Engineering",
    totalXp: 149600,
    activeStudents: 372,
  },
];

export const gamificationSparkleIcon = Sparkles;
export const gamificationStarIcon = Star;

