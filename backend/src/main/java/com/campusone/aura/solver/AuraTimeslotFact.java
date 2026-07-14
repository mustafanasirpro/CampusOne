package com.campusone.aura.solver;

import java.time.LocalTime;
import java.util.UUID;

public record AuraTimeslotFact(
        UUID id,
        int dayOfWeek,
        LocalTime startsAt,
        LocalTime endsAt) {
}
