package com.campusone.note.storage;

import com.campusone.note.entity.FileAsset;
import java.util.UUID;

public interface StorageService extends AutoCloseable {

    StoredObject upload(UUID ownerId, ValidatedNoteFile file);

    String createDownloadUrl(FileAsset fileAsset);

    void delete(StoredObject storedObject);

    @Override
    default void close() {
    }
}
