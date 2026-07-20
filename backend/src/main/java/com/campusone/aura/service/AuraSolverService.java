package com.campusone.aura.service;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import com.campusone.aura.repository.AuraJdbcRepository.SolverAssignment;
import com.campusone.aura.repository.AuraJdbcRepository.SolverInstructorAvailability;
import com.campusone.aura.repository.AuraJdbcRepository.SolverRequirement;
import com.campusone.aura.repository.AuraJdbcRepository.SolverRoomAvailability;
import com.campusone.aura.repository.AuraJdbcRepository.SolverRoom;
import com.campusone.aura.repository.AuraJdbcRepository.SolverSectionAvailability;
import com.campusone.aura.repository.AuraJdbcRepository.SolverStudentAvailability;
import com.campusone.aura.repository.AuraJdbcRepository.SolverTimeslot;
import com.campusone.aura.repository.AuraJdbcRepository.SolverTravelRuleInput;
import com.campusone.aura.solver.AuraConstraintProvider;
import com.campusone.aura.solver.AuraInstructorAvailabilityFact;
import com.campusone.aura.solver.AuraPlanningLesson;
import com.campusone.aura.solver.AuraRoomAvailabilityFact;
import com.campusone.aura.solver.AuraRoomFact;
import com.campusone.aura.solver.AuraSectionAvailabilityFact;
import com.campusone.aura.solver.AuraStudentAvailabilityFact;
import com.campusone.aura.solver.AuraTimeslotFact;
import com.campusone.aura.solver.AuraTimetableSolution;
import com.campusone.aura.solver.AuraTravelRule;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AuraSolverService {

    public SolverResult solve(
            List<SolverRequirement> requirements,
            List<SolverRoom> rooms,
            List<SolverTimeslot> timeslots,
            List<SolverInstructorAvailability> instructorAvailability,
            List<SolverRoomAvailability> roomAvailability,
            List<SolverSectionAvailability> sectionAvailability,
            int terminationSeconds) {
        return solve(
                requirements,
                rooms,
                timeslots,
                instructorAvailability,
                roomAvailability,
                sectionAvailability,
                List.of(),
                List.of(),
                terminationSeconds,
                Map.of(),
                0L);
    }

    public SolverResult solve(
            List<SolverRequirement> requirements,
            List<SolverRoom> rooms,
            List<SolverTimeslot> timeslots,
            List<SolverInstructorAvailability> instructorAvailability,
            List<SolverRoomAvailability> roomAvailability,
            List<SolverSectionAvailability> sectionAvailability,
            List<SolverStudentAvailability> studentAvailability,
            List<SolverTravelRuleInput> travelRules,
            int terminationSeconds) {
        return solve(
                requirements,
                rooms,
                timeslots,
                instructorAvailability,
                roomAvailability,
                sectionAvailability,
                studentAvailability,
                travelRules,
                terminationSeconds,
                Map.of(),
                0L);
    }

    public SolverResult solve(
            List<SolverRequirement> requirements,
            List<SolverRoom> rooms,
            List<SolverTimeslot> timeslots,
            List<SolverInstructorAvailability> instructorAvailability,
            List<SolverRoomAvailability> roomAvailability,
            List<SolverSectionAvailability> sectionAvailability,
            List<SolverStudentAvailability> studentAvailability,
            List<SolverTravelRuleInput> travelRules,
            int terminationSeconds,
            Map<String, HardMediumSoftScore> constraintWeights,
            long randomSeed) {
        List<AuraRoomFact> roomFacts = rooms.stream()
                .map(room -> new AuraRoomFact(
                        room.id(),
                        room.capacity(),
                        room.roomType(),
                        Set.copyOf(room.facilities()),
                        room.building()))
                .sorted(Comparator.comparingInt(AuraRoomFact::capacity))
                .toList();
        List<AuraTimeslotFact> timeslotFacts = buildTimeslotFacts(timeslots);
        List<AuraInstructorAvailabilityFact> instructorAvailabilityFacts =
                instructorAvailability.stream()
                        .map(availability -> new AuraInstructorAvailabilityFact(
                                availability.instructorId(),
                                availability.timeslotId(),
                                availability.availability()))
                        .toList();
        List<AuraRoomAvailabilityFact> roomAvailabilityFacts =
                roomAvailability.stream()
                        .map(availability -> new AuraRoomAvailabilityFact(
                                availability.roomId(),
                                availability.timeslotId(),
                                availability.availability()))
                        .toList();
        List<AuraSectionAvailabilityFact> sectionAvailabilityFacts =
                sectionAvailability.stream()
                        .map(availability -> new AuraSectionAvailabilityFact(
                                availability.sectionId(),
                                availability.timeslotId(),
                                availability.availability()))
                        .toList();
        List<AuraStudentAvailabilityFact> studentAvailabilityFacts =
                studentAvailability.stream()
                        .map(availability -> new AuraStudentAvailabilityFact(
                                availability.offeringId(),
                                availability.studentUserId(),
                                availability.timeslotId(),
                                availability.availability()))
                        .toList();
        Map<String, AuraTravelRule> travelRuleFacts = travelRules.stream()
                .map(rule -> new AuraTravelRule(
                        rule.fromBuilding(),
                        rule.toBuilding(),
                        rule.minutes(),
                        rule.difficulty()))
                .collect(Collectors.toUnmodifiableMap(
                        AuraTravelRule::key,
                        Function.identity(),
                        (left, right) -> left));
        List<AuraPlanningLesson> lessons =
                expandLessons(requirements, travelRuleFacts);
        seedInitialAssignments(
                lessons,
                roomFacts,
                timeslotFacts,
                instructorAvailabilityFacts,
                roomAvailabilityFacts,
                sectionAvailabilityFacts,
                studentAvailabilityFacts);

        AuraTimetableSolution problem =
                new AuraTimetableSolution(
                        roomFacts,
                        timeslotFacts,
                        instructorAvailabilityFacts,
                        roomAvailabilityFacts,
                        sectionAvailabilityFacts,
                        studentAvailabilityFacts,
                        lessons);
        problem.setConstraintWeightOverrides(
                ConstraintWeightOverrides.of(constraintWeights));
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(AuraTimetableSolution.class)
                .withEntityClasses(AuraPlanningLesson.class)
                .withEnvironmentMode(EnvironmentMode.NO_ASSERT)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProviderClass(
                                AuraConstraintProvider.class))
                .withRandomSeed(randomSeed)
                .withTerminationConfig(new TerminationConfig()
                        .withSecondsSpentLimit((long) Math.max(
                                1,
                                terminationSeconds)));

        AuraTimetableSolution solved =
                SolverFactory.<AuraTimetableSolution>create(solverConfig)
                        .buildSolver()
                        .solve(problem);
        List<SolverAssignment> assignments = solved.getLessons().stream()
                .filter(lesson -> lesson.getRoom() != null
                        && lesson.getTimeslot() != null)
                .map(lesson -> new SolverAssignment(
                        lesson.getRequirementId(),
                        lesson.getOfferingId(),
                        lesson.getSectionId(),
                        lesson.getInstructorId(),
                        lesson.getRoom().id(),
                        lesson.getTimeslot().id()))
                .toList();
        if (assignments.size() != lessons.size()) {
            throw new IllegalStateException(
                    "The solver stopped before assigning every required occurrence.");
        }
        return new SolverResult(
                solved.getScore() == null
                        ? "0hard/0medium/0soft"
                        : solved.getScore().toString(),
                assignments,
                lessons.size(),
                "COMPLETED");
    }

    private List<AuraPlanningLesson> expandLessons(
            List<SolverRequirement> requirements,
            Map<String, AuraTravelRule> travelRules) {
        List<AuraPlanningLesson> lessons = new ArrayList<>();
        for (SolverRequirement requirement : requirements) {
            for (int index = 0; index < requirement.sessionsPerWeek(); index++) {
                lessons.add(new AuraPlanningLesson(
                        occurrenceId(requirement.requirementId(), index + 1),
                        requirement.requirementId(),
                        requirement.offeringId(),
                        requirement.sectionId(),
                        requirement.instructorId(),
                        requirement.requiredCapacity(),
                        requirement.roomType(),
                        Set.copyOf(requirement.requiredFacilities()),
                        index + 1,
                        requirement.durationSlots(),
                        requirement.allowedDays(),
                        requirement.prohibitedDays(),
                        requirement.preferredDays(),
                        requirement.preferredStartTime(),
                        requirement.preferredEndTime(),
                        requirement.minimumDaySeparation(),
                        requirement.maximumOccurrencesPerDay(),
                        requirement.sameRoomPreferred(),
                        requirement.fixedRoomId(),
                        requirement.fixedTimeslotId(),
                        requirement.pinned(),
                        requirement.instructorHardWeeklyLoad(),
                        requirement.instructorHardDailyLoad(),
                        requirement.instructorPreferredWeeklyLoad(),
                        requirement.instructorPreferredDailyLoad(),
                        requirement.sectionHardDailyLoad(),
                        requirement.sectionPreferredDailyLoad(),
                        requirement.meetingType(),
                        requirement.teachingGroup(),
                        requirement.linkedRequirementId(),
                        requirement.lectureBeforeLinked(),
                        requirement.weekPattern(),
                        requirement.customWeeks(),
                        requirement.participatingSectionIds(),
                        requirement.studentUserIds(),
                        requirement.hardConflictOfferingIds(),
                        travelRules));
            }
        }
        return lessons;
    }

    private UUID occurrenceId(UUID requirementId, int occurrenceNumber) {
        return UUID.nameUUIDFromBytes(
                (requirementId + ":" + occurrenceNumber)
                        .getBytes(StandardCharsets.UTF_8));
    }

    private void seedInitialAssignments(
            List<AuraPlanningLesson> lessons,
            List<AuraRoomFact> rooms,
            List<AuraTimeslotFact> timeslots,
            List<AuraInstructorAvailabilityFact> instructorAvailability,
            List<AuraRoomAvailabilityFact> roomAvailability,
            List<AuraSectionAvailabilityFact> sectionAvailability,
            List<AuraStudentAvailabilityFact> studentAvailability) {
        Set<ResourceTimeslot> unavailableInstructors = new HashSet<>();
        instructorAvailability.stream()
                .filter(value -> "UNAVAILABLE".equals(value.availability()))
                .map(value -> new ResourceTimeslot(
                        value.instructorId(), value.timeslotId()))
                .forEach(unavailableInstructors::add);
        Set<ResourceTimeslot> unavailableRooms = new HashSet<>();
        roomAvailability.stream()
                .filter(value -> "UNAVAILABLE".equals(value.availability()))
                .map(value -> new ResourceTimeslot(
                        value.roomId(), value.timeslotId()))
                .forEach(unavailableRooms::add);
        Set<ResourceTimeslot> unavailableSections = new HashSet<>();
        sectionAvailability.stream()
                .filter(value -> "UNAVAILABLE".equals(value.availability()))
                .map(value -> new ResourceTimeslot(
                        value.sectionId(), value.timeslotId()))
                .forEach(unavailableSections::add);
        Set<ResourceTimeslot> unavailableOfferings = new HashSet<>();
        studentAvailability.stream()
                .filter(value -> "UNAVAILABLE".equals(value.availability()))
                .map(value -> new ResourceTimeslot(
                        value.offeringId(), value.timeslotId()))
                .forEach(unavailableOfferings::add);

        Set<ResourceTimeslot> usedRooms = new HashSet<>();
        Set<ResourceTimeslot> usedInstructors = new HashSet<>();
        Set<ResourceTimeslot> usedSections = new HashSet<>();
        Set<ResourceTimeslot> usedStudents = new HashSet<>();
        Set<ResourceTimeslot> usedOfferings = new HashSet<>();
        for (AuraPlanningLesson lesson : lessons) {
            List<AuraRoomFact> eligibleRooms = rooms.stream()
                    .filter(room -> staticRoomEligible(lesson, room))
                    .toList();
            List<AuraTimeslotFact> eligibleTimeslots = timeslots.stream()
                    .filter(timeslot -> staticTimeslotEligible(
                            lesson,
                            timeslot,
                            unavailableInstructors,
                            unavailableSections,
                            unavailableOfferings))
                    .toList();
            InitialAssignment fallback = null;
            InitialAssignment selected = null;
            for (AuraTimeslotFact timeslot : eligibleTimeslots) {
                for (AuraRoomFact room : eligibleRooms) {
                    if (unavailableRooms.contains(new ResourceTimeslot(
                            room.id(), timeslot.id()))) {
                        continue;
                    }
                    InitialAssignment candidate = new InitialAssignment(
                            room, timeslot);
                    if (fallback == null) {
                        fallback = candidate;
                    }
                    if (!hasExactOccupancyConflict(
                            lesson,
                            candidate,
                            usedRooms,
                            usedInstructors,
                            usedSections,
                            usedStudents,
                            usedOfferings)) {
                        selected = candidate;
                        break;
                    }
                }
                if (selected != null) {
                    break;
                }
            }
            InitialAssignment assignment = selected == null ? fallback : selected;
            if (assignment == null) {
                continue;
            }
            lesson.setRoom(assignment.room());
            lesson.setTimeslot(assignment.timeslot());
            recordOccupancy(
                    lesson,
                    assignment.timeslot().id(),
                    assignment.room().id(),
                    usedRooms,
                    usedInstructors,
                    usedSections,
                    usedStudents,
                    usedOfferings);
        }
    }

    private boolean staticRoomEligible(
            AuraPlanningLesson lesson,
            AuraRoomFact room) {
        return (lesson.getFixedRoomId() == null
                    || lesson.getFixedRoomId().equals(room.id()))
                && room.capacity() >= lesson.getRequiredCapacity()
                && (lesson.getRoomType() == null
                    || lesson.getRoomType().equals(room.roomType()))
                && room.facilities().containsAll(
                        lesson.getRequiredFacilities());
    }

    private boolean staticTimeslotEligible(
            AuraPlanningLesson lesson,
            AuraTimeslotFact timeslot,
            Set<ResourceTimeslot> unavailableInstructors,
            Set<ResourceTimeslot> unavailableSections,
            Set<ResourceTimeslot> unavailableOfferings) {
        ResourceTimeslot instructorSlot = new ResourceTimeslot(
                lesson.getInstructorId(), timeslot.id());
        ResourceTimeslot sectionSlot = new ResourceTimeslot(
                lesson.getSectionId(), timeslot.id());
        ResourceTimeslot offeringSlot = new ResourceTimeslot(
                lesson.getOfferingId(), timeslot.id());
        return (lesson.getFixedTimeslotId() == null
                    || lesson.getFixedTimeslotId().equals(timeslot.id()))
                && timeslot.supportsDuration(lesson.getDurationSlots())
                && (lesson.getAllowedDays().isEmpty()
                    || lesson.getAllowedDays().contains(timeslot.dayOfWeek()))
                && !lesson.getProhibitedDays().contains(timeslot.dayOfWeek())
                && !unavailableInstructors.contains(instructorSlot)
                && !unavailableSections.contains(sectionSlot)
                && !unavailableOfferings.contains(offeringSlot);
    }

    private boolean hasExactOccupancyConflict(
            AuraPlanningLesson lesson,
            InitialAssignment assignment,
            Set<ResourceTimeslot> usedRooms,
            Set<ResourceTimeslot> usedInstructors,
            Set<ResourceTimeslot> usedSections,
            Set<ResourceTimeslot> usedStudents,
            Set<ResourceTimeslot> usedOfferings) {
        UUID timeslotId = assignment.timeslot().id();
        if (usedRooms.contains(new ResourceTimeslot(
                    assignment.room().id(), timeslotId))
                || usedInstructors.contains(new ResourceTimeslot(
                    lesson.getInstructorId(), timeslotId))) {
            return true;
        }
        if (lesson.getParticipatingSectionIds().stream().anyMatch(sectionId ->
                usedSections.contains(new ResourceTimeslot(
                        sectionId, timeslotId)))) {
            return true;
        }
        if (lesson.getStudentUserIds().stream().anyMatch(studentId ->
                usedStudents.contains(new ResourceTimeslot(
                        studentId, timeslotId)))) {
            return true;
        }
        return lesson.getHardConflictOfferingIds().stream().anyMatch(offeringId ->
                usedOfferings.contains(new ResourceTimeslot(
                        offeringId, timeslotId)));
    }

    private void recordOccupancy(
            AuraPlanningLesson lesson,
            UUID timeslotId,
            UUID roomId,
            Set<ResourceTimeslot> usedRooms,
            Set<ResourceTimeslot> usedInstructors,
            Set<ResourceTimeslot> usedSections,
            Set<ResourceTimeslot> usedStudents,
            Set<ResourceTimeslot> usedOfferings) {
        usedRooms.add(new ResourceTimeslot(roomId, timeslotId));
        usedInstructors.add(new ResourceTimeslot(
                lesson.getInstructorId(), timeslotId));
        lesson.getParticipatingSectionIds().forEach(sectionId ->
                usedSections.add(new ResourceTimeslot(sectionId, timeslotId)));
        lesson.getStudentUserIds().forEach(studentId ->
                usedStudents.add(new ResourceTimeslot(studentId, timeslotId)));
        usedOfferings.add(new ResourceTimeslot(
                lesson.getOfferingId(), timeslotId));
    }

    private record InitialAssignment(
            AuraRoomFact room,
            AuraTimeslotFact timeslot) {
    }

    private record ResourceTimeslot(UUID resourceId, UUID timeslotId) {
    }

    private List<AuraTimeslotFact> buildTimeslotFacts(
            List<SolverTimeslot> timeslots) {
        List<SolverTimeslot> ordered = timeslots.stream()
                .sorted(Comparator
                        .comparingInt(SolverTimeslot::dayOfWeek)
                        .thenComparingInt(SolverTimeslot::slotOrder)
                        .thenComparing(SolverTimeslot::startsAt))
                .toList();
        List<AuraTimeslotFact> facts = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            SolverTimeslot start = ordered.get(index);
            List<UUID> contiguousIds = new ArrayList<>();
            List<java.time.LocalTime> contiguousEnds = new ArrayList<>();
            if ("INSTRUCTIONAL".equals(start.slotType())) {
                contiguousIds.add(start.id());
                contiguousEnds.add(start.endsAt());
                SolverTimeslot previous = start;
                for (int candidateIndex = index + 1;
                        candidateIndex < ordered.size();
                        candidateIndex++) {
                    SolverTimeslot candidate = ordered.get(candidateIndex);
                    if (candidate.dayOfWeek() != start.dayOfWeek()
                            || !"INSTRUCTIONAL".equals(candidate.slotType())
                            || candidate.slotOrder() != previous.slotOrder() + 1
                            || !candidate.startsAt().equals(previous.endsAt())) {
                        break;
                    }
                    contiguousIds.add(candidate.id());
                    contiguousEnds.add(candidate.endsAt());
                    previous = candidate;
                }
            }
            facts.add(new AuraTimeslotFact(
                    start.id(),
                    start.dayOfWeek(),
                    start.startsAt(),
                    start.endsAt(),
                    start.slotOrder(),
                    start.slotType(),
                    contiguousIds,
                    contiguousEnds));
        }
        return List.copyOf(facts);
    }

    public record SolverResult(
            String score,
            List<SolverAssignment> assignments,
            int candidateCount,
            String terminationReason) {

        public SolverResult(String score, List<SolverAssignment> assignments) {
            this(score, assignments, assignments.size(), "COMPLETED");
        }
    }
}
