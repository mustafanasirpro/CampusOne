package com.campusone.aura.solver;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuraConstraintProviderTest {

    private final ConstraintVerifier<
            AuraConstraintProvider,
            AuraTimetableSolution> verifier = ConstraintVerifier.build(
                    new AuraConstraintProvider(),
                    AuraTimetableSolution.class,
                    AuraPlanningLesson.class);

    @Test
    void registeredStudentOverlapIsHard() {
        UUID student = id(1);
        AuraPlanningLesson first = lesson(id(2), id(3), id(4), id(5),
                "EVERY_WEEK", List.of(), List.of(student), List.of(),
                Map.of());
        AuraPlanningLesson second = lesson(id(6), id(7), id(8), id(9),
                "EVERY_WEEK", List.of(), List.of(student), List.of(),
                Map.of());
        assign(first, room(id(10), "A"), slot(id(11), 1, 9, 10));
        assign(second, room(id(12), "A"), slot(id(11), 1, 9, 10));

        verifier.verifyThat()
                .given(first, second)
                .scores(HardMediumSoftScore.ofHard(-1));
    }

    @Test
    void hardOfferingConflictIsHard() {
        UUID firstOffering = id(21);
        UUID secondOffering = id(22);
        AuraPlanningLesson first = lesson(id(23), firstOffering, id(24), id(25),
                "EVERY_WEEK", List.of(), List.of(),
                List.of(secondOffering), Map.of());
        AuraPlanningLesson second = lesson(id(26), secondOffering, id(27), id(28),
                "EVERY_WEEK", List.of(), List.of(),
                List.of(firstOffering), Map.of());
        assign(first, room(id(29), "A"), slot(id(30), 1, 9, 10));
        assign(second, room(id(31), "A"), slot(id(30), 1, 9, 10));

        verifier.verifyThat()
                .given(first, second)
                .scores(HardMediumSoftScore.ofHard(-1));
    }

    @Test
    void registeredStudentUnavailabilityIsHard() {
        UUID offering = id(41);
        UUID student = id(42);
        UUID timeslot = id(43);
        AuraPlanningLesson lesson = lesson(id(44), offering, id(45), id(46),
                "EVERY_WEEK", List.of(), List.of(student), List.of(),
                Map.of());
        assign(lesson, room(id(47), "A"), slot(timeslot, 1, 9, 10));

        verifier.verifyThat()
                .given(
                        lesson,
                        new AuraStudentAvailabilityFact(
                                offering, student, timeslot, "UNAVAILABLE"))
                .scores(HardMediumSoftScore.ofHard(-1));
    }

    @Test
    void oddAndEvenWeekLessonsMayShareResources() {
        UUID instructor = id(51);
        UUID section = id(52);
        UUID sharedRoom = id(53);
        UUID sharedSlot = id(54);
        AuraPlanningLesson odd = lesson(id(55), id(56), section, instructor,
                "ODD_WEEK", List.of(), List.of(), List.of(), Map.of());
        AuraPlanningLesson even = lesson(id(57), id(58), section, instructor,
                "EVEN_WEEK", List.of(), List.of(), List.of(), Map.of());
        assign(odd, room(sharedRoom, "A"), slot(sharedSlot, 1, 9, 10));
        assign(even, room(sharedRoom, "A"), slot(sharedSlot, 1, 9, 10));

        verifier.verifyThat()
                .given(odd, even)
                .scores(HardMediumSoftScore.ZERO);
        assertThat(odd.overlapsWeeks(even)).isFalse();
    }

    @Test
    void insufficientTravelTimeIsHard() {
        UUID instructor = id(61);
        AuraTravelRule rule = new AuraTravelRule("North", "South", 30, "NORMAL");
        Map<String, AuraTravelRule> rules = Map.of(rule.key(), rule);
        AuraPlanningLesson first = lesson(id(62), id(63), id(64), instructor,
                "EVERY_WEEK", List.of(), List.of(), List.of(), rules);
        AuraPlanningLesson second = lesson(id(65), id(66), id(67), instructor,
                "EVERY_WEEK", List.of(), List.of(), List.of(), rules);
        assign(first, room(id(68), "North"), slot(id(69), 1, 9, 10));
        assign(second, room(id(70), "South"), slot(id(71), 1, 10, 11));

        verifier.verifyThat()
                .given(first, second)
                .scores(HardMediumSoftScore.ofHard(-1));
    }

    @Test
    void lectureMustPrecedeLinkedMeeting() {
        UUID linkedRequirement = id(81);
        AuraPlanningLesson lecture = lesson(
                id(82), id(83), id(84), id(85), "EVERY_WEEK",
                List.of(), List.of(), List.of(), Map.of(),
                linkedRequirement, true);
        AuraPlanningLesson lab = lesson(
                linkedRequirement, id(86), id(87), id(88), "EVERY_WEEK",
                List.of(), List.of(), List.of(), Map.of());
        assign(lecture, room(id(89), "A"), slot(id(90), 2, 9, 10));
        assign(lab, room(id(91), "A"), slot(id(92), 1, 9, 10));

        verifier.verifyThat()
                .given(lecture, lab)
                .scores(HardMediumSoftScore.ofHard(-1));
    }

    private AuraPlanningLesson lesson(
            UUID requirementId,
            UUID offeringId,
            UUID sectionId,
            UUID instructorId,
            String weekPattern,
            List<Integer> customWeeks,
            List<UUID> students,
            List<UUID> conflicts,
            Map<String, AuraTravelRule> travelRules) {
        return lesson(
                requirementId, offeringId, sectionId, instructorId,
                weekPattern, customWeeks, students, conflicts, travelRules,
                null, false);
    }

    private AuraPlanningLesson lesson(
            UUID requirementId,
            UUID offeringId,
            UUID sectionId,
            UUID instructorId,
            String weekPattern,
            List<Integer> customWeeks,
            List<UUID> students,
            List<UUID> conflicts,
            Map<String, AuraTravelRule> travelRules,
            UUID linkedRequirementId,
            boolean lectureBeforeLinked) {
        return new AuraPlanningLesson(
                UUID.randomUUID(), requirementId, offeringId, sectionId,
                instructorId, 20, "CLASSROOM", Set.of(), 1, 1,
                List.of(), List.of(), List.of(), null, null, 0, 1,
                true, null, null, false, 60, 24, 60, 24, 24, 24,
                "LECTURE", null, linkedRequirementId, lectureBeforeLinked,
                weekPattern, customWeeks, List.of(sectionId), students,
                conflicts, travelRules);
    }

    private void assign(
            AuraPlanningLesson lesson,
            AuraRoomFact room,
            AuraTimeslotFact timeslot) {
        lesson.setRoom(room);
        lesson.setTimeslot(timeslot);
    }

    private AuraRoomFact room(UUID id, String building) {
        return new AuraRoomFact(
                id, 40, "CLASSROOM", Set.of(), building);
    }

    private AuraTimeslotFact slot(
            UUID id,
            int day,
            int startHour,
            int endHour) {
        return new AuraTimeslotFact(
                id,
                day,
                LocalTime.of(startHour, 0),
                LocalTime.of(endHour, 0));
    }

    private UUID id(int suffix) {
        return UUID.fromString(String.format(
                "91000000-0000-4000-8000-%012d", suffix));
    }
}
