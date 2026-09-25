package com.tutorplatform.content.application.importpackage;

public record ContentPackageValidationError(Code code, String path, String message) {
    public enum Code {
        UNSUPPORTED_SCHEMA_VERSION,
        INVALID_PACKAGE_KIND,
        REQUIRED_FIELD,
        INVALID_MATERIAL_TYPE,
        INVALID_EXTERNAL_URL,
        LIMIT_EXCEEDED,
        FORBIDDEN_FIELD
    }
}
