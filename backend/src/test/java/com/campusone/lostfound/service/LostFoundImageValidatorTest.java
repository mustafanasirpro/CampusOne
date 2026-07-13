package com.campusone.lostfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusone.common.exception.InvalidFileUploadException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class LostFoundImageValidatorTest {

    private final LostFoundImageValidator validator =
            new LostFoundImageValidator(5);

    @Test
    void validate_acceptsRealPngBytes() {
        byte[] png = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00
        };

        var files = validator.validate(List.of(new MockMultipartFile(
                "images",
                "item.png",
                "image/png",
                png)));

        assertThat(files).hasSize(1);
        assertThat(files.getFirst().mimeType()).isEqualTo("image/png");
    }

    @Test
    void validate_rejectsDisguisedImageBytes() {
        byte[] disguisedPdf = "%PDF-not-an-image".getBytes(
                java.nio.charset.StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validator.validate(List.of(
                new MockMultipartFile(
                        "images",
                        "item.png",
                        "image/png",
                        disguisedPdf))))
                .isInstanceOf(InvalidFileUploadException.class)
                .hasMessage("Only JPG, PNG, or WebP images are allowed.");
    }

    @Test
    void validate_rejectsMimeExtensionMismatch() {
        byte[] png = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
        };

        assertThatThrownBy(() -> validator.validate(List.of(
                new MockMultipartFile(
                        "images",
                        "item.jpg",
                        "image/png",
                        png))))
                .isInstanceOf(InvalidFileUploadException.class)
                .hasMessage("Only JPG, PNG, or WebP images are allowed.");
    }
}
