package com.campusone.note.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.note.dto.request.CreateUploadedNoteRequest;
import com.campusone.note.entity.NoteFileType;
import com.campusone.note.entity.NoteVisibility;
import com.campusone.note.storage.NoteFileValidator;
import com.campusone.note.storage.StorageService;
import com.campusone.note.storage.StoredObject;
import com.campusone.note.storage.ValidatedNoteFile;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class NoteUploadServiceTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");

    @Mock
    private NoteFileValidator fileValidator;

    @Mock
    private StorageService storageService;

    @Mock
    private UploadQuotaService uploadQuotaService;

    @Mock
    private NoteService noteService;

    @Test
    void uploadAndCreate_databaseFailure_deletesUploadedObject() {
        byte[] content =
                "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "notes.pdf",
                "application/pdf",
                content);
        CreateUploadedNoteRequest request = request();
        ValidatedNoteFile file = new ValidatedNoteFile(
                "notes.pdf",
                "application/pdf",
                content,
                "a".repeat(64));
        StoredObject storedObject = new StoredObject(
                com.campusone.note.entity.StorageProvider.S3_COMPATIBLE,
                "campusone-notes",
                "notes/user/2026/notes.pdf",
                "notes.pdf",
                "application/pdf",
                multipartFile.getSize(),
                "a".repeat(64));
        when(fileValidator.validate(multipartFile, NoteFileType.PDF))
                .thenReturn(file);
        when(storageService.upload(USER_ID, file)).thenReturn(storedObject);
        when(noteService.createUploadedNote(USER_ID, request, storedObject))
                .thenThrow(new IllegalStateException("Database unavailable"));
        NoteUploadService uploadService = new NoteUploadService(
                fileValidator,
                uploadQuotaService,
                storageService,
                noteService);

        assertThatThrownBy(() -> uploadService.uploadAndCreate(
                USER_ID,
                request,
                multipartFile))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database unavailable");
        verify(uploadQuotaService).enforce(USER_ID, file.sizeBytes());
        verify(storageService).delete(storedObject);
    }

    private CreateUploadedNoteRequest request() {
        return new CreateUploadedNoteRequest(
                UUID.fromString("20000000-0000-4000-8000-000000000001"),
                "Complete OOP Notes",
                "Detailed object oriented programming lecture notes.",
                "Dr. Ahmed Khan",
                4,
                NoteFileType.PDF,
                NoteVisibility.PUBLIC,
                List.of("OOP"));
    }
}
