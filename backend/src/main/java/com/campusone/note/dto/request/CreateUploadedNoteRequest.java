package com.campusone.note.dto.request;

import com.campusone.note.entity.NoteFileType;
import com.campusone.note.entity.NoteVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateUploadedNoteRequest(
        @NotNull
        UUID courseId,

        @NotBlank
        @Size(min = 5, max = 160)
        String title,

        @NotBlank
        @Size(min = 10, max = 2000)
        String description,

        @NotBlank
        @Size(min = 2, max = 120)
        String teacherName,

        @NotNull
        @Min(1)
        @Max(8)
        Integer semester,

        @NotNull
        NoteFileType fileType,

        NoteVisibility visibility,

        @Size(max = 10)
        List<@NotBlank @Size(min = 2, max = 40) String> tags) {

    public CreateUploadedNoteRequest {
        title = trim(title);
        description = trim(description);
        teacherName = trim(teacherName);
        tags = tags == null
                ? List.of()
                : tags.stream().map(CreateUploadedNoteRequest::trim).toList();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
