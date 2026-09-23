package com.tutorplatform.file.infrastructure;

import com.tutorplatform.file.application.FileStorage;
import com.tutorplatform.file.application.FileStorageException;

/** One Spring FileStorage bean; preserve reads of LOCAL files after switching uploads to S3. */
public final class RoutingFileStorage implements FileStorage {
    private final FileStorage local;
    private final FileStorage s3;
    private final FileStorage active;

    public RoutingFileStorage(FileStorage local, FileStorage s3, String writeProvider) {
        this.local = local;
        this.s3 = s3;
        if ("LOCAL".equals(writeProvider)) {
            active = local;
        } else if ("S3".equals(writeProvider) && s3 != null) {
            active = s3;
        } else {
            throw new IllegalArgumentException("Unsupported or unconfigured file storage provider");
        }
    }

    @Override
    public StoredObject store(byte[] content) {
        return active.store(content);
    }

    @Override
    public byte[] read(String key, long expectedSize) {
        return active.read(key, expectedSize);
    }

    @Override
    public byte[] read(String provider, String key, long expectedSize) {
        if ("LOCAL".equals(provider)) {
            return local.read(key, expectedSize);
        }
        if ("S3".equals(provider) && s3 != null) {
            return s3.read(key, expectedSize);
        }
        throw new FileStorageException(
                new IllegalStateException("Stored file provider is not available"));
    }

    @Override
    public void delete(String key) {
        active.delete(key);
    }

    @Override
    public void delete(String provider, String key) {
        if ("LOCAL".equals(provider)) {
            local.delete(key);
        } else if ("S3".equals(provider) && s3 != null) {
            s3.delete(key);
        } else {
            throw new FileStorageException(
                    new IllegalStateException("Stored file provider is not available"));
        }
    }
}
