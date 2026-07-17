package com.campusone.aura.solver;

import java.util.Set;
import java.util.UUID;

public record AuraRoomFact(
        UUID id,
        int capacity,
        String roomType,
        Set<String> facilities,
        String building) {

    public AuraRoomFact(
            UUID id,
            int capacity,
            String roomType,
            Set<String> facilities) {
        this(id, capacity, roomType, facilities, null);
    }
}
