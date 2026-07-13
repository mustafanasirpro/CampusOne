package com.campusone.lostfound.dto.request;

import com.campusone.lostfound.entity.LostFoundCategory;
import com.campusone.lostfound.entity.LostFoundItemType;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateLostFoundItemRequest(
        LostFoundItemType type,
        LostFoundCategory category,
        @Size(min = 5, max = 160) String title,
        @Size(min = 10, max = 2000) String description,
        @Size(min = 2, max = 255) String locationText,
        @PastOrPresent LocalDate itemDate,
        @Size(max = 80) String brand,
        @Size(max = 60) String color) {
}
