package com.campusone.note.storage;

import com.campusone.common.exception.FileUploadTooLargeException;
import com.campusone.common.exception.InvalidFileUploadException;
import com.campusone.common.exception.StorageOperationException;
import com.campusone.note.entity.NoteFileType;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class NoteFileValidator {

    private static final String PDF_MIME_TYPE = "application/pdf";
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};

    private final StorageProperties properties;

    public NoteFileValidator(StorageProperties properties) {
        this.properties = properties;
    }

    public ValidatedNoteFile validate(
            MultipartFile multipartFile,
            NoteFileType requestedFileType) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new InvalidFileUploadException("Select a PDF file to upload.");
        }
        if (requestedFileType != NoteFileType.PDF) {
            throw new InvalidFileUploadException(
                    "Uploaded study resources must use the PDF file type.");
        }

        int maximumSizeMb = properties.getMaxUploadSizeMb();
        long maximumSizeBytes = maximumSizeMb * 1024L * 1024L;
        if (multipartFile.getSize() > maximumSizeBytes) {
            throw new FileUploadTooLargeException(maximumSizeMb);
        }

        String originalFilename = safeOriginalFilename(
                multipartFile.getOriginalFilename());
        if (!originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new InvalidFileUploadException("Only PDF files are supported.");
        }
        if (!PDF_MIME_TYPE.equalsIgnoreCase(multipartFile.getContentType())) {
            throw new InvalidFileUploadException(
                    "The selected file must have the application/pdf MIME type.");
        }

        byte[] content;
        try {
            content = multipartFile.getBytes();
        } catch (IOException exception) {
            throw new StorageOperationException(
                    "The uploaded PDF could not be read.",
                    exception);
        }
        if (content.length == 0) {
            throw new InvalidFileUploadException("The selected PDF is empty.");
        }
        if (content.length > maximumSizeBytes) {
            throw new FileUploadTooLargeException(maximumSizeMb);
        }
        if (!hasPdfSignature(content)) {
            throw new InvalidFileUploadException(
                    "The selected file does not contain a valid PDF signature.");
        }

        return new ValidatedNoteFile(
                originalFilename,
                PDF_MIME_TYPE,
                content,
                sha256(content));
    }

    private String safeOriginalFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidFileUploadException(
                    "The uploaded PDF must have a filename.");
        }
        String normalized = filename.replace('\\', '/');
        String basename = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (basename.isBlank()
                || basename.length() > 255
                || basename.chars().anyMatch(Character::isISOControl)) {
            throw new InvalidFileUploadException(
                    "The uploaded PDF filename is invalid.");
        }
        return basename;
    }

    private boolean hasPdfSignature(byte[] content) {
        if (content.length < PDF_SIGNATURE.length) {
            return false;
        }
        for (int index = 0; index < PDF_SIGNATURE.length; index++) {
            if (content[index] != PDF_SIGNATURE[index]) {
                return false;
            }
        }
        return true;
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
