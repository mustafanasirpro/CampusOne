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
