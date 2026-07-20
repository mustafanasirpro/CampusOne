package com.campusone.aura.solver;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Central allowlist and preset weights for every AURA solver constraint. */
public final class AuraConstraintCatalog {

    public static final String FAST_FEASIBLE = "FAST_FEASIBLE";
    public static final String BALANCED = "BALANCED";
    public static final String COMPACT = "COMPACT";
    public static final String ROOM_EFFICIENT = "ROOM_EFFICIENT";
    public static final String INSTRUCTOR_FRIENDLY = "INSTRUCTOR_FRIENDLY";
    public static final String QUALITY = "QUALITY";
    public static final String REPAIR = "REPAIR";
    public static final String WHAT_IF = "WHAT_IF";

    public static final Set<String> PROFILES = Set.of(
            FAST_FEASIBLE, BALANCED, COMPACT, ROOM_EFFICIENT,
            INSTRUCTOR_FRIENDLY, QUALITY, REPAIR, WHAT_IF);

    private static final Map<String, Level> CONSTRAINTS = buildConstraints();

    private AuraConstraintCatalog() {
    }

    public static String normalizeProfile(String value) {
        String normalized = value == null || value.isBlank()
                ? BALANCED
                : value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (!PROFILES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported AURA constraint profile.");
        }
        return normalized;
    }

    public static Map<String, Level> constraints() {
        return CONSTRAINTS;
    }

    public static Map<String, HardMediumSoftScore> weights(
            String profile,
            Map<String, ConstraintWeight> configured) {
        String normalized = normalizeProfile(profile);
        Map<String, HardMediumSoftScore> result = new LinkedHashMap<>();
        CONSTRAINTS.forEach((name, level) -> {
            ConstraintWeight override = configured.get(name);
            if (override != null && override.level() != level) {
                throw new IllegalArgumentException(
                        "Constraint level does not match the AURA constraint catalog: " + name);
            }
            long weight = override == null
                    ? presetWeight(normalized, name, level)
                    : override.weight();
            result.put(name, score(level, weight));
        });
        if (!CONSTRAINTS.keySet().containsAll(configured.keySet())) {
            throw new IllegalArgumentException("Unknown AURA constraint name.");
        }
        return Map.copyOf(result);
    }

    private static long presetWeight(String profile, String name, Level level) {
        if (level == Level.HARD) return 1;
        return switch (profile) {
            case FAST_FEASIBLE -> 0;
            case COMPACT -> switch (name) {
                case "Same room preference", "Preferred time window" -> 4;
                case "Spread section meetings" -> 0;
                default -> 1;
            };
            case ROOM_EFFICIENT -> switch (name) {
                case "Same room preference" -> 8;
                case "Room preferred timeslot", "Difficult building travel" -> 3;
                default -> 1;
            };
            case INSTRUCTOR_FRIENDLY -> switch (name) {
                case "Instructor preferred timeslot", "Instructor avoid timeslot" -> 8;
                case "Difficult building travel" -> 3;
                default -> 1;
            };
            case QUALITY -> 3;
            case REPAIR -> name.equals("Same room preference")
                    || name.equals("Spread section meetings") ? 8 : 2;
            case WHAT_IF -> 1;
            default -> 1;
        };
    }

    private static HardMediumSoftScore score(Level level, long weight) {
        int safeWeight = Math.toIntExact(Math.min(1_000_000L, Math.max(0, weight)));
        return switch (level) {
            case HARD -> HardMediumSoftScore.ofHard(safeWeight);
            case MEDIUM -> HardMediumSoftScore.ofMedium(safeWeight);
            case SOFT -> HardMediumSoftScore.ofSoft(safeWeight);
        };
    }

    private static Map<String, Level> buildConstraints() {
        Map<String, Level> values = new LinkedHashMap<>();
        add(values, Level.HARD,
                "Room double-booked",
                "Instructor double-booked",
                "Section double-booked",
                "Combined section double-booked",
                "Registered student double-booked",
                "Hard offering conflict",
                "Instructor unavailable",
                "Room unavailable",
                "Section unavailable",
                "Registered student unavailable",
                "Room capacity",
                "Room type match",
                "Required room facilities",
                "Instructional contiguous block",
                "Allowed and prohibited days",
                "Fixed room assignment",
                "Fixed timeslot assignment",
                "Pinned requirement has an assignment",
                "Maximum occurrences per day",
                "Minimum day separation",
                "Lecture before linked meeting",
                "Impossible building travel",
                "Instructor weekly hard load",
                "Instructor daily hard load",
                "Section daily hard load");
        add(values, Level.MEDIUM,
                "Instructor avoid timeslot",
                "Room avoid timeslot",
                "Section avoid timeslot",
                "Registered student avoid timeslot",
                "Difficult building travel",
                "Instructor preferred weekly load",
                "Instructor preferred daily load",
                "Section preferred daily load");
        add(values, Level.SOFT,
                "Instructor preferred timeslot",
                "Room preferred timeslot",
                "Section preferred timeslot",
                "Registered student preferred timeslot",
                "Preferred day",
                "Preferred time window",
                "Same room preference",
                "Spread section meetings");
        return Map.copyOf(values);
    }

    private static void add(
            Map<String, Level> target,
            Level level,
            String... names) {
        for (String name : names) target.put(name, level);
    }

    public enum Level {
        HARD,
        MEDIUM,
        SOFT
    }

    public record ConstraintWeight(Level level, long weight) {
    }
}
