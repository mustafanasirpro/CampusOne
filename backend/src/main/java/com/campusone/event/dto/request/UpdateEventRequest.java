package com.campusone.event.dto.request;

import com.campusone.event.entity.EventStatus;
import com.campusone.event.entity.EventVisibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateEventRequest(
        @Size(min = 5, max = 160)
        String title,

        @Size(min = 10, max = 5000)
        String description,

        @Size(min = 2, max = 255)
        String location,

        Instant startTime,

        Instant endTime,

        @Min(1)
        Integer capacity,

        EventVisibility visibility,

        EventStatus status) {

    public UpdateEventRequest {
        title = trim(title);
        description = trim(description);
        location = trim(location);
    }

    @JsonIgnore
    @AssertTrue(message = "endTime must be after startTime")
    public boolean isSuppliedTimeRangeValid() {
        return startTime == null
                || endTime == null
                || endTime.isAfter(startTime);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
