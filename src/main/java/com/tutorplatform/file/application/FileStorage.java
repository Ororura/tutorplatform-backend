package com.tutorplatform.file.application;

/** Opaque object keys only. The application checks permissions before reading files. */
public interface FileStorage {
    StoredObject store(byte[] content);

    /** Read from the active write provider (retained for existing callers). */
    byte[] read(String key, long expectedSize);

    /** Read a historical object using the provider saved with its database metadata. */
    byte[] read(String provider, String key, long expectedSize);

    /** Compensate a failed metadata transaction; must be idempotent. */
    void delete(String key);

    record StoredObject(String provider, String key) {}
}
