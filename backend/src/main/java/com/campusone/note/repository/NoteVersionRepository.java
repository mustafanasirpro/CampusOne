package com.campusone.note.repository;

import com.campusone.note.entity.NoteVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteVersionRepository extends JpaRepository<NoteVersion, UUID> {

    List<NoteVersion> findAllByNoteIdOrderByRevisionNumberDesc(UUID noteId);
}
