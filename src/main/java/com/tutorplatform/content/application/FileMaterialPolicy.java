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
import java.util.Set;

@Component
public class FileMaterialPolicy {
    private final int maxBytes;
    private final Set<String> allowedMimeTypes;

    public FileMaterialPolicy(
        @Value("${app.material-files.max-size-bytes:10485760}") int maxBytes,
        @Value("${app.material-files.allowed-mime-types:application/pdf,image/png,image/jpeg,text/plain}")
        Set<String> allowedMimeTypes
    ) {
        if (maxBytes <= 0 || maxBytes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid upload limit");
        }
        this.maxBytes = maxBytes;
        this.allowedMimeTypes = Set.copyOf(allowedMimeTypes);
    }

    public byte[] readAndValidate(InputStream input, long declaredSize, String mimeType, LessonMaterialType type) {
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
        // Small explicit allowlist with signature/UTF-8 checks, not an antivirus or full format parser.
        if (mimeType == null || !allowedMimeTypes.contains(mimeType) || !matches(content, mimeType)
                || (type == LessonMaterialType.IMAGE && !mimeType.startsWith("image/"))) {
            throw new InvalidLessonMaterialException("file", "unsupported or mismatched MIME type");
        }
        return content;
    }

    private boolean matches(byte[] bytes, String mime) {
        return switch (mime) {
            case "application/pdf" -> startsWith(bytes, new byte[]{37, 80, 68, 70, 45});
            case "image/png" -> startsWith(bytes, new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10});
            case "image/jpeg" -> startsWith(bytes, new byte[]{(byte)255, (byte)216, (byte)255});
            case "text/plain" -> isText(bytes);
            default -> false; // Enabling another format also requires a minimal verifier.
        };
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
