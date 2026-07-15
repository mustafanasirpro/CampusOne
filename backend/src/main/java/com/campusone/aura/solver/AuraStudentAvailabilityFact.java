package com.campusone.aura.solver;

import java.util.UUID;

public record AuraStudentAvailabilityFact(
        UUID offeringId,
        UUID studentUserId,
        UUID timeslotId,
        String availability) {
}
