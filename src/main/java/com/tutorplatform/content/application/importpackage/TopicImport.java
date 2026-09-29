package com.tutorplatform.content.application.importpackage;

import java.util.List;

public record TopicImport(
        String title, String description, List<MaterialImport> materials, List<TaskImport> tasks) {
    public TopicImport {
        materials = materials == null ? List.of() : List.copyOf(materials);
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    public TopicImport(String title, String description, List<MaterialImport> materials) {
        this(title, description, materials, List.of());
    }
}
