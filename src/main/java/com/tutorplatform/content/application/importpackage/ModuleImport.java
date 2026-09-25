package com.tutorplatform.content.application.importpackage;

import java.util.List;

public record ModuleImport(String title, String description, List<TopicImport> topics) {
    public ModuleImport {
        topics = topics == null ? null : List.copyOf(topics);
    }
}
