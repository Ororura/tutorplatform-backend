package com.tutorplatform.content.infrastructure.persistence;

import com.tutorplatform.content.domain.FileAssetEntity;
import com.tutorplatform.content.domain.FileAssetRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaFileAssetRepository implements FileAssetRepository {

    private final FileAssetDatabaseRepository databaseRepository;

    JpaFileAssetRepository(FileAssetDatabaseRepository databaseRepository) {
        this.databaseRepository = databaseRepository;
    }

    @Override
    public FileAssetEntity save(FileAssetEntity fileAsset) {
        return databaseRepository.save(new FileAssetDatabaseModel(fileAsset)).toEntity();
    }

    @Override
    public FileAssetEntity saveAndFlush(FileAssetEntity fileAsset) {
        return databaseRepository.saveAndFlush(new FileAssetDatabaseModel(fileAsset)).toEntity();
    }

    @Override
    public Optional<FileAssetEntity> findById(UUID fileAssetId) {
        return databaseRepository.findById(fileAssetId).map(FileAssetDatabaseModel::toEntity);
    }
}
