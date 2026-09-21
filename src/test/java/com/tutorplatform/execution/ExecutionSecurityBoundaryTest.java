package com.tutorplatform.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ExecutionSecurityBoundaryTest {

    @Test
    void backendDoesNotContainLocalUserCodeExecutionApis() throws IOException {
        try (var sources = Files.walk(Path.of("src/main/java"))) {
            var javaSources =
                    sources.filter(path -> path.toString().endsWith(".java"))
                            .map(ExecutionSecurityBoundaryTest::read)
                            .toList();

            assertThat(javaSources).noneMatch(source -> source.contains("new ProcessBuilder("));
            assertThat(javaSources)
                    .noneMatch(source -> source.contains("Runtime.getRuntime().exec("));
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
