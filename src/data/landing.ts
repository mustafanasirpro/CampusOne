import {
  Bot,
  BriefcaseBusiness,
  CalendarDays,
  FileText,
  MessageSquareText,
  SearchCheck,
  ShieldCheck,
  ShoppingBag,
  Sparkles,
  UsersRound,
  type LucideIcon,
} from "lucide-react";

interface LandingFeature {
  description: string;
  icon: LucideIcon;
  title: string;
}

interface CampusOneBenefit {
  description: string;
  icon: LucideIcon;
  title: string;
}

export const landingFeatures: LandingFeature[] = [
  {
    title: "Notes that actually help",
    description:
      "Find organized course notes, past papers, and study resources from students who took the same classes.",
    icon: FileText,
  },
  {
    title: "Better campus discussions",
    description:
      "Ask questions, exchange advice, and get useful answers from your university community.",
    icon: MessageSquareText,
  },
  {
    title: "Student marketplace",
    description:
      "Buy and sell books, calculators, electronics, and hostel essentials with students nearby.",
    icon: ShoppingBag,
  },
  {
    title: "Internships, in one feed",
    description:
      "Discover relevant roles, track deadlines, and save opportunities without juggling ten different groups.",
    icon: BriefcaseBusiness,
  },
  {
    title: "Events worth showing up for",
    description:
      "Keep up with workshops, society events, sports, seminars, and hackathons happening around campus.",
    icon: CalendarDays,
  },
  {
    title: "Your AI study partner",
    description:
      "Turn notes into summaries, MCQs, viva questions, explanations, and focused study plans.",
    icon: Bot,
  },
];

export const campusOneBenefits: CampusOneBenefit[] = [
  {
    title: "Built around your university",
    description:
      "Relevant resources, conversations, opportunities, and people—not another noisy global feed.",
    icon: UsersRound,
  },
  {
    title: "Everything is easier to find",
    description:
      "Search once across notes, discussions, listings, events, and internships.",
    icon: SearchCheck,
  },
  {
    title: "A community you can trust",
    description:
      "Student profiles, contribution history, and community signals help useful content rise.",
    icon: ShieldCheck,
  },
  {
    title: "Designed for how students work",
    description:
      "Fast, mobile-friendly, and focused enough to use between classes.",
    icon: Sparkles,
  },
];

export const landingFaqs = [
  {
    question: "What is CampusOne?",
    answer:
      "CampusOne is a digital campus community for Pakistani university students. It brings study resources, discussions, opportunities, events, a marketplace, and AI study tools into one organized platform.",
  },
  {
    question: "Is CampusOne free for students?",
    answer:
      "Yes. The CampusOne student experience is designed to be free to join and explore.",
  },
  {
    question: "Which universities are supported?",
    answer:
      "The current local seed supports COMSATS University Islamabad and its Computer Science department. The academic directory is designed to expand as more campuses are configured.",
  },
  {
    question: "Can I upload my own notes?",
    answer:
      "Yes. Students can contribute notes and study resources, build their profile, earn XP, and help classmates discover reliable material.",
  },
  {
    question: "Does the AI assistant use real course material?",
    answer:
      "The assistant works with the text and topics you provide to create explanations, summaries, flashcards, quizzes, and study plans through CampusOne's deterministic local provider.",
  },
  {
    question: "Is the marketplace only for students?",
    answer:
      "It is designed around campus communities, making it easier to discover relevant listings and connect with nearby student sellers.",
  },
];
