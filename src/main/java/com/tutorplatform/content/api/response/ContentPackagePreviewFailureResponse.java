package com.tutorplatform.content.api.response;

import com.tutorplatform.content.api.response.ContentPackagePreviewResponse.ContentPackagePreviewError;
import java.util.List;

public record ContentPackagePreviewFailureResponse(
        boolean valid, List<ContentPackagePreviewError> errors) {
    public ContentPackagePreviewFailureResponse(List<ContentPackagePreviewError> errors) {
        this(false, errors);
    }
}
