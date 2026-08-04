package com.campusone.aura.solver;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class AuraConstraintBehaviorMatrixTest {

    private final ConstraintVerifier<
            AuraConstraintProvider,
            AuraTimetableSolution> verifier = ConstraintVerifier.build(
                    new AuraConstraintProvider(),
                    AuraTimetableSolution.class,
                    AuraPlanningLesson.class);
    private int sequence = 1;

    @Test
    void behaviorMatrixCoversEveryCatalogConstraintExactlyOnce() {
        List<String> names = scenarios().stream().map(Scenario::name).toList();

        assertThat(names)
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrderElementsOf(
                        AuraConstraintCatalog.constraints().keySet());
    }

    @TestFactory
    Stream<DynamicTest> everyConstraintHasAnIsolatedBehaviorScenario() {
        return scenarios().stream().map(scenario -> DynamicTest.dynamicTest(
                scenario.name(),
                () -> verifyScenario(scenario)));
    }

    private void verifyScenario(Scenario scenario) {
        var assertion = verifier.verifyThat((provider, factory) ->
                        provider.constraintByName(factory, scenario.name()))
                .given(scenario.facts().toArray());
        int matchWeight = Math.abs(scenario.expected().hardScore())
                + Math.abs(scenario.expected().mediumScore())
                + Math.abs(scenario.expected().softScore());
        if (scenario.expected().hardScore() > 0
                || scenario.expected().mediumScore() > 0
                || scenario.expected().softScore() > 0) {
            assertion.rewardsWith(matchWeight);
        } else {
            assertion.penalizesBy(matchWeight);
        }
    }

    private List<Scenario> scenarios() {
        sequence = 1;
        List<Scenario> result = new ArrayList<>();
        overlappingPair(result, "Room double-booked", pair -> {
            AuraRoomFact room = room(40, "CLASSROOM", Set.of(), "A");
            pair.assign(room, room, slot(1, 9, 10), slot(1, 9, 10));
        });
        overlappingPair(result, "Instructor double-booked", pair -> {
            pair.right.instructorId = pair.left.instructorId;
            pair.assignDistinctRoomsSameTime();
        });
        overlappingPair(result, "Section double-booked", pair -> {
            pair.right.sectionId = pair.left.sectionId;
            pair.assignDistinctRoomsSameTime();
        });
        overlappingPair(result, "Combined section double-booked", pair -> {
            UUID shared = id();
            pair.left.participatingSections = List.of(
                    pair.left.sectionId, shared);
            pair.right.participatingSections = List.of(
                    pair.right.sectionId, shared);
            pair.assignDistinctRoomsSameTime();
        });
        overlappingPair(result, "Registered student double-booked", pair -> {
            UUID student = id();
            pair.left.students = List.of(student);
            pair.right.students = List.of(student);
            pair.assignDistinctRoomsSameTime();
        });
        overlappingPair(result, "Duplicate teaching-group attendance", pair -> {
            UUID student = id();
            pair.right.offeringId = pair.left.offeringId;
            pair.left.meetingType = "LAB";
            pair.right.meetingType = "LAB";
            pair.left.teachingGroup = "LAB-A";
            pair.right.teachingGroup = "LAB-B";
            pair.left.students = List.of(student);
            pair.right.students = List.of(student);
            pair.assign(
                    room(40, "LAB", Set.of(), "A"),
                    room(40, "LAB", Set.of(), "A"),
                    slot(1, 9, 10), slot(2, 11, 12));
        });
        overlappingPair(result, "Hard offering conflict", pair -> {
            pair.left.conflicts = List.of(pair.right.offeringId);
            pair.assignDistinctRoomsSameTime();
        });

        availabilityScenarios(result);
        roomAndAssignmentScenarios(result);
        occurrenceAndTravelScenarios(result);
        loadScenarios(result);
        preferenceScenarios(result);
        stabilityScenarios(result);
        return List.copyOf(result);
    }

    private void availabilityScenarios(List<Scenario> target) {
        availability(target, "Instructor unavailable", "UNAVAILABLE",
                (spec, slot, room) -> new AuraInstructorAvailabilityFact(
                        spec.instructorId, slot.id(), "UNAVAILABLE"));
        availability(target, "Room unavailable", "UNAVAILABLE",
                (spec, slot, room) -> new AuraRoomAvailabilityFact(
                        room.id(), slot.id(), "UNAVAILABLE"));
        availability(target, "Section unavailable", "UNAVAILABLE",
                (spec, slot, room) -> new AuraSectionAvailabilityFact(
                        spec.sectionId, slot.id(), "UNAVAILABLE"));
        studentAvailability(
                target, "Registered student unavailable", "UNAVAILABLE",
                HardMediumSoftScore.ofHard(-1));
        availability(target, "Instructor avoid timeslot", "AVOID",
                (spec, slot, room) -> new AuraInstructorAvailabilityFact(
                        spec.instructorId, slot.id(), "AVOID"));
        availability(target, "Room avoid timeslot", "AVOID",
                (spec, slot, room) -> new AuraRoomAvailabilityFact(
                        room.id(), slot.id(), "AVOID"));
        availability(target, "Section avoid timeslot", "AVOID",
                (spec, slot, room) -> new AuraSectionAvailabilityFact(
                        spec.sectionId, slot.id(), "AVOID"));
        studentAvailability(
                target, "Registered student avoid timeslot", "AVOID",
                HardMediumSoftScore.ofMedium(-1));
        availability(target, "Instructor preferred timeslot", "PREFERRED",
                (spec, slot, room) -> new AuraInstructorAvailabilityFact(
                        spec.instructorId, slot.id(), "PREFERRED"));
        availability(target, "Room preferred timeslot", "PREFERRED",
                (spec, slot, room) -> new AuraRoomAvailabilityFact(
                        room.id(), slot.id(), "PREFERRED"));
        availability(target, "Section preferred timeslot", "PREFERRED",
                (spec, slot, room) -> new AuraSectionAvailabilityFact(
                        spec.sectionId, slot.id(), "PREFERRED"));
        studentAvailability(
                target, "Registered student preferred timeslot", "PREFERRED",
                HardMediumSoftScore.ofSoft(1));
    }

    private void roomAndAssignmentScenarios(List<Scenario> target) {
        LessonSpec calendar = spec();
        AuraRoomFact calendarRoom = room(40, "CLASSROOM", Set.of(), "A");
        AuraTimeslotFact calendarSlot = slot(1, 9, 10);
        AuraPlanningLesson calendarLesson = assigned(
                calendar, calendarRoom, calendarSlot);
        calendarLesson.setCalendarBlockedAssignments(Set.of(
                AuraPlanningLesson.assignmentKey(
                        calendarRoom.id(), calendarSlot.id())));
        add(target, "Calendar exception assignment",
                HardMediumSoftScore.ofHard(-1), calendarLesson);

        LessonSpec capacity = spec();
        capacity.requiredCapacity = 20;
        add(target, "Room capacity", HardMediumSoftScore.ofHard(-1),
                assigned(capacity,
                        room(19, "CLASSROOM", Set.of(), "A"),
                        slot(1, 9, 10)));

        add(target, "Room type match", HardMediumSoftScore.ofHard(-1),
                assigned(spec(),
                        room(40, "LAB", Set.of(), "A"),
                        slot(1, 9, 10)));

        LessonSpec facilities = spec();
        facilities.requiredFacilities = Set.of("PROJECTOR");
        add(target, "Required room facilities",
                HardMediumSoftScore.ofHard(-1),
                assigned(facilities,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 9, 10)));

        LessonSpec contiguous = spec();
        contiguous.durationSlots = 2;
        add(target, "Instructional contiguous block",
                HardMediumSoftScore.ofHard(-1),
                assigned(contiguous,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 9, 10)));

        LessonSpec days = spec();
        days.allowedDays = List.of(2);
        add(target, "Allowed and prohibited days",
                HardMediumSoftScore.ofHard(-1),
                assigned(days,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 9, 10)));

        AuraRoomFact actualRoom = room(40, "CLASSROOM", Set.of(), "A");
        LessonSpec fixedRoom = spec();
        fixedRoom.fixedRoomId = id();
        add(target, "Fixed room assignment", HardMediumSoftScore.ofHard(-1),
                assigned(fixedRoom, actualRoom, slot(1, 9, 10)));

        AuraTimeslotFact actualSlot = slot(1, 9, 10);
        LessonSpec fixedSlot = spec();
        fixedSlot.fixedTimeslotId = id();
        add(target, "Fixed timeslot assignment",
                HardMediumSoftScore.ofHard(-1),
                assigned(fixedSlot,
                        room(40, "CLASSROOM", Set.of(), "A"), actualSlot));

        LessonSpec pinned = spec();
        pinned.pinned = true;
        add(target, "Pinned requirement has an assignment",
                HardMediumSoftScore.ofHard(-1),
                assigned(pinned,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 9, 10)));
    }

    private void occurrenceAndTravelScenarios(List<Scenario> target) {
        UUID requirement = id();
        LessonSpec firstOccurrence = spec();
        firstOccurrence.requirementId = requirement;
        LessonSpec secondOccurrence = spec();
        secondOccurrence.requirementId = requirement;
        add(target, "Maximum occurrences per day",
                HardMediumSoftScore.ofHard(-1),
                assigned(firstOccurrence,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 9, 10)),
                assigned(secondOccurrence,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 11, 12)));

        LessonSpec separatedFirst = spec();
        separatedFirst.requirementId = id();
        separatedFirst.minimumDaySeparation = 3;
        LessonSpec separatedSecond = spec();
        separatedSecond.requirementId = separatedFirst.requirementId;
        separatedSecond.minimumDaySeparation = 3;
        add(target, "Minimum day separation",
                HardMediumSoftScore.ofHard(-1),
                assigned(separatedFirst,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 9, 10)),
                assigned(separatedSecond,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(2, 9, 10)));

        LessonSpec lecture = spec();
        LessonSpec lab = spec();
        lecture.linkedRequirementId = lab.requirementId;
        lecture.lectureBeforeLinked = true;
        add(target, "Lecture before linked meeting",
                HardMediumSoftScore.ofHard(-1),
                assigned(lecture,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(2, 9, 10)),
                assigned(lab,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 9, 10)));

        travel(target, "Impossible building travel", "NORMAL",
                30, 10, 0, HardMediumSoftScore.ofHard(-1));
        travel(target, "Difficult building travel", "DIFFICULT",
                30, 10, 40, HardMediumSoftScore.ofMedium(-1));
    }

    private void loadScenarios(List<Scenario> target) {
        load(target, "Instructor weekly hard load", load -> {
            load.left.instructorHardWeekly = 1;
            load.right.instructorHardWeekly = 1;
            load.right.instructorId = load.left.instructorId;
            load.assign(1, 1);
        }, HardMediumSoftScore.ofHard(-1));
        load(target, "Instructor daily hard load", load -> {
            load.left.instructorHardDaily = 1;
            load.right.instructorHardDaily = 1;
            load.right.instructorId = load.left.instructorId;
            load.assign(1, 1);
        }, HardMediumSoftScore.ofHard(-1));
        load(target, "Section daily hard load", load -> {
            load.left.sectionHardDaily = 1;
            load.right.sectionHardDaily = 1;
            load.right.sectionId = load.left.sectionId;
            load.assign(1, 1);
        }, HardMediumSoftScore.ofHard(-1));
        load(target, "Instructor preferred weekly load", load -> {
            load.left.instructorPreferredWeekly = 1;
            load.right.instructorPreferredWeekly = 1;
            load.right.instructorId = load.left.instructorId;
            load.assign(1, 2);
        }, HardMediumSoftScore.ofMedium(-1));
        load(target, "Instructor preferred daily load", load -> {
            load.left.instructorPreferredDaily = 1;
            load.right.instructorPreferredDaily = 1;
            load.right.instructorId = load.left.instructorId;
            load.assign(1, 1);
        }, HardMediumSoftScore.ofMedium(-1));
        load(target, "Section preferred daily load", load -> {
            load.left.sectionPreferredDaily = 1;
            load.right.sectionPreferredDaily = 1;
            load.right.sectionId = load.left.sectionId;
            load.assign(1, 1);
        }, HardMediumSoftScore.ofMedium(-1));
    }

    private void preferenceScenarios(List<Scenario> target) {
        LessonSpec preferredDay = spec();
        preferredDay.preferredDays = List.of(1);
        add(target, "Preferred day", HardMediumSoftScore.ofSoft(1),
                assigned(preferredDay,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 9, 10)));

        LessonSpec preferredTime = spec();
        preferredTime.preferredStart = LocalTime.of(8, 0);
        preferredTime.preferredEnd = LocalTime.of(12, 0);
        add(target, "Preferred time window", HardMediumSoftScore.ofSoft(1),
                assigned(preferredTime,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 9, 10)));

        UUID requirement = id();
        LessonSpec first = spec();
        first.requirementId = requirement;
        LessonSpec second = spec();
        second.requirementId = requirement;
        add(target, "Same room preference", HardMediumSoftScore.ofSoft(-1),
                assigned(first,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 9, 10)),
                assigned(second,
                        room(40, "CLASSROOM", Set.of(), "B"),
                        slot(2, 9, 10)));

        LessonSpec spreadFirst = spec();
        LessonSpec spreadSecond = spec();
        spreadSecond.sectionId = spreadFirst.sectionId;
        add(target, "Spread section meetings",
                HardMediumSoftScore.ofSoft(-1),
                assigned(spreadFirst,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 9, 10)),
                assigned(spreadSecond,
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 11, 12)));
    }

    private void stabilityScenarios(List<Scenario> target) {
        idleGap(target, "Instructor idle gaps", pair ->
                pair.right.instructorId = pair.left.instructorId,
                HardMediumSoftScore.ofSoft(-2));
        idleGap(target, "Section idle gaps", pair ->
                pair.right.sectionId = pair.left.sectionId,
                HardMediumSoftScore.ofSoft(-2));
        idleGap(target, "Registered student idle gaps", pair -> {
            UUID student = id();
            pair.left.students = List.of(student);
            pair.right.students = List.of(student);
        }, HardMediumSoftScore.ofSoft(-2));

        LessonPair buildings = pair();
        buildings.right.sectionId = buildings.left.sectionId;
        buildings.assign(
                room(40, "CLASSROOM", Set.of(), "North"),
                room(40, "CLASSROOM", Set.of(), "South"),
                slot(1, 9, 10), slot(2, 9, 10));
        add(target, "Section building stability",
                HardMediumSoftScore.ofSoft(-1), buildings.lessons());

        add(target, "Undesirable time fairness",
                HardMediumSoftScore.ofSoft(-1),
                assigned(spec(),
                        room(40, "CLASSROOM", Set.of(), "A"),
                        slot(1, 7, 8)));

        AuraRoomFact original = room(40, "CLASSROOM", Set.of(), "A");
        AuraTimeslotFact originalSlot = slot(1, 9, 10);
        AuraPlanningLesson repair = assigned(
                spec(), room(40, "CLASSROOM", Set.of(), "A"), originalSlot);
        repair.setParentAssignment(original.id(), originalSlot.id());
        add(target, "Repair parent assignment stability",
                HardMediumSoftScore.ofSoft(-1), repair);
    }

    private void availability(
            List<Scenario> target,
            String name,
            String value,
            AvailabilityFactFactory factFactory) {
        LessonSpec spec = spec();
        AuraRoomFact room = room(40, "CLASSROOM", Set.of(), "A");
        AuraTimeslotFact slot = slot(1, 9, 10);
        HardMediumSoftScore expected = "UNAVAILABLE".equals(value)
                ? HardMediumSoftScore.ofHard(-1)
                : "AVOID".equals(value)
                    ? HardMediumSoftScore.ofMedium(-1)
                    : HardMediumSoftScore.ofSoft(1);
        add(target, name, expected,
                assigned(spec, room, slot),
                factFactory.create(spec, slot, room));
    }

    private void studentAvailability(
            List<Scenario> target,
            String name,
            String value,
            HardMediumSoftScore expected) {
        UUID student = id();
        LessonSpec spec = spec();
        spec.students = List.of(student);
        AuraTimeslotFact slot = slot(1, 9, 10);
        add(target, name, expected,
                assigned(spec,
                        room(40, "CLASSROOM", Set.of(), "A"), slot),
                new AuraStudentAvailabilityFact(
                        spec.offeringId, student, slot.id(), value));
    }

    private void travel(
            List<Scenario> target,
            String name,
            String difficulty,
            int minutes,
            int firstEndHour,
            int gapMinutes,
            HardMediumSoftScore expected) {
        AuraTravelRule rule = new AuraTravelRule(
                "North", "South", minutes, difficulty);
        LessonPair pair = pair();
        pair.right.instructorId = pair.left.instructorId;
        pair.left.travelRules = Map.of(rule.key(), rule);
        pair.right.travelRules = Map.of(rule.key(), rule);
        LocalTime secondStart = LocalTime.of(firstEndHour, 0)
                .plusMinutes(gapMinutes);
        pair.assign(
                room(40, "CLASSROOM", Set.of(), "North"),
                room(40, "CLASSROOM", Set.of(), "South"),
                slot(1, firstEndHour - 1, firstEndHour),
                new AuraTimeslotFact(
                        id(), 1, secondStart, secondStart.plusHours(1)));
        add(target, name, expected, pair.lessons());
    }

    private void load(
            List<Scenario> target,
            String name,
            Consumer<LessonPair> configuration,
            HardMediumSoftScore expected) {
        LessonPair pair = pair();
        configuration.accept(pair);
        add(target, name, expected, pair.lessons());
    }

    private void idleGap(
            List<Scenario> target,
            String name,
            Consumer<LessonPair> identity,
            HardMediumSoftScore expected) {
        LessonPair pair = pair();
        identity.accept(pair);
        pair.assign(
                room(40, "CLASSROOM", Set.of(), "A"),
                room(40, "CLASSROOM", Set.of(), "A"),
                slot(1, 9, 10), slot(1, 11, 12));
        add(target, name, expected, pair.lessons());
    }

    private void overlappingPair(
            List<Scenario> target,
            String name,
            Consumer<LessonPair> configuration) {
        LessonPair pair = pair();
        configuration.accept(pair);
        add(target, name, HardMediumSoftScore.ofHard(-1), pair.lessons());
    }

    private LessonPair pair() {
        return new LessonPair(spec(), spec());
    }

    private LessonSpec spec() {
        return new LessonSpec(id(), id(), id(), id());
    }

    private AuraPlanningLesson assigned(
            LessonSpec spec,
            AuraRoomFact room,
            AuraTimeslotFact timeslot) {
        AuraPlanningLesson lesson = spec.build(id());
        lesson.setRoom(room);
        lesson.setTimeslot(timeslot);
        return lesson;
    }

    private AuraRoomFact room(
            int capacity,
            String roomType,
            Set<String> facilities,
            String building) {
        return new AuraRoomFact(
                id(), capacity, roomType, facilities, building);
    }

    private AuraTimeslotFact slot(
            int day,
            int startHour,
            int endHour) {
        return new AuraTimeslotFact(
                id(), day, LocalTime.of(startHour, 0),
                LocalTime.of(endHour, 0));
    }

    private void add(
            List<Scenario> target,
            String name,
            HardMediumSoftScore expected,
            Object... facts) {
        target.add(new Scenario(name, expected, List.of(facts)));
    }

    private UUID id() {
        return UUID.fromString(String.format(
                "98000000-0000-4000-8000-%012d", sequence++));
    }

    private record Scenario(
            String name,
            HardMediumSoftScore expected,
            List<Object> facts) {
    }

    @FunctionalInterface
    private interface AvailabilityFactFactory {
        Object create(
                LessonSpec spec,
                AuraTimeslotFact slot,
                AuraRoomFact room);
    }

    private final class LessonPair {
        private final LessonSpec left;
        private final LessonSpec right;
        private AuraPlanningLesson leftLesson;
        private AuraPlanningLesson rightLesson;

        private LessonPair(LessonSpec left, LessonSpec right) {
            this.left = left;
            this.right = right;
        }

        private void assignDistinctRoomsSameTime() {
            assign(
                    room(40, "CLASSROOM", Set.of(), "A"),
                    room(40, "CLASSROOM", Set.of(), "A"),
                    slot(1, 9, 10), slot(1, 9, 10));
        }

        private void assign(int leftDay, int rightDay) {
            assign(
                    room(40, "CLASSROOM", Set.of(), "A"),
                    room(40, "CLASSROOM", Set.of(), "A"),
                    slot(leftDay, 9, 10),
                    slot(rightDay, 11, 12));
        }

        private void assign(
                AuraRoomFact leftRoom,
                AuraRoomFact rightRoom,
                AuraTimeslotFact leftSlot,
                AuraTimeslotFact rightSlot) {
            leftLesson = assigned(left, leftRoom, leftSlot);
            rightLesson = assigned(right, rightRoom, rightSlot);
        }

        private Object[] lessons() {
            return new Object[] {leftLesson, rightLesson};
        }
    }

    private static final class LessonSpec {
        private UUID requirementId;
        private UUID offeringId;
        private UUID sectionId;
        private UUID instructorId;
        private int requiredCapacity = 20;
        private String roomType = "CLASSROOM";
        private Set<String> requiredFacilities = Set.of();
        private int durationSlots = 1;
        private List<Integer> allowedDays = List.of();
        private List<Integer> prohibitedDays = List.of();
        private List<Integer> preferredDays = List.of();
        private LocalTime preferredStart;
        private LocalTime preferredEnd;
        private int minimumDaySeparation;
        private int maximumOccurrencesPerDay = 1;
        private boolean sameRoomPreferred = true;
        private UUID fixedRoomId;
        private UUID fixedTimeslotId;
        private boolean pinned;
        private int instructorHardWeekly = 60;
        private int instructorHardDaily = 24;
        private int instructorPreferredWeekly = 60;
        private int instructorPreferredDaily = 24;
        private int sectionHardDaily = 24;
        private int sectionPreferredDaily = 24;
        private String meetingType = "LECTURE";
        private String teachingGroup;
        private UUID linkedRequirementId;
        private boolean lectureBeforeLinked;
        private List<UUID> participatingSections;
        private List<UUID> students = List.of();
        private List<UUID> conflicts = List.of();
        private Map<String, AuraTravelRule> travelRules = Map.of();

        private LessonSpec(
                UUID requirementId,
                UUID offeringId,
                UUID sectionId,
                UUID instructorId) {
            this.requirementId = requirementId;
            this.offeringId = offeringId;
            this.sectionId = sectionId;
            this.instructorId = instructorId;
            this.participatingSections = List.of(sectionId);
        }

        private AuraPlanningLesson build(UUID id) {
            return new AuraPlanningLesson(
                    id, requirementId, offeringId, sectionId, instructorId,
                    requiredCapacity, roomType, requiredFacilities, 1,
                    durationSlots, allowedDays, prohibitedDays, preferredDays,
                    preferredStart, preferredEnd, minimumDaySeparation,
                    maximumOccurrencesPerDay, sameRoomPreferred, fixedRoomId,
                    fixedTimeslotId, pinned, instructorHardWeekly,
                    instructorHardDaily, instructorPreferredWeekly,
                    instructorPreferredDaily, sectionHardDaily,
                    sectionPreferredDaily, meetingType, teachingGroup,
                    linkedRequirementId, lectureBeforeLinked, "EVERY_WEEK",
                    List.of(), participatingSections, students, conflicts,
                    travelRules);
        }
    }
}
