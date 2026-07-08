package com.campusone.note.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.academic.entity.Course;
import com.campusone.academic.entity.Department;
import com.campusone.academic.entity.University;
import com.campusone.academic.mapper.CourseMapper;
import com.campusone.academic.repository.CourseRepository;
import com.campusone.academic.repository.DepartmentRepository;
import com.campusone.common.service.CommunityIntegrationService;
import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.note.dto.request.CreateNoteRequest;
import com.campusone.note.dto.request.CreateUploadedNoteRequest;
import com.campusone.note.dto.request.FileMetadataRequest;
import com.campusone.note.dto.request.NoteSort;
import com.campusone.note.dto.request.UpdateNoteRequest;
import com.campusone.note.dto.response.BookmarkStateResponse;
import com.campusone.note.dto.response.DownloadEventResponse;
import com.campusone.note.dto.response.NoteDetailResponse;
import com.campusone.note.dto.response.NotePageResponse;
import com.campusone.note.dto.response.RatingResponse;
import com.campusone.note.entity.FileAsset;
import com.campusone.note.entity.FileAssetStatus;
import com.campusone.note.entity.Note;
import com.campusone.note.entity.NoteBookmark;
import com.campusone.note.entity.NoteDownloadEvent;
import com.campusone.note.entity.NoteFileType;
import com.campusone.note.entity.NoteModerationAction;
import com.campusone.note.entity.NoteModerationStatus;
import com.campusone.note.entity.NoteRating;
import com.campusone.note.entity.NoteVisibility;
import com.campusone.note.entity.StorageProvider;
import com.campusone.note.mapper.FileAssetMapper;
import com.campusone.note.mapper.NoteMapper;
import com.campusone.note.mapper.TagMapper;
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
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    private static final UUID OWNER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000002");
    private static final UUID COURSE_ID = UUID.fromString(
            "20000000-0000-4000-8000-000000000001");
    private static final UUID NOTE_ID = UUID.fromString(
            "30000000-0000-4000-8000-000000000001");
    private static final UUID FILE_ID = UUID.fromString(
            "40000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-07-02T12:00:00Z");

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private FileAssetRepository fileAssetRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private NoteVersionRepository noteVersionRepository;

    @Mock
    private NoteRatingRepository noteRatingRepository;

    @Mock
    private NoteBookmarkRepository noteBookmarkRepository;

    @Mock
    private NoteDownloadEventRepository noteDownloadEventRepository;

    @Mock
    private NoteModerationActionRepository noteModerationActionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommunityIntegrationService integrationService;

    @Mock
    private StorageService storageService;

    @Mock
    private NoteAdminAuthorizationService adminAuthorizationService;

    private NoteService noteService;
    private User owner;
    private User otherUser;
    private Course course;
    private FileAsset fileAsset;
    private Note note;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(
                noteRepository,
                fileAssetRepository,
                tagRepository,
                noteVersionRepository,
                noteRatingRepository,
                noteBookmarkRepository,
                noteDownloadEventRepository,
                noteModerationActionRepository,
                courseRepository,
                departmentRepository,
                userRepository,
                new NoteMapper(
                         new CourseMapper(),
                         new FileAssetMapper(),
                         new TagMapper()),
                integrationService,
                storageService,
                adminAuthorizationService,
                Clock.fixed(NOW, ZoneOffset.UTC));

        University university = new University(
                "COMSATS University Islamabad",
                "COMSATS",
                "Islamabad");
        ReflectionTestUtils.setField(
                university,
                "id",
                UUID.fromString("50000000-0000-4000-8000-000000000001"));
        Department department = new Department(
                university,
                "Computer Science",
                "CS");
        ReflectionTestUtils.setField(
                department,
                "id",
                UUID.fromString("60000000-0000-4000-8000-000000000001"));
        course = new Course(
                department,
                "CS-201",
                "Object Oriented Programming");
        ReflectionTestUtils.setField(course, "id", COURSE_ID);

        owner = user(
                OWNER_ID,
                "owner@example.com",
                "Ali Khan",
                university,
                department);
        otherUser = user(
                OTHER_USER_ID,
                "student@example.com",
                "Ayesha Malik",
                university,
                department);
        fileAsset = fileAsset(owner);
        note = note(owner, NoteModerationStatus.PENDING);
    }

    @Test
    void createNote_normalUserCreatesPendingNoteAndInitialAuditRecords() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(fileAssetRepository.save(any(FileAsset.class)))
                .thenAnswer(invocation -> {
                    FileAsset saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", FILE_ID);
                    ReflectionTestUtils.setField(saved, "createdAt", NOW);
                    ReflectionTestUtils.setField(saved, "updatedAt", NOW);
                    return saved;
                });
        when(tagRepository.findAllByNormalizedNameIn(anyCollection()))
                .thenReturn(List.of());
        when(tagRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(noteRepository.save(any(Note.class)))
                .thenAnswer(invocation -> {
                    Note saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", NOTE_ID);
                    ReflectionTestUtils.setField(saved, "createdAt", NOW);
                    ReflectionTestUtils.setField(saved, "updatedAt", NOW);
                    return saved;
                });

        NoteDetailResponse response =
                noteService.createNote(OWNER_ID, createRequest());

        assertThat(response.id()).isEqualTo(NOTE_ID);
        assertThat(response.moderationStatus())
                .isEqualTo(NoteModerationStatus.PENDING);
        assertThat(response.tags())
                .extracting(tag -> tag.name())
                .containsExactlyInAnyOrder("Java", "OOP");
        verify(noteVersionRepository).save(any());
        verify(noteModerationActionRepository)
                .save(any(NoteModerationAction.class));
    }

    @Test
    void createNote_adminCreatesApprovedNoteAndInitialAuditRecords() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(adminAuthorizationService.canManage(OWNER_ID, "owner@example.com"))
                .thenReturn(true);
        when(fileAssetRepository.save(any(FileAsset.class)))
                .thenAnswer(invocation -> {
                    FileAsset saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", FILE_ID);
                    ReflectionTestUtils.setField(saved, "createdAt", NOW);
                    ReflectionTestUtils.setField(saved, "updatedAt", NOW);
                    return saved;
                });
        when(tagRepository.findAllByNormalizedNameIn(anyCollection()))
                .thenReturn(List.of());
        when(tagRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(noteRepository.save(any(Note.class)))
                .thenAnswer(invocation -> {
                    Note saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", NOTE_ID);
                    ReflectionTestUtils.setField(saved, "createdAt", NOW);
                    ReflectionTestUtils.setField(saved, "updatedAt", NOW);
                    return saved;
                });

        NoteDetailResponse response =
                noteService.createNote(OWNER_ID, createRequest());

        assertThat(response.id()).isEqualTo(NOTE_ID);
        assertThat(response.moderationStatus())
                .isEqualTo(NoteModerationStatus.APPROVED);
        verify(noteVersionRepository).save(any());
        verify(noteModerationActionRepository)
                .save(any(NoteModerationAction.class));
    }

    @Test
    void createNote_missingCourse_rejectsRequest() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                noteService.createNote(OWNER_ID, createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Course was not found.");
    }

    @Test
    void createUploadedNote_storesReadyR2Metadata() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(adminAuthorizationService.canManage(OWNER_ID, "owner@example.com"))
                .thenReturn(true);
        when(fileAssetRepository.save(any(FileAsset.class)))
                .thenAnswer(invocation -> {
                    FileAsset saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", FILE_ID);
                    ReflectionTestUtils.setField(saved, "createdAt", NOW);
                    ReflectionTestUtils.setField(saved, "updatedAt", NOW);
                    return saved;
                });
        when(tagRepository.findAllByNormalizedNameIn(anyCollection()))
                .thenReturn(List.of());
        when(tagRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(noteRepository.save(any(Note.class)))
                .thenAnswer(invocation -> {
                    Note saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", NOTE_ID);
                    ReflectionTestUtils.setField(saved, "createdAt", NOW);
                    ReflectionTestUtils.setField(saved, "updatedAt", NOW);
                    return saved;
                });
        StoredObject storedObject = new StoredObject(
                StorageProvider.S3_COMPATIBLE,
                "campusone-notes",
                "notes/" + OWNER_ID + "/2026/oop.pdf",
                "oop-notes.pdf",
                "application/pdf",
                4096,
                "a".repeat(64));
        CreateUploadedNoteRequest request = new CreateUploadedNoteRequest(
                COURSE_ID,
                "Complete OOP Notes",
                "Detailed object oriented programming lecture notes.",
                "Dr. Ahmed Khan",
                4,
                NoteFileType.PDF,
                NoteVisibility.PUBLIC,
                List.of("OOP", "Java"));

        NoteDetailResponse response = noteService.createUploadedNote(
                OWNER_ID,
                request,
                storedObject);

        ArgumentCaptor<FileAsset> assetCaptor =
                ArgumentCaptor.forClass(FileAsset.class);
        verify(fileAssetRepository).save(assetCaptor.capture());
        assertThat(assetCaptor.getValue().getStatus())
                .isEqualTo(FileAssetStatus.READY);
        assertThat(assetCaptor.getValue().getStorageProvider())
                .isEqualTo(StorageProvider.S3_COMPATIBLE);
        assertThat(response.file().originalFilename())
                .isEqualTo("oop-notes.pdf");
        assertThat(response.moderationStatus())
                .isEqualTo(NoteModerationStatus.APPROVED);
    }

    @Test
    void createUploadedNote_normalUserSubmissionStaysPending() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(fileAssetRepository.save(any(FileAsset.class)))
                .thenAnswer(invocation -> {
                    FileAsset saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", FILE_ID);
                    ReflectionTestUtils.setField(saved, "createdAt", NOW);
                    ReflectionTestUtils.setField(saved, "updatedAt", NOW);
                    return saved;
                });
        when(tagRepository.findAllByNormalizedNameIn(anyCollection()))
                .thenReturn(List.of());
        when(tagRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(noteRepository.save(any(Note.class)))
                .thenAnswer(invocation -> {
                    Note saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", NOTE_ID);
                    ReflectionTestUtils.setField(saved, "createdAt", NOW);
                    ReflectionTestUtils.setField(saved, "updatedAt", NOW);
                    return saved;
                });
        StoredObject storedObject = new StoredObject(
                StorageProvider.S3_COMPATIBLE,
                "campusone-notes",
                "notes/" + OWNER_ID + "/2026/oop.pdf",
                "oop-notes.pdf",
                "application/pdf",
                4096,
                "a".repeat(64));

        NoteDetailResponse response = noteService.createUploadedNote(
                OWNER_ID,
                createUploadedRequest(),
                storedObject);

        assertThat(response.moderationStatus())
                .isEqualTo(NoteModerationStatus.PENDING);
    }

    @Test
    void listPublicNotes_approvedPublicNotes_returnsPage() {
        setModerationStatus(note, NoteModerationStatus.APPROVED);
        when(noteRepository.findPublicNotes(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(note)));

        NotePageResponse response = noteService.listPublicNotes(
                null,
                null,
                0,
                20,
                NoteSort.NEWEST);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(NOTE_ID);
        assertThat(response.content().getFirst().moderationStatus())
                .isEqualTo(NoteModerationStatus.APPROVED);
        verify(noteRepository).findPublicNotes(
                eq(NoteModerationStatus.APPROVED),
                eq(NoteVisibility.PUBLIC),
                isNull(),
                isNull(),
                isNull(),
                org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    void getNote_pendingNote_allowsOwner() {
        when(noteRepository.findDetailedById(NOTE_ID)).thenReturn(Optional.of(note));
        when(noteBookmarkRepository.existsById(any())).thenReturn(false);
        when(noteRatingRepository.findById(any())).thenReturn(Optional.empty());

        NoteDetailResponse response = noteService.getNote(NOTE_ID, OWNER_ID);

        assertThat(response.id()).isEqualTo(NOTE_ID);
        assertThat(response.moderationStatus())
                .isEqualTo(NoteModerationStatus.PENDING);
    }

    @Test
    void getNote_pendingNote_hidesItFromAnonymousViewer() {
        when(noteRepository.findDetailedById(NOTE_ID)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> noteService.getNote(NOTE_ID, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Note was not found.");
    }

    @Test
    void updateNote_ownerUpdate_resubmitsApprovedNote() {
        setModerationStatus(note, NoteModerationStatus.APPROVED);
        when(noteRepository.findDetailedById(NOTE_ID)).thenReturn(Optional.of(note));
        when(noteBookmarkRepository.existsById(any())).thenReturn(false);
        when(noteRatingRepository.findById(any())).thenReturn(Optional.empty());

        NoteDetailResponse response = noteService.updateNote(
                OWNER_ID,
                NOTE_ID,
                new UpdateNoteRequest(
                        null,
                        "Updated OOP Notes",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        assertThat(response.title()).isEqualTo("Updated OOP Notes");
        assertThat(response.moderationStatus())
                .isEqualTo(NoteModerationStatus.PENDING);
        verify(noteModerationActionRepository)
                .save(any(NoteModerationAction.class));
    }

    @Test
    void updateNote_nonOwnerUpdate_isDenied() {
        when(noteRepository.findDetailedById(NOTE_ID)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> noteService.updateNote(
                OTHER_USER_ID,
                NOTE_ID,
                new UpdateNoteRequest(
                        null,
                        "Updated OOP Notes",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateNoteAsAdmin_nonOwnerUpdate_isAllowed() {
        when(noteRepository.findDetailedById(NOTE_ID))
                .thenReturn(Optional.of(note));
        when(userRepository.findById(OTHER_USER_ID))
                .thenReturn(Optional.of(otherUser));
        when(noteBookmarkRepository.existsById(any())).thenReturn(false);
        when(noteRatingRepository.findById(any())).thenReturn(Optional.empty());

        NoteDetailResponse response = noteService.updateNoteAsAdmin(
                OTHER_USER_ID,
                NOTE_ID,
                new UpdateNoteRequest(
                        null,
                        "Admin Updated OOP Notes",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        assertThat(response.title()).isEqualTo("Admin Updated OOP Notes");
        assertThat(response.moderationStatus())
                .isEqualTo(NoteModerationStatus.APPROVED);
    }

    @Test
    void deleteNoteAsAdmin_nonOwnerDelete_isAllowed() {
        when(noteRepository.findDetailedById(NOTE_ID))
                .thenReturn(Optional.of(note));

        noteService.deleteNoteAsAdmin(NOTE_ID);

        assertThat(note.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    void bookmarkAndUnbookmark_accessibleNote_areIdempotent() {
        setModerationStatus(note, NoteModerationStatus.APPROVED);
        when(noteRepository.findDetailedById(NOTE_ID)).thenReturn(Optional.of(note));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));
        when(noteBookmarkRepository.existsById(any()))
                .thenReturn(false, true);

        BookmarkStateResponse saved =
                noteService.bookmark(OTHER_USER_ID, NOTE_ID);
        BookmarkStateResponse removed =
                noteService.unbookmark(OTHER_USER_ID, NOTE_ID);

        assertThat(saved.bookmarked()).isTrue();
        assertThat(removed.bookmarked()).isFalse();
        verify(noteBookmarkRepository).save(any(NoteBookmark.class));
        verify(noteBookmarkRepository).deleteById(any());
    }

    @Test
    void rate_existingRating_updatesAggregateCounters() {
        setModerationStatus(note, NoteModerationStatus.APPROVED);
        ReflectionTestUtils.setField(note, "ratingCount", 1L);
        ReflectionTestUtils.setField(note, "ratingSum", 3L);
        NoteRating rating = new NoteRating(note, otherUser, 3);
        when(noteRepository.findActiveByIdForUpdate(NOTE_ID))
                .thenReturn(Optional.of(note));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));
        when(noteRatingRepository.findById(any())).thenReturn(Optional.of(rating));

        RatingResponse response =
                noteService.rate(OTHER_USER_ID, NOTE_ID, 5);

        assertThat(rating.getRating()).isEqualTo(5);
        assertThat(response.ratingCount()).isEqualTo(1);
        assertThat(response.averageRating()).isEqualByComparingTo(
                new BigDecimal("5.00"));
    }

    @Test
    void recordDownload_accessibleNote_recordsEventAndIncrementsCounter() {
        setModerationStatus(note, NoteModerationStatus.APPROVED);
        when(noteRepository.findActiveByIdForUpdate(NOTE_ID))
                .thenReturn(Optional.of(note));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));
        when(storageService.createDownloadUrl(fileAsset))
                .thenReturn("https://files.example.test/oop-notes.pdf");
        when(noteDownloadEventRepository.save(any(NoteDownloadEvent.class)))
                .thenAnswer(invocation -> {
                    NoteDownloadEvent event = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            event,
                            "id",
                            UUID.fromString(
                                    "70000000-0000-4000-8000-000000000001"));
                    ReflectionTestUtils.setField(event, "downloadedAt", NOW);
                    return event;
                });

        DownloadEventResponse response = noteService.recordDownload(
                OTHER_USER_ID,
                NOTE_ID,
                "127.0.0.1",
                "JUnit");

        assertThat(response.downloadCount()).isEqualTo(1);
        assertThat(response.downloadedAt()).isEqualTo(NOW);
        assertThat(response.downloadUrl())
                .isEqualTo("https://files.example.test/oop-notes.pdf");
        verify(noteDownloadEventRepository)
                .save(any(NoteDownloadEvent.class));
    }

    private CreateNoteRequest createRequest() {
        return new CreateNoteRequest(
                COURSE_ID,
                "Complete OOP Notes",
                "Detailed object oriented programming lecture notes.",
                "Dr. Ahmed Khan",
                4,
                NoteFileType.PDF,
                NoteVisibility.PUBLIC,
                List.of("OOP", "Java"),
                new FileMetadataRequest(
                        StorageProvider.MINIO,
                        "campusone-notes",
                        "notes/oop.pdf",
                        "oop-notes.pdf",
                        "application/pdf",
                        4096L,
                        "a".repeat(64),
                        null));
    }

    private CreateUploadedNoteRequest createUploadedRequest() {
        return new CreateUploadedNoteRequest(
                COURSE_ID,
                "Complete OOP Notes",
                "Detailed object oriented programming lecture notes.",
                "Dr. Ahmed Khan",
                4,
                NoteFileType.PDF,
                NoteVisibility.PUBLIC,
                List.of("OOP", "Java"));
    }

    private User user(
            UUID id,
            String email,
            String fullName,
            University university,
            Department department) {
        User user = new User(email, "$2a$12$encoded-password");
        ReflectionTestUtils.setField(user, "id", id);
        user.setStudentProfile(new StudentProfile(
                user,
                university,
                department,
                fullName,
                4));
        return user;
    }

    private FileAsset fileAsset(User user) {
        FileAsset asset = new FileAsset(
                user,
                StorageProvider.MINIO,
                "campusone-notes",
                "notes/oop.pdf",
                "oop-notes.pdf",
                "application/pdf",
                4096L,
                "a".repeat(64),
                null);
        ReflectionTestUtils.setField(asset, "id", FILE_ID);
        ReflectionTestUtils.setField(asset, "createdAt", NOW);
        ReflectionTestUtils.setField(asset, "updatedAt", NOW);
        return asset;
    }

    private Note note(User uploader, NoteModerationStatus status) {
        Note result = new Note(
                uploader,
                course,
                fileAsset,
                "Complete OOP Notes",
                "Detailed object oriented programming lecture notes.",
                "Dr. Ahmed Khan",
                4,
                NoteFileType.PDF,
                NoteVisibility.PUBLIC);
        ReflectionTestUtils.setField(result, "id", NOTE_ID);
        ReflectionTestUtils.setField(result, "createdAt", NOW);
        ReflectionTestUtils.setField(result, "updatedAt", NOW);
        setModerationStatus(result, status);
        return result;
    }

    private void setModerationStatus(
            Note target,
            NoteModerationStatus status) {
        ReflectionTestUtils.setField(target, "moderationStatus", status);
        if (status == NoteModerationStatus.APPROVED) {
            ReflectionTestUtils.setField(target, "moderatedAt", NOW);
            ReflectionTestUtils.setField(target, "publishedAt", NOW);
        }
    }
}
