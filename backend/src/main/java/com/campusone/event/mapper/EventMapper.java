package com.campusone.event.mapper;

import com.campusone.event.dto.response.EventDetailResponse;
import com.campusone.event.dto.response.EventOrganizerResponse;
import com.campusone.event.dto.response.EventParticipantResponse;
import com.campusone.event.dto.response.EventSummaryResponse;
import com.campusone.event.entity.CampusEvent;
import com.campusone.event.entity.EventParticipant;
import com.campusone.user.entity.StudentProfile;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventSummaryResponse toSummary(
            CampusEvent event,
            boolean joinedByCurrentUser,
            boolean ownedByCurrentUser) {
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartTime(),
                event.getEndTime(),
                event.getCapacity(),
                event.getParticipantCount(),
                event.getVisibility(),
                event.getStatus(),
                toOrganizer(event),
                joinedByCurrentUser,
                ownedByCurrentUser,
                event.getCreatedAt(),
                event.getUpdatedAt());
    }

    public EventDetailResponse toDetail(
            CampusEvent event,
            boolean joinedByCurrentUser,
            boolean ownedByCurrentUser) {
        return new EventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartTime(),
                event.getEndTime(),
                event.getCapacity(),
                event.getParticipantCount(),
                event.getVisibility(),
                event.getStatus(),
                toOrganizer(event),
                joinedByCurrentUser,
                ownedByCurrentUser,
                event.getCreatedAt(),
                event.getUpdatedAt());
    }

    public EventParticipantResponse toParticipant(
            EventParticipant participant,
            int participantCount) {
        return new EventParticipantResponse(
                participant.getEvent().getId(),
                participant.getUser().getId(),
                true,
                participant.getJoinedAt(),
                participantCount);
    }

    private EventOrganizerResponse toOrganizer(CampusEvent event) {
        StudentProfile profile = event.getOrganizer().getStudentProfile();
        return new EventOrganizerResponse(
                event.getOrganizer().getId(),
                profile == null ? null : profile.getFullName(),
                profile == null ? null : profile.getAvatarUrl(),
                profile == null ? null : profile.getUniversity().getName());
    }
}
