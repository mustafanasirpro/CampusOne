package com.campusone.note.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.note.dto.request.CreateNoteRequest;
import com.campusone.note.dto.request.FileMetadataRequest;
import com.campusone.note.dto.request.RateNoteRequest;
import com.campusone.note.entity.NoteFileType;
import com.campusone.note.entity.NoteVisibility;
import com.campusone.note.entity.StorageProvider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NoteRequestValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void createNote_invalidMetadata_reportsAllRequiredBoundaries() {
        List<String> tags = IntStream.rangeClosed(1, 11)
                .mapToObj(number -> "Tag " + number)
                .toList();
        CreateNoteRequest request = new CreateNoteRequest(
                UUID.randomUUID(),
                "Tiny",
                "Too short",
                "T".repeat(121),
                9,
                NoteFileType.PDF,
                NoteVisibility.PUBLIC,
                tags,
                new FileMetadataRequest(
                        StorageProvider.MINIO,
                        "notes",
                        "notes/test.pdf",
                        "test.pdf",
                        "application/pdf",
                        0L,
                        null,
                        null));

        Set<ConstraintViolation<CreateNoteRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(
                        "title",
                        "description",
                        "teacherName",
                        "semester",
                        "tags",
                        "file.sizeBytes");
    }

    @Test
    void createNote_shortTag_isRejected() {
        CreateNoteRequest request = new CreateNoteRequest(
                UUID.randomUUID(),
                "Complete OOP Notes",
                "Detailed object oriented programming lecture notes.",
                "Dr. Ahmed Khan",
                4,
                NoteFileType.PDF,
                NoteVisibility.PUBLIC,
                List.of("A"),
                validFile());

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("tags[0].<list element>");
    }

    @Test
    void rateNote_ratingOutsideOneToFive_isRejected() {
        assertThat(validator.validate(new RateNoteRequest(0)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("rating");
        assertThat(validator.validate(new RateNoteRequest(6)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("rating");
    }

    private FileMetadataRequest validFile() {
        return new FileMetadataRequest(
                StorageProvider.MINIO,
                "campusone-notes",
                "notes/oop.pdf",
                "oop-notes.pdf",
                "application/pdf",
                4096L,
                "a".repeat(64),
                null);
    }
}
