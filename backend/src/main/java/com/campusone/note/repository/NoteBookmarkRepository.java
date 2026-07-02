package com.campusone.note.repository;

import com.campusone.note.entity.NoteBookmark;
import com.campusone.note.entity.NoteBookmarkId;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteBookmarkRepository
        extends JpaRepository<NoteBookmark, NoteBookmarkId> {

    Page<NoteBookmark> findAllByUserIdOrderByCreatedAtDesc(
            UUID userId,
            Pageable pageable);
}
