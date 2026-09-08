package com.tutorplatform.homework.application;

import java.util.List;

public record HomeworkPageResult(
        List<HomeworkSummaryResult> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
