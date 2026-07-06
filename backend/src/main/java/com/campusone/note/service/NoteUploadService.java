package com.campusone.note.service;

import com.campusone.note.dto.request.CreateUploadedNoteRequest;
import com.campusone.note.dto.response.NoteDetailResponse;
import com.campusone.note.storage.NoteFileValidator;
import com.campusone.note.storage.StorageService;
import com.campusone.note.storage.StoredObject;
import com.campusone.note.storage.ValidatedNoteFile;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class NoteUploadService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(NoteUploadService.class);

    private final NoteFileValidator fileValidator;
    private final UploadQuotaService uploadQuotaService;
    private final StorageService storageService;
    private final NoteService noteService;

    public NoteUploadService(
            NoteFileValidator fileValidator,
            UploadQuotaService uploadQuotaService,
            StorageService storageService,
            NoteService noteService) {
        this.fileValidator = fileValidator;
        this.uploadQuotaService = uploadQuotaService;
        this.storageService = storageService;
        this.noteService = noteService;
    }

    @Transactional
    public NoteDetailResponse uploadAndCreate(
            UUID userId,
            CreateUploadedNoteRequest request,
            MultipartFile multipartFile) {
        ValidatedNoteFile file = fileValidator.validate(
                multipartFile,
                request.fileType());
        uploadQuotaService.enforce(userId, file.sizeBytes());
        StoredObject storedObject = storageService.upload(userId, file);
        AtomicBoolean objectDeleted = new AtomicBoolean();
        registerRollbackCleanup(storedObject, objectDeleted);
        try {
            return noteService.createUploadedNote(
                    userId,
                    request,
                    storedObject);
        } catch (RuntimeException exception) {
            deleteUploadedObject(storedObject, objectDeleted, exception);
            throw exception;
        }
    }

    private void registerRollbackCleanup(
            StoredObject storedObject,
            AtomicBoolean objectDeleted) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            deleteUploadedObject(
                                    storedObject,
                                    objectDeleted,
                                    null);
                        }
                    }
                });
    }

    private void deleteUploadedObject(
            StoredObject storedObject,
            AtomicBoolean objectDeleted,
            RuntimeException originalException) {
        if (!objectDeleted.compareAndSet(false, true)) {
            return;
        }
        try {
            storageService.delete(storedObject);
        } catch (RuntimeException cleanupException) {
            objectDeleted.set(false);
            if (originalException != null) {
                originalException.addSuppressed(cleanupException);
            }
            LOGGER.error(
                    "Could not clean up uploaded object {} after note creation failed.",
                    storedObject.objectKey(),
                    cleanupException);
        }
    }
}
