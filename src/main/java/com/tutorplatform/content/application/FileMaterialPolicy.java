package com.tutorplatform.content.application;

import com.tutorplatform.content.application.exception.InvalidLessonMaterialException;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.file.application.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class FileMaterialPolicy {
    private static final Set<String> TEXT_MIME_TYPES = Set.of("text/plain", "application/octet-stream");
    private static final Map<String, Set<String>> FILE_MIME_TYPES = Map.ofEntries(
        Map.entry(".pdf", Set.of("application/pdf")),
        Map.entry(".png", Set.of("image/png")),
        Map.entry(".jpg", Set.of("image/jpeg")),
        Map.entry(".jpeg", Set.of("image/jpeg")),
        Map.entry(".zip", Set.of("application/zip", "application/x-zip-compressed", "application/octet-stream")),
        Map.entry(".txt", TEXT_MIME_TYPES),
        Map.entry(".md", withTextMimes("text/markdown")),
        Map.entry(".csv", withTextMimes("text/csv")),
        Map.entry(".json", withTextMimes("application/json", "text/json")),
        Map.entry(".py", withTextMimes("text/x-python", "application/x-python-code")),
        Map.entry(".sh", withTextMimes("application/x-sh", "text/x-shellscript")),
        Map.entry(".js", withTextMimes("application/javascript", "text/javascript")),
        Map.entry(".ts", withTextMimes("application/typescript", "text/typescript")),
        Map.entry(".java", withTextMimes("text/x-java-source"))
    );
    private static final Map<String, String> IMAGE_MIME_TYPES = Map.of(
        ".png", "image/png",
        ".jpg", "image/jpeg",
        ".jpeg", "image/jpeg"
    );
    private final int maxBytes;
    private final Set<String> allowedMimeTypes;

    public FileMaterialPolicy(
        @Value("${app.material-files.max-size-bytes:10485760}") int maxBytes,
        @Value("${app.material-files.allowed-mime-types:application/pdf,image/png,image/jpeg,text/plain,application/octet-stream,application/zip,application/x-zip-compressed,text/markdown,text/csv,application/json,text/json,text/x-python,application/x-python-code,application/x-sh,text/x-shellscript,application/javascript,text/javascript,application/typescript,text/typescript,text/x-java-source}")
        Set<String> allowedMimeTypes
    ) {
        if (maxBytes <= 0 || maxBytes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid upload limit");
        }
        this.maxBytes = maxBytes;
        this.allowedMimeTypes = Set.copyOf(allowedMimeTypes);
    }

    public byte[] readAndValidate(InputStream input, long declaredSize, String filename, String mimeType,
                                  LessonMaterialType type) {
        if (declaredSize > maxBytes) {
            throw new FileTooLargeException();
        }
        byte[] content;
        try {
            content = input.readNBytes(maxBytes + 1);
        } catch (IOException exception) {
            throw new FileStorageException(exception);
        }
        if (content.length > maxBytes) {
            throw new FileTooLargeException();
        }
        if (content.length == 0) {
            throw new InvalidLessonMaterialException("file", "must not be empty");
        }
        String extension = extension(filename);
        // Extension, declared MIME and content must all agree. Browser MIME is never trusted by itself.
        if (mimeType == null || !allowedMimeTypes.contains(mimeType)
            || !isAllowedCombination(extension, mimeType, type) || !matches(content, extension)) {
            throw new InvalidLessonMaterialException("file", "unsupported or mismatched MIME type");
        }
        return content;
    }

    private boolean isAllowedCombination(String extension, String mimeType, LessonMaterialType type) {
        if (type == LessonMaterialType.IMAGE) {
            return mimeType.equals(IMAGE_MIME_TYPES.get(extension));
        }
        Set<String> mimeTypes = FILE_MIME_TYPES.get(extension);
        return mimeTypes != null && mimeTypes.contains(mimeType);
    }

    private boolean matches(byte[] bytes, String extension) {
        return switch (extension) {
            case ".pdf" -> startsWith(bytes, new byte[]{37, 80, 68, 70, 45});
            case ".png" -> startsWith(bytes, new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});
            case ".jpg", ".jpeg" -> startsWith(bytes, new byte[]{(byte) 255, (byte) 216, (byte) 255});
            case ".zip" -> isZip(bytes);
            case ".txt", ".md", ".csv", ".json", ".py", ".sh", ".js", ".ts", ".java" -> isText(bytes);
            default -> false;
        };
    }

    private String extension(String filename) {
        if (filename == null) return "";
        int separator = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        int dot = filename.lastIndexOf('.');
        return dot > separator ? filename.substring(dot).toLowerCase(Locale.ROOT) : "";
    }

    private boolean isZip(byte[] bytes) {
        return startsWith(bytes, new byte[]{80, 75, 3, 4})
            || startsWith(bytes, new byte[]{80, 75, 5, 6})
            || startsWith(bytes, new byte[]{80, 75, 7, 8});
    }

    private static Set<String> withTextMimes(String... specificMimes) {
        var result = new java.util.HashSet<>(TEXT_MIME_TYPES);
        result.addAll(Set.of(specificMimes));
        return Set.copyOf(result);
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isText(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            return text.codePoints().noneMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t');
        } catch (CharacterCodingException exception) {
            return false;
        }
    }
}
