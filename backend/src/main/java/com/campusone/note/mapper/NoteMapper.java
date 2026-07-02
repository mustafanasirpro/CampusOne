package com.campusone.note.mapper;

import com.campusone.academic.mapper.CourseMapper;
import com.campusone.note.dto.response.NoteDetailResponse;
import com.campusone.note.dto.response.NoteSummaryResponse;
import com.campusone.note.dto.response.NoteUploaderResponse;
import com.campusone.note.dto.response.TagResponse;
import com.campusone.note.entity.Note;
import com.campusone.user.entity.StudentProfile;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

    private final CourseMapper courseMapper;
    private final FileAssetMapper fileAssetMapper;
    private final TagMapper tagMapper;

    public NoteMapper(
            CourseMapper courseMapper,
            FileAssetMapper fileAssetMapper,
            TagMapper tagMapper) {
        this.courseMapper = courseMapper;
        this.fileAssetMapper = fileAssetMapper;
        this.tagMapper = tagMapper;
    }

    public NoteSummaryResponse toSummary(Note note) {
        return new NoteSummaryResponse(
                note.getId(),
                note.getTitle(),
                note.getTeacherName(),
                courseMapper.toResponse(note.getCourse()),
                note.getSemester(),
                note.getFileType(),
                note.getVisibility(),
                note.getModerationStatus(),
                toUploader(note),
                toTags(note),
                note.getRatingCount(),
                note.getAverageRating(),
                note.getDownloadCount(),
                note.getCreatedAt(),
                note.getUpdatedAt());
    }

    public NoteDetailResponse toDetail(
            Note note,
            boolean bookmarked,
            Integer currentUserRating) {
        return new NoteDetailResponse(
                note.getId(),
                note.getTitle(),
                note.getDescription(),
                note.getTeacherName(),
                courseMapper.toResponse(note.getCourse()),
                note.getSemester(),
                note.getFileType(),
                note.getVisibility(),
                note.getModerationStatus(),
                note.getModerationReason(),
                toUploader(note),
                fileAssetMapper.toResponse(note.getFileAsset()),
                toTags(note),
                note.getRatingCount(),
                note.getAverageRating(),
                note.getDownloadCount(),
                bookmarked,
                currentUserRating,
                note.getContentVersion(),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                note.getPublishedAt());
    }

    private NoteUploaderResponse toUploader(Note note) {
        StudentProfile profile = note.getUploader().getStudentProfile();
        return new NoteUploaderResponse(
                note.getUploader().getId(),
                profile == null ? null : profile.getFullName(),
                profile == null ? null : profile.getAvatarUrl(),
                profile == null ? null : profile.getUniversity().getName());
    }

    private List<TagResponse> toTags(Note note) {
        return note.getTags().stream()
                .map(tagMapper::toResponse)
                .sorted(java.util.Comparator.comparing(
                        TagResponse::name,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
