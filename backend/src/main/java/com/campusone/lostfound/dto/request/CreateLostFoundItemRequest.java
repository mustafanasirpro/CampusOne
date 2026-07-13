package com.campusone.lostfound.dto.request;

import com.campusone.lostfound.entity.LostFoundCategory;
import com.campusone.lostfound.entity.LostFoundItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateLostFoundItemRequest(
        @NotNull LostFoundItemType type,
        @NotNull LostFoundCategory category,
        @NotBlank @Size(min = 5, max = 160) String title,
        @NotBlank @Size(min = 10, max = 2000) String description,
        @NotBlank @Size(min = 2, max = 255) String locationText,
        @NotNull @PastOrPresent LocalDate itemDate,
        @Size(max = 80) String brand,
        @Size(max = 60) String color) {
}
