import {
  BadgeCheck,
  Hash,
  MessageSquareText,
  TrendingUp,
  UsersRound,
  type LucideIcon,
} from "lucide-react";

export interface CampusDiscussion {
  author: string;
  category: string;
  comments: number;
  course: string;
  department: string;
  fullQuestion: string;
  id: string;
  postedAt: string;
  preview: string;
  tags: string[];
  time: string;
  title: string;
  university: string;
  upvotes: number;
  views: number;
}

export interface DiscussionComment {
  author: string;
  body: string;
  id: string;
  likes: number;
  replyingTo?: string;
  time: string;
  university: string;
}

export const discussionStats: Array<{
  change: number;
  icon: LucideIcon;
  label: string;
  value: string;
}> = [
  {
    label: "Total Discussions",
    value: "8,942",
    change: 12,
    icon: MessageSquareText,
  },
  {
    label: "Questions Answered",
    value: "6,718",
    change: 9,
    icon: BadgeCheck,
  },
  {
    label: "Active Students",
    value: "2,346",
    change: 18,
    icon: UsersRound,
  },
  {
    label: "Trending Topics",
    value: "128",
    change: 6,
    icon: TrendingUp,
  },
];

export const discussionCategories = [
  "All",
  "General",
  "Programming",
  "Exams",
  "Assignments",
  "Career",
  "Internships",
  "Campus Life",
  "Hostel",
  "Events",
] as const;

export const discussionTrendingTags = [
  "Java",
  "React",
  "OOP",
  "Database",
  "FinalExams",
  "Internships",
  "Hackathon",
  "COMSATS",
];

export const discussionCourseOptions = [
  { label: "Select a course", value: "", disabled: true },
  { label: "General / No course", value: "General" },
  { label: "Programming Fundamentals", value: "Programming Fundamentals" },
  { label: "Object Oriented Programming", value: "OOP" },
  { label: "Data Structures", value: "Data Structures" },
  { label: "Database Systems", value: "Database Systems" },
  { label: "Computer Networks", value: "Computer Networks" },
  { label: "Software Engineering", value: "Software Engineering" },
];

export const discussionCategoryOptions = [
  { label: "Select a category", value: "", disabled: true },
  ...discussionCategories
    .filter((category) => category !== "All")
    .map((category) => ({ label: category, value: category })),
];

export const campusDiscussions: CampusDiscussion[] = [
  {
    id: "java-interfaces-final",
    author: "Hamza Raza",
    university: "FAST Islamabad",
    department: "Computer Science",
    time: "18 min ago",
    postedAt: "2026-06-29T14:42:00",
    title: "How do I build intuition for Java interfaces before the OOP final?",
    preview:
      "I understand the syntax, but I still struggle to decide when an interface is better than an abstract class in exam scenarios.",
    fullQuestion:
      "I understand the syntax for interfaces and abstract classes, but I still struggle to decide which one fits a design question. Our final usually gives a small system and asks us to justify the relationship between classes.\n\nCan someone explain a practical decision process, preferably with one university-style example? I would also appreciate any common mistakes to avoid in the exam.",
    category: "Programming",
    course: "OOP",
    tags: ["Java", "OOP", "FinalExams"],
    upvotes: 126,
    comments: 34,
    views: 892,
  },
  {
    id: "database-indexing-query",
    author: "Sara Ahmed",
    university: "COMSATS Islamabad",
    department: "Software Engineering",
    time: "42 min ago",
    postedAt: "2026-06-29T14:18:00",
    title: "Which database index would you choose for this query pattern?",
    preview:
      "Most queries filter by student_id and semester, then sort by created_at. Would a composite index be the right answer?",
    fullQuestion:
      "I am optimizing a course project where the results table is usually filtered by student_id and semester, then ordered by created_at. Individual indexes helped a little, but the query plan still scans more rows than expected.\n\nWould a composite index on (student_id, semester, created_at) be appropriate here? How should I explain the leftmost-prefix rule in my project viva?",
    category: "Programming",
    course: "Database Systems",
    tags: ["Database", "SQL", "Projects"],
    upvotes: 94,
    comments: 27,
    views: 641,
  },
  {
    id: "final-exam-schedule",
    author: "Ayesha Malik",
    university: "NUST",
    department: "Data Science",
    time: "1 hour ago",
    postedAt: "2026-06-29T13:35:00",
    title: "Final exam schedule overlap — who should we contact first?",
    preview:
      "Two departmental electives appear at the same time on the provisional schedule. Has anyone handled this before?",
    fullQuestion:
      "The provisional final schedule places two electives from our approved study plan in the same time slot. Several students in our batch are registered in both.\n\nShould we email the department coordinator first, or submit the clash form directly to the examination office? If anyone has resolved this before, please share the fastest process.",
    category: "Exams",
    course: "General",
    tags: ["FinalExams", "NUST", "Schedule"],
    upvotes: 78,
    comments: 19,
    views: 533,
  },
  {
    id: "react-assignment-state",
    author: "Bilal Ahmed",
    university: "UET Lahore",
    department: "Computer Science",
    time: "2 hours ago",
    postedAt: "2026-06-29T12:24:00",
    title: "React assignment: should filters live in state or the URL?",
    preview:
      "Our task is a searchable product dashboard. I want refresh and back navigation to preserve the selected filters.",
    fullQuestion:
      "For our web engineering assignment, we are building a searchable product dashboard with categories, price range, and sorting. Keeping everything in component state works, but refresh resets the view.\n\nWould synchronizing filters with URL search parameters be considered overengineering for a student project, or is it the cleaner approach? I am using React Router.",
    category: "Assignments",
    course: "Software Engineering",
    tags: ["React", "Assignments", "Frontend"],
    upvotes: 72,
    comments: 22,
    views: 487,
  },
  {
    id: "frontend-internship-interview",
    author: "Maham Iqbal",
    university: "LUMS",
    department: "Computer Science",
    time: "3 hours ago",
    postedAt: "2026-06-29T11:08:00",
    title: "What should I expect in a Pakistani frontend internship interview?",
    preview:
      "I have my first technical interview next week. The role mentions JavaScript, React, APIs, and basic data structures.",
    fullQuestion:
      "I have my first technical interview for a frontend internship next week. The job description mentions JavaScript, React, REST APIs, Git, and basic data structures.\n\nFor students who recently interviewed at local software companies: what was the balance between theory, live coding, and portfolio discussion? Which topics gave you the highest return during preparation?",
    category: "Internships",
    course: "General",
    tags: ["Internships", "React", "Career"],
    upvotes: 143,
    comments: 46,
    views: 1204,
  },
  {
    id: "career-fair-companies",
    author: "Usman Tariq",
    university: "COMSATS Islamabad",
    department: "Computer Science",
    time: "5 hours ago",
    postedAt: "2026-06-29T09:20:00",
    title: "Companies confirmed for the Islamabad campus career fair",
    preview:
      "Sharing the current list, booth timings, and a few practical tips from last year’s event.",
    fullQuestion:
      "The placement office has confirmed Systems Limited, Arbisoft, Devsinc, 10Pearls, and several Islamabad-based startups for Friday’s career fair.\n\nI have added the booth timings below. Students who attended last year: please share advice on CV copies, introductions, and whether companies conducted screening interviews on the spot.",
    category: "Career",
    course: "General",
    tags: ["Career", "COMSATS", "Internships"],
    upvotes: 187,
    comments: 53,
    views: 1638,
  },
  {
    id: "hostel-internet",
    author: "Noor Fatima",
    university: "FAST Islamabad",
    department: "Artificial Intelligence",
    time: "Yesterday",
    postedAt: "2026-06-28T19:15:00",
    title: "Reliable backup internet options for hostel students?",
    preview:
      "The hostel connection becomes unusable during evening quizzes. Looking for affordable packages with stable coverage.",
    fullQuestion:
      "Our hostel internet becomes unreliable during evening quizzes and assignment submissions. I need a backup connection that works consistently around H-11 without an expensive long-term contract.\n\nWhich mobile network or portable device has worked best for other hostel students? Please mention approximate monthly cost and real-world speed.",
    category: "Hostel",
    course: "General",
    tags: ["Hostel", "FAST", "StudentLife"],
    upvotes: 61,
    comments: 38,
    views: 714,
  },
  {
    id: "ai-hackathon-team",
    author: "Hira Javed",
    university: "Punjab University",
    department: "Data Science",
    time: "Yesterday",
    postedAt: "2026-06-28T16:40:00",
    title: "Looking for two teammates for the AI Builders Hackathon",
    preview:
      "We have a frontend developer and an idea around Urdu study assistance. Looking for ML and product-minded teammates.",
    fullQuestion:
      "We are forming a team for the AI Builders Hackathon next weekend. Our idea is an Urdu-friendly study assistant for first-year university students, and we already have a frontend developer.\n\nWe are looking for one teammate comfortable with ML/NLP and another who enjoys product design or pitching. Beginners are welcome if you can commit for the full weekend.",
    category: "Events",
    course: "General",
    tags: ["Hackathon", "AI", "Team"],
    upvotes: 109,
    comments: 41,
    views: 928,
  },
  {
    id: "society-joining-advice",
    author: "Zain Hassan",
    university: "UET Lahore",
    department: "Software Engineering",
    time: "2 days ago",
    postedAt: "2026-06-27T13:12:00",
    title: "Is joining two technical societies manageable with coursework?",
    preview:
      "I want to join both ACM and the robotics society, but I am unsure how much weekly time active members usually spend.",
    fullQuestion:
      "I am entering my third semester and want to become more involved on campus. Both ACM and the robotics society are recruiting, and their projects sound useful.\n\nIs it realistic to contribute actively to both while managing a full course load? I would love to hear how current society members plan their week.",
    category: "Campus Life",
    course: "General",
    tags: ["CampusLife", "Societies", "UET"],
    upvotes: 55,
    comments: 29,
    views: 462,
  },
  {
    id: "campusone-feedback",
    author: "Ali Khan",
    university: "COMSATS Islamabad",
    department: "Computer Science",
    time: "3 days ago",
    postedAt: "2026-06-26T10:05:00",
    title: "What feature would make one campus platform genuinely useful?",
    preview:
      "Notes and discussions are obvious, but I am curious which daily student problem deserves better software.",
    fullQuestion:
      "Most campus platforms begin with notes, announcements, and discussions. Those are valuable, but students still switch between many groups and apps every day.\n\nWhat is one recurring campus problem you would want a unified student platform to solve exceptionally well? Concrete examples from your university would be especially helpful.",
    category: "General",
    course: "General",
    tags: ["COMSATS", "CampusLife", "Product"],
    upvotes: 96,
    comments: 44,
    views: 817,
  },
];

export const discussionComments: Record<string, DiscussionComment[]> = {
  "java-interfaces-final": [
    {
      id: "comment-java-1",
      author: "Sara Ahmed",
      university: "COMSATS Islamabad",
      time: "10 min ago",
      likes: 31,
      body: "A useful rule is that an abstract class models a shared identity with partial implementation, while an interface describes a capability. In an exam, ask whether the classes truly belong to one hierarchy or simply promise the same behavior.",
    },
    {
      id: "comment-java-2",
      author: "Dr. Hina Tariq",
      university: "COMSATS Islamabad",
      time: "4 min ago",
      likes: 46,
      body: "Try modeling PaymentMethod as an interface implemented by CardPayment and BankTransfer. They share a contract, not state. A Vehicle base class is a better abstract-class example because the subclasses share identity and common data.",
    },
  ],
  "database-indexing-query": [
    {
      id: "comment-db-1",
      author: "Usman Tariq",
      university: "NUST",
      time: "25 min ago",
      likes: 24,
      body: "That composite index is a sensible starting point. Verify it with EXPLAIN, and mention that created_at can support ordering only after the equality predicates on the leading columns are satisfied.",
    },
  ],
  "final-exam-schedule": [
    {
      id: "comment-exam-1",
      author: "Ayesha Noor",
      university: "NUST",
      time: "48 min ago",
      likes: 18,
      body: "Send one consolidated email through the class representative to the department coordinator, then attach that email when submitting the clash form. That resolved ours last semester.",
    },
  ],
  "react-assignment-state": [
    {
      id: "comment-react-1",
      author: "Hamza Raza",
      university: "FAST Islamabad",
      time: "1 hour ago",
      likes: 29,
      body: "URL search parameters are appropriate here because the filtered view becomes shareable and survives refresh. Keep transient UI state, such as an open dropdown, inside the component.",
    },
  ],
  "frontend-internship-interview": [
    {
      id: "comment-intern-1",
      author: "Bilal Ahmed",
      university: "UET Lahore",
      time: "2 hours ago",
      likes: 41,
      body: "My interview was roughly 40% JavaScript fundamentals, 30% a small React task, and 30% discussing projects. Be ready to explain one tradeoff you made instead of only showing the final UI.",
    },
    {
      id: "comment-intern-2",
      author: "Maham Iqbal",
      university: "LUMS",
      time: "90 min ago",
      likes: 16,
      replyingTo: "Bilal Ahmed",
      body: "This breakdown is exactly what I needed. I will revisit closures, event loop, state updates, and API error handling first.",
    },
  ],
  "career-fair-companies": [
    {
      id: "comment-career-1",
      author: "Noor Fatima",
      university: "FAST Islamabad",
      time: "4 hours ago",
      likes: 37,
      body: "Bring ten concise CV copies and prepare a 20-second introduction. Last year two companies scheduled first-round calls directly from the booth.",
    },
  ],
  "hostel-internet": [
    {
      id: "comment-hostel-1",
      author: "Zain Hassan",
      university: "UET Lahore",
      time: "Yesterday",
      likes: 21,
      body: "Test coverage with prepaid SIMs before buying a device. Performance can change dramatically between hostel blocks even on the same network.",
    },
  ],
  "ai-hackathon-team": [
    {
      id: "comment-hackathon-1",
      author: "Ayesha Malik",
      university: "NUST",
      time: "Yesterday",
      likes: 34,
      body: "I have worked on Urdu text classification and would be interested in the NLP role. I have sent you a message with my GitHub profile.",
    },
  ],
  "society-joining-advice": [
    {
      id: "comment-society-1",
      author: "Ali Raza",
      university: "UET Lahore",
      time: "2 days ago",
      likes: 17,
      body: "Join both introductory sessions, then choose one society for a core role and remain a general member in the other. Two leadership commitments become difficult near exams.",
    },
  ],
  "campusone-feedback": [
    {
      id: "comment-product-1",
      author: "Sara Ahmed",
      university: "COMSATS Islamabad",
      time: "3 days ago",
      likes: 28,
      body: "A trustworthy course-specific resource trail would help: notes, teacher reviews, past papers, and active seniors for the exact course offering in one place.",
    },
  ],
};

export const topDiscussionContributors = [
  { name: "Sara Ahmed", university: "COMSATS", points: 2840 },
  { name: "Hamza Raza", university: "FAST", points: 2510 },
  { name: "Ayesha Malik", university: "NUST", points: 2325 },
  { name: "Bilal Ahmed", university: "UET", points: 1980 },
];

export const popularDiscussionTags = [
  { label: "Java", count: 284 },
  { label: "OOP", count: 246 },
  { label: "Internships", count: 218 },
  { label: "Database", count: 193 },
  { label: "FinalExams", count: 176 },
  { label: "React", count: 164 },
];

export const discussionHashIcon = Hash;

