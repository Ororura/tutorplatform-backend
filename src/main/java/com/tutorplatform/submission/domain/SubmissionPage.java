package com.tutorplatform.submission.domain;

import java.util.List;

public record SubmissionPage(
    List<SubmissionEntity> items,
    long totalElements,
    int totalPages
) {
    public SubmissionPage {
        items = List.copyOf(items);
    }
}
