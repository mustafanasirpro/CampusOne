export type NotificationPreferenceKey =
  | "emailDigest"
  | "discussionReplies"
  | "notesUpdates"
  | "internshipAlerts"
  | "eventReminders"
  | "marketplaceMessages";

export type PrivacyPreferenceKey =
  | "showActivity"
  | "showContributions"
  | "allowMessages"
  | "personalizedSuggestions";

export const notificationPreferences: Array<{
  description: string;
  key: NotificationPreferenceKey;
  label: string;
}> = [
  {
    key: "emailDigest",
    label: "Weekly email digest",
    description: "A concise Sunday summary of your campus activity.",
  },
  {
    key: "discussionReplies",
    label: "Discussion replies",
    description: "Notify me when someone answers or mentions me.",
  },
  {
    key: "notesUpdates",
    label: "Notes and course updates",
    description: "New material for courses and teachers I follow.",
  },
  {
    key: "internshipAlerts",
    label: "Internship alerts",
    description: "Relevant opportunities and approaching deadlines.",
  },
  {
    key: "eventReminders",
    label: "Event reminders",
    description: "Remind me before registered events begin.",
  },
  {
    key: "marketplaceMessages",
    label: "Marketplace messages",
    description: "Replies from buyers and sellers on my listings.",
  },
];

export const initialNotificationPreferences: Record<
  NotificationPreferenceKey,
  boolean
> = {
  emailDigest: true,
  discussionReplies: true,
  notesUpdates: true,
  internshipAlerts: true,
  eventReminders: true,
  marketplaceMessages: false,
};

export const privacyPreferences: Array<{
  description: string;
  key: PrivacyPreferenceKey;
  label: string;
}> = [
  {
    key: "showActivity",
    label: "Show active status",
    description: "Let other students see when I am active on CampusOne.",
  },
  {
    key: "showContributions",
    label: "Public contribution history",
    description: "Display notes, answers, badges, and campus activity.",
  },
  {
    key: "allowMessages",
    label: "Allow student messages",
    description: "Let verified students at my university contact me.",
  },
  {
    key: "personalizedSuggestions",
    label: "Personalized suggestions",
    description: "Use my activity to improve course and opportunity matches.",
  },
];

export const initialPrivacyPreferences: Record<
  PrivacyPreferenceKey,
  boolean
> = {
  showActivity: true,
  showContributions: true,
  allowMessages: true,
  personalizedSuggestions: false,
};

export const universityOptions = [
  { label: "COMSATS Islamabad", value: "comsats-islamabad" },
  { label: "FAST Islamabad", value: "fast-islamabad" },
  { label: "NUST", value: "nust" },
  { label: "UET Lahore", value: "uet-lahore" },
  { label: "Punjab University", value: "pu" },
  { label: "LUMS", value: "lums" },
];

export const departmentOptions = [
  { label: "Computer Science", value: "computer-science" },
  { label: "Software Engineering", value: "software-engineering" },
  { label: "Artificial Intelligence", value: "artificial-intelligence" },
  { label: "Data Science", value: "data-science" },
];

export const semesterOptions = Array.from({ length: 8 }, (_, index) => ({
  label: `Semester ${index + 1}`,
  value: String(index + 1),
}));

export const languageOptions = [
  { label: "English", value: "english" },
  { label: "Urdu", value: "urdu" },
  { label: "English & Urdu", value: "bilingual" },
];

export const profileVisibilityOptions = [
  { label: "CampusOne community", value: "community" },
  { label: "My university only", value: "university" },
  { label: "Only me", value: "private" },
];

export const themeOptions = [
  {
    id: "light" as const,
    title: "Light",
    description: "Clean and bright",
  },
  {
    id: "system" as const,
    title: "System",
    description: "Match this device",
  },
  {
    id: "dark" as const,
    title: "Dark",
    description: "Low-light preview",
  },
];

export type ThemePreference = (typeof themeOptions)[number]["id"];

export const initialAccount = {
  fullName: "Ali Khan",
  email: "ali.khan@student.comsats.edu.pk",
  phone: "+92 300 1234567",
  studentId: "FA22-BCS-117",
};

export const initialProfilePreferences = {
  university: "comsats-islamabad",
  department: "computer-science",
  semester: "6",
  language: "english",
  compactDashboard: false,
};

export const activeSessions = [
  {
    id: "current-session",
    device: "Chrome on Windows",
    location: "Islamabad, Pakistan",
    lastActive: "Active now",
    current: true,
  },
  {
    id: "mobile-session",
    device: "CampusOne on Android",
    location: "Islamabad, Pakistan",
    lastActive: "2 hours ago",
    current: false,
  },
];
