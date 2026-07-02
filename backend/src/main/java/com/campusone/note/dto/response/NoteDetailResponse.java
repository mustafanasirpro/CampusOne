package com.campusone.note.dto.response;

import com.campusone.academic.dto.response.CourseResponse;
import com.campusone.note.entity.NoteFileType;
import com.campusone.note.entity.NoteModerationStatus;
import com.campusone.note.entity.NoteVisibility;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NoteDetailResponse(
        UUID id,
        String title,
        String description,
        String teacherName,
        CourseResponse course,
        int semester,
        NoteFileType fileType,
        NoteVisibility visibility,
        NoteModerationStatus moderationStatus,
        String moderationReason,
        NoteUploaderResponse uploader,
        FileMetadataResponse file,
        List<TagResponse> tags,
        long ratingCount,
        BigDecimal averageRating,
        long downloadCount,
        boolean bookmarked,
        Integer currentUserRating,
        int contentVersion,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt) {
}
