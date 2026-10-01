package com.tutorplatform.file.api;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Builds download headers from untrusted display metadata, including historical asset rows. */
public final class FileDownloadResponse {
    private FileDownloadResponse() {}

    public static ResponseEntity<byte[]> create(
            String originalFilename, String mimeType, boolean inlineImage, byte[] content) {
        MediaType mediaType = safeMediaType(mimeType);
        // Only the supported image formats may render inline, even for historical metadata.
        boolean inline =
                inlineImage
                        && (MediaType.IMAGE_PNG.equals(mediaType)
                                || MediaType.IMAGE_JPEG.equals(mediaType));
        ContentDisposition.Builder disposition =
                inline ? ContentDisposition.inline() : ContentDisposition.attachment();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(content.length)
                .header(
                        "Content-Disposition",
                        disposition
                                .filename(safeFilename(originalFilename), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "no-store")
                .body(content);
    }

    private static String safeFilename(String filename) {
        if (filename == null) return "file";
        String basename = filename.replace('\\', '/');
        basename = basename.substring(basename.lastIndexOf('/') + 1);
        StringBuilder safe = new StringBuilder();
        basename.codePoints()
                .limit(255)
                .forEach(
                        c ->
                                safe.appendCodePoint(
                                        Character.isISOControl(c)
                                                        || Character.getType(c) == Character.FORMAT
                                                ? '_'
                                                : c));
        String result = safe.toString().strip();
        return result.isBlank() || result.equals(".") || result.equals("..") ? "file" : result;
    }

    private static MediaType safeMediaType(String mimeType) {
        if (mimeType == null || mimeType.codePoints().anyMatch(Character::isISOControl)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            MediaType parsed = MediaType.parseMediaType(mimeType);
            return parsed.isWildcardType() || parsed.isWildcardSubtype()
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : parsed;
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
