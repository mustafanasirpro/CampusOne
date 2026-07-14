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
import com.campusone.aura.repository.AuraJdbcRepository.SolverTimeslot;
import com.campusone.aura.solver.AuraConstraintProvider;
import com.campusone.aura.solver.AuraInstructorAvailabilityFact;
import com.campusone.aura.solver.AuraPlanningLesson;
import com.campusone.aura.solver.AuraRoomAvailabilityFact;
import com.campusone.aura.solver.AuraRoomFact;
import com.campusone.aura.solver.AuraTimeslotFact;
import com.campusone.aura.solver.AuraTimetableSolution;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuraSolverService {

    public SolverResult solve(
            List<SolverRequirement> requirements,
            List<SolverRoom> rooms,
            List<SolverTimeslot> timeslots,
            List<SolverInstructorAvailability> instructorAvailability,
            List<SolverRoomAvailability> roomAvailability,
            int terminationSeconds) {
        List<AuraRoomFact> roomFacts = rooms.stream()
                .map(room -> new AuraRoomFact(
                        room.id(),
                        room.capacity(),
                        room.roomType()))
                .sorted(Comparator.comparingInt(AuraRoomFact::capacity))
                .toList();
        List<AuraTimeslotFact> timeslotFacts = timeslots.stream()
                .map(timeslot -> new AuraTimeslotFact(
                        timeslot.id(),
                        timeslot.dayOfWeek(),
                        timeslot.startsAt(),
                        timeslot.endsAt()))
                .toList();
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
        List<AuraPlanningLesson> lessons = expandLessons(requirements);

        AuraTimetableSolution problem =
                new AuraTimetableSolution(
                        roomFacts,
                        timeslotFacts,
                        instructorAvailabilityFacts,
                        roomAvailabilityFacts,
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
            List<SolverRequirement> requirements) {
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
                        requirement.roomType()));
            }
        }
        return lessons;
    }

    public record SolverResult(
            String score,
            List<SolverAssignment> assignments) {
    }
}
