package com.campusone.aura.service;

import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.repository.AuraJdbcRepository.DetectedClash;
import com.campusone.aura.repository.AuraJdbcRepository.ClashDetectionContext;
import com.campusone.aura.repository.AuraJdbcRepository.LinkageRule;
import com.campusone.aura.repository.AuraJdbcRepository.TravelContextRule;
import java.time.Duration;
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
        return detect(sessions, ClashDetectionContext.empty());
    }

    public List<DetectedClash> detect(
            List<SessionResponse> sessions,
            ClashDetectionContext context) {
        ClashDetectionContext resolvedContext = context == null
                ? ClashDetectionContext.empty() : context;
        List<DetectedClash> clashes = new ArrayList<>();
        sessions.forEach(session -> detectSessionViolations(
                session, resolvedContext, clashes));
        for (int leftIndex = 0; leftIndex < sessions.size(); leftIndex++) {
            SessionResponse left = sessions.get(leftIndex);
            for (int rightIndex = leftIndex + 1; rightIndex < sessions.size(); rightIndex++) {
                SessionResponse right = sessions.get(rightIndex);
                boolean overlaps = overlaps(left, right);
                if (overlaps && left.roomId().equals(right.roomId())) {
                    clashes.add(clash(
                            "ROOM_DOUBLE_BOOKED",
                            "HARD",
                            "Two sessions are assigned to the same room and time.",
                            left.id(),
                            right.id()));
                }
                if (overlaps && left.instructorId().equals(right.instructorId())) {
                    clashes.add(clash(
                            "INSTRUCTOR_DOUBLE_BOOKED",
                            "HARD",
                            "An instructor is assigned to two sessions at the same time.",
                            left.id(),
                            right.id()));
                }
                if (overlaps && left.sectionId().equals(right.sectionId())) {
                    clashes.add(clash(
                            "SECTION_DOUBLE_BOOKED",
                            "HARD",
                            "A section is assigned to two sessions at the same time.",
                            left.id(),
                            right.id()));
                }
                if (overlaps && (left.hardConflictOfferingIds().contains(right.offeringId())
                        || right.hardConflictOfferingIds().contains(left.offeringId()))) {
                    clashes.add(clash(
                            "HARD_OFFERING_CONFLICT",
                            "HARD",
                            "Offerings that must not overlap are scheduled at the same time.",
                            left.id(), right.id()));
                }
                if (overlaps && left.meetingRequirementId().equals(right.meetingRequirementId())
                        && left.occurrenceIndex() == right.occurrenceIndex()) {
                    clashes.add(clash(
                            "DUPLICATE_OCCURRENCE",
                            "HARD",
                            "A weekly meeting occurrence is scheduled more than once.",
                            left.id(), right.id()));
                }
                Set<UUID> sharedStudents = sharedStudents(left, right, resolvedContext);
                if (overlaps && !sharedStudents.isEmpty()) {
                    clashes.add(new DetectedClash(
                            "STUDENT_DOUBLE_BOOKED", "HARD",
                            "A registered student is assigned to overlapping sessions.",
                            left.id(), right.id(), "STUDENT_TIME_OVERLAP",
                            "Move one session or assign an eligible parallel teaching group.",
                            sharedStudents.stream().sorted().toList(),
                            List.of(), List.of(), List.of()));
                }
                detectLinkedOrdering(left, right, resolvedContext, clashes);
                detectTravel(left, right, sharedStudents, resolvedContext, clashes);
            }
        }
        detectMissingOccurrences(sessions, clashes);
        return deduplicate(clashes);
    }

    private void detectSessionViolations(
            SessionResponse session,
            ClashDetectionContext context,
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
        if (context.studentUnavailableSessions().contains(session.id())) {
            clashes.add(clash(
                    "STUDENT_UNAVAILABLE", "HARD",
                    "A registered student is unavailable during this session.",
                    session.id(), null));
        }
        if (context.overCapacityGroupSessions().contains(session.id())) {
            clashes.add(clash(
                    "TEACHING_GROUP_CAPACITY", "HARD",
                    "The assigned teaching group exceeds its configured capacity.",
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
        return previewMove(sessions, replacement, ClashDetectionContext.empty());
    }

    public List<DetectedClash> previewMove(
            List<SessionResponse> sessions,
            SessionResponse replacement,
            ClashDetectionContext context) {
        List<SessionResponse> moved = sessions.stream()
                .map(session -> session.id().equals(replacement.id())
                        ? replacement
                        : session)
                .toList();
        return detect(moved, context);
    }

    private Set<UUID> sharedStudents(
            SessionResponse left,
            SessionResponse right,
            ClashDetectionContext context) {
        Set<UUID> leftStudents = context.studentsBySession()
                .getOrDefault(left.id(), Set.of());
        Set<UUID> rightStudents = context.studentsBySession()
                .getOrDefault(right.id(), Set.of());
        if (leftStudents.isEmpty() || rightStudents.isEmpty()) return Set.of();
        Set<UUID> shared = new java.util.HashSet<>(leftStudents);
        shared.retainAll(rightStudents);
        return Set.copyOf(shared);
    }

    private void detectLinkedOrdering(
            SessionResponse left,
            SessionResponse right,
            ClashDetectionContext context,
            List<DetectedClash> clashes) {
        LinkageRule leftRule = context.linkageByRequirement()
                .get(left.meetingRequirementId());
        if (leftRule != null
                && leftRule.linkedRequirementId().equals(right.meetingRequirementId())
                && leftRule.lectureBeforeLinked()
                && !isBefore(left, right)) {
            clashes.add(new DetectedClash(
                    "LINKED_ACTIVITY_ORDER", "HARD",
                    "A linked lecture must occur before its laboratory or tutorial.",
                    left.id(), right.id(), "LECTURE_ORDER",
                    "Move the linked activity after its lecture.",
                    List.of(), List.of(left.instructorId()),
                    List.of(left.sectionId()), List.of()));
        }
        LinkageRule rightRule = context.linkageByRequirement()
                .get(right.meetingRequirementId());
        if (rightRule != null
                && rightRule.linkedRequirementId().equals(left.meetingRequirementId())
                && rightRule.lectureBeforeLinked()
                && !isBefore(right, left)) {
            clashes.add(new DetectedClash(
                    "LINKED_ACTIVITY_ORDER", "HARD",
                    "A linked lecture must occur before its laboratory or tutorial.",
                    right.id(), left.id(), "LECTURE_ORDER",
                    "Move the linked activity after its lecture.",
                    List.of(), List.of(right.instructorId()),
                    List.of(right.sectionId()), List.of()));
        }
    }

    private boolean isBefore(SessionResponse earlier, SessionResponse later) {
        return earlier.dayOfWeek() < later.dayOfWeek()
                || (earlier.dayOfWeek() == later.dayOfWeek()
                    && earlier.endsAt() != null && later.startsAt() != null
                    && !earlier.endsAt().isAfter(later.startsAt()));
    }

    private void detectTravel(
            SessionResponse left,
            SessionResponse right,
            Set<UUID> sharedStudents,
            ClashDetectionContext context,
            List<DetectedClash> clashes) {
        if (left.dayOfWeek() != right.dayOfWeek() || overlaps(left, right)) return;
        boolean sharedInstructor = left.instructorId().equals(right.instructorId());
        boolean sharedSection = left.sectionId().equals(right.sectionId());
        if (!sharedInstructor && !sharedSection && sharedStudents.isEmpty()) return;
        String leftBuilding = normalizeBuilding(context.buildingBySession().get(left.id()));
        String rightBuilding = normalizeBuilding(context.buildingBySession().get(right.id()));
        if (leftBuilding.isBlank() || rightBuilding.isBlank()
                || leftBuilding.equals(rightBuilding)) return;
        if (left.startsAt() == null || right.startsAt() == null) return;
        SessionResponse earlier = left.startsAt().isBefore(right.startsAt()) ? left : right;
        SessionResponse later = earlier == left ? right : left;
        if (earlier.endsAt() == null || later.startsAt() == null) return;
        long gap = Duration.between(earlier.endsAt(), later.startsAt()).toMinutes();
        TravelContextRule rule = context.travelRules().stream()
                .filter(candidate -> samePair(
                        leftBuilding, rightBuilding,
                        normalizeBuilding(candidate.fromBuilding()),
                        normalizeBuilding(candidate.toBuilding())))
                .findFirst().orElse(null);
        if (rule == null) return;
        if ("IMPOSSIBLE".equals(rule.difficulty()) || gap < rule.minutes()) {
            clashes.add(new DetectedClash(
                    "BUILDING_TRAVEL_TIME", "HARD",
                    "There is not enough time to travel between these buildings.",
                    earlier.id(), later.id(), "INSUFFICIENT_TRAVEL_TIME",
                    "Move one session or choose rooms in the same building.",
                    sharedStudents.stream().sorted().toList(),
                    sharedInstructor ? List.of(left.instructorId()) : List.of(),
                    sharedSection ? List.of(left.sectionId()) : List.of(),
                    List.of(left.roomId(), right.roomId())));
        }
    }

    private boolean samePair(String left, String right, String first, String second) {
        return (left.equals(first) && right.equals(second))
                || (left.equals(second) && right.equals(first));
    }

    private String normalizeBuilding(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private List<DetectedClash> deduplicate(List<DetectedClash> clashes) {
        Map<String, DetectedClash> unique = new java.util.LinkedHashMap<>();
        for (DetectedClash clash : clashes) {
            String sessions = java.util.stream.Stream
                    .of(clash.primarySessionId(), clash.secondarySessionId())
                    .filter(java.util.Objects::nonNull).map(UUID::toString).sorted()
                    .collect(java.util.stream.Collectors.joining("|"));
            unique.putIfAbsent(clash.clashType() + "|" + sessions, clash);
        }
        return List.copyOf(unique.values());
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
