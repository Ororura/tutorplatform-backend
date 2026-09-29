package com.tutorplatform.content.application.importpackage;

public final class ContentPackageImportSchemaUnsupportedException extends RuntimeException {
    public ContentPackageImportSchemaUnsupportedException() {
        super("Content package schema version is not supported for import");
    }
}
