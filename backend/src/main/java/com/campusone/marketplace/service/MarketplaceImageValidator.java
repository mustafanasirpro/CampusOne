package com.campusone.marketplace.service;

import com.campusone.common.exception.InvalidFileUploadException;
import com.campusone.common.exception.StorageOperationException;
import com.campusone.note.storage.StorageProperties;
import com.campusone.note.storage.ValidatedNoteFile;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MarketplaceImageValidator {

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private final StorageProperties properties;

    public MarketplaceImageValidator(StorageProperties properties) {
        this.properties = properties;
    }

    public List<ValidatedNoteFile> validate(List<MultipartFile> multipartFiles) {
        if (multipartFiles == null || multipartFiles.isEmpty()) {
            return List.of();
        }
        int maxImages = properties.getMarketplaceMaxImagesPerListing();
        if (multipartFiles.size() > maxImages) {
            throw new InvalidFileUploadException(
                    "You can upload up to " + maxImages + " images.");
        }

        List<ValidatedNoteFile> validatedFiles = new ArrayList<>();
        for (MultipartFile multipartFile : multipartFiles) {
            validatedFiles.add(validateOne(multipartFile));
        }
        return List.copyOf(validatedFiles);
    }

    private ValidatedNoteFile validateOne(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new InvalidFileUploadException("Select a non-empty image file.");
        }

        int maxSizeMb = properties.getMarketplaceMaxImageSizeMb();
        long maxSizeBytes = maxSizeMb * 1024L * 1024L;
        if (multipartFile.getSize() > maxSizeBytes) {
            throw new InvalidFileUploadException(
                    "Each image must be " + maxSizeMb + " MB or less.");
        }

        String originalFilename = safeOriginalFilename(
                multipartFile.getOriginalFilename());
        String extension = extension(originalFilename);
        String contentType = multipartFile.getContentType();
        if (!isAllowedExtension(extension) || !isAllowedMimeType(contentType)) {
            throw new InvalidFileUploadException(
                    "Only JPG, PNG, or WebP images are allowed.");
        }

        byte[] content;
        try {
            content = multipartFile.getBytes();
        } catch (IOException exception) {
            throw new StorageOperationException(
                    "Storage is temporarily unavailable.",
                    exception);
        }
        if (content.length == 0) {
            throw new InvalidFileUploadException("Select a non-empty image file.");
        }
        if (content.length > maxSizeBytes) {
            throw new InvalidFileUploadException(
                    "Each image must be " + maxSizeMb + " MB or less.");
        }
        if (!hasExpectedSignature(content, contentType)) {
            throw new InvalidFileUploadException(
                    "Only JPG, PNG, or WebP images are allowed.");
        }

        return new ValidatedNoteFile(
                originalFilename,
                normalizedMimeType(contentType),
                content,
                sha256(content));
    }

    private String safeOriginalFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidFileUploadException(
                    "The uploaded image must have a filename.");
        }
        String safeFilename = filename.trim();
        if (safeFilename.contains("/")
                || safeFilename.contains("\\")
                || safeFilename.contains("..")
                || safeFilename.length() > 255
                || safeFilename.chars().anyMatch(Character::isISOControl)) {
            throw new InvalidFileUploadException(
                    "The uploaded image filename is invalid.");
        }
        return safeFilename;
    }

    private boolean isAllowedExtension(String extension) {
        return ".jpg".equals(extension)
                || ".jpeg".equals(extension)
                || ".png".equals(extension)
                || ".webp".equals(extension);
    }

    private boolean isAllowedMimeType(String contentType) {
        return contentType != null
                && ALLOWED_MIME_TYPES.contains(
                        contentType.toLowerCase(Locale.ROOT));
    }

    private String normalizedMimeType(String contentType) {
        return contentType.toLowerCase(Locale.ROOT);
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        return index == -1
                ? ""
                : filename.substring(index).toLowerCase(Locale.ROOT);
    }

    private boolean hasExpectedSignature(byte[] content, String contentType) {
        String normalizedContentType = normalizedMimeType(contentType);
        return switch (normalizedContentType) {
            case "image/jpeg" -> content.length >= 3
                    && Byte.toUnsignedInt(content[0]) == 0xFF
                    && Byte.toUnsignedInt(content[1]) == 0xD8
                    && Byte.toUnsignedInt(content[2]) == 0xFF;
            case "image/png" -> content.length >= 8
                    && Byte.toUnsignedInt(content[0]) == 0x89
                    && content[1] == 'P'
                    && content[2] == 'N'
                    && content[3] == 'G'
                    && content[4] == '\r'
                    && content[5] == '\n'
                    && Byte.toUnsignedInt(content[6]) == 0x1A
                    && content[7] == '\n';
            case "image/webp" -> content.length >= 12
                    && content[0] == 'R'
                    && content[1] == 'I'
                    && content[2] == 'F'
                    && content[3] == 'F'
                    && content[8] == 'W'
                    && content[9] == 'E'
                    && content[10] == 'B'
                    && content[11] == 'P';
            default -> false;
        };
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
