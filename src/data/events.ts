import {
  CalendarDays,
  CalendarRange,
  Radio,
  UsersRound,
  type LucideIcon,
} from "lucide-react";

export type EventCategory =
  | "Workshops"
  | "Seminars"
  | "Hackathons"
  | "Competitions"
  | "Sports"
  | "Society Events"
  | "Career Fair"
  | "Bootcamps";

export interface CampusEvent {
  attendees: number;
  category: EventCategory;
  date: string;
  dateLabel: string;
  description: string;
  featured: boolean;
  id: string;
  live: boolean;
  organizer: string;
  schedule: Array<{ time: string; title: string }>;
  seats: number;
  speakers: Array<{ name: string; role: string }>;
  time: string;
  title: string;
  tone: "brand" | "emerald" | "amber" | "sky" | "rose" | "violet";
  university: string;
  venue: string;
}

export const eventStats: Array<{
  change: number;
  icon: LucideIcon;
  label: string;
  value: string;
}> = [
  {
    label: "Upcoming Events",
    value: "48",
    change: 12,
    icon: CalendarDays,
  },
  {
    label: "Events This Month",
    value: "21",
    change: 8,
    icon: CalendarRange,
  },
  {
    label: "Registered Students",
    value: "3,284",
    change: 18,
    icon: UsersRound,
  },
  {
    label: "Live Events",
    value: "3",
    change: 5,
    icon: Radio,
  },
];

export const eventCategories = [
  "All",
  "Workshops",
  "Seminars",
  "Hackathons",
  "Competitions",
  "Sports",
  "Society Events",
  "Career Fair",
  "Bootcamps",
] as const;

export const campusEvents: CampusEvent[] = [
  {
    id: "open-source-sprint",
    title: "Open Source Contribution Sprint",
    category: "Workshops",
    date: "2026-06-30",
    dateLabel: "June 30, 2026",
    time: "2:00 PM – 6:00 PM",
    venue: "CS Lab 3",
    organizer: "COMSATS ACM Chapter",
    university: "COMSATS Islamabad",
    attendees: 84,
    seats: 100,
    live: true,
    featured: false,
    tone: "emerald",
    description:
      "A guided contribution sprint where students learn GitHub workflows, select beginner-friendly issues, and submit their first meaningful open-source pull request.",
    schedule: [
      { time: "2:00 PM", title: "GitHub workflow walkthrough" },
      { time: "2:45 PM", title: "Project and issue selection" },
      { time: "3:15 PM", title: "Contribution sprint" },
      { time: "5:30 PM", title: "Pull-request review and wrap-up" },
    ],
    speakers: [
      { name: "Saad Rehman", role: "Open-source maintainer" },
      { name: "Sara Ahmed", role: "ACM Technical Lead" },
    ],
  },
  {
    id: "pakistan-ai-hackathon",
    title: "Pakistan Inter-University AI Hackathon",
    category: "Hackathons",
    date: "2026-07-04",
    dateLabel: "July 4–5, 2026",
    time: "9:00 AM onwards",
    venue: "NUST Main Auditorium",
    organizer: "NUST AI Society",
    university: "NUST",
    attendees: 426,
    seats: 500,
    live: false,
    featured: true,
    tone: "brand",
    description:
      "A 30-hour national student hackathon focused on AI solutions for education, climate resilience, accessibility, and public services. Teams receive mentorship, cloud credits, and opportunities to pitch to industry judges.",
    schedule: [
      { time: "9:00 AM", title: "Registration and team check-in" },
      { time: "10:00 AM", title: "Opening keynote and challenge reveal" },
      { time: "11:00 AM", title: "Hacking begins" },
      { time: "6:00 PM", title: "Mentor review round" },
      { time: "Next day, 3:00 PM", title: "Final pitches and awards" },
    ],
    speakers: [
      { name: "Dr. Areeba Noor", role: "AI Researcher, NUST" },
      { name: "Fahad Mahmood", role: "Engineering Director, 10Pearls" },
      { name: "Hina Javed", role: "Product Lead, Securiti" },
    ],
  },
  {
    id: "islamabad-career-fair",
    title: "Islamabad Tech Career Fair 2026",
    category: "Career Fair",
    date: "2026-07-07",
    dateLabel: "July 7, 2026",
    time: "10:00 AM – 4:00 PM",
    venue: "Student Activity Centre",
    organizer: "COMSATS Career Development Centre",
    university: "COMSATS Islamabad",
    attendees: 680,
    seats: 900,
    live: false,
    featured: false,
    tone: "sky",
    description:
      "Meet recruiters from leading Pakistani technology companies, explore graduate and internship roles, and receive quick CV feedback from industry volunteers.",
    schedule: [
      { time: "10:00 AM", title: "Employer booths open" },
      { time: "11:30 AM", title: "Graduate hiring panel" },
      { time: "1:00 PM", title: "CV review corner" },
      { time: "3:00 PM", title: "Startup networking hour" },
    ],
    speakers: [
      { name: "Mariam Ali", role: "Talent Partner, Arbisoft" },
      { name: "Usman Qureshi", role: "Campus Lead, Systems Limited" },
    ],
  },
  {
    id: "portfolio-workshop",
    title: "Designing a Portfolio Recruiters Remember",
    category: "Workshops",
    date: "2026-07-10",
    dateLabel: "July 10, 2026",
    time: "2:30 PM – 5:00 PM",
    venue: "FAST Seminar Hall",
    organizer: "FAST Google Developer Group",
    university: "FAST Islamabad",
    attendees: 126,
    seats: 160,
    live: false,
    featured: false,
    tone: "violet",
    description:
      "A practical portfolio workshop covering project storytelling, case-study structure, GitHub presentation, and mistakes that make strong student work difficult to evaluate.",
    schedule: [
      { time: "2:30 PM", title: "What recruiters scan first" },
      { time: "3:15 PM", title: "Project-storytelling teardown" },
      { time: "4:00 PM", title: "Peer portfolio review" },
    ],
    speakers: [
      { name: "Maham Iqbal", role: "Product Designer" },
      { name: "Bilal Ahmed", role: "Frontend Engineer" },
    ],
  },
  {
    id: "cybersecurity-seminar",
    title: "Cybersecurity Careers and Threat Intelligence",
    category: "Seminars",
    date: "2026-07-12",
    dateLabel: "July 12, 2026",
    time: "11:00 AM – 1:00 PM",
    venue: "UET Auditorium B",
    organizer: "UET Cyber Security Society",
    university: "UET Lahore",
    attendees: 210,
    seats: 300,
    live: false,
    featured: false,
    tone: "amber",
    description:
      "Industry practitioners explain modern security roles, threat-intelligence workflows, ethical learning paths, and how students can build credible hands-on experience.",
    schedule: [
      { time: "11:00 AM", title: "Security career landscape" },
      { time: "11:40 AM", title: "Threat intelligence case study" },
      { time: "12:25 PM", title: "Student roadmap and Q&A" },
    ],
    speakers: [
      { name: "Ahmed Farooq", role: "Security Engineer" },
      { name: "Noor Fatima", role: "SOC Analyst" },
    ],
  },
  {
    id: "futsal-cup",
    title: "Inter-University Futsal Cup",
    category: "Sports",
    date: "2026-07-15",
    dateLabel: "July 15–17, 2026",
    time: "4:00 PM – 9:00 PM",
    venue: "LUMS Sports Complex",
    organizer: "LUMS Sports Society",
    university: "LUMS",
    attendees: 340,
    seats: 600,
    live: false,
    featured: false,
    tone: "rose",
    description:
      "Three evenings of competitive futsal featuring university teams from Lahore and Islamabad, with student commentary, food stalls, and a closing awards ceremony.",
    schedule: [
      { time: "4:00 PM", title: "Group-stage matches" },
      { time: "6:30 PM", title: "Quarterfinals" },
      { time: "Day 3, 7:30 PM", title: "Championship final" },
    ],
    speakers: [{ name: "LUMS Sports Council", role: "Event host" }],
  },
  {
    id: "startup-pitch",
    title: "Student Startup Pitch Competition",
    category: "Competitions",
    date: "2026-07-18",
    dateLabel: "July 18, 2026",
    time: "12:00 PM – 5:00 PM",
    venue: "PU Executive Club",
    organizer: "Punjab University Business Incubator",
    university: "Punjab University",
    attendees: 192,
    seats: 250,
    live: false,
    featured: false,
    tone: "emerald",
    description:
      "Student teams pitch early-stage ventures to founders and investors for incubation support, cloud credits, and a Rs. 300,000 prototype grant.",
    schedule: [
      { time: "12:00 PM", title: "Founder keynote" },
      { time: "1:00 PM", title: "First pitch round" },
      { time: "3:15 PM", title: "Finalist pitches" },
      { time: "4:30 PM", title: "Awards and networking" },
    ],
    speakers: [
      { name: "Sana Malik", role: "Startup Founder" },
      { name: "Omer Khan", role: "Early-stage Investor" },
    ],
  },
  {
    id: "music-society-night",
    title: "Campus Music & Culture Night",
    category: "Society Events",
    date: "2026-07-20",
    dateLabel: "July 20, 2026",
    time: "6:00 PM – 9:30 PM",
    venue: "NUST Central Courtyard",
    organizer: "NUST Music Society",
    university: "NUST",
    attendees: 520,
    seats: 700,
    live: false,
    featured: false,
    tone: "violet",
    description:
      "An evening of student bands, regional music, poetry, and campus food stalls celebrating the creative communities across NUST schools.",
    schedule: [
      { time: "6:00 PM", title: "Food stalls and acoustic sets" },
      { time: "7:00 PM", title: "Poetry and regional performances" },
      { time: "8:00 PM", title: "Student band showcase" },
    ],
    speakers: [{ name: "NUST Music Society", role: "Student performers" }],
  },
  {
    id: "react-bootcamp",
    title: "Modern React Development Bootcamp",
    category: "Bootcamps",
    date: "2026-07-22",
    dateLabel: "July 22–24, 2026",
    time: "10:00 AM – 4:00 PM",
    venue: "FAST Computing Lab 2",
    organizer: "FAST Developer Student Club",
    university: "FAST Islamabad",
    attendees: 98,
    seats: 120,
    live: false,
    featured: false,
    tone: "brand",
    description:
      "A three-day hands-on bootcamp covering React fundamentals, routing, API integration, state patterns, testing, and deploying a complete student project.",
    schedule: [
      { time: "Day 1", title: "Components, state, and composition" },
      { time: "Day 2", title: "APIs, routing, and forms" },
      { time: "Day 3", title: "Testing, polish, and deployment" },
    ],
    speakers: [
      { name: "Hamza Raza", role: "Frontend Engineer" },
      { name: "Sara Ahmed", role: "Developer Community Lead" },
    ],
  },
  {
    id: "robotics-challenge",
    title: "Autonomous Robotics Challenge",
    category: "Competitions",
    date: "2026-07-25",
    dateLabel: "July 25, 2026",
    time: "9:00 AM – 6:00 PM",
    venue: "UET Innovation Centre",
    organizer: "UET Robotics Society",
    university: "UET Lahore",
    attendees: 168,
    seats: 240,
    live: false,
    featured: false,
    tone: "amber",
    description:
      "University teams design autonomous robots for navigation, object detection, and timed engineering challenges judged by faculty and industry engineers.",
    schedule: [
      { time: "9:00 AM", title: "Technical inspection" },
      { time: "10:30 AM", title: "Navigation challenge" },
      { time: "2:00 PM", title: "Object-detection round" },
      { time: "5:00 PM", title: "Final run and awards" },
    ],
    speakers: [
      { name: "Dr. Salman Riaz", role: "Robotics Researcher" },
      { name: "UET Robotics Society", role: "Competition host" },
    ],
  },
  {
    id: "data-ethics-seminar",
    title: "Responsible AI and Data Ethics",
    category: "Seminars",
    date: "2026-08-02",
    dateLabel: "August 2, 2026",
    time: "3:00 PM – 5:00 PM",
    venue: "LUMS Academic Block",
    organizer: "LUMS Data Science Society",
    university: "LUMS",
    attendees: 144,
    seats: 220,
    live: false,
    featured: false,
    tone: "sky",
    description:
      "A cross-disciplinary seminar on bias, privacy, responsible model deployment, and the questions student technologists should ask while building AI systems.",
    schedule: [
      { time: "3:00 PM", title: "Bias and responsible datasets" },
      { time: "3:45 PM", title: "Privacy and deployment tradeoffs" },
      { time: "4:30 PM", title: "Student Q&A" },
    ],
    speakers: [
      { name: "Dr. Maryam Siddiqui", role: "Data Ethics Researcher" },
      { name: "Hira Javed", role: "Machine Learning Engineer" },
    ],
  },
];

export const createEventCategoryOptions = [
  { label: "Select category", value: "", disabled: true },
  ...eventCategories
    .filter((category) => category !== "All")
    .map((category) => ({ label: category, value: category })),
];

export const initiallyRegisteredEventIds = [
  "pakistan-ai-hackathon",
  "portfolio-workshop",
];

