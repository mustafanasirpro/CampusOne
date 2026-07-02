package com.campusone.note.repository;

import com.campusone.note.entity.NoteModerationAction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteModerationActionRepository
        extends JpaRepository<NoteModerationAction, UUID> {

    List<NoteModerationAction> findAllByNoteIdOrderByCreatedAtDesc(UUID noteId);
}
