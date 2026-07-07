package com.campusone.note.dto.request;

import com.campusone.note.entity.NoteFileType;
import com.campusone.note.entity.NoteVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateNoteRequest(
        UUID courseId,

        @Size(min = 2, max = 30, message = "Use a normal course code like CSC275.")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9._ -]*$",
                message = "Use a normal course code like CSC275.")
        String courseCode,

        @Size(min = 2, max = 160)
        String courseName,

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
        List<@NotBlank @Size(min = 2, max = 40) String> tags,

        @NotNull
        @Valid
        FileMetadataRequest file) {

    public CreateNoteRequest {
        courseCode = trim(courseCode);
        courseName = trim(courseName);
        title = trim(title);
        description = trim(description);
        teacherName = trim(teacherName);
        tags = tags == null
                ? List.of()
                : tags.stream().map(CreateNoteRequest::trim).toList();
    }

    public CreateNoteRequest(
            UUID courseId,
            String title,
            String description,
            String teacherName,
            Integer semester,
            NoteFileType fileType,
            NoteVisibility visibility,
            List<String> tags,
            FileMetadataRequest file) {
        this(
                courseId,
                null,
                null,
                title,
                description,
                teacherName,
                semester,
                fileType,
                visibility,
                tags,
                file);
    }

    @AssertTrue(message = "Course code is required.")
    public boolean hasCourseReference() {
        return courseId != null || (courseCode != null && !courseCode.isBlank());
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
