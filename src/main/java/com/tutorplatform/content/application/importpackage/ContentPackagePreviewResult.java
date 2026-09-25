package com.tutorplatform.content.application.importpackage;

import com.tutorplatform.content.domain.LessonMaterialType;
import java.util.List;
import java.util.UUID;

public record ContentPackagePreviewResult(
        UUID programId,
        String sha256Digest,
        int moduleCount,
        int topicCount,
        int materialCount,
        List<Module> modules) {
    public ContentPackagePreviewResult {
        modules = List.copyOf(modules);
    }

    public record Module(String title, String description, List<Topic> topics) {
        public Module {
            topics = List.copyOf(topics);
        }
    }

    public record Topic(String title, String description, List<Material> materials) {
        public Topic {
            materials = List.copyOf(materials);
        }
    }

    public record Material(
            String title, LessonMaterialType materialType, String content, String externalUrl) {}
}
