package com.tutorplatform.execution.infrastructure.http;

import java.nio.charset.StandardCharsets;

final class BoundedOutput {
    private BoundedOutput() {
    }

    static String truncateUtf8(String value, int maxBytes) {
        if (value == null || value.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return value;
        }

        var result = new StringBuilder();
        var usedBytes = 0;
        for (var offset = 0; offset < value.length();) {
            var codePoint = value.codePointAt(offset);
            var character = new String(Character.toChars(codePoint));
            var characterBytes = character.getBytes(StandardCharsets.UTF_8).length;
            if (usedBytes + characterBytes > maxBytes) {
                break;
            }
            result.append(character);
            usedBytes += characterBytes;
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }
}
