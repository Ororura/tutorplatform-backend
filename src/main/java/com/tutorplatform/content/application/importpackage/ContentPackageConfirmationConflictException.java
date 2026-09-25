package com.tutorplatform.content.application.importpackage;

public final class ContentPackageConfirmationConflictException extends RuntimeException {
    public ContentPackageConfirmationConflictException() {
        super("Confirmation was already used for a different package digest");
    }
}
