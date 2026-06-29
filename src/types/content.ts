export interface ProductSummary {
  condition: string;
  imageUrl?: string;
  location: string;
  price: string;
  seller: string;
  title: string;
}

export interface NoteSummary {
  bookmarked?: boolean;
  course: string;
  downloads: number;
  rating: number;
  title: string;
  uploader: string;
}

export interface DiscussionSummary {
  author: string;
  category: string;
  comments: number;
  tags: string[];
  time: string;
  title: string;
  upvotes: number;
}

export interface EventSummary {
  category: string;
  date: string;
  organizer: string;
  title: string;
  venue: string;
}

export interface InternshipSummary {
  company: string;
  deadline: string;
  location: string;
  paid: boolean;
  remote: boolean;
  role: string;
  saved?: boolean;
}

export interface LeaderboardEntry {
  badges: number;
  name: string;
  rank: number;
  university: string;
  xp: number;
}

