package com.tutorplatform.file.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class FileDownloadResponseTest {
    @Test
    void preservesUnicodeQuotesAndBytesWithSafeAttachmentHeaders() {
        String name = "Конспект \"урок\"; №1.txt";
        byte[] content = "lesson".getBytes(StandardCharsets.UTF_8);
        var response = FileDownloadResponse.create(name, "text/plain", false, content);
        String header = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        var disposition = ContentDisposition.parse(header);
        assertThat(disposition.getType()).isEqualTo("attachment");
        assertThat(disposition.getFilename()).isEqualTo(name);
        assertThat(header).contains("filename*=UTF-8''").doesNotContain("\r", "\n");
        assertThat(response.getBody()).containsExactly(content);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(content.length);
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test
    void sanitizesLegacyTraversalControlsAndBidiInFilename() {
        var response =
                FileDownloadResponse.create(
                        "..\\private/../lesson\r\nX-Injected: yes\u0000\u202E.txt",
                        "text/plain",
                        false,
                        new byte[] {1});
        String header = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(header).doesNotContain("\r", "\n", "\u0000", "\u202E", "private", "../");
        assertThat(ContentDisposition.parse(header).getFilename())
                .isEqualTo("lesson__X-Injected: yes__.txt");
        assertThat(response.getHeaders().getFirst("X-Injected")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", ".", "..", "../../", "C:\\private\\"})
    void fallsBackForMissingBasename(String filename) {
        var response = FileDownloadResponse.create(filename, "text/plain", false, new byte[] {1});
        assertThat(
                        ContentDisposition.parse(
                                        response.getHeaders()
                                                .getFirst(HttpHeaders.CONTENT_DISPOSITION))
                                .getFilename())
                .isEqualTo("file");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not a mime", "*/*", "image/*", "image/png\r\nX-Injected: yes"})
    void unsafeMimeFallsBackToBinaryAttachment(String mime) {
        var response = FileDownloadResponse.create("lesson.png", mime, true, new byte[] {1});
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(
                        ContentDisposition.parse(
                                        response.getHeaders()
                                                .getFirst(HttpHeaders.CONTENT_DISPOSITION))
                                .getType())
                .isEqualTo("attachment");
        assertThat(response.getHeaders().getFirst("X-Injected")).isNull();
    }

    @Test
    void onlySupportedImagesRenderInlineAndFilenameLengthIsBounded() {
        var image = FileDownloadResponse.create("image.png", "image/png", true, new byte[] {1});
        assertThat(image.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("inline;");
        var html = FileDownloadResponse.create("x".repeat(300), "text/html", true, new byte[] {1});
        var disposition =
                ContentDisposition.parse(
                        html.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertThat(disposition.getType()).isEqualTo("attachment");
        assertThat(disposition.getFilename()).hasSize(255);
    }
}
