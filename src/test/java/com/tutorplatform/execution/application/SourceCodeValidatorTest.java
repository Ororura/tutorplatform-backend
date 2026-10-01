package com.tutorplatform.execution.application;

import static org.assertj.core.api.Assertions.*;

import com.tutorplatform.execution.infrastructure.protection.ExecutionAbuseProtectionProperties;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SourceCodeValidatorTest {
    private final SourceCodeValidator validator =
            new SourceCodeValidator(
                    new ExecutionAbuseProtectionProperties(
                            10, Duration.ofMinutes(1), 10000, Duration.ofSeconds(30), 16));

    @ParameterizedTest
    @ValueSource(strings = {"a", "я", "😀"})
    void utf8BoundaryAcceptsExactBytesAndRejectsNextByte(String character) {
        int bytes = character.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        String boundary = character.repeat(16 / bytes);
        assertThatCode(() -> validator.validate(boundary)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate(boundary + "a"))
                .isInstanceOf(SourceCodeTooLargeException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {17, 1000000})
    void oversizedAsciiIsRejected(int size) {
        assertThatThrownBy(() -> validator.validate("a".repeat(size)))
                .isInstanceOf(SourceCodeTooLargeException.class);
    }
}
