package com.tutorplatform.submission.application;

import java.util.List;

public record SubmissionPageResult(
        List<SubmissionResult> items, int page, int size, long totalElements, int totalPages) {}
