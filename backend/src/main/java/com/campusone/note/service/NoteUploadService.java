package com.campusone.note.service;

import com.campusone.note.dto.request.CreateUploadedNoteRequest;
import com.campusone.note.dto.response.NoteDetailResponse;
import com.campusone.note.storage.NoteFileValidator;
import com.campusone.note.storage.StorageService;
import com.campusone.note.storage.StoredObject;
import com.campusone.note.storage.ValidatedNoteFile;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class NoteUploadService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(NoteUploadService.class);

    private final NoteFileValidator fileValidator;
    private final StorageService storageService;
    private final NoteService noteService;

    public NoteUploadService(
            NoteFileValidator fileValidator,
            StorageService storageService,
            NoteService noteService) {
        this.fileValidator = fileValidator;
        this.storageService = storageService;
        this.noteService = noteService;
    }

    public NoteDetailResponse uploadAndCreate(
            UUID userId,
            CreateUploadedNoteRequest request,
            MultipartFile multipartFile) {
        ValidatedNoteFile file = fileValidator.validate(
                multipartFile,
                request.fileType());
        StoredObject storedObject = storageService.upload(userId, file);
        try {
            return noteService.createUploadedNote(
                    userId,
                    request,
                    storedObject);
        } catch (RuntimeException exception) {
            try {
                storageService.delete(storedObject);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
                LOGGER.error(
                        "Could not clean up uploaded object {} after note creation failed.",
                        storedObject.objectKey(),
                        cleanupException);
            }
            throw exception;
        }
    }
}
