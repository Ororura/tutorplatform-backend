package com.tutorplatform.file.infrastructure;

import com.tutorplatform.file.application.FileStorage;
import com.tutorplatform.file.application.FileStorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.file-storage",
        name = "provider",
        havingValue = "LOCAL",
        matchIfMissing = true)
public class LocalFileStorage implements FileStorage {
    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);
    private final Path directory;

    public LocalFileStorage(@Value("${app.file-storage.directory:./var/files}") String directory) {
        if (directory == null || directory.isBlank()) {
            throw new IllegalArgumentException(
                    "app.file-storage.directory must not be blank for LOCAL storage");
        }
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public StoredObject store(byte[] content) {
        String key = UUID.randomUUID().toString();
        Path target = resolve(key);
        boolean created = false;
        try {
            Files.createDirectories(directory);
            // CREATE_NEW prevents overwrites, including existing symlinks.
            try (var output =
                    Files.newOutputStream(
                            target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                created = true;
                output.write(content);
            }
            return new StoredObject("LOCAL", key);
        } catch (IOException exception) {
            if (created) {
                try {
                    Files.deleteIfExists(target);
                } catch (IOException cleanup) {
                    exception.addSuppressed(cleanup);
                    log.error("FILE_STORAGE_CLEANUP_REQUIRED key={}", key, cleanup);
                }
            }
            throw new FileStorageException(exception);
        }
    }

    @Override
    public byte[] read(String key, long expectedSize) {
        if (expectedSize < 0 || expectedSize >= Integer.MAX_VALUE) {
            throw new FileStorageException(new IllegalArgumentException("Invalid object size"));
        }
        try (var input = Files.newInputStream(resolve(key), LinkOption.NOFOLLOW_LINKS)) {
            byte[] content = input.readNBytes((int) expectedSize + 1);
            if (content.length != expectedSize) throw new IOException("Object size mismatch");
            return content;
        } catch (IOException exception) {
            throw new FileStorageException(exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            throw new FileStorageException(exception);
        }
    }

    private Path resolve(String key) {
        if (key == null
                || !key.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new FileStorageException(new IllegalArgumentException("Invalid storage key"));
        }
        Path resolved = directory.resolve(key).normalize();
        if (!resolved.startsWith(directory)) {
            throw new FileStorageException(
                    new IllegalArgumentException("Storage key escapes directory"));
        }
        return resolved;
    }
}
