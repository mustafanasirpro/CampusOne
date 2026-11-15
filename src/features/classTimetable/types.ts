/** Shared types for the class-schedule feature (parsed from an aSc timetable PDF). */

export interface TimetableSlot {
  key: string;
  label: string;
  start: string;
  end: string;
}

export interface TimetableSection {
  id: string;
  term: string;
  program: string;
  semester: number;
  section: string;
  strength: number;
}

export interface TimetableSession {
  sectionId: string;
  day: string;
  slot: string;
  start: string;
  end: string;
  span: number;
  course: string;
  instructor: string | null;
  room: string | null;
  lab: boolean;
}

export interface TimetableMeta {
  university: string;
  term: string;
  generatedOn: string;
  source: string;
  days: string[];
  slots: TimetableSlot[];
  importedAt?: string;
}

export interface TimetableData {
  meta: TimetableMeta;
  sections: TimetableSection[];
  sessions: TimetableSession[];
}
