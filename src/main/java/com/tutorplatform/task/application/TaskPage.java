package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.task.TaskEntity;

import java.util.List;

public record TaskPage(
        List<TaskEntity> items,
        long totalElements,
        int totalPages
) {
}
