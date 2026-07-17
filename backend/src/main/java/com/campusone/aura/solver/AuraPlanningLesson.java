package com.campusone.aura.solver;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private Set<String> requiredFacilities;
    private int occurrenceIndex;
    private int durationSlots = 1;
    private Set<Integer> allowedDays = Set.of();
    private Set<Integer> prohibitedDays = Set.of();
    private Set<Integer> preferredDays = Set.of();
    private LocalTime preferredStartTime;
    private LocalTime preferredEndTime;
    private int minimumDaySeparation;
    private int maximumOccurrencesPerDay = 1;
    private boolean sameRoomPreferred = true;
    private UUID fixedRoomId;
    private UUID fixedTimeslotId;
    private boolean pinned;
    private int instructorHardWeeklyLoad = 60;
    private int instructorHardDailyLoad = 24;
    private int instructorPreferredWeeklyLoad = 60;
    private int instructorPreferredDailyLoad = 24;
    private int sectionHardDailyLoad = 24;
    private int sectionPreferredDailyLoad = 24;
    private String meetingType = "LECTURE";
    private String teachingGroup;
    private UUID linkedRequirementId;
    private boolean lectureBeforeLinked;
    private String weekPattern = "EVERY_WEEK";
    private Set<Integer> customWeeks = Set.of();
    private Set<UUID> participatingSectionIds = Set.of();
    private Set<UUID> studentUserIds = Set.of();
    private Set<UUID> hardConflictOfferingIds = Set.of();
    private Map<String, AuraTravelRule> travelRules = Map.of();

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
            String roomType,
            Set<String> requiredFacilities) {
        this.id = id;
        this.requirementId = requirementId;
        this.offeringId = offeringId;
        this.sectionId = sectionId;
        this.instructorId = instructorId;
        this.requiredCapacity = requiredCapacity;
        this.roomType = roomType;
        this.requiredFacilities = requiredFacilities;
        this.participatingSectionIds = sectionId == null
                ? Set.of()
                : Set.of(sectionId);
    }

    public AuraPlanningLesson(
            UUID id,
            UUID requirementId,
            UUID offeringId,
            UUID sectionId,
            UUID instructorId,
            int requiredCapacity,
            String roomType,
            Set<String> requiredFacilities,
            int occurrenceIndex,
            int durationSlots,
            List<Integer> allowedDays,
            List<Integer> prohibitedDays,
            List<Integer> preferredDays,
            LocalTime preferredStartTime,
            LocalTime preferredEndTime,
            int minimumDaySeparation,
            int maximumOccurrencesPerDay,
            boolean sameRoomPreferred,
            UUID fixedRoomId,
            UUID fixedTimeslotId,
            boolean pinned,
            int instructorHardWeeklyLoad,
            int instructorHardDailyLoad,
            int instructorPreferredWeeklyLoad,
            int instructorPreferredDailyLoad,
            int sectionHardDailyLoad,
            int sectionPreferredDailyLoad) {
        this(
                id, requirementId, offeringId, sectionId, instructorId,
                requiredCapacity, roomType, requiredFacilities);
        this.occurrenceIndex = occurrenceIndex;
        this.durationSlots = durationSlots;
        this.allowedDays = Set.copyOf(allowedDays);
        this.prohibitedDays = Set.copyOf(prohibitedDays);
        this.preferredDays = Set.copyOf(preferredDays);
        this.preferredStartTime = preferredStartTime;
        this.preferredEndTime = preferredEndTime;
        this.minimumDaySeparation = minimumDaySeparation;
        this.maximumOccurrencesPerDay = maximumOccurrencesPerDay;
        this.sameRoomPreferred = sameRoomPreferred;
        this.fixedRoomId = fixedRoomId;
        this.fixedTimeslotId = fixedTimeslotId;
        this.pinned = pinned;
        this.instructorHardWeeklyLoad = instructorHardWeeklyLoad;
        this.instructorHardDailyLoad = instructorHardDailyLoad;
        this.instructorPreferredWeeklyLoad = instructorPreferredWeeklyLoad;
        this.instructorPreferredDailyLoad = instructorPreferredDailyLoad;
        this.sectionHardDailyLoad = sectionHardDailyLoad;
        this.sectionPreferredDailyLoad = sectionPreferredDailyLoad;
    }

    public AuraPlanningLesson(
            UUID id,
            UUID requirementId,
            UUID offeringId,
            UUID sectionId,
            UUID instructorId,
            int requiredCapacity,
            String roomType,
            Set<String> requiredFacilities,
            int occurrenceIndex,
            int durationSlots,
            List<Integer> allowedDays,
            List<Integer> prohibitedDays,
            List<Integer> preferredDays,
            LocalTime preferredStartTime,
            LocalTime preferredEndTime,
            int minimumDaySeparation,
            int maximumOccurrencesPerDay,
            boolean sameRoomPreferred,
            UUID fixedRoomId,
            UUID fixedTimeslotId,
            boolean pinned,
            int instructorHardWeeklyLoad,
            int instructorHardDailyLoad,
            int instructorPreferredWeeklyLoad,
            int instructorPreferredDailyLoad,
            int sectionHardDailyLoad,
            int sectionPreferredDailyLoad,
            String meetingType,
            String teachingGroup,
            UUID linkedRequirementId,
            boolean lectureBeforeLinked,
            String weekPattern,
            List<Integer> customWeeks,
            List<UUID> participatingSectionIds,
            List<UUID> studentUserIds,
            List<UUID> hardConflictOfferingIds,
            Map<String, AuraTravelRule> travelRules) {
        this(
                id, requirementId, offeringId, sectionId, instructorId,
                requiredCapacity, roomType, requiredFacilities,
                occurrenceIndex, durationSlots, allowedDays, prohibitedDays,
                preferredDays, preferredStartTime, preferredEndTime,
                minimumDaySeparation, maximumOccurrencesPerDay,
                sameRoomPreferred, fixedRoomId, fixedTimeslotId, pinned,
                instructorHardWeeklyLoad, instructorHardDailyLoad,
                instructorPreferredWeeklyLoad, instructorPreferredDailyLoad,
                sectionHardDailyLoad, sectionPreferredDailyLoad);
        this.meetingType = meetingType == null ? "LECTURE" : meetingType;
        this.teachingGroup = teachingGroup;
        this.linkedRequirementId = linkedRequirementId;
        this.lectureBeforeLinked = lectureBeforeLinked;
        this.weekPattern = weekPattern == null ? "EVERY_WEEK" : weekPattern;
        this.customWeeks = customWeeks == null
                ? Set.of()
                : Set.copyOf(customWeeks);
        this.participatingSectionIds = participatingSectionIds == null
                || participatingSectionIds.isEmpty()
                ? (sectionId == null ? Set.of() : Set.of(sectionId))
                : Set.copyOf(participatingSectionIds);
        this.studentUserIds = studentUserIds == null
                ? Set.of()
                : Set.copyOf(studentUserIds);
        this.hardConflictOfferingIds = hardConflictOfferingIds == null
                ? Set.of()
                : Set.copyOf(hardConflictOfferingIds);
        this.travelRules = travelRules == null
                ? Map.of()
                : Map.copyOf(travelRules);
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

    public Set<String> getRequiredFacilities() {
        return requiredFacilities == null ? Set.of() : requiredFacilities;
    }

    public int getOccurrenceIndex() {
        return occurrenceIndex;
    }

    public int getDurationSlots() {
        return durationSlots;
    }

    public Set<Integer> getAllowedDays() {
        return allowedDays;
    }

    public Set<Integer> getProhibitedDays() {
        return prohibitedDays;
    }

    public Set<Integer> getPreferredDays() {
        return preferredDays;
    }

    public LocalTime getPreferredStartTime() {
        return preferredStartTime;
    }

    public LocalTime getPreferredEndTime() {
        return preferredEndTime;
    }

    public int getMinimumDaySeparation() {
        return minimumDaySeparation;
    }

    public int getMaximumOccurrencesPerDay() {
        return maximumOccurrencesPerDay;
    }

    public boolean isSameRoomPreferred() {
        return sameRoomPreferred;
    }

    public UUID getFixedRoomId() {
        return fixedRoomId;
    }

    public UUID getFixedTimeslotId() {
        return fixedTimeslotId;
    }

    public boolean isPinned() {
        return pinned;
    }

    public int getInstructorHardWeeklyLoad() {
        return instructorHardWeeklyLoad;
    }

    public int getInstructorHardDailyLoad() {
        return instructorHardDailyLoad;
    }

    public int getInstructorPreferredWeeklyLoad() {
        return instructorPreferredWeeklyLoad;
    }

    public int getInstructorPreferredDailyLoad() {
        return instructorPreferredDailyLoad;
    }

    public int getSectionHardDailyLoad() {
        return sectionHardDailyLoad;
    }

    public int getSectionPreferredDailyLoad() {
        return sectionPreferredDailyLoad;
    }

    public String getMeetingType() {
        return meetingType;
    }

    public String getTeachingGroup() {
        return teachingGroup;
    }

    public UUID getLinkedRequirementId() {
        return linkedRequirementId;
    }

    public boolean isLectureBeforeLinked() {
        return lectureBeforeLinked;
    }

    public String getWeekPattern() {
        return weekPattern;
    }

    public Set<Integer> getCustomWeeks() {
        return customWeeks;
    }

    public Set<UUID> getParticipatingSectionIds() {
        return participatingSectionIds;
    }

    public Set<UUID> getStudentUserIds() {
        return studentUserIds;
    }

    public Set<UUID> getHardConflictOfferingIds() {
        return hardConflictOfferingIds;
    }

    public boolean sharesSectionWith(AuraPlanningLesson other) {
        return participatingSectionIds.stream()
                .anyMatch(other.participatingSectionIds::contains);
    }

    public int sharedStudentCount(AuraPlanningLesson other) {
        return (int) studentUserIds.stream()
                .filter(other.studentUserIds::contains)
                .count();
    }

    public boolean conflictsWithOffering(AuraPlanningLesson other) {
        return hardConflictOfferingIds.contains(other.offeringId)
                || other.hardConflictOfferingIds.contains(offeringId);
    }

    public boolean overlapsWeeks(AuraPlanningLesson other) {
        if ("ODD_WEEK".equals(weekPattern)
                && "EVEN_WEEK".equals(other.weekPattern)
                || "EVEN_WEEK".equals(weekPattern)
                && "ODD_WEEK".equals(other.weekPattern)) {
            return false;
        }
        if ("CUSTOM_WEEK_SET".equals(weekPattern)
                && "CUSTOM_WEEK_SET".equals(other.weekPattern)) {
            return customWeeks.stream().anyMatch(other.customWeeks::contains);
        }
        if ("CUSTOM_WEEK_SET".equals(weekPattern)) {
            return customWeeks.stream().anyMatch(
                    week -> matchesWeekPattern(other.weekPattern, week));
        }
        if ("CUSTOM_WEEK_SET".equals(other.weekPattern)) {
            return other.customWeeks.stream().anyMatch(
                    week -> matchesWeekPattern(weekPattern, week));
        }
        return true;
    }

    public AuraTravelRule travelRuleTo(AuraPlanningLesson other) {
        if (room == null || other.room == null) {
            return null;
        }
        return travelRules.get(AuraTravelRule.key(
                room.building(), other.room.building()));
    }

    private boolean matchesWeekPattern(String pattern, int week) {
        return switch (pattern == null ? "EVERY_WEEK" : pattern) {
            case "ODD_WEEK" -> week % 2 == 1;
            case "EVEN_WEEK" -> week % 2 == 0;
            default -> true;
        };
    }

    public LocalTime getEffectiveEndTime() {
        return timeslot == null
                ? null
                : timeslot.effectiveEnd(durationSlots);
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
