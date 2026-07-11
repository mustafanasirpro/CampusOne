package com.campusone.note.repository;

import java.util.UUID;

public interface NoteSearchRepository {

    NoteSearchResult searchPublicNotes(
            String normalizedQuery,
            UUID courseId,
            String normalizedCourseFilter,
            String normalizedTagFilter,
            long offset,
            int limit);
}
