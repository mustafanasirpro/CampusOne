package com.campusone.note.mapper;

import com.campusone.note.dto.response.FileMetadataResponse;
import com.campusone.note.entity.FileAsset;
import org.springframework.stereotype.Component;

@Component
public class FileAssetMapper {

    public FileMetadataResponse toResponse(FileAsset fileAsset) {
        return new FileMetadataResponse(
                fileAsset.getId(),
                fileAsset.getStorageProvider(),
                fileAsset.getOriginalFilename(),
                fileAsset.getMimeType(),
                fileAsset.getSizeBytes(),
                fileAsset.getStatus(),
                fileAsset.getCreatedAt());
    }
}
