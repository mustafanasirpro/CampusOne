import {
  BadgeDollarSign,
  BrainCircuit,
  BriefcaseBusiness,
  CalendarClock,
  Code2,
  ContactRound,
  FileText,
  MessagesSquare,
  MonitorCheck,
  type LucideIcon,
} from "lucide-react";

export type InternshipType =
  | "Frontend"
  | "Backend"
  | "Java"
  | "UI/UX"
  | "Data Analyst"
  | "AI/ML"
  | "Marketing";

export type WorkMode = "Remote" | "Onsite" | "Hybrid";

export interface InternshipOpportunity {
  city: string;
  company: string;
  deadline: string;
  deadlineDate: string;
  description: string;
  duration: string;
  featured: boolean;
  id: string;
  paid: boolean;
  postedAt: string;
  postedTime: string;
  requirements: string[];
  responsibilities: string[];
  role: string;
  skills: string[];
  stipend: string;
  tone: "brand" | "emerald" | "amber" | "sky" | "rose" | "violet";
  type: InternshipType;
  workMode: WorkMode;
}

export const internshipStats: Array<{
  change: number;
  icon: LucideIcon;
  label: string;
  value: string;
}> = [
  {
    label: "Active Internships",
    value: "326",
    change: 14,
    icon: BriefcaseBusiness,
  },
  {
    label: "Remote Opportunities",
    value: "94",
    change: 18,
    icon: MonitorCheck,
  },
  {
    label: "Paid Internships",
    value: "248",
    change: 11,
    icon: BadgeDollarSign,
  },
  {
    label: "Application Deadlines",
    value: "38",
    change: 7,
    icon: CalendarClock,
  },
];

export const internshipOpportunities: InternshipOpportunity[] = [
  {
    id: "arbisoft-frontend",
    company: "Arbisoft",
    role: "Frontend Engineering Intern",
    type: "Frontend",
    city: "Lahore",
    workMode: "Hybrid",
    paid: true,
    stipend: "Rs. 45,000 / month",
    duration: "3 months",
    skills: ["React", "TypeScript", "CSS", "Git"],
    deadline: "July 7, 2026",
    deadlineDate: "2026-07-07",
    postedAt: "2026-06-30T09:30:00",
    postedTime: "2 hours ago",
    featured: false,
    tone: "brand",
    description:
      "Join Arbisoft’s product engineering team to build polished web experiences for global education and technology clients while learning from senior frontend engineers.",
    responsibilities: [
      "Build reusable React components from product designs.",
      "Integrate REST APIs and handle client-side state.",
      "Participate in code reviews, testing, and sprint planning.",
    ],
    requirements: [
      "Strong JavaScript fundamentals and working React knowledge.",
      "Comfort with HTML, modern CSS, Git, and responsive layouts.",
      "A portfolio, course project, or GitHub repository to discuss.",
    ],
  },
  {
    id: "systems-data-analyst",
    company: "Systems Limited",
    role: "Data Analyst Intern",
    type: "Data Analyst",
    city: "Islamabad",
    workMode: "Onsite",
    paid: true,
    stipend: "Rs. 35,000 / month",
    duration: "8 weeks",
    skills: ["SQL", "Excel", "Power BI", "Python"],
    deadline: "July 10, 2026",
    deadlineDate: "2026-07-10",
    postedAt: "2026-06-30T08:10:00",
    postedTime: "3 hours ago",
    featured: true,
    tone: "emerald",
    description:
      "Support business intelligence projects by cleaning datasets, writing SQL queries, building dashboards, and presenting insights to delivery teams.",
    responsibilities: [
      "Prepare and validate operational datasets.",
      "Build clear dashboards and recurring analytical reports.",
      "Document insights and communicate findings to stakeholders.",
    ],
    requirements: [
      "Comfort with SQL and spreadsheet-based analysis.",
      "Basic knowledge of statistics and data visualization.",
      "Clear written communication and attention to detail.",
    ],
  },
  {
    id: "tenpearls-ai",
    company: "10Pearls",
    role: "AI/ML Research Intern",
    type: "AI/ML",
    city: "Karachi",
    workMode: "Hybrid",
    paid: true,
    stipend: "Rs. 50,000 / month",
    duration: "3 months",
    skills: ["Python", "PyTorch", "NLP", "Machine Learning"],
    deadline: "July 12, 2026",
    deadlineDate: "2026-07-12",
    postedAt: "2026-06-29T17:20:00",
    postedTime: "Yesterday",
    featured: false,
    tone: "violet",
    description:
      "Experiment with applied machine-learning ideas, evaluate models, and contribute to prototypes alongside the AI innovation team.",
    responsibilities: [
      "Prepare datasets and run repeatable model experiments.",
      "Evaluate model quality and document research findings.",
      "Support prototypes for NLP and predictive applications.",
    ],
    requirements: [
      "Python proficiency and foundational machine-learning knowledge.",
      "Exposure to PyTorch, TensorFlow, or scikit-learn.",
      "Curiosity, structured experimentation, and clear documentation.",
    ],
  },
  {
    id: "devsinc-uiux",
    company: "Devsinc",
    role: "UI/UX Design Intern",
    type: "UI/UX",
    city: "Lahore",
    workMode: "Onsite",
    paid: true,
    stipend: "Rs. 30,000 / month",
    duration: "10 weeks",
    skills: ["Figma", "Wireframing", "Prototyping", "Design Systems"],
    deadline: "July 14, 2026",
    deadlineDate: "2026-07-14",
    postedAt: "2026-06-29T14:45:00",
    postedTime: "Yesterday",
    featured: false,
    tone: "rose",
    description:
      "Work with product designers to turn user problems into wireframes, prototypes, and consistent interfaces for web and mobile products.",
    responsibilities: [
      "Create user flows, wireframes, and interactive prototypes.",
      "Maintain components within shared design systems.",
      "Support design reviews and usability feedback sessions.",
    ],
    requirements: [
      "A portfolio demonstrating interface and interaction thinking.",
      "Working knowledge of Figma and responsive design.",
      "Ability to explain design choices clearly.",
    ],
  },
  {
    id: "contour-backend",
    company: "Contour Software",
    role: "Backend Development Intern",
    type: "Backend",
    city: "Karachi",
    workMode: "Hybrid",
    paid: true,
    stipend: "Rs. 40,000 / month",
    duration: "3 months",
    skills: ["Node.js", "SQL", "REST APIs", "Docker"],
    deadline: "July 16, 2026",
    deadlineDate: "2026-07-16",
    postedAt: "2026-06-28T12:20:00",
    postedTime: "2 days ago",
    featured: false,
    tone: "sky",
    description:
      "Contribute to backend services, database integrations, and automated tests for enterprise software products used by international customers.",
    responsibilities: [
      "Implement and test API endpoints.",
      "Work with relational databases and service logs.",
      "Document technical decisions and support code reviews.",
    ],
    requirements: [
      "Understanding of server-side programming and HTTP APIs.",
      "SQL fundamentals and familiarity with Git.",
      "Basic testing and debugging skills.",
    ],
  },
  {
    id: "securiti-ml",
    company: "Securiti",
    role: "Machine Learning Intern",
    type: "AI/ML",
    city: "Islamabad",
    workMode: "Remote",
    paid: true,
    stipend: "Rs. 55,000 / month",
    duration: "12 weeks",
    skills: ["Python", "Machine Learning", "Pandas", "APIs"],
    deadline: "July 18, 2026",
    deadlineDate: "2026-07-18",
    postedAt: "2026-06-28T09:00:00",
    postedTime: "2 days ago",
    featured: true,
    tone: "brand",
    description:
      "Help prototype machine-learning features for privacy and data-intelligence products in a remote, mentor-supported internship.",
    responsibilities: [
      "Explore structured and text datasets.",
      "Develop baseline models and evaluation notebooks.",
      "Collaborate remotely through documented experiments.",
    ],
    requirements: [
      "Solid Python and data-manipulation skills.",
      "Understanding of supervised learning and evaluation metrics.",
      "Ability to work independently and communicate progress.",
    ],
  },
  {
    id: "tkxel-java",
    company: "Tkxel",
    role: "Java Engineering Intern",
    type: "Java",
    city: "Lahore",
    workMode: "Hybrid",
    paid: true,
    stipend: "Rs. 38,000 / month",
    duration: "3 months",
    skills: ["Java", "OOP", "Spring Boot", "SQL"],
    deadline: "July 20, 2026",
    deadlineDate: "2026-07-20",
    postedAt: "2026-06-27T16:25:00",
    postedTime: "3 days ago",
    featured: false,
    tone: "amber",
    description:
      "Build and test Java services while strengthening object-oriented design, API development, and database fundamentals.",
    responsibilities: [
      "Implement small Spring Boot features under mentorship.",
      "Write unit tests and fix clearly scoped defects.",
      "Participate in team stand-ups and code reviews.",
    ],
    requirements: [
      "Strong OOP concepts and Java fundamentals.",
      "Basic SQL and REST API understanding.",
      "Course projects demonstrating clean code.",
    ],
  },
  {
    id: "motive-frontend",
    company: "Motive",
    role: "Web Product Intern",
    type: "Frontend",
    city: "Islamabad",
    workMode: "Remote",
    paid: true,
    stipend: "Rs. 60,000 / month",
    duration: "12 weeks",
    skills: ["React", "JavaScript", "Testing", "Accessibility"],
    deadline: "July 23, 2026",
    deadlineDate: "2026-07-23",
    postedAt: "2026-06-27T10:00:00",
    postedTime: "3 days ago",
    featured: true,
    tone: "emerald",
    description:
      "Support customer-facing web product work with a focus on reliable React components, accessibility, and thoughtful product execution.",
    responsibilities: [
      "Develop and test frontend components.",
      "Fix usability and accessibility issues.",
      "Collaborate with product, design, and engineering partners.",
    ],
    requirements: [
      "Confident JavaScript and React fundamentals.",
      "Understanding of semantic HTML and responsive CSS.",
      "Strong written communication for remote work.",
    ],
  },
  {
    id: "jazz-marketing",
    company: "Jazz",
    role: "Digital Marketing Intern",
    type: "Marketing",
    city: "Islamabad",
    workMode: "Onsite",
    paid: true,
    stipend: "Rs. 25,000 / month",
    duration: "8 weeks",
    skills: ["Content", "Analytics", "Social Media", "Excel"],
    deadline: "July 25, 2026",
    deadlineDate: "2026-07-25",
    postedAt: "2026-06-26T15:15:00",
    postedTime: "4 days ago",
    featured: false,
    tone: "rose",
    description:
      "Support digital campaigns, social content, performance reporting, and student-focused market research with Jazz’s marketing team.",
    responsibilities: [
      "Assist with content calendars and campaign execution.",
      "Compile weekly social and web performance reports.",
      "Research audience trends and competitor campaigns.",
    ],
    requirements: [
      "Clear writing and strong interest in digital marketing.",
      "Comfort with spreadsheets and basic analytics.",
      "Organized, curious, and comfortable presenting ideas.",
    ],
  },
  {
    id: "careem-data",
    company: "Careem",
    role: "Product Data Intern",
    type: "Data Analyst",
    city: "Karachi",
    workMode: "Hybrid",
    paid: true,
    stipend: "Rs. 50,000 / month",
    duration: "3 months",
    skills: ["SQL", "Python", "Tableau", "Product Analytics"],
    deadline: "July 28, 2026",
    deadlineDate: "2026-07-28",
    postedAt: "2026-06-26T10:30:00",
    postedTime: "4 days ago",
    featured: false,
    tone: "sky",
    description:
      "Partner with product teams to explore customer behavior, define useful metrics, and turn operational data into clear recommendations.",
    responsibilities: [
      "Write SQL queries for product and operational analysis.",
      "Build recurring dashboards and investigate metric changes.",
      "Present concise insights to cross-functional partners.",
    ],
    requirements: [
      "Strong analytical reasoning and SQL fundamentals.",
      "Experience with Python or a visualization tool.",
      "Ability to explain findings without unnecessary jargon.",
    ],
  },
  {
    id: "codeninja-backend",
    company: "CodeNinja",
    role: "Backend Software Intern",
    type: "Backend",
    city: "Faisalabad",
    workMode: "Remote",
    paid: false,
    stipend: "Unpaid · completion certificate",
    duration: "6 weeks",
    skills: ["Node.js", "MongoDB", "Git", "APIs"],
    deadline: "August 5, 2026",
    deadlineDate: "2026-08-05",
    postedAt: "2026-06-25T12:00:00",
    postedTime: "5 days ago",
    featured: false,
    tone: "violet",
    description:
      "A structured remote learning internship for students building their first complete backend project with weekly mentor reviews.",
    responsibilities: [
      "Build a small API project through weekly milestones.",
      "Document endpoints and test core flows.",
      "Present the final project to mentors and peers.",
    ],
    requirements: [
      "Basic JavaScript and programming fundamentals.",
      "Willingness to learn Git and backend development.",
      "Reliable weekly availability for mentor sessions.",
    ],
  },
  {
    id: "netsol-java",
    company: "NETSOL Technologies",
    role: "Associate Java Intern",
    type: "Java",
    city: "Lahore",
    workMode: "Onsite",
    paid: true,
    stipend: "Rs. 32,000 / month",
    duration: "10 weeks",
    skills: ["Java", "Data Structures", "SQL", "Problem Solving"],
    deadline: "August 10, 2026",
    deadlineDate: "2026-08-10",
    postedAt: "2026-06-24T11:00:00",
    postedTime: "6 days ago",
    featured: false,
    tone: "amber",
    description:
      "Strengthen enterprise Java fundamentals through guided feature work, debugging exercises, and technical workshops at NETSOL’s Lahore campus.",
    responsibilities: [
      "Complete guided Java and SQL development tasks.",
      "Debug defects and write clear technical notes.",
      "Join technical workshops and peer review sessions.",
    ],
    requirements: [
      "Java, OOP, and data-structure fundamentals.",
      "Comfort writing SQL queries.",
      "Strong problem-solving habits and willingness to learn.",
    ],
  },
];

export const internshipCityOptions = [
  { label: "All cities", value: "all" },
  { label: "Islamabad", value: "Islamabad" },
  { label: "Lahore", value: "Lahore" },
  { label: "Karachi", value: "Karachi" },
  { label: "Rawalpindi", value: "Rawalpindi" },
  { label: "Faisalabad", value: "Faisalabad" },
];

export const internshipTypeOptions = [
  { label: "All internship types", value: "all" },
  { label: "Frontend", value: "Frontend" },
  { label: "Backend", value: "Backend" },
  { label: "Java", value: "Java" },
  { label: "UI/UX", value: "UI/UX" },
  { label: "Data Analyst", value: "Data Analyst" },
  { label: "AI/ML", value: "AI/ML" },
  { label: "Marketing", value: "Marketing" },
];

export const internshipWorkModeOptions = [
  { label: "All work modes", value: "all" },
  { label: "Remote", value: "Remote" },
  { label: "Onsite", value: "Onsite" },
  { label: "Hybrid", value: "Hybrid" },
];

export const internshipPaidOptions = [
  { label: "Paid and unpaid", value: "all" },
  { label: "Paid", value: "paid" },
  { label: "Unpaid", value: "unpaid" },
];

export const internshipSkillOptions = [
  { label: "All skills", value: "all" },
  { label: "React", value: "React" },
  { label: "Java", value: "Java" },
  { label: "Python", value: "Python" },
  { label: "SQL", value: "SQL" },
  { label: "Figma", value: "Figma" },
  { label: "Machine Learning", value: "Machine Learning" },
  { label: "Node.js", value: "Node.js" },
  { label: "Marketing", value: "Content" },
];

export const internshipDeadlineOptions = [
  { label: "Any deadline", value: "all" },
  { label: "Next 7 days", value: "week" },
  { label: "Next 14 days", value: "two-weeks" },
  { label: "Next 30 days", value: "month" },
  { label: "More than 30 days", value: "later" },
];

export const internshipSortOptions = [
  { label: "Newest first", value: "newest" },
  { label: "Deadline soon", value: "deadline" },
  { label: "Paid first", value: "paid" },
];

export const internshipPostCityOptions = [
  { label: "Select city", value: "", disabled: true },
  ...internshipCityOptions.slice(1),
];

export const internshipPostWorkModeOptions = [
  { label: "Select work mode", value: "", disabled: true },
  ...internshipWorkModeOptions.slice(1),
];

export const internshipPostPaidOptions = [
  { label: "Select compensation", value: "", disabled: true },
  { label: "Paid", value: "paid" },
  { label: "Unpaid", value: "unpaid" },
];

export const careerTips: Array<{
  description: string;
  icon: LucideIcon;
  title: string;
  tone: "brand" | "emerald" | "amber" | "sky";
}> = [
  {
    title: "Write a strong CV",
    description:
      "Turn course projects into clear evidence of skills and outcomes.",
    icon: FileText,
    tone: "brand",
  },
  {
    title: "Prepare for interviews",
    description:
      "Practice explaining your decisions, not only the final answer.",
    icon: MessagesSquare,
    tone: "emerald",
  },
  {
    title: "Build valuable CS skills",
    description:
      "Prioritize fundamentals, one practical stack, Git, and communication.",
    icon: Code2,
    tone: "amber",
  },
  {
    title: "Improve your LinkedIn",
    description:
      "Use a focused headline, featured projects, and measurable details.",
    icon: ContactRound,
    tone: "sky",
  },
];

export const internshipBrainIcon = BrainCircuit;
