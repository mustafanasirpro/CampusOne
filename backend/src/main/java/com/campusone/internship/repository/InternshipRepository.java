package com.campusone.internship.repository;

import com.campusone.internship.entity.Internship;
import com.campusone.internship.entity.InternshipStatus;
import com.campusone.internship.entity.InternshipType;
import com.campusone.internship.entity.WorkMode;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InternshipRepository
        extends JpaRepository<Internship, UUID> {

    @EntityGraph(attributePaths = {
        "poster",
        "poster.studentProfile",
        "poster.studentProfile.university"
    })
    @Query("""
            select internship
            from Internship internship
            where internship.deleted = false
              and internship.status not in :hiddenStatuses
              and (:status is null or internship.status = :status)
              and (:internshipType is null
                    or internship.internshipType = :internshipType)
              and (:workMode is null or internship.workMode = :workMode)
              and (:paid is null or internship.paid = :paid)
              and (
                    :searchPattern is null
                    or lower(internship.title) like :searchPattern escape '\\'
                    or lower(internship.companyName) like :searchPattern escape '\\'
                    or lower(internship.location) like :searchPattern escape '\\'
              )
            """)
    Page<Internship> findVisibleInternships(
            @Param("hiddenStatuses") Set<InternshipStatus> hiddenStatuses,
            @Param("status") InternshipStatus status,
            @Param("internshipType") InternshipType internshipType,
            @Param("workMode") WorkMode workMode,
            @Param("paid") Boolean paid,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "poster",
        "poster.studentProfile",
        "poster.studentProfile.university"
    })
    Page<Internship> findAllByStatusAndDeletedFalse(
            InternshipStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "poster",
        "poster.studentProfile",
        "poster.studentProfile.university"
    })
    @Query("""
            select internship
            from Internship internship
            where internship.deleted = false
              and internship.poster.id = :posterUserId
              and (:status is null or internship.status = :status)
              and (:internshipType is null
                    or internship.internshipType = :internshipType)
              and (:workMode is null or internship.workMode = :workMode)
              and (:paid is null or internship.paid = :paid)
              and (
                    :searchPattern is null
                    or lower(internship.title) like :searchPattern escape '\\'
                    or lower(internship.companyName) like :searchPattern escape '\\'
                    or lower(internship.location) like :searchPattern escape '\\'
              )
            """)
    Page<Internship> findPostedByUser(
            @Param("posterUserId") UUID posterUserId,
            @Param("status") InternshipStatus status,
            @Param("internshipType") InternshipType internshipType,
            @Param("workMode") WorkMode workMode,
            @Param("paid") Boolean paid,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "poster",
        "poster.studentProfile",
        "poster.studentProfile.university"
    })
    @Query("""
            select internship
            from Internship internship
            where internship.deleted = false
              and internship.status not in :hiddenStatuses
              and exists (
                    select saved.id
                    from SavedInternship saved
                    where saved.internship = internship
                      and saved.id.userId = :userId
              )
            """)
    Page<Internship> findSavedByUser(
            @Param("userId") UUID userId,
            @Param("hiddenStatuses") Set<InternshipStatus> hiddenStatuses,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "poster",
        "poster.studentProfile",
        "poster.studentProfile.university"
    })
    @Query("""
            select internship
            from Internship internship
            where internship.id = :internshipId
              and internship.deleted = false
            """)
    Optional<Internship> findActiveById(
            @Param("internshipId") UUID internshipId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select internship
            from Internship internship
            where internship.id = :internshipId
              and internship.deleted = false
            """)
    Optional<Internship> findActiveByIdForUpdate(
            @Param("internshipId") UUID internshipId);
}
