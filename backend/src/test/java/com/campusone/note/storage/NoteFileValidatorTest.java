package com.campusone.note.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusone.common.exception.FileUploadTooLargeException;
import com.campusone.common.exception.InvalidFileUploadException;
import com.campusone.note.entity.NoteFileType;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class NoteFileValidatorTest {

    private StorageProperties properties;
    private NoteFileValidator validator;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        validator = new NoteFileValidator(properties);
    }

    @Test
    void validate_realPdf_returnsServerDerivedMetadata() {
        byte[] content = "%PDF-1.7\nCampusOne".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "past-paper.pdf",
                "application/pdf",
                content);

        ValidatedNoteFile result = validator.validate(file, NoteFileType.PDF);

        assertThat(result.originalFilename()).isEqualTo("past-paper.pdf");
        assertThat(result.mimeType()).isEqualTo("application/pdf");
        assertThat(result.content()).isEqualTo(content);
        assertThat(result.checksumSha256()).matches("^[0-9a-f]{64}$");
    }

    @Test
    void validate_emptyPdf_isRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "past-paper.pdf",
                "application/pdf",
                new byte[0]);

        assertThatThrownBy(() -> validator.validate(file, NoteFileType.PDF))
                .isInstanceOf(InvalidFileUploadException.class)
                .hasMessage("Select a PDF file to upload.");
    }

    @Test
    void validate_spoofedPdf_rejectsMissingPdfSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "past-paper.pdf",
                "application/pdf",
                "not a pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validator.validate(file, NoteFileType.PDF))
                .isInstanceOf(InvalidFileUploadException.class)
                .hasMessage("Only PDF files are allowed.");
    }

    @Test
    void validate_nonPdfMimeType_isRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "past-paper.pdf",
                "text/plain",
                "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> validator.validate(file, NoteFileType.PDF))
                .isInstanceOf(InvalidFileUploadException.class)
                .hasMessage("Only PDF files are allowed.");
    }

    @Test
    void validate_nonPdfExtension_isRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "past-paper.exe",
                "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> validator.validate(file, NoteFileType.PDF))
                .isInstanceOf(InvalidFileUploadException.class)
                .hasMessage("Only PDF files are allowed.");
    }

    @Test
    void validate_pathTraversalFilename_isRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../past-paper.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> validator.validate(file, NoteFileType.PDF))
                .isInstanceOf(InvalidFileUploadException.class)
                .hasMessage("The uploaded PDF filename is invalid.");
    }

    @Test
    void validate_fileOverConfiguredLimit_isRejected() {
        properties.setMaxUploadSizeMb(1);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "past-paper.pdf",
                "application/pdf",
                new byte[(1024 * 1024) + 1]);

        assertThatThrownBy(() -> validator.validate(file, NoteFileType.PDF))
                .isInstanceOf(FileUploadTooLargeException.class)
                .hasMessage("File size must be 1 MB or less.");
    }
}
