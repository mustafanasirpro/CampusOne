package com.campusone.note.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.academic.dto.response.CourseResponse;
import com.campusone.note.dto.request.CreateNoteRequest;
import com.campusone.note.dto.request.CreateUploadedNoteRequest;
import com.campusone.note.dto.request.UpdateNoteRequest;
import com.campusone.common.exception.StorageNotConfiguredException;
import com.campusone.common.exception.UploadLimitExceededException;
import com.campusone.common.exception.NoteManagementAccessDeniedException;
import com.campusone.note.dto.response.FileMetadataResponse;
import com.campusone.note.dto.response.NoteDetailResponse;
import com.campusone.note.dto.response.NotePageResponse;
import com.campusone.note.dto.response.NoteUploaderResponse;
import com.campusone.note.entity.FileAssetStatus;
import com.campusone.note.entity.NoteFileType;
import com.campusone.note.entity.NoteModerationStatus;
import com.campusone.note.entity.NoteVisibility;
import com.campusone.note.entity.StorageProvider;
import com.campusone.note.service.NoteService;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.note.service.NoteUploadService;
import com.campusone.security.CampusOneUserDetailsService;
import com.campusone.security.CampusOneUserPrincipal;
import com.campusone.security.JwtAuthenticationFilter;
import com.campusone.security.JwtService;
import com.campusone.security.RestAccessDeniedHandler;
import com.campusone.security.RestAuthenticationEntryPoint;
import com.campusone.security.SecurityConfig;
import com.campusone.security.SecurityErrorResponseWriter;
import com.campusone.user.entity.AccountStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

@WebMvcTest(NoteController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class NoteControllerTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID NOTE_ID = UUID.fromString(
            "20000000-0000-4000-8000-000000000001");
    private static final UUID COURSE_ID = UUID.fromString(
            "30000000-0000-4000-8000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString(
            "40000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-07-02T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoteService noteService;

    @MockitoBean
    private NoteUploadService noteUploadService;

    @MockitoBean
    private NoteAdminAuthorizationService adminAuthorizationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CampusOneUserDetailsService userDetailsService;

    private UsernamePasswordAuthenticationToken authentication;
    private NoteDetailResponse detailResponse;

    @BeforeEach
    void setUp() {
        CampusOneUserPrincipal principal = new CampusOneUserPrincipal(
                USER_ID,
                "student@example.com",
                "$2a$12$encoded-password",
                AccountStatus.ACTIVE,
                Set.of("STUDENT"));
        authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities());
        when(adminAuthorizationService.canManage(
                USER_ID,
                "student@example.com"))
                .thenReturn(true);
        detailResponse = detailResponse();
    }

    @Test
    void listPublicNotes_withoutAuthentication_isPublic() throws Exception {
        when(noteService.listPublicNotes(
                null,
                null,
                null,
                null,
                0,
                20,
                com.campusone.note.dto.request.NoteSort.NEWEST))
                .thenReturn(new NotePageResponse(
                        List.of(),
                        0,
                        20,
                        0,
                        0,
                        true,
                        true));

        mockMvc.perform(get("/api/v1/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listPublicNotes_withQueryPassesSearchFiltersToService()
            throws Exception {
        when(noteService.listPublicNotes(
                null,
                "CSC275",
                "Machine Learning",
                "midterm",
                0,
                20,
                com.campusone.note.dto.request.NoteSort.RELEVANCE))
                .thenReturn(new NotePageResponse(
                        List.of(),
                        0,
                        20,
                        0,
                        0,
                        true,
                        true));

        mockMvc.perform(get("/api/v1/notes")
                        .param("q", "Machine Learning")
                        .param("course", "CSC275")
                        .param("tag", "midterm")
                        .param("sort", "RELEVANCE"))
                .andExpect(status().isOk());

        verify(noteService).listPublicNotes(
                null,
                "CSC275",
                "Machine Learning",
                "midterm",
                0,
                20,
                com.campusone.note.dto.request.NoteSort.RELEVANCE);
    }

    @Test
    void getNote_withoutAuthentication_passesAnonymousViewer() throws Exception {
        when(noteService.getNote(NOTE_ID, null)).thenReturn(detailResponse);

        mockMvc.perform(get("/api/v1/notes/{noteId}", NOTE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NOTE_ID.toString()));

        verify(noteService).getNote(NOTE_ID, null);
    }

    @Test
    void myNotes_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notes/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void createNote_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void createNote_authenticatedUserValidRequest_returnsCreated() throws Exception {
        when(noteService.createNote(eq(USER_ID), any(CreateNoteRequest.class)))
                .thenReturn(detailResponse);

        mockMvc.perform(post("/api/v1/notes")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/notes/" + NOTE_ID))
                .andExpect(jsonPath("$.moderationStatus").value("PENDING"));
    }

    @Test
    void createNote_normalUserSubmitsForReview_returnsCreated() throws Exception {
        when(noteService.createNote(eq(USER_ID), any(CreateNoteRequest.class)))
                .thenReturn(detailResponse);

        mockMvc.perform(post("/api/v1/notes")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moderationStatus").value("PENDING"));
    }

    @Test
    void createNote_invalidRequest_returnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/notes")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseId": "%s",
                                  "title": "Bad",
                                  "description": "short",
                                  "teacherName": "A",
                                  "semester": 9,
                                  "fileType": "PDF",
                                  "tags": ["A"],
                                  "file": {
                                    "storageProvider": "MINIO",
                                    "bucketName": "notes",
                                    "objectKey": "notes/test.pdf",
                                    "originalFilename": "test.pdf",
                                    "mimeType": "application/pdf",
                                    "sizeBytes": 0
                                  }
                                }
                                """.formatted(COURSE_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.semester").exists())
                .andExpect(jsonPath("$.fieldErrors['file.sizeBytes']").exists());

        verify(noteService, never())
                .createNote(eq(USER_ID), any(CreateNoteRequest.class));
    }

    @Test
    void uploadNote_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/v1/notes/upload")
                        .file(validUploadRequestPart())
                        .file(validPdfPart()))
                .andExpect(status().isUnauthorized());

        verify(noteUploadService, never()).uploadAndCreate(
                eq(USER_ID),
                any(CreateUploadedNoteRequest.class),
                any());
    }

    @Test
    void uploadNote_authenticatedUserMultipartRequest_returnsCreated()
            throws Exception {
        when(noteUploadService.uploadAndCreate(
                eq(USER_ID),
                any(CreateUploadedNoteRequest.class),
                any()))
                .thenReturn(detailResponse);

        mockMvc.perform(multipart("/api/v1/notes/upload")
                        .file(validUploadRequestPart())
                        .file(validPdfPart())
                        .with(authentication(authentication)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/notes/" + NOTE_ID))
                .andExpect(jsonPath("$.file.originalFilename")
                        .value("oop-notes.pdf"));
    }

    @Test
    void uploadNote_normalUserSubmitsForReview_returnsCreated() throws Exception {
        when(noteUploadService.uploadAndCreate(
                eq(USER_ID),
                any(CreateUploadedNoteRequest.class),
                any()))
                .thenReturn(detailResponse);

        mockMvc.perform(multipart("/api/v1/notes/upload")
                        .file(validUploadRequestPart())
                        .file(validPdfPart())
                        .with(authentication(authentication)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moderationStatus").value("PENDING"));
    }

    @Test
    void uploadNote_storageNotConfigured_returnsCleanServiceUnavailable()
            throws Exception {
        when(noteUploadService.uploadAndCreate(
                eq(USER_ID),
                any(CreateUploadedNoteRequest.class),
                any()))
                .thenThrow(new StorageNotConfiguredException());

        mockMvc.perform(multipart("/api/v1/notes/upload")
                        .file(validUploadRequestPart())
                        .file(validPdfPart())
                        .with(authentication(authentication)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code")
                        .value("STORAGE_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.message")
                        .value("Storage is temporarily unavailable."));
    }

    @Test
    void uploadNote_dailyLimitReached_returnsCleanRateLimitError()
            throws Exception {
        when(noteUploadService.uploadAndCreate(
                eq(USER_ID),
                any(CreateUploadedNoteRequest.class),
                any()))
                .thenThrow(new UploadLimitExceededException(
                        "Daily upload limit reached."));

        mockMvc.perform(multipart("/api/v1/notes/upload")
                        .file(validUploadRequestPart())
                        .file(validPdfPart())
                        .with(authentication(authentication)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code")
                        .value("UPLOAD_LIMIT_REACHED"))
                .andExpect(jsonPath("$.message")
                        .value("Daily upload limit reached."));
    }

    @Test
    void updateNote_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/notes/{noteId}", NOTE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated OOP Notes\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateNote_normalUser_isForbidden() throws Exception {
        rejectNoteManagement();

        mockMvc.perform(patch("/api/v1/notes/{noteId}", NOTE_ID)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated OOP Notes\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Only admins can edit or delete notes after submission."));

        verify(noteService, never()).updateNoteAsAdmin(
                eq(USER_ID),
                eq(NOTE_ID),
                any(UpdateNoteRequest.class));
    }

    @Test
    void updateNote_admin_canManageAnyNote() throws Exception {
        when(noteService.updateNoteAsAdmin(
                eq(USER_ID),
                eq(NOTE_ID),
                any(UpdateNoteRequest.class)))
                .thenReturn(detailResponse);

        mockMvc.perform(patch("/api/v1/notes/{noteId}", NOTE_ID)
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated OOP Notes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NOTE_ID.toString()));
    }

    @Test
    void deleteNote_normalUser_isForbidden() throws Exception {
        rejectNoteManagement();

        mockMvc.perform(delete("/api/v1/notes/{noteId}", NOTE_ID)
                        .with(authentication(authentication)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Only admins can edit or delete notes after submission."));

        verify(noteService, never()).deleteNoteAsAdmin(NOTE_ID);
    }

    @Test
    void deleteNote_admin_canManageAnyNote() throws Exception {
        mockMvc.perform(delete("/api/v1/notes/{noteId}", NOTE_ID)
                        .with(authentication(authentication)))
                .andExpect(status().isNoContent());

        verify(noteService).deleteNoteAsAdmin(NOTE_ID);
    }

    @Test
    void managementStatus_withoutAuthentication_isUnauthorized()
            throws Exception {
        mockMvc.perform(get("/api/v1/notes/management-status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void managementStatus_admin_returnsTrue() throws Exception {
        when(adminAuthorizationService.canManage(
                USER_ID,
                "student@example.com"))
                .thenReturn(true);

        mockMvc.perform(get("/api/v1/notes/management-status")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canManage").value(true));
    }

    private void rejectNoteManagement() {
        when(adminAuthorizationService.canManage(
                USER_ID,
                "student@example.com"))
                .thenReturn(false);
        doThrow(new NoteManagementAccessDeniedException())
                .when(adminAuthorizationService)
                .requireAdmin(USER_ID, "student@example.com");
    }

    private String validCreateJson() {
        return """
                {
                  "courseId": "%s",
                  "title": "Complete OOP Notes",
                  "description": "Detailed object oriented programming lecture notes.",
                  "teacherName": "Dr. Ahmed Khan",
                  "semester": 4,
                  "fileType": "PDF",
                  "visibility": "PUBLIC",
                  "tags": ["OOP", "Java"],
                  "file": {
                    "storageProvider": "MINIO",
                    "bucketName": "campusone-notes",
                    "objectKey": "notes/oop.pdf",
                    "originalFilename": "oop-notes.pdf",
                    "mimeType": "application/pdf",
                    "sizeBytes": 4096,
                    "checksumSha256": "%s"
                  }
                }
                """.formatted(COURSE_ID, "a".repeat(64));
    }

    private MockMultipartFile validUploadRequestPart() {
        String json = """
                {
                  "courseId": "%s",
                  "title": "Complete OOP Notes",
                  "description": "Detailed object oriented programming lecture notes.",
                  "teacherName": "Dr. Ahmed Khan",
                  "semester": 4,
                  "fileType": "PDF",
                  "visibility": "PUBLIC",
                  "tags": ["OOP", "Java"]
                }
                """.formatted(COURSE_ID);
        return new MockMultipartFile(
                "note",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private MockMultipartFile validPdfPart() {
        return new MockMultipartFile(
                "file",
                "oop-notes.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.7\nCampusOne test PDF".getBytes(
                        java.nio.charset.StandardCharsets.US_ASCII));
    }

    private NoteDetailResponse detailResponse() {
        return new NoteDetailResponse(
                NOTE_ID,
                "Complete OOP Notes",
                "Detailed object oriented programming lecture notes.",
                "Dr. Ahmed Khan",
                new CourseResponse(
                        COURSE_ID,
                        DEPARTMENT_ID,
                        "CS-201",
                        "Object Oriented Programming",
                        4,
                        true),
                4,
                NoteFileType.PDF,
                NoteVisibility.PUBLIC,
                NoteModerationStatus.PENDING,
                null,
                new NoteUploaderResponse(
                        USER_ID,
                        "Ali Khan",
                        null,
                        "COMSATS University Islamabad"),
                new FileMetadataResponse(
                        UUID.fromString(
                                "50000000-0000-4000-8000-000000000001"),
                        StorageProvider.MINIO,
                        "oop-notes.pdf",
                        "application/pdf",
                        4096,
                        FileAssetStatus.PENDING,
                        NOW),
                List.of(),
                0,
                new BigDecimal("0.00"),
                0,
                false,
                null,
                1,
                NOW,
                NOW,
                null);
    }
}
