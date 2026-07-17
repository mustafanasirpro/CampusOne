package com.campusone.aura.solver;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import java.time.Duration;

public class AuraConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                roomDoubleBooked(factory),
                instructorDoubleBooked(factory),
                sectionDoubleBooked(factory),
                combinedSectionDoubleBooked(factory),
                studentDoubleBooked(factory),
                hardOfferingConflict(factory),
                instructorUnavailable(factory),
                roomUnavailable(factory),
                sectionUnavailable(factory),
                studentUnavailable(factory),
                instructorAvoid(factory),
                roomAvoid(factory),
                sectionAvoid(factory),
                studentAvoid(factory),
                instructorPreferred(factory),
                roomPreferred(factory),
                sectionPreferred(factory),
                studentPreferred(factory),
                roomTooSmall(factory),
                roomTypeMismatch(factory),
                missingRequiredFacility(factory),
                instructionalContiguousBlock(factory),
                allowedAndProhibitedDays(factory),
                fixedRoomAssignment(factory),
                fixedTimeslotAssignment(factory),
                validPinnedAssignment(factory),
                maximumOccurrencesPerDay(factory),
                minimumDaySeparation(factory),
                lectureBeforeLinkedMeeting(factory),
                impossibleBuildingTravel(factory),
                difficultBuildingTravel(factory),
                instructorWeeklyHardLoad(factory),
                instructorDailyHardLoad(factory),
                sectionDailyHardLoad(factory),
                instructorWeeklyPreferredLoad(factory),
                instructorDailyPreferredLoad(factory),
                sectionDailyPreferredLoad(factory),
                preferredDay(factory),
                preferredTimeWindow(factory),
                sameRoomPreference(factory),
                spreadSectionMeetings(factory)
        };
    }

    private Constraint roomDoubleBooked(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        AuraPlanningLesson.class,
                        Joiners.equal(AuraPlanningLesson::getRoom))
                .filter((left, right) -> overlaps(left, right))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Room double-booked");
    }

    private Constraint instructorDoubleBooked(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        AuraPlanningLesson.class,
                        Joiners.equal(AuraPlanningLesson::getInstructorId))
                .filter((left, right) -> overlaps(left, right))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Instructor double-booked");
    }

    private Constraint sectionDoubleBooked(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        AuraPlanningLesson.class,
                        Joiners.equal(AuraPlanningLesson::getSectionId))
                .filter((left, right) -> overlaps(left, right))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Section double-booked");
    }

    private Constraint combinedSectionDoubleBooked(ConstraintFactory factory) {
        return factory.forEachUniquePair(AuraPlanningLesson.class)
                .filter((left, right) -> !left.getSectionId().equals(
                                right.getSectionId())
                        && left.sharesSectionWith(right)
                        && overlaps(left, right))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Combined section double-booked");
    }

    private Constraint studentDoubleBooked(ConstraintFactory factory) {
        return factory.forEachUniquePair(AuraPlanningLesson.class)
                .filter((left, right) -> left.sharedStudentCount(right) > 0
                        && overlaps(left, right))
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        AuraPlanningLesson::sharedStudentCount)
                .asConstraint("Registered student double-booked");
    }

    private Constraint hardOfferingConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(AuraPlanningLesson.class)
                .filter((left, right) -> left.conflictsWithOffering(right)
                        && overlaps(left, right))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Hard offering conflict");
    }

    private Constraint instructorUnavailable(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(
                        AuraInstructorAvailabilityFact.class,
                        Joiners.equal(
                                AuraPlanningLesson::getInstructorId,
                                AuraInstructorAvailabilityFact::instructorId))
                .filter((lesson, availability) ->
                        "UNAVAILABLE".equals(availability.availability())
                                && lesson.getTimeslot().containsSlot(
                                        availability.timeslotId(),
                                        lesson.getDurationSlots()))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Instructor unavailable");
    }

    private Constraint roomUnavailable(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getRoom() != null
                        && lesson.getTimeslot() != null)
                .join(
                        AuraRoomAvailabilityFact.class,
                        Joiners.equal(
                                lesson -> lesson.getRoom().id(),
                                AuraRoomAvailabilityFact::roomId))
                .filter((lesson, availability) ->
                        "UNAVAILABLE".equals(availability.availability())
                                && lesson.getTimeslot().containsSlot(
                                        availability.timeslotId(),
                                        lesson.getDurationSlots()))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Room unavailable");
    }

    private Constraint sectionUnavailable(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(
                        AuraSectionAvailabilityFact.class,
                        Joiners.equal(
                                AuraPlanningLesson::getSectionId,
                                AuraSectionAvailabilityFact::sectionId))
                .filter((lesson, availability) ->
                        "UNAVAILABLE".equals(availability.availability())
                                && lesson.getTimeslot().containsSlot(
                                        availability.timeslotId(),
                                        lesson.getDurationSlots()))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Section unavailable");
    }

    private Constraint studentUnavailable(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(
                        AuraStudentAvailabilityFact.class,
                        Joiners.equal(
                                AuraPlanningLesson::getOfferingId,
                                AuraStudentAvailabilityFact::offeringId))
                .filter((lesson, availability) ->
                        "UNAVAILABLE".equals(availability.availability())
                                && lesson.getStudentUserIds().contains(
                                        availability.studentUserId())
                                && lesson.getTimeslot().containsSlot(
                                        availability.timeslotId(),
                                        lesson.getDurationSlots()))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Registered student unavailable");
    }

    private Constraint instructorAvoid(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(
                        AuraInstructorAvailabilityFact.class,
                        Joiners.equal(
                                AuraPlanningLesson::getInstructorId,
                                AuraInstructorAvailabilityFact::instructorId))
                .filter((lesson, availability) ->
                        "AVOID".equals(availability.availability())
                                && lesson.getTimeslot().containsSlot(
                                        availability.timeslotId(),
                                        lesson.getDurationSlots()))
                .penalize(HardMediumSoftScore.ONE_MEDIUM)
                .asConstraint("Instructor avoid timeslot");
    }

    private Constraint roomAvoid(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getRoom() != null
                        && lesson.getTimeslot() != null)
                .join(
                        AuraRoomAvailabilityFact.class,
                        Joiners.equal(
                                lesson -> lesson.getRoom().id(),
                                AuraRoomAvailabilityFact::roomId))
                .filter((lesson, availability) ->
                        "AVOID".equals(availability.availability())
                                && lesson.getTimeslot().containsSlot(
                                        availability.timeslotId(),
                                        lesson.getDurationSlots()))
                .penalize(HardMediumSoftScore.ONE_MEDIUM)
                .asConstraint("Room avoid timeslot");
    }

    private Constraint sectionAvoid(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(
                        AuraSectionAvailabilityFact.class,
                        Joiners.equal(
                                AuraPlanningLesson::getSectionId,
                                AuraSectionAvailabilityFact::sectionId))
                .filter((lesson, availability) ->
                        "AVOID".equals(availability.availability())
                                && lesson.getTimeslot().containsSlot(
                                        availability.timeslotId(),
                                        lesson.getDurationSlots()))
                .penalize(HardMediumSoftScore.ONE_MEDIUM)
                .asConstraint("Section avoid timeslot");
    }

    private Constraint studentAvoid(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(
                        AuraStudentAvailabilityFact.class,
                        Joiners.equal(
                                AuraPlanningLesson::getOfferingId,
                                AuraStudentAvailabilityFact::offeringId))
                .filter((lesson, availability) ->
                        "AVOID".equals(availability.availability())
                                && lesson.getStudentUserIds().contains(
                                        availability.studentUserId())
                                && lesson.getTimeslot().containsSlot(
                                        availability.timeslotId(),
                                        lesson.getDurationSlots()))
                .penalize(HardMediumSoftScore.ONE_MEDIUM)
                .asConstraint("Registered student avoid timeslot");
    }

    private Constraint instructorPreferred(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(
                        AuraInstructorAvailabilityFact.class,
                        Joiners.equal(
                                AuraPlanningLesson::getInstructorId,
                                AuraInstructorAvailabilityFact::instructorId))
                .filter((lesson, availability) ->
                        "PREFERRED".equals(availability.availability())
                                && lesson.getTimeslot().containsSlot(
                                        availability.timeslotId(),
                                        lesson.getDurationSlots()))
                .reward(HardMediumSoftScore.ONE_SOFT)
                .asConstraint("Instructor preferred timeslot");
    }

    private Constraint roomPreferred(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getRoom() != null
                        && lesson.getTimeslot() != null)
                .join(
                        AuraRoomAvailabilityFact.class,
                        Joiners.equal(
                                lesson -> lesson.getRoom().id(),
                                AuraRoomAvailabilityFact::roomId))
                .filter((lesson, availability) ->
                        "PREFERRED".equals(availability.availability())
                                && lesson.getTimeslot().containsSlot(
                                        availability.timeslotId(),
                                        lesson.getDurationSlots()))
                .reward(HardMediumSoftScore.ONE_SOFT)
                .asConstraint("Room preferred timeslot");
    }

    private Constraint sectionPreferred(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(
                        AuraSectionAvailabilityFact.class,
                        Joiners.equal(
                                AuraPlanningLesson::getSectionId,
                                AuraSectionAvailabilityFact::sectionId))
                .filter((lesson, availability) ->
                        "PREFERRED".equals(availability.availability())
                                && lesson.getTimeslot().containsSlot(
                                        availability.timeslotId(),
                                        lesson.getDurationSlots()))
                .reward(HardMediumSoftScore.ONE_SOFT)
                .asConstraint("Section preferred timeslot");
    }

    private Constraint studentPreferred(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(
                        AuraStudentAvailabilityFact.class,
                        Joiners.equal(
                                AuraPlanningLesson::getOfferingId,
                                AuraStudentAvailabilityFact::offeringId))
                .filter((lesson, availability) ->
                        "PREFERRED".equals(availability.availability())
                                && lesson.getStudentUserIds().contains(
                                        availability.studentUserId())
                                && lesson.getTimeslot().containsSlot(
                                        availability.timeslotId(),
                                        lesson.getDurationSlots()))
                .reward(HardMediumSoftScore.ONE_SOFT)
                .asConstraint("Registered student preferred timeslot");
    }

    private Constraint roomTooSmall(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getRoom() != null
                        && lesson.getRoom().capacity()
                                < lesson.getRequiredCapacity())
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        lesson -> Math.max(
                                1,
                                lesson.getRequiredCapacity()
                                        - lesson.getRoom().capacity()))
                .asConstraint("Room capacity");
    }

    private Constraint roomTypeMismatch(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getRoom() != null
                        && lesson.getRoomType() != null
                        && !lesson.getRoomType().equals(
                                lesson.getRoom().roomType()))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Room type match");
    }

    private Constraint missingRequiredFacility(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getRoom() != null
                        && !lesson.getRoom().facilities()
                                .containsAll(lesson.getRequiredFacilities()))
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        lesson -> (int) lesson.getRequiredFacilities().stream()
                                .filter(facility -> !lesson.getRoom()
                                        .facilities().contains(facility))
                                .count())
                .asConstraint("Required room facilities");
    }

    private boolean overlaps(
            AuraPlanningLesson left,
            AuraPlanningLesson right) {
        if (left.getTimeslot() == null || right.getTimeslot() == null) {
            return false;
        }
        return left.getTimeslot().dayOfWeek()
                        == right.getTimeslot().dayOfWeek()
                && left.overlapsWeeks(right)
                && left.getTimeslot().startsAt()
                        .isBefore(right.getEffectiveEndTime())
                && right.getTimeslot().startsAt()
                        .isBefore(left.getEffectiveEndTime());
    }

    private Constraint instructionalContiguousBlock(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null
                        && !lesson.getTimeslot().supportsDuration(
                                lesson.getDurationSlots()))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Instructional contiguous block");
    }

    private Constraint allowedAndProhibitedDays(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null
                        && ((!lesson.getAllowedDays().isEmpty()
                                && !lesson.getAllowedDays().contains(
                                        lesson.getTimeslot().dayOfWeek()))
                            || lesson.getProhibitedDays().contains(
                                    lesson.getTimeslot().dayOfWeek())))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Allowed and prohibited days");
    }

    private Constraint fixedRoomAssignment(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getFixedRoomId() != null
                        && (lesson.getRoom() == null
                            || !lesson.getFixedRoomId().equals(
                                    lesson.getRoom().id())))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Fixed room assignment");
    }

    private Constraint fixedTimeslotAssignment(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getFixedTimeslotId() != null
                        && (lesson.getTimeslot() == null
                            || !lesson.getFixedTimeslotId().equals(
                                    lesson.getTimeslot().id())))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Fixed timeslot assignment");
    }

    private Constraint validPinnedAssignment(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.isPinned()
                        && lesson.getFixedRoomId() == null
                        && lesson.getFixedTimeslotId() == null)
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Pinned requirement has an assignment");
    }

    private Constraint maximumOccurrencesPerDay(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .groupBy(
                        AuraPlanningLesson::getRequirementId,
                        lesson -> lesson.getTimeslot().dayOfWeek(),
                        AuraPlanningLesson::getMaximumOccurrencesPerDay,
                        ConstraintCollectors.count())
                .filter((requirement, day, maximum, count) -> count > maximum)
                .penalize(
                        HardMediumSoftScore.ONE_HARD,
                        (requirement, day, maximum, count) -> count - maximum)
                .asConstraint("Maximum occurrences per day");
    }

    private Constraint minimumDaySeparation(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        AuraPlanningLesson.class,
                        Joiners.equal(AuraPlanningLesson::getRequirementId))
                .filter((left, right) -> left.getTimeslot() != null
                        && right.getTimeslot() != null
                        && left.getMinimumDaySeparation() > 0
                        && Math.abs(left.getTimeslot().dayOfWeek()
                                - right.getTimeslot().dayOfWeek())
                                < left.getMinimumDaySeparation())
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Minimum day separation");
    }

    private Constraint lectureBeforeLinkedMeeting(ConstraintFactory factory) {
        return factory.forEachUniquePair(AuraPlanningLesson.class)
                .filter((left, right) -> linkedOrderViolation(left, right)
                        || linkedOrderViolation(right, left))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Lecture before linked meeting");
    }

    private Constraint impossibleBuildingTravel(ConstraintFactory factory) {
        return factory.forEachUniquePair(AuraPlanningLesson.class)
                .filter(this::hasHardTravelViolation)
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Impossible building travel");
    }

    private Constraint difficultBuildingTravel(ConstraintFactory factory) {
        return factory.forEachUniquePair(AuraPlanningLesson.class)
                .filter(this::hasDifficultTravel)
                .penalize(HardMediumSoftScore.ONE_MEDIUM)
                .asConstraint("Difficult building travel");
    }

    private boolean linkedOrderViolation(
            AuraPlanningLesson lecture,
            AuraPlanningLesson linked) {
        if (!lecture.isLectureBeforeLinked()
                || lecture.getLinkedRequirementId() == null
                || !lecture.getLinkedRequirementId().equals(
                        linked.getRequirementId())
                || lecture.getTimeslot() == null
                || linked.getTimeslot() == null
                || !lecture.overlapsWeeks(linked)) {
            return false;
        }
        return lecture.getTimeslot().dayOfWeek()
                        > linked.getTimeslot().dayOfWeek()
                || lecture.getTimeslot().dayOfWeek()
                        == linked.getTimeslot().dayOfWeek()
                && !lecture.getEffectiveEndTime().isBefore(
                        linked.getTimeslot().startsAt())
                && !lecture.getEffectiveEndTime().equals(
                        linked.getTimeslot().startsAt());
    }

    private boolean hasHardTravelViolation(
            AuraPlanningLesson left,
            AuraPlanningLesson right) {
        AuraTravelRule rule = travelRule(left, right);
        if (rule == null) {
            return false;
        }
        return "IMPOSSIBLE".equals(rule.difficulty())
                || travelGapMinutes(left, right) < rule.minutes();
    }

    private boolean hasDifficultTravel(
            AuraPlanningLesson left,
            AuraPlanningLesson right) {
        AuraTravelRule rule = travelRule(left, right);
        if (rule == null || !"DIFFICULT".equals(rule.difficulty())) {
            return false;
        }
        int gap = travelGapMinutes(left, right);
        return gap >= rule.minutes()
                && gap < Math.max(rule.minutes() * 2, 30);
    }

    private AuraTravelRule travelRule(
            AuraPlanningLesson left,
            AuraPlanningLesson right) {
        if (left.getTimeslot() == null
                || right.getTimeslot() == null
                || left.getRoom() == null
                || right.getRoom() == null
                || left.getTimeslot().dayOfWeek()
                        != right.getTimeslot().dayOfWeek()
                || !left.overlapsWeeks(right)
                || overlaps(left, right)
                || !sharesTraveler(left, right)) {
            return null;
        }
        AuraTravelRule rule = left.travelRuleTo(right);
        if (rule == null || AuraTravelRule.key(
                        left.getRoom().building(), right.getRoom().building())
                .equals(AuraTravelRule.key(
                        left.getRoom().building(), left.getRoom().building()))) {
            return null;
        }
        return rule;
    }

    private boolean sharesTraveler(
            AuraPlanningLesson left,
            AuraPlanningLesson right) {
        return left.getInstructorId().equals(right.getInstructorId())
                || left.sharesSectionWith(right)
                || left.sharedStudentCount(right) > 0;
    }

    private int travelGapMinutes(
            AuraPlanningLesson left,
            AuraPlanningLesson right) {
        if (!left.getEffectiveEndTime().isAfter(
                right.getTimeslot().startsAt())) {
            return (int) Duration.between(
                    left.getEffectiveEndTime(),
                    right.getTimeslot().startsAt()).toMinutes();
        }
        return (int) Duration.between(
                right.getEffectiveEndTime(),
                left.getTimeslot().startsAt()).toMinutes();
    }

    private Constraint instructorWeeklyHardLoad(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .groupBy(
                        AuraPlanningLesson::getInstructorId,
                        AuraPlanningLesson::getInstructorHardWeeklyLoad,
                        ConstraintCollectors.sum(
                                AuraPlanningLesson::getDurationSlots))
                .filter((instructor, maximum, load) -> load > maximum)
                .penalize(HardMediumSoftScore.ONE_HARD,
                        (instructor, maximum, load) -> load - maximum)
                .asConstraint("Instructor weekly hard load");
    }

    private Constraint instructorDailyHardLoad(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .groupBy(
                        AuraPlanningLesson::getInstructorId,
                        lesson -> lesson.getTimeslot().dayOfWeek(),
                        AuraPlanningLesson::getInstructorHardDailyLoad,
                        ConstraintCollectors.sum(
                                AuraPlanningLesson::getDurationSlots))
                .filter((instructor, day, maximum, load) -> load > maximum)
                .penalize(HardMediumSoftScore.ONE_HARD,
                        (instructor, day, maximum, load) -> load - maximum)
                .asConstraint("Instructor daily hard load");
    }

    private Constraint sectionDailyHardLoad(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .groupBy(
                        AuraPlanningLesson::getSectionId,
                        lesson -> lesson.getTimeslot().dayOfWeek(),
                        AuraPlanningLesson::getSectionHardDailyLoad,
                        ConstraintCollectors.sum(
                                AuraPlanningLesson::getDurationSlots))
                .filter((section, day, maximum, load) -> load > maximum)
                .penalize(HardMediumSoftScore.ONE_HARD,
                        (section, day, maximum, load) -> load - maximum)
                .asConstraint("Section daily hard load");
    }

    private Constraint instructorWeeklyPreferredLoad(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .groupBy(
                        AuraPlanningLesson::getInstructorId,
                        AuraPlanningLesson::getInstructorPreferredWeeklyLoad,
                        ConstraintCollectors.sum(
                                AuraPlanningLesson::getDurationSlots))
                .filter((instructor, preferred, load) -> load > preferred)
                .penalize(HardMediumSoftScore.ONE_MEDIUM,
                        (instructor, preferred, load) -> load - preferred)
                .asConstraint("Instructor preferred weekly load");
    }

    private Constraint instructorDailyPreferredLoad(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .groupBy(
                        AuraPlanningLesson::getInstructorId,
                        lesson -> lesson.getTimeslot().dayOfWeek(),
                        AuraPlanningLesson::getInstructorPreferredDailyLoad,
                        ConstraintCollectors.sum(
                                AuraPlanningLesson::getDurationSlots))
                .filter((instructor, day, preferred, load) -> load > preferred)
                .penalize(HardMediumSoftScore.ONE_MEDIUM,
                        (instructor, day, preferred, load) -> load - preferred)
                .asConstraint("Instructor preferred daily load");
    }

    private Constraint sectionDailyPreferredLoad(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .groupBy(
                        AuraPlanningLesson::getSectionId,
                        lesson -> lesson.getTimeslot().dayOfWeek(),
                        AuraPlanningLesson::getSectionPreferredDailyLoad,
                        ConstraintCollectors.sum(
                                AuraPlanningLesson::getDurationSlots))
                .filter((section, day, preferred, load) -> load > preferred)
                .penalize(HardMediumSoftScore.ONE_MEDIUM,
                        (section, day, preferred, load) -> load - preferred)
                .asConstraint("Section preferred daily load");
    }

    private Constraint preferredDay(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null
                        && !lesson.getPreferredDays().isEmpty()
                        && lesson.getPreferredDays().contains(
                                lesson.getTimeslot().dayOfWeek()))
                .reward(HardMediumSoftScore.ONE_SOFT)
                .asConstraint("Preferred day");
    }

    private Constraint preferredTimeWindow(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null
                        && lesson.getPreferredStartTime() != null
                        && lesson.getPreferredEndTime() != null
                        && !lesson.getTimeslot().startsAt().isBefore(
                                lesson.getPreferredStartTime())
                        && !lesson.getEffectiveEndTime().isAfter(
                                lesson.getPreferredEndTime()))
                .reward(HardMediumSoftScore.ONE_SOFT)
                .asConstraint("Preferred time window");
    }

    private Constraint sameRoomPreference(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        AuraPlanningLesson.class,
                        Joiners.equal(AuraPlanningLesson::getRequirementId))
                .filter((left, right) -> left.isSameRoomPreferred()
                        && left.getRoom() != null
                        && right.getRoom() != null
                        && !left.getRoom().id().equals(right.getRoom().id()))
                .penalize(HardMediumSoftScore.ONE_SOFT)
                .asConstraint("Same room preference");
    }

    private Constraint spreadSectionMeetings(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        AuraPlanningLesson.class,
                        Joiners.equal(AuraPlanningLesson::getSectionId))
                .filter((left, right) -> left.getTimeslot() != null
                        && right.getTimeslot() != null
                        && left.overlapsWeeks(right)
                        && left.getTimeslot().dayOfWeek()
                                == right.getTimeslot().dayOfWeek())
                .penalize(HardMediumSoftScore.ONE_SOFT)
                .asConstraint("Spread section meetings");
    }
}
