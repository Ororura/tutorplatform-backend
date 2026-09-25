package com.tutorplatform.content.application.importpackage;

public final class ContentPackageValidationException extends RuntimeException {
    private final ContentPackageValidationResult result;

    public ContentPackageValidationException(ContentPackageValidationResult result) {
        super("Content package validation failed");
        this.result = result;
    }

    public ContentPackageValidationResult result() {
        return result;
    }
}
