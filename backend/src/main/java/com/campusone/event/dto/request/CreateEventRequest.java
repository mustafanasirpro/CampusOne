package com.campusone.event.dto.request;

import com.campusone.event.entity.EventVisibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateEventRequest(
        @NotBlank
        @Size(min = 5, max = 160)
        String title,

        @NotBlank
        @Size(min = 10, max = 5000)
        String description,

        @NotBlank
        @Size(min = 2, max = 255)
        String location,

        @NotNull
        Instant startTime,

        @NotNull
        Instant endTime,

        @Min(1)
        int capacity,

        @NotNull
        EventVisibility visibility) {

    public CreateEventRequest {
        title = trim(title);
        description = trim(description);
        location = trim(location);
    }

    @JsonIgnore
    @AssertTrue(message = "endTime must be after startTime")
    public boolean isTimeRangeValid() {
        return startTime == null
                || endTime == null
                || endTime.isAfter(startTime);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
