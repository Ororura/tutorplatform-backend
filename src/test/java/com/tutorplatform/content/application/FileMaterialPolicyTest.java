package com.tutorplatform.content.application;

import com.tutorplatform.content.application.exception.InvalidLessonMaterialException;
import com.tutorplatform.content.domain.LessonMaterialType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileMaterialPolicyTest {
    @Test
    void enforcesActualSizeEvenIfDeclaredSizeIsFalse() {
        var policy = new FileMaterialPolicy(4, Set.of("text/plain"));
        assertThatThrownBy(() -> policy.readAndValidate(new ByteArrayInputStream("hello".getBytes()),
            1, "notes.txt", "text/plain", LessonMaterialType.FILE)).isInstanceOf(FileTooLargeException.class);
        assertThat(policy.readAndValidate(new ByteArrayInputStream("four".getBytes()),
            4, "notes.txt", "text/plain", LessonMaterialType.FILE)).isEqualTo("four".getBytes());
    }

    @Test
    void configurableAllowlistCannotBypassVerification() {
        var policy = new FileMaterialPolicy(100, Set.of("application/pdf", "application/octet-stream"));
        assertThatThrownBy(() -> policy.readAndValidate(new ByteArrayInputStream("hello".getBytes()),
            5, "notes.txt", "text/plain", LessonMaterialType.FILE)).isInstanceOf(InvalidLessonMaterialException.class);
        assertThatThrownBy(() -> policy.readAndValidate(new ByteArrayInputStream("hello".getBytes()),
            5, "notes.pdf", "application/pdf", LessonMaterialType.FILE)).isInstanceOf(InvalidLessonMaterialException.class);
        assertThatThrownBy(() -> policy.readAndValidate(new ByteArrayInputStream(new byte[]{1}),
            1, "script.sh", "application/octet-stream", LessonMaterialType.FILE))
            .isInstanceOf(InvalidLessonMaterialException.class);
    }

    @Test
    void textMustBeNonemptyUtf8WithoutBinaryControlCharacters() {
        var policy = new FileMaterialPolicy(100, Set.of("text/plain"));
        for (byte[] invalid : new byte[][]{new byte[0], new byte[]{0}, new byte[]{(byte) 255}}) {
            assertThatThrownBy(() -> policy.readAndValidate(new ByteArrayInputStream(invalid),
                invalid.length, "notes.txt", "text/plain", LessonMaterialType.FILE))
                .isInstanceOf(InvalidLessonMaterialException.class);
        }
    }

    @Test
    void executableTextExtensionsStillRequireMatchingMimeAndTextContent() {
        var policy = new FileMaterialPolicy(100, Set.of("text/plain", "application/octet-stream", "image/png"));
        assertThat(policy.readAndValidate(new ByteArrayInputStream("#!/bin/sh\necho ok\n".getBytes()),
            18, "lesson.sh", "application/octet-stream", LessonMaterialType.FILE)).isNotEmpty();
        assertThatThrownBy(() -> policy.readAndValidate(new ByteArrayInputStream("#!/bin/sh".getBytes()),
            9, "lesson.sh", "image/png", LessonMaterialType.FILE))
            .isInstanceOf(InvalidLessonMaterialException.class);
        assertThatThrownBy(() -> policy.readAndValidate(new ByteArrayInputStream("#!/bin/sh".getBytes()),
            9, "lesson.sh", "text/plain", LessonMaterialType.IMAGE))
            .isInstanceOf(InvalidLessonMaterialException.class);
    }
}
