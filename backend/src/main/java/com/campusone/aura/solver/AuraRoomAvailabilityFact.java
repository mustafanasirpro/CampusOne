package com.campusone.aura.solver;

import java.util.UUID;

public record AuraRoomAvailabilityFact(
        UUID roomId,
        UUID timeslotId,
        String availability) {
}
