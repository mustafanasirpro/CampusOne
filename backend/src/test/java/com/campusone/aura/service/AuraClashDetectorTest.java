package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.repository.AuraJdbcRepository.DetectedClash;
import com.campusone.aura.repository.AuraJdbcRepository.ClashDetectionContext;
import com.campusone.aura.repository.AuraJdbcRepository.LinkageRule;
import com.campusone.aura.repository.AuraJdbcRepository.TravelContextRule;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuraClashDetectorTest {

    private static final UUID VERSION_ID = UUID.fromString(
            "20000000-0000-4000-8000-000000000001");
    private static final UUID TIMESLOT_ID = UUID.fromString(
            "30000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_TIMESLOT_ID = UUID.fromString(
            "30000000-0000-4000-8000-000000000002");
    private static final UUID ROOM_ID = UUID.fromString(
            "40000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_ROOM_ID = UUID.fromString(
            "40000000-0000-4000-8000-000000000002");
    private static final UUID INSTRUCTOR_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final UUID SECTION_ID = UUID.fromString(
            "60000000-0000-4000-8000-000000000001");

    private final AuraClashDetector detector = new AuraClashDetector();

    @Test
    void detect_sameRoomInstructorAndSectionAtSameTime_reportsHardClashes() {
        List<DetectedClash> clashes = detector.detect(List.of(
                session(
                        UUID.fromString("70000000-0000-4000-8000-000000000001"),
                        ROOM_ID,
                        TIMESLOT_ID,
                        INSTRUCTOR_ID,
                        SECTION_ID),
                session(
                        UUID.fromString("70000000-0000-4000-8000-000000000002"),
                        ROOM_ID,
                        TIMESLOT_ID,
                        INSTRUCTOR_ID,
                        SECTION_ID)));

        assertThat(clashes)
                .extracting(DetectedClash::clashType)
                .containsExactlyInAnyOrder(
                        "ROOM_DOUBLE_BOOKED",
                        "INSTRUCTOR_DOUBLE_BOOKED",
                        "SECTION_DOUBLE_BOOKED");
    }

    @Test
    void detect_sameResourcesAtDifferentTimes_hasNoClash() {
        List<DetectedClash> clashes = detector.detect(List.of(
                session(
                        UUID.fromString("70000000-0000-4000-8000-000000000001"),
                        ROOM_ID,
                        TIMESLOT_ID,
                        INSTRUCTOR_ID,
                        SECTION_ID),
                session(
                        UUID.fromString("70000000-0000-4000-8000-000000000002"),
                        ROOM_ID,
                        OTHER_TIMESLOT_ID,
                        INSTRUCTOR_ID,
                        SECTION_ID,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0))));

        assertThat(clashes).isEmpty();
    }

    @Test
    void detect_differentTimeslotIdsWithOverlappingRanges_reportsClashes() {
        List<DetectedClash> clashes = detector.detect(List.of(
                session(
                        UUID.fromString("70000000-0000-4000-8000-000000000001"),
                        ROOM_ID,
                        TIMESLOT_ID,
                        INSTRUCTOR_ID,
                        SECTION_ID,
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 30)),
                session(
                        UUID.fromString("70000000-0000-4000-8000-000000000002"),
                        ROOM_ID,
                        OTHER_TIMESLOT_ID,
                        INSTRUCTOR_ID,
                        SECTION_ID,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0))));

        assertThat(clashes)
                .extracting(DetectedClash::clashType)
                .containsExactlyInAnyOrder(
                        "ROOM_DOUBLE_BOOKED",
                        "INSTRUCTOR_DOUBLE_BOOKED",
                        "SECTION_DOUBLE_BOOKED");
    }

    @Test
    void previewMove_onlyEvaluatesTheMovedTimetable() {
        UUID movedSessionId =
                UUID.fromString("70000000-0000-4000-8000-000000000001");
        List<DetectedClash> clashes = detector.previewMove(
                List.of(
                        session(
                                movedSessionId,
                                OTHER_ROOM_ID,
                                OTHER_TIMESLOT_ID,
                                UUID.fromString("50000000-0000-4000-8000-000000000002"),
                                UUID.fromString("60000000-0000-4000-8000-000000000002")),
                        session(
                                UUID.fromString("70000000-0000-4000-8000-000000000002"),
                                ROOM_ID,
                                TIMESLOT_ID,
                                INSTRUCTOR_ID,
                                SECTION_ID)),
                session(
                        movedSessionId,
                        ROOM_ID,
                        TIMESLOT_ID,
                        UUID.fromString("50000000-0000-4000-8000-000000000002"),
                        UUID.fromString("60000000-0000-4000-8000-000000000002")));

        assertThat(clashes)
                .extracting(DetectedClash::clashType)
                .contains("ROOM_DOUBLE_BOOKED");
    }

    @Test
    void detect_oddAndEvenWeekSessionsDoNotClash() {
        SessionResponse odd = detailedSession(
                UUID.randomUUID(), "ODD_WEEK", List.of(), 60, 60,
                List.of(), List.of(), 1, 1);
        SessionResponse even = detailedSession(
                UUID.randomUUID(), "EVEN_WEEK", List.of(), 60, 60,
                List.of(), List.of(), 1, 1);

        assertThat(detector.detect(List.of(odd, even))).isEmpty();
    }

    @Test
    void detect_capacityFacilitiesAndContiguousDurationAreIndependentlyChecked() {
        SessionResponse invalid = detailedSession(
                UUID.randomUUID(), "EVERY_WEEK", List.of(), 100, 40,
                List.of("COMPUTERS"), List.of("PROJECTOR"), 2, 1);

        assertThat(detector.detect(List.of(invalid)))
                .extracting(DetectedClash::clashType)
                .contains(
                        "ROOM_CAPACITY",
                        "MISSING_FACILITY",
                        "INVALID_CONTIGUOUS_DURATION");
    }

    @Test
    void detect_registeredStudentOverlap_reportsAffectedStudentOnce() {
        SessionResponse first = contextualSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ROOM_ID, INSTRUCTOR_ID, SECTION_ID, 1,
                LocalTime.of(9, 0), LocalTime.of(10, 0));
        SessionResponse second = contextualSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OTHER_ROOM_ID, UUID.randomUUID(), UUID.randomUUID(), 1,
                LocalTime.of(9, 30), LocalTime.of(10, 30));
        UUID studentId = UUID.randomUUID();
        ClashDetectionContext context = new ClashDetectionContext(
                Map.of(first.id(), Set.of(studentId), second.id(), Set.of(studentId)),
                Set.of(), Set.of(), Map.of(), Map.of(), List.of());

        assertThat(detector.detect(List.of(first, second), context))
                .filteredOn(clash -> "STUDENT_DOUBLE_BOOKED".equals(clash.clashType()))
                .singleElement()
                .satisfies(clash -> assertThat(clash.affectedStudents())
                        .containsExactly(studentId));
    }

    @Test
    void detect_insufficientTravelTime_reportsSharedInstructorImpact() {
        UUID instructor = UUID.randomUUID();
        SessionResponse first = contextualSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                ROOM_ID, instructor, UUID.randomUUID(), 1,
                LocalTime.of(9, 0), LocalTime.of(10, 0));
        SessionResponse second = contextualSession(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OTHER_ROOM_ID, instructor, UUID.randomUUID(), 1,
                LocalTime.of(10, 5), LocalTime.of(11, 0));
        ClashDetectionContext context = new ClashDetectionContext(
                Map.of(), Set.of(), Set.of(), Map.of(),
                Map.of(first.id(), "North", second.id(), "South"),
                List.of(new TravelContextRule("North", "South", 15, "NORMAL")));

        assertThat(detector.detect(List.of(first, second), context))
                .filteredOn(clash -> "BUILDING_TRAVEL_TIME".equals(clash.clashType()))
                .singleElement()
                .satisfies(clash -> assertThat(clash.affectedInstructors())
                        .containsExactly(instructor));
    }

    @Test
    void detect_linkedActivityOutOfOrder_reportsHardClash() {
        UUID lectureRequirement = UUID.randomUUID();
        UUID labRequirement = UUID.randomUUID();
        SessionResponse lecture = contextualSession(
                UUID.randomUUID(), UUID.randomUUID(), lectureRequirement,
                ROOM_ID, INSTRUCTOR_ID, SECTION_ID, 2,
                LocalTime.of(11, 0), LocalTime.of(12, 0));
        SessionResponse lab = contextualSession(
                UUID.randomUUID(), UUID.randomUUID(), labRequirement,
                OTHER_ROOM_ID, UUID.randomUUID(), UUID.randomUUID(), 1,
                LocalTime.of(9, 0), LocalTime.of(10, 0));
        ClashDetectionContext context = new ClashDetectionContext(
                Map.of(), Set.of(), Set.of(),
                Map.of(lectureRequirement, new LinkageRule(labRequirement, true)),
                Map.of(), List.of());

        assertThat(detector.detect(List.of(lecture, lab), context))
                .extracting(DetectedClash::clashType)
                .contains("LINKED_ACTIVITY_ORDER");
    }

    private SessionResponse session(
            UUID sessionId,
            UUID roomId,
            UUID timeslotId,
            UUID instructorId,
            UUID sectionId) {
        return session(
                sessionId,
                roomId,
                timeslotId,
                instructorId,
                sectionId,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0));
    }

    private SessionResponse session(
            UUID sessionId,
            UUID roomId,
            UUID timeslotId,
            UUID instructorId,
            UUID sectionId,
            LocalTime startsAt,
            LocalTime endsAt) {
        return new SessionResponse(
                sessionId,
                VERSION_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CS101",
                "Programming Fundamentals",
                sectionId,
                "BSCS-1A",
                instructorId,
                "Dr Ahmed",
                roomId,
                "Room 101",
                "CLASSROOM",
                timeslotId,
                1,
                startsAt,
                endsAt,
                false,
                "SOLVER");
    }

    private SessionResponse detailedSession(
            UUID id,
            String weekPattern,
            List<Integer> customWeeks,
            int requiredCapacity,
            int roomCapacity,
            List<String> requiredFacilities,
            List<String> roomFacilities,
            int duration,
            int contiguous) {
        return new SessionResponse(
                id, VERSION_ID, UUID.randomUUID(), UUID.randomUUID(),
                "CS101", "Programming Fundamentals", SECTION_ID, "BSCS-1A",
                INSTRUCTOR_ID, "Dr Ahmed", ROOM_ID, "Room 101", "CLASSROOM",
                TIMESLOT_ID, 1, LocalTime.of(9, 0), LocalTime.of(10, 0),
                false, "SOLVER", 1, 1, duration, contiguous,
                requiredCapacity, "CLASSROOM", roomCapacity,
                requiredFacilities, roomFacilities, "INSTRUCTIONAL",
                true, true, true, null, null, weekPattern, customWeeks,
                List.of(), false, false, false, false);
    }

    private SessionResponse contextualSession(
            UUID id,
            UUID offeringId,
            UUID requirementId,
            UUID roomId,
            UUID instructorId,
            UUID sectionId,
            int day,
            LocalTime startsAt,
            LocalTime endsAt) {
        return new SessionResponse(
                id, VERSION_ID, offeringId, requirementId,
                "CS101", "Programming Fundamentals", sectionId, "BSCS-1A",
                instructorId, "Dr Ahmed", roomId, "Room 101", "CLASSROOM",
                UUID.randomUUID(), day, startsAt, endsAt, false, "SOLVER",
                1, 1, 1, 1, 30, "CLASSROOM", 40,
                List.of(), List.of(), "INSTRUCTIONAL",
                true, true, true, null, null, "EVERY_WEEK", List.of(),
                List.of(), false, false, false, false);
    }
}
