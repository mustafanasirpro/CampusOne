package com.campusone.note.dto.request;

import com.campusone.note.entity.NoteFileType;
import com.campusone.note.entity.NoteVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateNoteRequest(
        UUID courseId,

        @Size(min = 5, max = 160)
        String title,

        @Size(min = 10, max = 2000)
        String description,

        @Size(min = 2, max = 120)
        String teacherName,

        @Min(1)
        @Max(8)
        Integer semester,

        NoteFileType fileType,

        NoteVisibility visibility,

        @Size(max = 10)
        List<@NotBlank @Size(min = 2, max = 40) String> tags) {

    public UpdateNoteRequest {
        title = trim(title);
        description = trim(description);
        teacherName = trim(teacherName);
        if (tags != null) {
            tags = tags.stream().map(UpdateNoteRequest::trim).toList();
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
