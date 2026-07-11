package com.campusone.note.repository;

import com.campusone.note.entity.Note;
import com.campusone.note.entity.NoteModerationStatus;
import com.campusone.note.entity.NoteVisibility;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, UUID> {

    @EntityGraph(attributePaths = {
        "uploader",
        "uploader.studentProfile",
        "uploader.studentProfile.university",
        "course",
        "course.department",
        "fileAsset",
        "tags"
    })
    @Query("""
            select note
            from Note note
            where note.deletedAt is null
              and note.moderationStatus = :status
              and note.visibility = :visibility
              and (:courseId is null or note.course.id = :courseId)
              and (
                    :courseQuery is null
                    or lower(note.course.courseCode) like :courseQuery escape '\\'
                    or lower(note.course.title) like :courseQuery escape '\\'
              )
              and (
                    :normalizedTag is null
                    or exists (
                        select tag.id
                        from note.tags tag
                        where tag.normalizedName = :normalizedTag
                    )
              )
            """)
    Page<Note> findPublicNotes(
            @Param("status") NoteModerationStatus status,
            @Param("visibility") NoteVisibility visibility,
            @Param("courseId") UUID courseId,
            @Param("courseQuery") String courseQuery,
            @Param("normalizedTag") String normalizedTag,
            Pageable pageable);

    default Page<Note> findPublicNotes(
            NoteModerationStatus status,
            NoteVisibility visibility,
            UUID courseId,
            String normalizedTag,
            Pageable pageable) {
        return findPublicNotes(
                status,
                visibility,
                courseId,
                null,
                normalizedTag,
                pageable);
    }

    @EntityGraph(attributePaths = {
        "uploader",
        "uploader.studentProfile",
        "uploader.studentProfile.university",
        "course",
        "course.department",
        "fileAsset",
        "tags"
    })
    @Query("""
            select note
            from Note note
            where note.deletedAt is null
              and note.uploader.id = :userId
            """)
    Page<Note> findMyNotes(
            @Param("userId") UUID userId,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "uploader",
        "uploader.studentProfile",
        "uploader.studentProfile.university",
        "course",
        "course.department",
        "fileAsset",
        "tags"
    })
    @Query("""
            select note
            from Note note
            where note.id in :noteIds
            """)
    List<Note> findSummariesByIdIn(@Param("noteIds") Collection<UUID> noteIds);

    @EntityGraph(attributePaths = {
        "uploader",
        "uploader.studentProfile",
        "uploader.studentProfile.university",
        "course",
        "course.department",
        "course.department.university",
        "fileAsset",
        "tags"
    })
    @Query("""
            select note
            from Note note
            where note.id = :noteId
              and note.deletedAt is null
            """)
    Optional<Note> findDetailedById(@Param("noteId") UUID noteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select note
            from Note note
            where note.id = :noteId
              and note.deletedAt is null
            """)
    Optional<Note> findActiveByIdForUpdate(@Param("noteId") UUID noteId);

    Page<Note> findAllByModerationStatusAndDeletedAtIsNull(
            NoteModerationStatus moderationStatus,
            Pageable pageable);
}
