package com.tutorplatform.file.infrastructure;

import com.tutorplatform.file.application.FileStorage;
import com.tutorplatform.file.application.FileStorageException;
import java.util.UUID;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public final class S3FileStorage implements FileStorage {
    private static final String KEY_PATTERN =
            "materials/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private final S3Client client;
    private final String bucket;

    public S3FileStorage(S3Client client, String bucket) {
        if (client == null || bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("S3 client and bucket are required");
        }
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public StoredObject store(byte[] content) {
        String key = "materials/" + UUID.randomUUID();
        try {
            client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key).build(),
                    RequestBody.fromBytes(content));
            return new StoredObject("S3", key);
        } catch (SdkException exception) {
            throw new FileStorageException(exception);
        }
    }

    @Override
    public byte[] read(String key, long expectedSize) {
        validateKey(key);
        if (expectedSize < 0 || expectedSize >= Integer.MAX_VALUE) {
            throw new FileStorageException(new IllegalArgumentException("Invalid object size"));
        }
        try {
            byte[] content =
                    client.getObjectAsBytes(
                                    GetObjectRequest.builder().bucket(bucket).key(key).build())
                            .asByteArray();
            if (content.length != expectedSize) {
                throw new FileStorageException(new IllegalStateException("Object size mismatch"));
            }
            return content;
        } catch (SdkException exception) {
            throw new FileStorageException(exception);
        }
    }

    @Override
    public byte[] read(String provider, String key, long expectedSize) {
        if (!"S3".equals(provider)) {
            throw new FileStorageException(new IllegalArgumentException("Wrong storage provider"));
        }
        return read(key, expectedSize);
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        try {
            // S3 DELETE is idempotent for an object that is already absent.
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException exception) {
            throw new FileStorageException(exception);
        }
    }

    private static void validateKey(String key) {
        if (key == null || !key.matches(KEY_PATTERN)) {
            throw new FileStorageException(new IllegalArgumentException("Invalid storage key"));
        }
    }
}
