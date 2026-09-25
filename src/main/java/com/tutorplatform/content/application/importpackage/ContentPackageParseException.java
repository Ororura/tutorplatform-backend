package com.tutorplatform.content.application.importpackage;

public final class ContentPackageParseException extends RuntimeException {
    public enum Code {
        INVALID_YAML,
        INVALID_UTF8,
        FILE_TOO_LARGE,
        DUPLICATE_KEY,
        UNSUPPORTED_YAML_FEATURE,
        INVALID_FIELD_TYPE,
        UNKNOWN_FIELD,
        INVALID_FILE_EXTENSION,
        FILE_READ_ERROR
    }

    private final Code code;
    private final String location;

    public ContentPackageParseException(Code code, String location, String message) {
        super(message);
        this.code = code;
        this.location = location;
    }

    public Code code() {
        return code;
    }

    public String location() {
        return location;
    }
}
