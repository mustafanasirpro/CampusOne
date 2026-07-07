package com.campusone.note.storage;

import com.campusone.note.entity.FileAsset;
import java.util.UUID;

public interface StorageService extends AutoCloseable {

    StoredObject upload(UUID ownerId, ValidatedNoteFile file);

    StoredObject uploadMarketplaceImage(UUID ownerId, ValidatedNoteFile file);

    String createDownloadUrl(FileAsset fileAsset);

    String createObjectUrl(
            com.campusone.note.entity.StorageProvider storageProvider,
            String bucketName,
            String objectKey,
            String mimeType,
            String originalFilename);

    void delete(StoredObject storedObject);

    @Override
    default void close() {
    }
}
