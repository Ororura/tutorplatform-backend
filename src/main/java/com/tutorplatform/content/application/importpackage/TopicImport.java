package com.tutorplatform.content.application.importpackage;

import java.util.List;

public record TopicImport(String title, String description, List<MaterialImport> materials) {
    public TopicImport {
        materials = materials == null ? List.of() : List.copyOf(materials);
    }
}
