package com.campusone.note.service;

import com.campusone.academic.entity.Course;
import com.campusone.academic.repository.CourseRepository;
import com.campusone.academic.repository.DepartmentRepository;
import com.campusone.common.exception.InvalidNoteStateException;
import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.common.service.CommunityIntegrationService;
import com.campusone.note.dto.request.CreateNoteRequest;
import com.campusone.note.dto.request.CreateUploadedNoteRequest;
import com.campusone.note.dto.request.FileMetadataRequest;
import com.campusone.note.dto.request.NoteSort;
import com.campusone.note.dto.request.UpdateNoteRequest;
import com.campusone.note.dto.response.BookmarkStateResponse;
import com.campusone.note.dto.response.DownloadEventResponse;
import com.campusone.note.dto.response.NoteDetailResponse;
import com.campusone.note.dto.response.NotePageResponse;
import com.campusone.note.dto.response.NoteSummaryResponse;
import com.campusone.note.dto.response.RatingResponse;
import com.campusone.note.entity.FileAsset;
import com.campusone.note.entity.Note;
import com.campusone.note.entity.NoteBookmark;
import com.campusone.note.entity.NoteBookmarkId;
import com.campusone.note.entity.NoteDownloadEvent;
import com.campusone.note.entity.NoteFileType;
import com.campusone.note.entity.NoteModerationAction;
import com.campusone.note.entity.NoteModerationStatus;
import com.campusone.note.entity.NoteRating;
import com.campusone.note.entity.NoteRatingId;
import com.campusone.note.entity.NoteVersion;
import com.campusone.note.entity.NoteVisibility;
import com.campusone.note.entity.Tag;
import com.campusone.note.mapper.NoteMapper;
import com.campusone.note.repository.FileAssetRepository;
import com.campusone.note.repository.NoteBookmarkRepository;
import com.campusone.note.repository.NoteDownloadEventRepository;
import com.campusone.note.repository.NoteModerationActionRepository;
import com.campusone.note.repository.NoteRatingRepository;
import com.campusone.note.repository.NoteRepository;
import com.campusone.note.repository.NoteVersionRepository;
import com.campusone.note.repository.TagRepository;
import com.campusone.note.storage.StorageService;
import com.campusone.note.storage.StoredObject;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final FileAssetRepository fileAssetRepository;
    private final TagRepository tagRepository;
    private final NoteVersionRepository noteVersionRepository;
    private final NoteRatingRepository noteRatingRepository;
    private final NoteBookmarkRepository noteBookmarkRepository;
    private final NoteDownloadEventRepository noteDownloadEventRepository;
    private final NoteModerationActionRepository noteModerationActionRepository;
    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final NoteMapper noteMapper;
    private final CommunityIntegrationService integrationService;
    private final StorageService storageService;
    private final NoteAdminAuthorizationService adminAuthorizationService;
    private final Clock clock;

    public NoteService(
            NoteRepository noteRepository,
            FileAssetRepository fileAssetRepository,
            TagRepository tagRepository,
            NoteVersionRepository noteVersionRepository,
            NoteRatingRepository noteRatingRepository,
            NoteBookmarkRepository noteBookmarkRepository,
            NoteDownloadEventRepository noteDownloadEventRepository,
            NoteModerationActionRepository noteModerationActionRepository,
            CourseRepository courseRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            NoteMapper noteMapper,
            CommunityIntegrationService integrationService,
            StorageService storageService,
            NoteAdminAuthorizationService adminAuthorizationService,
            Clock clock) {
        this.noteRepository = noteRepository;
        this.fileAssetRepository = fileAssetRepository;
        this.tagRepository = tagRepository;
        this.noteVersionRepository = noteVersionRepository;
        this.noteRatingRepository = noteRatingRepository;
        this.noteBookmarkRepository = noteBookmarkRepository;
        this.noteDownloadEventRepository = noteDownloadEventRepository;
        this.noteModerationActionRepository = noteModerationActionRepository;
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.noteMapper = noteMapper;
        this.integrationService = integrationService;
        this.storageService = storageService;
        this.adminAuthorizationService = adminAuthorizationService;
        this.clock = clock;
    }

    @Transactional
    public NoteDetailResponse createNote(
            UUID userId,
            CreateNoteRequest request) {
        User uploader = requireUser(userId);
        Course course = resolveCourse(
                request.courseId(),
                request.courseCode(),
                request.courseName(),
                request.semester());
        validateFileExpiry(request.file());

        FileAsset fileAsset = fileAssetRepository.save(toFileAsset(
                uploader,
                request.file()));
        return persistNewNote(
                userId,
                uploader,
                course,
                fileAsset,
                request.title(),
                request.description(),
                request.teacherName(),
                request.semester(),
                request.fileType(),
                request.visibility(),
                request.tags());
    }

    @Transactional
    public NoteDetailResponse createUploadedNote(
            UUID userId,
            CreateUploadedNoteRequest request,
            StoredObject storedObject) {
        User uploader = requireUser(userId);
        Course course = resolveCourse(
                request.courseId(),
                request.courseCode(),
                request.courseName(),
                request.semester());
        FileAsset fileAsset = fileAssetRepository.save(FileAsset.uploaded(
                uploader,
                storedObject.storageProvider(),
                storedObject.bucketName(),
                storedObject.objectKey(),
                storedObject.originalFilename(),
                storedObject.mimeType(),
                storedObject.sizeBytes(),
                storedObject.checksumSha256()));
        return persistNewNote(
                userId,
                uploader,
                course,
                fileAsset,
                request.title(),
                request.description(),
                request.teacherName(),
                request.semester(),
                request.fileType(),
                request.visibility(),
                request.tags());
    }

    private NoteDetailResponse persistNewNote(
            UUID userId,
            User uploader,
            Course course,
            FileAsset fileAsset,
            String title,
            String description,
            String teacherName,
            int semester,
            NoteFileType fileType,
            NoteVisibility visibility,
            List<String> tags) {
        Note note = new Note(
                uploader,
                course,
                fileAsset,
                title,
                description,
                teacherName,
                semester,
                fileType,
                visibility);
        note.replaceTags(resolveTags(tags));
        Note savedNote = noteRepository.save(note);
        savedNote.approve(uploader, clock.instant());

        noteVersionRepository.save(NoteVersion.initial(savedNote));
        noteModerationActionRepository.save(
                NoteModerationAction.approved(
                        savedNote,
                        uploader,
                        NoteModerationStatus.PENDING));
        integrationService.noteCreated(userId, savedNote.getId());
        return noteMapper.toDetail(savedNote, false, null);
    }

    @Transactional
    public NoteDetailResponse updateNote(
            UUID userId,
            UUID noteId,
            UpdateNoteRequest request) {
        return updateNoteInternal(userId, noteId, request, true);
    }

    @Transactional
    public NoteDetailResponse updateNoteAsAdmin(
            UUID adminUserId,
            UUID noteId,
            UpdateNoteRequest request) {
        return updateNoteInternal(adminUserId, noteId, request, false);
    }

    private NoteDetailResponse updateNoteInternal(
            UUID userId,
            UUID noteId,
            UpdateNoteRequest request,
            boolean requireOwnership) {
        Note note = requireDetailedNote(noteId);
        if (requireOwnership) {
            requireOwner(note, userId);
        }
        if (note.getModerationStatus() == NoteModerationStatus.HIDDEN) {
            throw new InvalidNoteStateException(
                    "A hidden note cannot be edited until a moderator restores it.");
        }

        boolean hasChanges = hasChanges(request);
        Course course = request.courseId() == null
                && (request.courseCode() == null || request.courseCode().isBlank())
                && (request.courseName() == null || request.courseName().isBlank())
                ? null
                : resolveCourse(
                        request.courseId(),
                        request.courseCode(),
                        request.courseName(),
                        request.semester());
        note.updateMetadata(
                course,
                request.title(),
                request.description(),
                request.teacherName(),
                request.semester(),
                request.fileType(),
                request.visibility());
        if (request.tags() != null) {
            note.replaceTags(resolveTags(request.tags()));
        }

        if (hasChanges) {
            if (requireOwnership) {
                NoteModerationStatus previousStatus = note.resubmitForReview();
                if (previousStatus != NoteModerationStatus.PENDING) {
                    noteModerationActionRepository.save(
                            NoteModerationAction.submitted(
                                    note,
                                    previousStatus));
                }
            } else {
                User admin = requireUser(userId);
                NoteModerationStatus previousStatus =
                        note.getModerationStatus();
                note.approve(admin, clock.instant());
                if (previousStatus != NoteModerationStatus.APPROVED) {
                    noteModerationActionRepository.save(
                            NoteModerationAction.approved(
                                    note,
                                    admin,
                                    previousStatus));
                }
            }
        }
        return toDetail(note, userId);
    }

    @Transactional
    public void deleteNote(UUID userId, UUID noteId) {
        Note note = requireDetailedNote(noteId);
        requireOwner(note, userId);
        note.softDelete(clock.instant());
    }

    @Transactional
    public void deleteNoteAsAdmin(UUID noteId) {
        Note note = requireDetailedNote(noteId);
        note.softDelete(clock.instant());
    }

    @Transactional(readOnly = true)
    public NoteDetailResponse getNote(
            UUID noteId,
            UUID viewerUserId) {
        Note note = requireDetailedNote(noteId);
        requireViewable(note, viewerUserId);
        return toDetail(note, viewerUserId);
    }

    @Transactional(readOnly = true)
    public NoteDetailResponse getNoteAsAdmin(
            UUID noteId,
            UUID adminUserId) {
        return toDetail(requireDetailedNote(noteId), adminUserId);
    }

    @Transactional(readOnly = true)
    public NotePageResponse listPublicNotes(
            UUID courseId,
            String tag,
            int page,
            int size,
            NoteSort sort) {
        return listPublicNotes(courseId, null, tag, page, size, sort);
    }

    @Transactional(readOnly = true)
    public NotePageResponse listPublicNotes(
            UUID courseId,
            String courseQuery,
            String tag,
            int page,
            int size,
            NoteSort sort) {
        String normalizedTag = tag == null || tag.isBlank()
                ? null
                : Tag.normalize(tag);
        String normalizedCourseQuery = toSearchPattern(courseQuery);
        Page<Note> notes = noteRepository.findPublicNotes(
                NoteModerationStatus.APPROVED,
                NoteVisibility.PUBLIC,
                courseId,
                normalizedCourseQuery,
                normalizedTag,
                PageRequest.of(page, size, sort(sort)));
        return toPageResponse(notes);
    }

    @Transactional(readOnly = true)
    public NotePageResponse listMyNotes(
            UUID userId,
            int page,
            int size,
            NoteSort sort) {
        Page<Note> notes = noteRepository.findMyNotes(
                userId,
                PageRequest.of(page, size, sort(sort)));
        return toPageResponse(notes);
    }

    @Transactional
    public BookmarkStateResponse bookmark(UUID userId, UUID noteId) {
        Note note = requireDetailedNote(noteId);
        requireViewable(note, userId);
        User user = requireUser(userId);
        NoteBookmarkId bookmarkId = new NoteBookmarkId(noteId, userId);
        if (!noteBookmarkRepository.existsById(bookmarkId)) {
            noteBookmarkRepository.save(new NoteBookmark(note, user));
        }
        return new BookmarkStateResponse(noteId, true);
    }

    @Transactional
    public BookmarkStateResponse unbookmark(UUID userId, UUID noteId) {
        Note note = requireDetailedNote(noteId);
        requireViewable(note, userId);
        NoteBookmarkId bookmarkId = new NoteBookmarkId(noteId, userId);
        if (noteBookmarkRepository.existsById(bookmarkId)) {
            noteBookmarkRepository.deleteById(bookmarkId);
        }
        return new BookmarkStateResponse(noteId, false);
    }

    @Transactional
    public RatingResponse rate(
            UUID userId,
            UUID noteId,
            int newRating) {
        Note note = noteRepository.findActiveByIdForUpdate(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note"));
        requireViewable(note, userId);
        User user = requireUser(userId);
        NoteRatingId ratingId = new NoteRatingId(noteId, userId);
        NoteRating rating = noteRatingRepository.findById(ratingId).orElse(null);
        Integer previousRating = rating == null ? null : rating.getRating();

        if (rating == null) {
            noteRatingRepository.save(new NoteRating(note, user, newRating));
        } else {
            rating.updateRating(newRating);
        }
        note.applyRating(previousRating, newRating);
        integrationService.noteRated(
                note.getUploader().getId(),
                userId,
                noteId);

        return new RatingResponse(
                noteId,
                newRating,
                note.getRatingCount(),
                note.calculateAverageRating());
    }

    @Transactional
    public DownloadEventResponse recordDownload(
            UUID userId,
            UUID noteId,
            String requestFingerprint,
            String userAgent) {
        Note note = noteRepository.findActiveByIdForUpdate(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note"));
        requireViewable(note, userId);
        User user = requireUser(userId);
        String downloadUrl = storageService.createDownloadUrl(
                note.getFileAsset());

        NoteDownloadEvent event = noteDownloadEventRepository.save(
                new NoteDownloadEvent(
                        note,
                        user,
                        sha256(requestFingerprint),
                        sha256(userAgent)));
        note.recordDownload();
        integrationService.noteDownloaded(
                note.getUploader().getId(),
                userId,
                noteId);
        return new DownloadEventResponse(
                event.getId(),
                noteId,
                event.getDownloadedAt(),
                note.getDownloadCount(),
                downloadUrl);
    }

    private NoteDetailResponse toDetail(Note note, UUID viewerUserId) {
        if (viewerUserId == null) {
            return noteMapper.toDetail(note, false, null);
        }
        NoteBookmarkId bookmarkId = new NoteBookmarkId(note.getId(), viewerUserId);
        NoteRatingId ratingId = new NoteRatingId(note.getId(), viewerUserId);
        boolean bookmarked = noteBookmarkRepository.existsById(bookmarkId);
        Integer currentRating = noteRatingRepository.findById(ratingId)
                .map(NoteRating::getRating)
                .orElse(null);
        return noteMapper.toDetail(note, bookmarked, currentRating);
    }

    private NotePageResponse toPageResponse(Page<Note> page) {
        List<NoteSummaryResponse> content = page.getContent().stream()
                .map(noteMapper::toSummary)
                .toList();
        return new NotePageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    private List<Tag> resolveTags(List<String> requestedTags) {
        Map<String, String> displayNames = new LinkedHashMap<>();
        requestedTags.forEach(tag ->
                displayNames.putIfAbsent(Tag.normalize(tag), tag.trim()));
        Set<String> normalizedNames = displayNames.keySet();

        Map<String, Tag> tagsByName = tagRepository
                .findAllByNormalizedNameIn(normalizedNames)
                .stream()
                .collect(Collectors.toMap(
                        Tag::getNormalizedName,
                        tag -> tag));
        List<Tag> missingTags = displayNames.entrySet().stream()
                .filter(entry -> !tagsByName.containsKey(entry.getKey()))
                .map(entry -> new Tag(entry.getValue()))
                .toList();
        if (!missingTags.isEmpty()) {
            tagRepository.saveAll(missingTags);
            missingTags.forEach(tag ->
                    tagsByName.put(tag.getNormalizedName(), tag));
        }

        List<Tag> resolvedTags = new ArrayList<>(displayNames.size());
        displayNames.keySet().forEach(name -> resolvedTags.add(tagsByName.get(name)));
        return resolvedTags;
    }

    private FileAsset toFileAsset(
            User owner,
            FileMetadataRequest request) {
        return new FileAsset(
                owner,
                request.storageProvider(),
                request.bucketName(),
                request.objectKey(),
                request.originalFilename(),
                request.mimeType(),
                request.sizeBytes(),
                request.checksumSha256(),
                request.expiresAt());
    }

    private boolean hasChanges(UpdateNoteRequest request) {
        return request.courseId() != null
                || request.courseCode() != null
                || request.courseName() != null
                || request.title() != null
                || request.description() != null
                || request.teacherName() != null
                || request.semester() != null
                || request.fileType() != null
                || request.visibility() != null
                || request.tags() != null;
    }

    private void validateFileExpiry(FileMetadataRequest file) {
        if (file.expiresAt() != null && !file.expiresAt().isAfter(clock.instant())) {
            throw new InvalidNoteStateException(
                    "File metadata expiry must be in the future.");
        }
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
    }

    private Course resolveCourse(
            UUID courseId,
            String courseCode,
            String courseName,
            Integer recommendedSemester) {
        if (courseId != null) {
            return requireActiveCourse(courseId);
        }
        String normalizedCode = normalizeCourseCode(courseCode);
        if (normalizedCode == null) {
            throw new InvalidNoteStateException("Course code is required.");
        }
        String normalizedName = normalizeCourseName(courseName);
        return courseRepository
                .findFirstByCourseCodeIgnoreCaseAndActiveTrueOrderByCourseCodeAsc(
                        normalizedCode)
                .or(() -> normalizedName == null
                        ? java.util.Optional.empty()
                        : courseRepository
                                .findFirstByTitleIgnoreCaseAndActiveTrueOrderByCourseCodeAsc(
                                        normalizedName))
                .orElseGet(() -> createLightweightCourse(
                        normalizedCode,
                        normalizedName,
                        recommendedSemester));
    }

    private Course createLightweightCourse(
            String courseCode,
            String courseName,
            Integer recommendedSemester) {
        var department = departmentRepository
                .findFirstByActiveTrueOrderByNameAsc()
                .orElseThrow(() -> new ResourceNotFoundException("Department"));
        Course course = new Course(
                department,
                courseCode,
                courseName == null ? courseCode : courseName);
        if (recommendedSemester != null) {
            course.setRecommendedSemester(recommendedSemester);
        }
        return courseRepository.save(course);
    }

    private String normalizeCourseCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(java.util.Locale.ROOT);
    }

    private String normalizeCourseName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String toSearchPattern(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String escaped = value.trim()
                .toLowerCase(java.util.Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private Course requireActiveCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course"));
        if (!course.isActive()) {
            throw new InvalidNoteStateException(
                    "Notes cannot be assigned to an inactive course.");
        }
        return course;
    }

    private Note requireDetailedNote(UUID noteId) {
        return noteRepository.findDetailedById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note"));
    }

    private void requireOwner(Note note, UUID userId) {
        if (!note.isOwnedBy(userId)) {
            throw new AccessDeniedException("Only the note owner may modify this note.");
        }
    }

    private void requireViewable(Note note, UUID viewerUserId) {
        boolean owner = viewerUserId != null && note.isOwnedBy(viewerUserId);
        if (owner || note.isPubliclyVisible()) {
            return;
        }
        if (viewerUserId != null && isAdmin(viewerUserId)) {
            return;
        }
        throw new ResourceNotFoundException("Note");
    }

    private boolean isAdmin(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> adminAuthorizationService.canManage(
                        user.getId(),
                        user.getEmail()))
                .orElse(false);
    }

    private Sort sort(NoteSort noteSort) {
        return switch (noteSort) {
            case RATING -> Sort.by(
                    Sort.Order.desc("averageRating"),
                    Sort.Order.desc("ratingCount"),
                    Sort.Order.asc("id"));
            case DOWNLOADS -> Sort.by(
                    Sort.Order.desc("downloadCount"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.asc("id"));
            case NEWEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.asc("id"));
        };
    }

    private String sha256(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
