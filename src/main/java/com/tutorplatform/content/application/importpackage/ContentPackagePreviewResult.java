package com.tutorplatform.content.application.importpackage;

import com.tutorplatform.content.domain.LessonMaterialType;
import java.util.List;
import java.util.UUID;

public record ContentPackagePreviewResult(
        UUID programId,
        String sha256Digest,
        int schemaVersion,
        int moduleCount,
        int topicCount,
        int materialCount,
        int taskCount,
        List<Module> modules) {
    public ContentPackagePreviewResult {
        modules = List.copyOf(modules);
    }

    public record Module(String title, String description, List<Topic> topics) {
        public Module {
            topics = List.copyOf(topics);
        }
    }

    public record Topic(
            String title, String description, List<Material> materials, List<Task> tasks) {
        public Topic {
            materials = List.copyOf(materials);
            tasks = List.copyOf(tasks);
        }
    }

    public record Material(
            String title, LessonMaterialType materialType, String content, String externalUrl) {}

    public record Task(
            String title,
            String descriptionMarkdown,
            String taskType,
            String difficulty,
            boolean required,
            ProgrammingConfig programmingConfig,
            int testCaseCount,
            int hiddenTestCaseCount) {}

    public record ProgrammingConfig(
            String language,
            String starterCode,
            boolean executionEnabled,
            int timeLimitMs,
            int memoryLimitMb) {}
}
