package com.campusone.aura.solver;

import java.util.UUID;

public record AuraInstructorAvailabilityFact(
        UUID instructorId,
        UUID timeslotId,
        String availability) {
}
