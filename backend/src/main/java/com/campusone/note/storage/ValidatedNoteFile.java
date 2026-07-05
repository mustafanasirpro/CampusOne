package com.campusone.note.storage;

public record ValidatedNoteFile(
        String originalFilename,
        String mimeType,
        byte[] content,
        String checksumSha256) {

    public long sizeBytes() {
        return content.length;
    }
}
