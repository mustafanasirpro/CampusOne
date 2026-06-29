import {
  Download,
  FileCheck2,
  FileText,
  Star,
  UsersRound,
  type LucideIcon,
} from "lucide-react";

export type NoteFileType = "PDF" | "PPT" | "DOCX" | "Images";

export interface NotesLibraryItem {
  course: string;
  department: string;
  description: string;
  downloads: number;
  fileType: NoteFileType;
  id: string;
  rating: number;
  semester: string;
  tags: string[];
  teacher: string;
  title: string;
  university: string;
  uploadedAt: string;
  uploadDate: string;
  uploader: string;
}

export const notesLibraryStats: Array<{
  description: string;
  icon: LucideIcon;
  label: string;
  value: string;
}> = [
  {
    label: "Total Notes",
    value: "1,248",
    description: "Across 68 courses",
    icon: FileText,
  },
  {
    label: "Downloads",
    value: "48.6k",
    description: "This semester",
    icon: Download,
  },
  {
    label: "Top Rated Notes",
    value: "96",
    description: "Rated 4.8 or higher",
    icon: Star,
  },
  {
    label: "Contributors",
    value: "342",
    description: "Verified students",
    icon: UsersRound,
  },
];

export const notesLibrary: NotesLibraryItem[] = [
  {
    id: "oop-final-uml",
    title: "OOP Final Revision Notes & UML Guide",
    course: "OOP",
    teacher: "Dr. Hina Tariq",
    university: "COMSATS",
    department: "Computer Science",
    semester: "6",
    uploader: "Sara Ahmed",
    uploadedAt: "2026-06-28",
    uploadDate: "Jun 28, 2026",
    fileType: "PDF",
    rating: 4.9,
    downloads: 1842,
    description:
      "A complete final-exam revision pack covering inheritance, polymorphism, interfaces, design principles, and UML diagrams with solved examples.",
    tags: ["oop", "uml", "finals", "java"],
  },
  {
    id: "database-sql-normalization",
    title: "SQL, Normalization & Transactions Cheatsheet",
    course: "Database Systems",
    teacher: "Sir Usman Khalid",
    university: "FAST",
    department: "Computer Science",
    semester: "5",
    uploader: "Hamza Raza",
    uploadedAt: "2026-06-26",
    uploadDate: "Jun 26, 2026",
    fileType: "PDF",
    rating: 4.9,
    downloads: 2316,
    description:
      "Concise database revision notes with SQL syntax, normalization steps, transaction schedules, indexing, and common exam patterns.",
    tags: ["sql", "normalization", "database", "transactions"],
  },
  {
    id: "dsa-past-papers",
    title: "Data Structures Past Papers — Solved",
    course: "Data Structures",
    teacher: "Dr. Areeba Noor",
    university: "NUST",
    department: "Software Engineering",
    semester: "4",
    uploader: "Ayesha Malik",
    uploadedAt: "2026-06-24",
    uploadDate: "Jun 24, 2026",
    fileType: "PDF",
    rating: 4.8,
    downloads: 2984,
    description:
      "Five years of solved past papers covering linked lists, trees, graphs, hashing, sorting, and asymptotic analysis.",
    tags: ["dsa", "past-papers", "graphs", "trees"],
  },
  {
    id: "networks-complete-handouts",
    title: "Computer Networks Complete Handouts",
    course: "Computer Networks",
    teacher: "Prof. Salman Qureshi",
    university: "UET",
    department: "Computer Science",
    semester: "6",
    uploader: "Bilal Ahmed",
    uploadedAt: "2026-06-21",
    uploadDate: "Jun 21, 2026",
    fileType: "PPT",
    rating: 4.7,
    downloads: 1653,
    description:
      "Lecture-by-lecture slides for the OSI model, TCP/IP, routing, subnetting, congestion control, and application protocols.",
    tags: ["networks", "tcp-ip", "subnetting", "routing"],
  },
  {
    id: "pf-cpp-beginners",
    title: "Programming Fundamentals with C++",
    course: "Programming Fundamentals",
    teacher: "Ms. Mahnoor Ali",
    university: "PU",
    department: "Computer Science",
    semester: "1",
    uploader: "Zain Hassan",
    uploadedAt: "2026-06-18",
    uploadDate: "Jun 18, 2026",
    fileType: "DOCX",
    rating: 4.6,
    downloads: 1427,
    description:
      "Beginner-friendly notes with syntax, flow control, functions, arrays, pointers, and practice exercises in C++.",
    tags: ["cpp", "programming", "beginners", "practice"],
  },
  {
    id: "se-srs-templates",
    title: "Software Engineering SRS & UML Templates",
    course: "Software Engineering",
    teacher: "Dr. Farah Shah",
    university: "LUMS",
    department: "Software Engineering",
    semester: "5",
    uploader: "Maham Iqbal",
    uploadedAt: "2026-06-15",
    uploadDate: "Jun 15, 2026",
    fileType: "DOCX",
    rating: 4.8,
    downloads: 1196,
    description:
      "Editable templates for SRS documents, use cases, sequence diagrams, class diagrams, testing plans, and project estimation.",
    tags: ["srs", "uml", "templates", "documentation"],
  },
  {
    id: "ai-neural-networks",
    title: "Neural Networks — Visual Lecture Deck",
    course: "Artificial Intelligence",
    teacher: "Dr. Saad Rehman",
    university: "FAST",
    department: "AI",
    semester: "6",
    uploader: "Hira Javed",
    uploadedAt: "2026-06-12",
    uploadDate: "Jun 12, 2026",
    fileType: "PPT",
    rating: 4.7,
    downloads: 986,
    description:
      "A visual introduction to perceptrons, activation functions, forward propagation, backpropagation, and model evaluation.",
    tags: ["ai", "neural-networks", "deep-learning", "visual"],
  },
  {
    id: "probability-data-science",
    title: "Probability for Data Science",
    course: "Probability & Statistics",
    teacher: "Dr. Maryam Siddiqui",
    university: "NUST",
    department: "Data Science",
    semester: "4",
    uploader: "Usman Tariq",
    uploadedAt: "2026-06-09",
    uploadDate: "Jun 9, 2026",
    fileType: "PDF",
    rating: 4.6,
    downloads: 875,
    description:
      "Core probability concepts for data science with distributions, Bayes theorem, expectation, variance, and solved numerical examples.",
    tags: ["probability", "data-science", "statistics", "bayes"],
  },
  {
    id: "oop-lab-tasks",
    title: "OOP Lab Tasks with Output Screenshots",
    course: "OOP",
    teacher: "Mr. Ahmed Farooq",
    university: "COMSATS",
    department: "Software Engineering",
    semester: "3",
    uploader: "Noor Fatima",
    uploadedAt: "2026-06-06",
    uploadDate: "Jun 6, 2026",
    fileType: "Images",
    rating: 4.5,
    downloads: 742,
    description:
      "Twelve object-oriented programming lab tasks with annotated source code and expected output screenshots.",
    tags: ["oop", "lab", "java", "solutions"],
  },
  {
    id: "db-midterm-pack",
    title: "Database Systems Midterm Preparation Pack",
    course: "Database Systems",
    teacher: "Dr. Kamran Aziz",
    university: "UET",
    department: "Computer Science",
    semester: "5",
    uploader: "Ali Raza",
    uploadedAt: "2026-06-02",
    uploadDate: "Jun 2, 2026",
    fileType: "PDF",
    rating: 4.7,
    downloads: 1318,
    description:
      "Midterm-focused notes covering ER modeling, relational algebra, SQL queries, functional dependencies, and normalization.",
    tags: ["database", "midterm", "sql", "er-model"],
  },
];

export const notesUniversityOptions = [
  { label: "All universities", value: "all" },
  { label: "COMSATS", value: "COMSATS" },
  { label: "FAST", value: "FAST" },
  { label: "NUST", value: "NUST" },
  { label: "UET", value: "UET" },
  { label: "PU", value: "PU" },
  { label: "LUMS", value: "LUMS" },
];

export const notesDepartmentOptions = [
  { label: "All departments", value: "all" },
  { label: "Computer Science", value: "Computer Science" },
  { label: "Software Engineering", value: "Software Engineering" },
  { label: "AI", value: "AI" },
  { label: "Data Science", value: "Data Science" },
];

export const notesSemesterOptions = [
  { label: "All semesters", value: "all" },
  ...Array.from({ length: 8 }, (_, index) => ({
    label: `Semester ${index + 1}`,
    value: String(index + 1),
  })),
];

export const notesCourseOptions = [
  { label: "All courses", value: "all" },
  { label: "Programming Fundamentals", value: "Programming Fundamentals" },
  { label: "OOP", value: "OOP" },
  { label: "Data Structures", value: "Data Structures" },
  { label: "Database Systems", value: "Database Systems" },
  { label: "Computer Networks", value: "Computer Networks" },
  { label: "Software Engineering", value: "Software Engineering" },
  { label: "Artificial Intelligence", value: "Artificial Intelligence" },
  { label: "Probability & Statistics", value: "Probability & Statistics" },
];

export const notesTeacherOptions = [
  { label: "All teachers", value: "all" },
  ...Array.from(new Set(notesLibrary.map((note) => note.teacher)))
    .sort()
    .map((teacher) => ({ label: teacher, value: teacher })),
];

export const notesFileTypeOptions = [
  { label: "All file types", value: "all" },
  { label: "PDF", value: "PDF" },
  { label: "PPT", value: "PPT" },
  { label: "DOCX", value: "DOCX" },
  { label: "Images", value: "Images" },
];

export const noteUploadUniversityOptions = notesUniversityOptions.slice(1);
export const noteUploadDepartmentOptions = notesDepartmentOptions.slice(1);
export const noteUploadSemesterOptions = notesSemesterOptions.slice(1);
export const noteUploadCourseOptions = notesCourseOptions.slice(1);
export const noteUploadFileTypeOptions = notesFileTypeOptions.slice(1);

export const notesFeaturedTabs = [
  { label: "Top rated", value: "top-rated" as const },
  { label: "Most downloaded", value: "most-downloaded" as const },
  { label: "Recently uploaded", value: "recent" as const },
];

export const notesSortOptions = [
  { label: "Newest first", value: "newest" },
  { label: "Highest rated", value: "rating" },
  { label: "Most downloaded", value: "downloads" },
];

export const notesEmptyIcon = FileCheck2;
