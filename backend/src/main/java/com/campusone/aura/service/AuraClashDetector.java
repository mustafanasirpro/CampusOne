package com.campusone.aura.service;

import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.repository.AuraJdbcRepository.DetectedClash;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuraClashDetector {

    public List<DetectedClash> detect(List<SessionResponse> sessions) {
        List<DetectedClash> clashes = new ArrayList<>();
        sessions.forEach(session -> detectSessionViolations(session, clashes));
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
                if (left.hardConflictOfferingIds().contains(right.offeringId())
                        || right.hardConflictOfferingIds().contains(left.offeringId())) {
                    clashes.add(clash(
                            "HARD_OFFERING_CONFLICT",
                            "HARD",
                            "Offerings that must not overlap are scheduled at the same time.",
                            left.id(), right.id()));
                }
                if (left.meetingRequirementId().equals(right.meetingRequirementId())
                        && left.occurrenceIndex() == right.occurrenceIndex()) {
                    clashes.add(clash(
                            "DUPLICATE_OCCURRENCE",
                            "HARD",
                            "A weekly meeting occurrence is scheduled more than once.",
                            left.id(), right.id()));
                }
            }
        }
        detectMissingOccurrences(sessions, clashes);
        return clashes;
    }

    private void detectSessionViolations(
            SessionResponse session,
            List<DetectedClash> clashes) {
        if (session.roomCapacity() < session.requiredCapacity()) {
            clashes.add(clash(
                    "ROOM_CAPACITY", "HARD",
                    "The assigned room is too small for this session.",
                    session.id(), null));
        }
        if (!session.roomType().equals(session.requiredRoomType())) {
            clashes.add(clash(
                    "ROOM_TYPE", "HARD",
                    "The assigned room type does not meet this session's requirement.",
                    session.id(), null));
        }
        if (!session.roomFacilities().containsAll(session.requiredFacilities())) {
            clashes.add(clash(
                    "MISSING_FACILITY", "HARD",
                    "The assigned room is missing a required facility.",
                    session.id(), null));
        }
        if (session.contiguousSlotsAvailable() < session.durationSlots()
                || !"INSTRUCTIONAL".equals(session.timeslotType())) {
            clashes.add(clash(
                    "INVALID_CONTIGUOUS_DURATION", "HARD",
                    "The session crosses a break or lacks enough contiguous instructional slots.",
                    session.id(), null));
        }
        if (!session.roomActive() || !session.instructorActive()
                || !session.sectionActive()) {
            clashes.add(clash(
                    "INACTIVE_RESOURCE", "HARD",
                    "The session references an inactive scheduling resource.",
                    session.id(), null));
        }
        if (session.fixedRoomId() != null
                && !session.fixedRoomId().equals(session.roomId())) {
            clashes.add(clash(
                    "FIXED_ROOM_MISMATCH", "HARD",
                    "The session does not use its fixed room assignment.",
                    session.id(), null));
        }
        if (session.fixedTimeslotId() != null
                && !session.fixedTimeslotId().equals(session.timeslotId())) {
            clashes.add(clash(
                    "FIXED_TIMESLOT_MISMATCH", "HARD",
                    "The session does not use its fixed timeslot assignment.",
                    session.id(), null));
        }
        if (session.instructorUnavailable()) {
            clashes.add(clash(
                    "INSTRUCTOR_UNAVAILABLE", "HARD",
                    "The instructor is unavailable during this session.",
                    session.id(), null));
        }
        if (session.roomUnavailable()) {
            clashes.add(clash(
                    "ROOM_UNAVAILABLE", "HARD",
                    "The room is unavailable during this session.",
                    session.id(), null));
        }
        if (session.sectionUnavailable()) {
            clashes.add(clash(
                    "SECTION_UNAVAILABLE", "HARD",
                    "The section is unavailable during this session.",
                    session.id(), null));
        }
        if (session.calendarException()) {
            clashes.add(clash(
                    "CALENDAR_EXCEPTION", "HARD",
                    "The session conflicts with an active calendar exception.",
                    session.id(), null));
        }
    }

    private void detectMissingOccurrences(
            List<SessionResponse> sessions,
            List<DetectedClash> clashes) {
        Map<UUID, List<SessionResponse>> byRequirement = new HashMap<>();
        sessions.forEach(session -> byRequirement
                .computeIfAbsent(session.meetingRequirementId(), ignored -> new ArrayList<>())
                .add(session));
        byRequirement.values().forEach(requirementSessions -> {
            SessionResponse sample = requirementSessions.getFirst();
            Set<Integer> occurrences = requirementSessions.stream()
                    .map(SessionResponse::occurrenceIndex)
                    .collect(java.util.stream.Collectors.toSet());
            if (occurrences.size() < sample.sessionsPerWeek()) {
                clashes.add(clash(
                        "MISSING_OCCURRENCE", "HARD",
                        "One or more required weekly occurrences are not scheduled.",
                        sample.id(), null));
            }
        });
    }

    public List<DetectedClash> previewMove(
            List<SessionResponse> sessions,
            SessionResponse replacement) {
        List<SessionResponse> moved = sessions.stream()
                .map(session -> session.id().equals(replacement.id())
                        ? replacement
                        : session)
                .toList();
        return detect(moved);
    }

    private boolean overlaps(SessionResponse left, SessionResponse right) {
        if (left.dayOfWeek() != right.dayOfWeek()) {
            return false;
        }
        if (!weekPatternsOverlap(left, right)) {
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

    private boolean weekPatternsOverlap(
            SessionResponse left,
            SessionResponse right) {
        if ("EVERY_WEEK".equals(left.weekPattern())
                || "EVERY_WEEK".equals(right.weekPattern())) return true;
        if (("ODD_WEEK".equals(left.weekPattern())
                && "EVEN_WEEK".equals(right.weekPattern()))
                || ("EVEN_WEEK".equals(left.weekPattern())
                && "ODD_WEEK".equals(right.weekPattern()))) return false;
        if ("CUSTOM_WEEK_SET".equals(left.weekPattern())
                && "CUSTOM_WEEK_SET".equals(right.weekPattern())) {
            return left.customWeeks().stream().anyMatch(right.customWeeks()::contains);
        }
        if ("CUSTOM_WEEK_SET".equals(left.weekPattern())) {
            return customMatches(left.customWeeks(), right.weekPattern());
        }
        if ("CUSTOM_WEEK_SET".equals(right.weekPattern())) {
            return customMatches(right.customWeeks(), left.weekPattern());
        }
        return true;
    }

    private boolean customMatches(List<Integer> weeks, String pattern) {
        return weeks.stream().anyMatch(week ->
                "ODD_WEEK".equals(pattern) ? week % 2 == 1 : week % 2 == 0);
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
