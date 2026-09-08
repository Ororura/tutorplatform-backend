package com.tutorplatform.task.application;

import java.util.List;

public record TaskPageResult(
        List<TaskResult> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
