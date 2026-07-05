package com.campusone.note.storage;

import com.campusone.common.exception.StorageNotConfiguredException;
import com.campusone.note.entity.FileAsset;
import java.util.UUID;

public class DisabledStorageService implements StorageService {

    @Override
    public StoredObject upload(UUID ownerId, ValidatedNoteFile file) {
        throw new StorageNotConfiguredException();
    }

    @Override
    public String createDownloadUrl(FileAsset fileAsset) {
        throw new StorageNotConfiguredException();
    }

    @Override
    public void delete(StoredObject storedObject) {
        // No remote object can exist while storage is disabled.
    }
}
