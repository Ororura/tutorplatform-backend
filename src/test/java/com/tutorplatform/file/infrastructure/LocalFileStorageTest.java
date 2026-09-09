package com.tutorplatform.file.infrastructure;

import com.tutorplatform.file.application.FileStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {
    @TempDir Path directory;

    @Test void keysCannotTraverseAndDeleteIsIdempotent() throws Exception {
        var storage = new LocalFileStorage(directory.toString());
        var object = storage.store(new byte[]{1, 2});
        assertThat(storage.read(object.key(), 2)).containsExactly(1, 2);
        assertThatThrownBy(() -> storage.read("../outside", 2)).isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.delete("/tmp/outside")).isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.read(object.key(), 1)).isInstanceOf(FileStorageException.class);
        storage.delete(object.key());
        storage.delete(object.key());
        assertThat(Files.exists(directory.resolve(object.key()))).isFalse();
    }

    @Test void refusesSymlinksAndReportsIoErrors() throws Exception {
        var storage = new LocalFileStorage(directory.toString());
        Path outside = Files.createTempFile(directory, "outside", ".txt");
        String key = java.util.UUID.randomUUID().toString();
        Files.createSymbolicLink(directory.resolve(key), outside);
        assertThatThrownBy(() -> storage.read(key, 0)).isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.read(java.util.UUID.randomUUID().toString(), 0))
            .isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> new LocalFileStorage(outside.toString()).store(new byte[]{1}))
            .isInstanceOf(FileStorageException.class);
    }
}
