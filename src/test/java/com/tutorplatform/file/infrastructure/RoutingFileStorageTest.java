package com.tutorplatform.file.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.tutorplatform.file.application.FileStorage;
import com.tutorplatform.file.application.FileStorageException;
import org.junit.jupiter.api.Test;

class RoutingFileStorageTest {
    @Test
    void writesToS3AndReadsUsingRecordedProvider() {
        FileStorage local = mock(FileStorage.class);
        FileStorage s3 = mock(FileStorage.class);
        FileStorage storage = new RoutingFileStorage(local, s3, "S3");
        byte[] content = {1, 2, 3};
        when(s3.store(content)).thenReturn(new FileStorage.StoredObject("S3", "materials/id"));
        when(local.read("old-key", 3)).thenReturn(content);
        when(s3.read("materials/id", 3)).thenReturn(content);

        assertThat(storage.store(content).provider()).isEqualTo("S3");
        assertThat(storage.read("LOCAL", "old-key", 3)).containsExactly(1, 2, 3);
        assertThat(storage.read("S3", "materials/id", 3)).containsExactly(1, 2, 3);
        storage.delete("materials/id");
        verify(s3).delete("materials/id");
        verify(local, never()).delete("materials/id");
    }

    @Test
    void localModeAndUnknownProvider() {
        FileStorage local = mock(FileStorage.class);
        FileStorage storage = new RoutingFileStorage(local, null, "LOCAL");
        byte[] content = {9};
        when(local.store(content)).thenReturn(new FileStorage.StoredObject("LOCAL", "old"));
        assertThat(storage.store(content).provider()).isEqualTo("LOCAL");
        assertThatThrownBy(() -> storage.read("S3", "materials/id", 1))
                .isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> new RoutingFileStorage(local, null, "S3"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
