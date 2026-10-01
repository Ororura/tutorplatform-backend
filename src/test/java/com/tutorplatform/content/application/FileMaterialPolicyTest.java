package com.tutorplatform.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tutorplatform.content.application.exception.InvalidLessonMaterialException;
import com.tutorplatform.content.domain.LessonMaterialType;
import java.io.ByteArrayInputStream;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FileMaterialPolicyTest {
    @Test
    void enforcesActualSizeEvenIfDeclaredSizeIsFalse() {
        var policy = new FileMaterialPolicy(4, Set.of("text/plain"));
        assertThatThrownBy(
                        () ->
                                policy.readAndValidate(
                                        new ByteArrayInputStream("hello".getBytes()),
                                        1,
                                        "notes.txt",
                                        "text/plain",
                                        LessonMaterialType.FILE))
                .isInstanceOf(FileTooLargeException.class);
        assertThat(
                        policy.readAndValidate(
                                new ByteArrayInputStream("four".getBytes()),
                                4,
                                "notes.txt",
                                "text/plain",
                                LessonMaterialType.FILE))
                .isEqualTo("four".getBytes());
    }

    @Test
    void configurableAllowlistCannotBypassVerification() {
        var policy =
                new FileMaterialPolicy(100, Set.of("application/pdf", "application/octet-stream"));
        assertThatThrownBy(
                        () ->
                                policy.readAndValidate(
                                        new ByteArrayInputStream("hello".getBytes()),
                                        5,
                                        "notes.txt",
                                        "text/plain",
                                        LessonMaterialType.FILE))
                .isInstanceOf(InvalidLessonMaterialException.class);
        assertThatThrownBy(
                        () ->
                                policy.readAndValidate(
                                        new ByteArrayInputStream("hello".getBytes()),
                                        5,
                                        "notes.pdf",
                                        "application/pdf",
                                        LessonMaterialType.FILE))
                .isInstanceOf(InvalidLessonMaterialException.class);
        assertThatThrownBy(
                        () ->
                                policy.readAndValidate(
                                        new ByteArrayInputStream(new byte[] {1}),
                                        1,
                                        "script.sh",
                                        "application/octet-stream",
                                        LessonMaterialType.FILE))
                .isInstanceOf(InvalidLessonMaterialException.class);
    }

    @Test
    void textMustBeNonemptyUtf8WithoutBinaryControlCharacters() {
        var policy = new FileMaterialPolicy(100, Set.of("text/plain"));
        for (byte[] invalid : new byte[][] {new byte[0], new byte[] {0}, new byte[] {(byte) 255}}) {
            assertThatThrownBy(
                            () ->
                                    policy.readAndValidate(
                                            new ByteArrayInputStream(invalid),
                                            invalid.length,
                                            "notes.txt",
                                            "text/plain",
                                            LessonMaterialType.FILE))
                    .isInstanceOf(InvalidLessonMaterialException.class);
        }
    }

    @Test
    void executableTextExtensionsStillRequireMatchingMimeAndTextContent() {
        var policy =
                new FileMaterialPolicy(
                        100,
                        Set.of(
                                "text/plain",
                                "application/octet-stream",
                                "image/png",
                                "video/mp2t"));
        assertThat(
                        policy.readAndValidate(
                                new ByteArrayInputStream("#!/bin/sh\necho ok\n".getBytes()),
                                18,
                                "lesson.sh",
                                "application/octet-stream",
                                LessonMaterialType.FILE))
                .isNotEmpty();
        assertThatThrownBy(
                        () ->
                                policy.readAndValidate(
                                        new ByteArrayInputStream("#!/bin/sh".getBytes()),
                                        9,
                                        "lesson.sh",
                                        "image/png",
                                        LessonMaterialType.FILE))
                .isInstanceOf(InvalidLessonMaterialException.class);
        assertThatThrownBy(
                        () ->
                                policy.readAndValidate(
                                        new ByteArrayInputStream("#!/bin/sh".getBytes()),
                                        9,
                                        "lesson.sh",
                                        "text/plain",
                                        LessonMaterialType.IMAGE))
                .isInstanceOf(InvalidLessonMaterialException.class);
        assertThat(
                        policy.readAndValidate(
                                new ByteArrayInputStream("export const lesson = 1;".getBytes()),
                                24,
                                "har-parser.ts",
                                "video/mp2t",
                                LessonMaterialType.FILE))
                .isNotEmpty();
        assertThat(
                        policy.readAndValidate(
                                new ByteArrayInputStream(
                                        "export const Lesson = () => <div />;".getBytes()),
                                37,
                                "lesson.tsx",
                                "application/octet-stream",
                                LessonMaterialType.FILE))
                .isNotEmpty();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {
                " ",
                ".",
                "..",
                "../../notes.txt",
                "..\\..\\notes.txt",
                "/tmp/notes.txt",
                "C:\\notes.txt",
                "folder/notes.txt",
                "notes\r\nX-Injected: yes.txt",
                "notes\u0000.txt"
            })
    void rejectsUnsafeNamesBeforeReading(String filename) {
        var policy = new FileMaterialPolicy(100, Set.of("text/plain"));
        var input = org.mockito.Mockito.mock(java.io.InputStream.class);
        assertThatThrownBy(
                        () ->
                                policy.readAndValidate(
                                        input, 5, filename, "text/plain", LessonMaterialType.FILE))
                .isInstanceOf(InvalidLessonMaterialException.class);
        org.mockito.Mockito.verifyNoInteractions(input);
    }

    @Test
    void declaredOversizeAndInvalidSizeAreRejectedBeforeReading() {
        var policy = new FileMaterialPolicy(4, Set.of("text/plain"));
        var input = org.mockito.Mockito.mock(java.io.InputStream.class);
        assertThatThrownBy(
                        () ->
                                policy.readAndValidate(
                                        input,
                                        5,
                                        "notes.txt",
                                        "text/plain",
                                        LessonMaterialType.FILE))
                .isInstanceOf(FileTooLargeException.class);
        assertThatThrownBy(
                        () ->
                                policy.readAndValidate(
                                        input,
                                        -1,
                                        "notes.txt",
                                        "text/plain",
                                        LessonMaterialType.FILE))
                .isInstanceOf(InvalidLessonMaterialException.class);
        org.mockito.Mockito.verifyNoInteractions(input);
    }

    @Test
    void limitsReadToOneByteBeyondConfiguredMaximum() throws Exception {
        var policy = new FileMaterialPolicy(4, Set.of("text/plain"));
        var input = new ByteArrayInputStream("0123456789".getBytes());
        assertThatThrownBy(
                        () ->
                                policy.readAndValidate(
                                        input,
                                        0,
                                        "notes.txt",
                                        "text/plain",
                                        LessonMaterialType.FILE))
                .isInstanceOf(FileTooLargeException.class);
        assertThat(input.available()).isEqualTo(5);
        assertThatThrownBy(
                        () ->
                                policy.readAndValidate(
                                        new ByteArrayInputStream(new byte[] {1}),
                                        1,
                                        "x".repeat(256) + ".txt",
                                        "text/plain",
                                        LessonMaterialType.FILE))
                .isInstanceOf(InvalidLessonMaterialException.class);
    }
}
