package com.tutorplatform.file.application;

/**
 * Opaque keys only; callers authorize access before reading. No filesystem locations cross this port.
 */
public interface FileStorage {
    StoredObject store(byte[] content);

    byte[] read(String key, long expectedSize);

    /**
     * Used to compensate a failed metadata transaction. Must be idempotent.
     */
    void delete(String key);

    record StoredObject(String provider, String key) {
    }
}
