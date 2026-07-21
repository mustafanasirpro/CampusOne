package com.campusone.aura.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import com.campusone.aura.solver.AuraConstraintCatalog.ConstraintWeight;
import com.campusone.aura.solver.AuraConstraintCatalog.Level;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuraConstraintCatalogTest {

    @Test
    void balancedProfileContainsEveryConstraintWithItsDeclaredScoreLevel() {
        Map<String, HardMediumSoftScore> weights =
                AuraConstraintCatalog.weights(" balanced ", Map.of());

        assertThat(weights).hasSameSizeAs(AuraConstraintCatalog.constraints());
        assertThat(weights.get("Room double-booked"))
                .isEqualTo(HardMediumSoftScore.ONE_HARD);
        assertThat(weights.get("Instructor avoid timeslot"))
                .isEqualTo(HardMediumSoftScore.ONE_MEDIUM);
        assertThat(weights.get("Preferred day"))
                .isEqualTo(HardMediumSoftScore.ONE_SOFT);
    }

    @Test
    void configuredWeightOverridesOnlyTheNamedConstraint() {
        Map<String, HardMediumSoftScore> weights = AuraConstraintCatalog.weights(
                AuraConstraintCatalog.QUALITY,
                Map.of("Preferred day", new ConstraintWeight(Level.SOFT, 17)));

        assertThat(weights.get("Preferred day"))
                .isEqualTo(HardMediumSoftScore.ofSoft(17));
        assertThat(weights.get("Room double-booked"))
                .isEqualTo(HardMediumSoftScore.ONE_HARD);
    }

    @Test
    void userOrientedProfilesApplyDistinctSchedulingPriorities() {
        assertThat(AuraConstraintCatalog.weights(
                AuraConstraintCatalog.COMPACT, Map.of()).get("Spread section meetings"))
                .isEqualTo(HardMediumSoftScore.ZERO);
        assertThat(AuraConstraintCatalog.weights(
                AuraConstraintCatalog.ROOM_EFFICIENT, Map.of()).get("Same room preference"))
                .isEqualTo(HardMediumSoftScore.ofSoft(8));
        assertThat(AuraConstraintCatalog.weights(
                AuraConstraintCatalog.INSTRUCTOR_FRIENDLY, Map.of())
                .get("Instructor preferred timeslot"))
                .isEqualTo(HardMediumSoftScore.ofSoft(8));
    }

    @Test
    void unknownProfileConstraintAndWrongLevelAreRejected() {
        assertThatThrownBy(() -> AuraConstraintCatalog.weights("unknown", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuraConstraintCatalog.weights(
                AuraConstraintCatalog.BALANCED,
                Map.of("Not a constraint", new ConstraintWeight(Level.SOFT, 1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuraConstraintCatalog.weights(
                AuraConstraintCatalog.BALANCED,
                Map.of("Room double-booked", new ConstraintWeight(Level.SOFT, 1))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
