package com.campusone.aura.solver;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import java.util.ArrayList;
import java.util.List;

@PlanningSolution
public class AuraTimetableSolution {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "roomRange")
    private List<AuraRoomFact> rooms = new ArrayList<>();

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "timeslotRange")
    private List<AuraTimeslotFact> timeslots = new ArrayList<>();

    @ProblemFactCollectionProperty
    private List<AuraInstructorAvailabilityFact> instructorAvailability =
            new ArrayList<>();

    @ProblemFactCollectionProperty
    private List<AuraRoomAvailabilityFact> roomAvailability =
            new ArrayList<>();

    @ProblemFactCollectionProperty
    private List<AuraSectionAvailabilityFact> sectionAvailability =
            new ArrayList<>();

    @PlanningEntityCollectionProperty
    private List<AuraPlanningLesson> lessons = new ArrayList<>();

    @PlanningScore
    private HardMediumSoftScore score;

    public AuraTimetableSolution() {
    }

    public AuraTimetableSolution(
            List<AuraRoomFact> rooms,
            List<AuraTimeslotFact> timeslots,
            List<AuraInstructorAvailabilityFact> instructorAvailability,
            List<AuraRoomAvailabilityFact> roomAvailability,
            List<AuraSectionAvailabilityFact> sectionAvailability,
            List<AuraPlanningLesson> lessons) {
        this.rooms = new ArrayList<>(rooms);
        this.timeslots = new ArrayList<>(timeslots);
        this.instructorAvailability = new ArrayList<>(instructorAvailability);
        this.roomAvailability = new ArrayList<>(roomAvailability);
        this.sectionAvailability = new ArrayList<>(sectionAvailability);
        this.lessons = new ArrayList<>(lessons);
    }

    public List<AuraRoomFact> getRooms() {
        return rooms;
    }

    public List<AuraTimeslotFact> getTimeslots() {
        return timeslots;
    }

    public List<AuraInstructorAvailabilityFact> getInstructorAvailability() {
        return instructorAvailability;
    }

    public List<AuraRoomAvailabilityFact> getRoomAvailability() {
        return roomAvailability;
    }

    public List<AuraSectionAvailabilityFact> getSectionAvailability() {
        return sectionAvailability;
    }

    public List<AuraPlanningLesson> getLessons() {
        return lessons;
    }

    public HardMediumSoftScore getScore() {
        return score;
    }

    public void setScore(HardMediumSoftScore score) {
        this.score = score;
    }
}
