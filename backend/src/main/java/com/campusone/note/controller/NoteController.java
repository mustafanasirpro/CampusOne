package com.campusone.note.controller;

import com.campusone.note.dto.request.CreateNoteRequest;
import com.campusone.note.dto.request.CreateUploadedNoteRequest;
import com.campusone.note.dto.request.NoteSort;
import com.campusone.note.dto.request.RateNoteRequest;
import com.campusone.note.dto.request.UpdateNoteRequest;
import com.campusone.note.dto.response.BookmarkStateResponse;
import com.campusone.note.dto.response.DownloadEventResponse;
import com.campusone.note.dto.response.NoteDetailResponse;
import com.campusone.note.dto.response.NoteManagementStatusResponse;
import com.campusone.note.dto.response.NotePageResponse;
import com.campusone.note.dto.response.RatingResponse;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.note.service.NoteService;
import com.campusone.note.service.NoteUploadService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/notes")
@Validated
@Tag(name = "Notes")
public class NoteController {

    private final NoteService noteService;
    private final NoteUploadService noteUploadService;
    private final NoteAdminAuthorizationService adminAuthorizationService;

    public NoteController(
            NoteService noteService,
            NoteUploadService noteUploadService,
            NoteAdminAuthorizationService adminAuthorizationService) {
        this.noteService = noteService;
        this.noteUploadService = noteUploadService;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @PostMapping
    @Operation(
            summary = "Create a note from existing file metadata (admin only)",
            description = "Admin-only deprecated compatibility endpoint. "
                    + "Use /api/v1/notes/upload for real PDF uploads.",
            deprecated = true)
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<NoteDetailResponse> createNote(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody CreateNoteRequest request) {
        requireAdmin(principal);
        NoteDetailResponse response =
                noteService.createNote(principal.getUserId(), request);
        return ResponseEntity.created(
                        URI.create("/api/v1/notes/" + response.id()))
                .body(response);
    }

    @PostMapping(
            path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Create a note and upload its PDF (admin only)",
            description = "Admin-only multipart endpoint. Accepts a JSON "
                    + "`note` part and an application/pdf "
                    + "`file` part. CampusOne validates and stores the PDF in "
                    + "configured S3-compatible storage.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<NoteDetailResponse> uploadNote(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestPart("note") CreateUploadedNoteRequest request,
            @RequestPart("file") MultipartFile file) {
        requireAdmin(principal);
        NoteDetailResponse response = noteUploadService.uploadAndCreate(
                principal.getUserId(),
                request,
                file);
        return ResponseEntity.created(
                        URI.create("/api/v1/notes/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List approved public notes")
    public ResponseEntity<NotePageResponse> listPublicNotes(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) @Size(min = 2, max = 40) String tag,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "NEWEST") NoteSort sort) {
        return ResponseEntity.ok(
                noteService.listPublicNotes(courseId, tag, page, size, sort));
    }

    @GetMapping("/my")
    @Operation(summary = "List notes uploaded by the authenticated account")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<NotePageResponse> listMyNotes(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "NEWEST") NoteSort sort) {
        return ResponseEntity.ok(
                noteService.listMyNotes(
                        principal.getUserId(),
                        page,
                        size,
                        sort));
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "Get a public note or an owned pending note")
    public ResponseEntity<NoteDetailResponse> getNote(
            @PathVariable UUID noteId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        UUID viewerUserId = principal == null ? null : principal.getUserId();
        if (principal != null && canManage(principal)) {
            return ResponseEntity.ok(
                    noteService.getNoteAsAdmin(noteId, viewerUserId));
        }
        return ResponseEntity.ok(noteService.getNote(noteId, viewerUserId));
    }

    @GetMapping("/management-status")
    @Operation(summary = "Check whether the current user can manage notes")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<NoteManagementStatusResponse> getManagementStatus(
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(new NoteManagementStatusResponse(
                canManage(principal)));
    }

    @PatchMapping("/{noteId}")
    @Operation(summary = "Update a note (admin only)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<NoteDetailResponse> updateNote(
            @PathVariable UUID noteId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody UpdateNoteRequest request) {
        requireAdmin(principal);
        return ResponseEntity.ok(
                noteService.updateNoteAsAdmin(
                        principal.getUserId(),
                        noteId,
                        request));
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "Soft-delete a note (admin only)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteNote(
            @PathVariable UUID noteId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        requireAdmin(principal);
        noteService.deleteNoteAsAdmin(noteId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{noteId}/bookmark")
    @Operation(summary = "Bookmark a note")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BookmarkStateResponse> bookmark(
            @PathVariable UUID noteId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(
                noteService.bookmark(principal.getUserId(), noteId));
    }

    @DeleteMapping("/{noteId}/bookmark")
    @Operation(summary = "Remove a note bookmark")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BookmarkStateResponse> unbookmark(
            @PathVariable UUID noteId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(
                noteService.unbookmark(principal.getUserId(), noteId));
    }

    @PutMapping("/{noteId}/rating")
    @Operation(summary = "Create or replace the current student's note rating")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<RatingResponse> rate(
            @PathVariable UUID noteId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody RateNoteRequest request) {
        return ResponseEntity.ok(
                noteService.rate(
                        principal.getUserId(),
                        noteId,
                        request.rating()));
    }

    @PostMapping("/{noteId}/download-events")
    @Operation(summary = "Record a note download event")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<DownloadEventResponse> recordDownload(
            @PathVariable UUID noteId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            HttpServletRequest request) {
        DownloadEventResponse response = noteService.recordDownload(
                principal.getUserId(),
                noteId,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"));
        return ResponseEntity.created(
                        URI.create("/api/v1/notes/"
                                + noteId
                                + "/download-events/"
                                + response.eventId()))
                .body(response);
    }

    private boolean canManage(CampusOneUserPrincipal principal) {
        return adminAuthorizationService.canManage(
                principal.getUserId(),
                principal.getUsername());
    }

    private void requireAdmin(CampusOneUserPrincipal principal) {
        adminAuthorizationService.requireAdmin(
                principal.getUserId(),
                principal.getUsername());
    }
}
