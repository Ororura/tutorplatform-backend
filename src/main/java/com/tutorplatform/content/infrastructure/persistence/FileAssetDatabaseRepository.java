package com.tutorplatform.content.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FileAssetDatabaseRepository extends JpaRepository<FileAssetDatabaseModel, UUID> {}
