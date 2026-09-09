package com.tutorplatform.homework.application;

import java.util.List;

public record StudentHomeworkPageResult(
    List<StudentHomeworkSummaryResult> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
