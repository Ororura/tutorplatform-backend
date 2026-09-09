package com.tutorplatform.homework.application;

import com.tutorplatform.homework.domain.HomeworkStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HomeworkResult(
    UUID id,
    UUID studentProgramId,
    String title,
    String description,
    HomeworkStatus status,
    Instant assignedAt,
    Instant dueAt,
    boolean overdue,
    Instant completedAt,
    List<HomeworkItemResult> items,
    long version,
    Instant createdAt,
    Instant updatedAt
) {
}
