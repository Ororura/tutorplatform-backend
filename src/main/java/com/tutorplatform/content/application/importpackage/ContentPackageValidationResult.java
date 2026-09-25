package com.tutorplatform.content.application.importpackage;

import java.util.List;

public record ContentPackageValidationResult(List<ContentPackageValidationError> errors) {
    public ContentPackageValidationResult {
        errors = List.copyOf(errors);
    }

    public boolean valid() {
        return errors.isEmpty();
    }
}
