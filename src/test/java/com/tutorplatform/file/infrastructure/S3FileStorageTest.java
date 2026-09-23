package com.tutorplatform.file.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tutorplatform.file.application.FileStorageException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3FileStorageTest {
    @Test
    void putGetDeleteAndValidateOpaqueKeys() {
        S3Client client = mock(S3Client.class);
        S3FileStorage storage = new S3FileStorage(client, "demo-files");
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        var object = storage.store(content);
        assertThat(object.provider()).isEqualTo("S3");
        assertThat(object.key())
                .matches("materials/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        when(client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(
                        ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), content));
        assertThat(storage.read(object.key(), 5)).containsExactly(content);
        assertThatThrownBy(() -> storage.read(object.key(), 4))
                .isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> storage.read("../backups/private", 5))
                .isInstanceOf(FileStorageException.class);
        storage.delete("S3", object.key());
        verify(client).deleteObject(any(DeleteObjectRequest.class));
    }
}
