package com.tutorplatform.content.domain;

import java.util.Optional;
import java.util.UUID;

public interface FileAssetRepository {
    FileAssetEntity save(FileAssetEntity fileAsset);

    FileAssetEntity saveAndFlush(FileAssetEntity fileAsset);

    Optional<FileAssetEntity> findById(UUID fileAssetId);
}
