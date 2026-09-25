package com.tutorplatform.content.application.importpackage;

public interface ContentPackageParser {
    TutorContentPackage parse(byte[] yamlBytes);
}
