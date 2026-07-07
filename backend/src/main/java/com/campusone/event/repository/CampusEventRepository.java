package com.campusone.event.repository;

import com.campusone.event.entity.CampusEvent;
import com.campusone.event.entity.EventStatus;
import com.campusone.event.entity.EventVisibility;
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

public interface CampusEventRepository
        extends JpaRepository<CampusEvent, UUID> {

    @EntityGraph(attributePaths = {
        "organizer",
        "organizer.studentProfile",
        "organizer.studentProfile.university"
    })
    @Query("""
            select campusEvent
            from CampusEvent campusEvent
            where campusEvent.deleted = false
              and campusEvent.visibility = :visibility
              and campusEvent.status not in :hiddenStatuses
              and (:status is null or campusEvent.status = :status)
              and (
                    :searchPattern is null
                    or lower(campusEvent.title) like :searchPattern escape '\\'
                    or lower(campusEvent.location) like :searchPattern escape '\\'
              )
            """)
    Page<CampusEvent> findPublicEvents(
            @Param("visibility") EventVisibility visibility,
            @Param("hiddenStatuses") Set<EventStatus> hiddenStatuses,
            @Param("status") EventStatus status,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "organizer",
        "organizer.studentProfile",
        "organizer.studentProfile.university"
    })
    Page<CampusEvent> findAllByStatusAndDeletedFalse(
            EventStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "organizer",
        "organizer.studentProfile",
        "organizer.studentProfile.university"
    })
    @Query("""
            select campusEvent
            from CampusEvent campusEvent
            where campusEvent.deleted = false
              and campusEvent.organizer.id = :organizerUserId
            """)
    Page<CampusEvent> findOrganizedByUser(
            @Param("organizerUserId") UUID organizerUserId,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "organizer",
        "organizer.studentProfile",
        "organizer.studentProfile.university"
    })
    @Query("""
            select campusEvent
            from CampusEvent campusEvent
            where campusEvent.deleted = false
              and exists (
                    select participant.id
                    from EventParticipant participant
                    where participant.event = campusEvent
                      and participant.id.userId = :userId
              )
            """)
    Page<CampusEvent> findJoinedByUser(
            @Param("userId") UUID userId,
            Pageable pageable);

    @EntityGraph(attributePaths = {
        "organizer",
        "organizer.studentProfile",
        "organizer.studentProfile.university"
    })
    @Query("""
            select campusEvent
            from CampusEvent campusEvent
            where campusEvent.id = :eventId
              and campusEvent.deleted = false
            """)
    Optional<CampusEvent> findActiveById(@Param("eventId") UUID eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select campusEvent
            from CampusEvent campusEvent
            where campusEvent.id = :eventId
              and campusEvent.deleted = false
            """)
    Optional<CampusEvent> findActiveByIdForUpdate(
            @Param("eventId") UUID eventId);
}
