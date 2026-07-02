package com.campusone.note.repository;

import com.campusone.note.entity.NoteRating;
import com.campusone.note.entity.NoteRatingId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRatingRepository extends JpaRepository<NoteRating, NoteRatingId> {

    Optional<NoteRating> findByNoteIdAndUserId(UUID noteId, UUID userId);
}
