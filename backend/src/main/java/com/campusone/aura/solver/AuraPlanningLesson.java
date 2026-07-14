package com.campusone.aura.solver;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import java.util.UUID;

@PlanningEntity
public class AuraPlanningLesson {

    private UUID id;
    private UUID requirementId;
    private UUID offeringId;
    private UUID sectionId;
    private UUID instructorId;
    private int requiredCapacity;
    private String roomType;

    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    private AuraRoomFact room;

    @PlanningVariable(valueRangeProviderRefs = "timeslotRange")
    private AuraTimeslotFact timeslot;

    public AuraPlanningLesson() {
    }

    public AuraPlanningLesson(
            UUID id,
            UUID requirementId,
            UUID offeringId,
            UUID sectionId,
            UUID instructorId,
            int requiredCapacity,
            String roomType) {
        this.id = id;
        this.requirementId = requirementId;
        this.offeringId = offeringId;
        this.sectionId = sectionId;
        this.instructorId = instructorId;
        this.requiredCapacity = requiredCapacity;
        this.roomType = roomType;
    }

    public UUID getId() {
        return id;
    }

    @PlanningId
    public String getPlanningId() {
        return id == null ? null : id.toString();
    }

    public UUID getRequirementId() {
        return requirementId;
    }

    public UUID getOfferingId() {
        return offeringId;
    }

    public UUID getSectionId() {
        return sectionId;
    }

    public UUID getInstructorId() {
        return instructorId;
    }

    public int getRequiredCapacity() {
        return requiredCapacity;
    }

    public String getRoomType() {
        return roomType;
    }

    public AuraRoomFact getRoom() {
        return room;
    }

    public void setRoom(AuraRoomFact room) {
        this.room = room;
    }

    public AuraTimeslotFact getTimeslot() {
        return timeslot;
    }

    public void setTimeslot(AuraTimeslotFact timeslot) {
        this.timeslot = timeslot;
    }
}
