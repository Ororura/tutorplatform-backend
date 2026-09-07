package com.tutorplatform.content.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface FileAssetDatabaseRepository extends JpaRepository<FileAssetDatabaseModel, UUID> {
}
