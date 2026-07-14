package com.campusone.aura.solver;

import java.util.UUID;

public record AuraSectionAvailabilityFact(
        UUID sectionId,
        UUID timeslotId,
        String availability) {
}
