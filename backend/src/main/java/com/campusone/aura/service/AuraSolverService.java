package com.campusone.aura.service;

import ai.timefold.solver.core.api.solver.SolverFactory;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
                terminationSeconds);
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

        AuraTimetableSolution problem =
                new AuraTimetableSolution(
                        roomFacts,
                        timeslotFacts,
                        instructorAvailabilityFacts,
                        roomAvailabilityFacts,
                        sectionAvailabilityFacts,
                        studentAvailabilityFacts,
                        lessons);
        SolverConfig solverConfig = new SolverConfig()
                .withSolutionClass(AuraTimetableSolution.class)
                .withEntityClasses(AuraPlanningLesson.class)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProviderClass(
                                AuraConstraintProvider.class))
                .withTerminationConfig(new TerminationConfig()
                        .withSecondsSpentLimit((long) Math.max(
                                1,
                                terminationSeconds)));

        AuraTimetableSolution solved =
                SolverFactory.<AuraTimetableSolution>create(solverConfig)
                        .buildSolver()
                        .solve(problem);
        return new SolverResult(
                solved.getScore() == null
                        ? "0hard/0medium/0soft"
                        : solved.getScore().toString(),
                solved.getLessons().stream()
                        .filter(lesson -> lesson.getRoom() != null
                                && lesson.getTimeslot() != null)
                        .map(lesson -> new SolverAssignment(
                                lesson.getRequirementId(),
                                lesson.getOfferingId(),
                                lesson.getSectionId(),
                                lesson.getInstructorId(),
                                lesson.getRoom().id(),
                                lesson.getTimeslot().id()))
                        .toList());
    }

    private List<AuraPlanningLesson> expandLessons(
            List<SolverRequirement> requirements,
            Map<String, AuraTravelRule> travelRules) {
        List<AuraPlanningLesson> lessons = new ArrayList<>();
        for (SolverRequirement requirement : requirements) {
            for (int index = 0; index < requirement.sessionsPerWeek(); index++) {
                lessons.add(new AuraPlanningLesson(
                        UUID.randomUUID(),
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
            List<SolverAssignment> assignments) {
    }
}
