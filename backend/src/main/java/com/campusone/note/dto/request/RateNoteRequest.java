package com.campusone.note.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RateNoteRequest(
        @NotNull
        @Min(1)
        @Max(5)
        Integer rating) {
}
