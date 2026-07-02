package com.campusone.note.repository;

import com.campusone.note.entity.NoteDownloadEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteDownloadEventRepository
        extends JpaRepository<NoteDownloadEvent, UUID> {

    long countByNoteId(UUID noteId);
}
