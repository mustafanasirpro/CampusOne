package com.campusone.event.repository;

import com.campusone.event.entity.EventParticipant;
import com.campusone.event.entity.EventParticipantId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventParticipantRepository
        extends JpaRepository<EventParticipant, EventParticipantId> {

    @Query("""
            select participant.id.eventId
            from EventParticipant participant
            where participant.id.userId = :userId
              and participant.id.eventId in :eventIds
            """)
    List<UUID> findJoinedEventIds(
            @Param("userId") UUID userId,
            @Param("eventIds") List<UUID> eventIds);
}
