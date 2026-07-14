package com.campusone.aura.solver;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

public class AuraConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                roomDoubleBooked(factory),
                instructorDoubleBooked(factory),
                sectionDoubleBooked(factory),
                instructorUnavailable(factory),
                roomUnavailable(factory),
                instructorAvoid(factory),
                roomAvoid(factory),
                instructorPreferred(factory),
                roomPreferred(factory),
                roomTooSmall(factory),
                roomTypeMismatch(factory),
                spreadSectionMeetings(factory)
        };
    }

    private Constraint roomDoubleBooked(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        AuraPlanningLesson.class,
                        Joiners.equal(AuraPlanningLesson::getRoom),
                        Joiners.equal(AuraPlanningLesson::getTimeslot))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Room double-booked");
    }

    private Constraint instructorDoubleBooked(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        AuraPlanningLesson.class,
                        Joiners.equal(AuraPlanningLesson::getInstructorId),
                        Joiners.equal(AuraPlanningLesson::getTimeslot))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Instructor double-booked");
    }

    private Constraint sectionDoubleBooked(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        AuraPlanningLesson.class,
                        Joiners.equal(AuraPlanningLesson::getSectionId),
                        Joiners.equal(AuraPlanningLesson::getTimeslot))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Section double-booked");
    }

    private Constraint instructorUnavailable(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(
                        AuraInstructorAvailabilityFact.class,
                        Joiners.equal(
                                AuraPlanningLesson::getInstructorId,
                                AuraInstructorAvailabilityFact::instructorId),
                        Joiners.equal(
                                lesson -> lesson.getTimeslot().id(),
                                AuraInstructorAvailabilityFact::timeslotId))
                .filter((lesson, availability) ->
                        "UNAVAILABLE".equals(availability.availability()))
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
                                AuraRoomAvailabilityFact::roomId),
                        Joiners.equal(
                                lesson -> lesson.getTimeslot().id(),
                                AuraRoomAvailabilityFact::timeslotId))
                .filter((lesson, availability) ->
                        "UNAVAILABLE".equals(availability.availability()))
                .penalize(HardMediumSoftScore.ONE_HARD)
                .asConstraint("Room unavailable");
    }

    private Constraint instructorAvoid(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(
                        AuraInstructorAvailabilityFact.class,
                        Joiners.equal(
                                AuraPlanningLesson::getInstructorId,
                                AuraInstructorAvailabilityFact::instructorId),
                        Joiners.equal(
                                lesson -> lesson.getTimeslot().id(),
                                AuraInstructorAvailabilityFact::timeslotId))
                .filter((lesson, availability) ->
                        "AVOID".equals(availability.availability()))
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
                                AuraRoomAvailabilityFact::roomId),
                        Joiners.equal(
                                lesson -> lesson.getTimeslot().id(),
                                AuraRoomAvailabilityFact::timeslotId))
                .filter((lesson, availability) ->
                        "AVOID".equals(availability.availability()))
                .penalize(HardMediumSoftScore.ONE_MEDIUM)
                .asConstraint("Room avoid timeslot");
    }

    private Constraint instructorPreferred(ConstraintFactory factory) {
        return factory.forEach(AuraPlanningLesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .join(
                        AuraInstructorAvailabilityFact.class,
                        Joiners.equal(
                                AuraPlanningLesson::getInstructorId,
                                AuraInstructorAvailabilityFact::instructorId),
                        Joiners.equal(
                                lesson -> lesson.getTimeslot().id(),
                                AuraInstructorAvailabilityFact::timeslotId))
                .filter((lesson, availability) ->
                        "PREFERRED".equals(availability.availability()))
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
                                AuraRoomAvailabilityFact::roomId),
                        Joiners.equal(
                                lesson -> lesson.getTimeslot().id(),
                                AuraRoomAvailabilityFact::timeslotId))
                .filter((lesson, availability) ->
                        "PREFERRED".equals(availability.availability()))
                .reward(HardMediumSoftScore.ONE_SOFT)
                .asConstraint("Room preferred timeslot");
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
                .penalize(HardMediumSoftScore.ONE_MEDIUM)
                .asConstraint("Room type match");
    }

    private Constraint spreadSectionMeetings(ConstraintFactory factory) {
        return factory.forEachUniquePair(
                        AuraPlanningLesson.class,
                        Joiners.equal(AuraPlanningLesson::getSectionId))
                .filter((left, right) -> left.getTimeslot() != null
                        && right.getTimeslot() != null
                        && left.getTimeslot().dayOfWeek()
                                == right.getTimeslot().dayOfWeek())
                .penalize(HardMediumSoftScore.ONE_SOFT)
                .asConstraint("Spread section meetings");
    }
}
