package com.campusone.note.repository;

import com.campusone.note.entity.FileAsset;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileAssetRepository extends JpaRepository<FileAsset, UUID> {
}
