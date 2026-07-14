package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.repository.AuraJdbcRepository.DetectedClash;
import java.time.LocalTime;
import java.util.List;
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
                movedSessionId,
                ROOM_ID,
                TIMESLOT_ID);

        assertThat(clashes)
                .extracting(DetectedClash::clashType)
                .contains("ROOM_DOUBLE_BOOKED");
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
}
