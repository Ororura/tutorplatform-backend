package com.tutorplatform.content.application.importpackage;

import java.util.List;

public record TutorContentPackage(Integer schemaVersion, String kind, List<ModuleImport> modules) {
    public TutorContentPackage {
        modules = modules == null ? null : List.copyOf(modules);
    }
}
