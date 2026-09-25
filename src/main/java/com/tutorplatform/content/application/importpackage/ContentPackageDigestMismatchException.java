package com.tutorplatform.content.application.importpackage;

public final class ContentPackageDigestMismatchException extends RuntimeException {
    public ContentPackageDigestMismatchException() {
        super("YAML bytes do not match the expected digest");
    }
}
