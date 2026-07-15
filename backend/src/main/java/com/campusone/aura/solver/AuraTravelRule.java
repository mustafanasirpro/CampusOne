package com.campusone.aura.solver;

import java.util.Locale;

public record AuraTravelRule(
        String fromBuilding,
        String toBuilding,
        int minutes,
        String difficulty) {

    public String key() {
        return key(fromBuilding, toBuilding);
    }

    public static String key(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return normalizedLeft.compareTo(normalizedRight) <= 0
                ? normalizedLeft + "\u0000" + normalizedRight
                : normalizedRight + "\u0000" + normalizedLeft;
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim().replaceAll("\\s+", " ")
                        .toLowerCase(Locale.ROOT);
    }
}
