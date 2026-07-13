package com.campusone.lostfound.service;

import com.campusone.common.exception.FileUploadTooLargeException;
import com.campusone.common.exception.InvalidFileUploadException;
import com.campusone.common.exception.StorageOperationException;
import com.campusone.note.storage.ValidatedNoteFile;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LostFoundImageValidator {

    private static final int MAX_IMAGES = 3;
    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final int maximumImageSizeMb;

    public LostFoundImageValidator(
            @Value("${app.lost-found.max-image-size-mb:5}")
            int maximumImageSizeMb) {
        this.maximumImageSizeMb = Math.max(1, maximumImageSizeMb);
    }

    public List<ValidatedNoteFile> validate(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        if (files.size() > MAX_IMAGES) {
            throw new InvalidFileUploadException(
                    "You can upload up to 3 photos.");
        }
        return files.stream()
                .map(this::validateOne)
                .toList();
    }

    private ValidatedNoteFile validateOne(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileUploadException(
                    "Select a non-empty image file.");
        }
        long maximumSizeBytes = maximumImageSizeMb * 1024L * 1024L;
        if (file.getSize() > maximumSizeBytes) {
            throw new FileUploadTooLargeException(maximumImageSizeMb);
        }
        String originalFilename = safeOriginalFilename(file.getOriginalFilename());
        String lowerFilename = originalFilename.toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(file.getContentType())
                || ALLOWED_EXTENSIONS.stream().noneMatch(lowerFilename::endsWith)) {
            throw new InvalidFileUploadException(
                    "Only JPG, PNG, or WebP images are allowed.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new StorageOperationException(
                    "Storage is temporarily unavailable.",
                    exception);
        }
        if (content.length == 0) {
            throw new InvalidFileUploadException(
                    "Select a non-empty image file.");
        }
        if (content.length > maximumSizeBytes) {
            throw new FileUploadTooLargeException(maximumImageSizeMb);
        }
        return new ValidatedNoteFile(
                originalFilename,
                file.getContentType(),
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

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
