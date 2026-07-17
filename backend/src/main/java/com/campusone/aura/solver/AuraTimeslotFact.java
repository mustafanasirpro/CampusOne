package com.campusone.aura.solver;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record AuraTimeslotFact(
        UUID id,
        int dayOfWeek,
        LocalTime startsAt,
        LocalTime endsAt,
        int slotOrder,
        String slotType,
        List<UUID> contiguousSlotIds,
        List<LocalTime> contiguousEnds) {

    public AuraTimeslotFact {
        contiguousSlotIds = contiguousSlotIds == null
                ? List.of(id)
                : List.copyOf(contiguousSlotIds);
        contiguousEnds = contiguousEnds == null
                ? List.of(endsAt)
                : List.copyOf(contiguousEnds);
    }

    public AuraTimeslotFact(
            UUID id,
            int dayOfWeek,
            LocalTime startsAt,
            LocalTime endsAt) {
        this(
                id,
                dayOfWeek,
                startsAt,
                endsAt,
                100,
                "INSTRUCTIONAL",
                List.of(id),
                List.of(endsAt));
    }

    public boolean supportsDuration(int durationSlots) {
        return "INSTRUCTIONAL".equals(slotType)
                && durationSlots > 0
                && contiguousSlotIds.size() >= durationSlots;
    }

    public LocalTime effectiveEnd(int durationSlots) {
        if (!supportsDuration(durationSlots)) {
            return endsAt;
        }
        return contiguousEnds.get(durationSlots - 1);
    }

    public boolean containsSlot(UUID timeslotId, int durationSlots) {
        int limit = Math.min(Math.max(durationSlots, 1), contiguousSlotIds.size());
        return contiguousSlotIds.subList(0, limit).contains(timeslotId);
    }
}
