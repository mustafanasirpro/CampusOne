package com.campusone.aura.service;

import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.repository.AuraJdbcRepository.DetectedClash;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuraClashDetector {

    public List<DetectedClash> detect(List<SessionResponse> sessions) {
        List<DetectedClash> clashes = new ArrayList<>();
        for (int leftIndex = 0; leftIndex < sessions.size(); leftIndex++) {
            SessionResponse left = sessions.get(leftIndex);
            for (int rightIndex = leftIndex + 1; rightIndex < sessions.size(); rightIndex++) {
                SessionResponse right = sessions.get(rightIndex);
                if (!overlaps(left, right)) {
                    continue;
                }
                if (left.roomId().equals(right.roomId())) {
                    clashes.add(clash(
                            "ROOM_DOUBLE_BOOKED",
                            "HARD",
                            "Two sessions are assigned to the same room and time.",
                            left.id(),
                            right.id()));
                }
                if (left.instructorId().equals(right.instructorId())) {
                    clashes.add(clash(
                            "INSTRUCTOR_DOUBLE_BOOKED",
                            "HARD",
                            "An instructor is assigned to two sessions at the same time.",
                            left.id(),
                            right.id()));
                }
                if (left.sectionId().equals(right.sectionId())) {
                    clashes.add(clash(
                            "SECTION_DOUBLE_BOOKED",
                            "HARD",
                            "A section is assigned to two sessions at the same time.",
                            left.id(),
                            right.id()));
                }
            }
        }
        return clashes;
    }

    public List<DetectedClash> previewMove(
            List<SessionResponse> sessions,
            UUID sessionId,
            UUID newRoomId,
            UUID newTimeslotId) {
        List<SessionResponse> moved = sessions.stream()
                .map(session -> session.id().equals(sessionId)
                        ? move(session, newRoomId, newTimeslotId)
                        : session)
                .toList();
        return detect(moved);
    }

    private SessionResponse move(
            SessionResponse session,
            UUID roomId,
            UUID timeslotId) {
        return new SessionResponse(
                session.id(),
                session.versionId(),
                session.offeringId(),
                session.meetingRequirementId(),
                session.courseCode(),
                session.courseTitle(),
                session.sectionId(),
                session.sectionName(),
                session.instructorId(),
                session.instructorName(),
                roomId,
                session.roomName(),
                session.roomType(),
                timeslotId,
                session.dayOfWeek(),
                session.startsAt(),
                session.endsAt(),
                session.locked(),
                session.source());
    }

    private boolean overlaps(SessionResponse left, SessionResponse right) {
        if (left.dayOfWeek() != right.dayOfWeek()) {
            return false;
        }
        if (left.timeslotId().equals(right.timeslotId())) {
            return true;
        }
        if (left.startsAt() == null || left.endsAt() == null
                || right.startsAt() == null || right.endsAt() == null) {
            return false;
        }
        return left.startsAt().isBefore(right.endsAt())
                && right.startsAt().isBefore(left.endsAt());
    }

    private DetectedClash clash(
            String type,
            String severity,
            String message,
            UUID primary,
            UUID secondary) {
        return new DetectedClash(type, severity, message, primary, secondary);
    }
}
